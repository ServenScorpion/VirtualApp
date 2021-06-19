package com.lody.virtual.client.hook.proxies.storage_stats;

import android.annotation.TargetApi;
import android.app.usage.StorageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelableException;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.utils.ArrayUtils;

import java.lang.reflect.Method;

import mirror.android.app.usage.IStorageStatsManager;

/**
 * @author Lody
 */
@TargetApi(Build.VERSION_CODES.O)
public class StorageStatsStub extends BinderInvocationProxy {

    public StorageStatsStub() {
        super(IStorageStatsManager.Stub.TYPE, Context.STORAGE_STATS_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceLastPkgMethodProxy("getTotalBytes"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getCacheBytes"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getCacheQuotaBytes"));
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


}
