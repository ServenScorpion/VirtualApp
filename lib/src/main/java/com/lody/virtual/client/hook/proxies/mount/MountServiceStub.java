package com.lody.virtual.client.hook.proxies.mount;

import android.app.usage.StorageStats;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IInterface;
import android.os.ParcelableException;

import com.lody.virtual.client.hook.annotations.Inject;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.ArrayUtils;

import java.lang.reflect.Method;

import mirror.RefStaticMethod;
import mirror.android.os.mount.IMountService;
import mirror.android.os.storage.IStorageManager;

/**
 * @author Lody
 */
@Inject(MethodProxies.class)
public class MountServiceStub extends BinderInvocationProxy {

    public MountServiceStub() {
        super(getInterfaceMethod(), "mount");
    }


    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceLastPkgMethodProxy("getTotalBytes"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getCacheBytes"));
        addMethodProxy(new StaticMethodProxy("getCacheQuotaBytes") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                if (args[args.length - 1] instanceof Integer) {
                    args[args.length - 1] = getRealUid();
                }
                return method.invoke(who, args);
            }
        });
        addMethodProxy(new ReplaceLastPkgMethodProxy("queryStatsForUser"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("queryExternalStatsForUser"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("queryStatsForUid"));
        addMethodProxy(new StaticMethodProxy("queryStatsForPackage") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                int packageNameIndex = ArrayUtils.indexOfFirst(args, String.class);
                int userIdIndex = ArrayUtils.indexOfLast(args, Integer.class);
                if (packageNameIndex != -1 && userIdIndex != -1) {
                    String packageName = (String) args[packageNameIndex];
                    int userId = (int) args[userIdIndex];
                    return queryStatsForPackage(packageName, userId);
                }
                return super.call(who, method, args);
            }
        });
    }

    private StorageStats queryStatsForPackage(String packageName, int userId) {
        ApplicationInfo appInfo = VPackageManager.get().getApplicationInfo(packageName, 0, userId);
        if (appInfo == null) {
            throw new ParcelableException(new PackageManager.NameNotFoundException(packageName));
        }
        StorageStats stats = mirror.android.app.usage.StorageStats.ctor.newInstance();
        mirror.android.app.usage.StorageStats.cacheBytes.set(stats, 0);
        mirror.android.app.usage.StorageStats.codeBytes.set(stats, 0);
        mirror.android.app.usage.StorageStats.dataBytes.set(stats, 0);
        return stats;
    }


    private static RefStaticMethod<IInterface> getInterfaceMethod() {
        if (BuildCompat.isOreo()) {
            return IStorageManager.Stub.asInterface;
        } else {
            return IMountService.Stub.asInterface;
        }
    }
}
