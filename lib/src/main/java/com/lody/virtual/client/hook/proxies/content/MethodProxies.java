package com.lody.virtual.client.hook.proxies.content;


import android.content.ContentResolver;
import android.content.pm.ProviderInfo;
import android.database.IContentObserver;
import android.net.Uri;
import android.os.Build;

import com.lody.virtual.client.ipc.VContentManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.Keep;
import com.lody.virtual.os.VUserHandle;

import java.lang.reflect.Method;

/**
 * @author Lody
 */
@Keep
public class MethodProxies {

    private static boolean isAppUri(Uri uri) {
        ProviderInfo info = VPackageManager.get().resolveContentProvider(uri.getAuthority(), 0, VUserHandle.myUserId());
        return info != null && info.enabled;
    }

    public static Object registerContentObserver(Object who, Method method, Object[] args) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (args.length >= 5) {
                args[4] = Build.VERSION_CODES.LOLLIPOP_MR1;
            }
        }
        Uri uri = (Uri) args[0];
        boolean notifyForDescendents = (boolean) args[1];
        IContentObserver observer = (IContentObserver) args[2];
        if (isAppUri(uri)) {
//            VContentService.get().registerContentObserver(uri, notifyForDescendents, observer);
            VContentManager.get().registerContentObserver(uri, notifyForDescendents, observer, VUserHandle.myUserId());
            return 0;
        } else {
            return method.invoke(who, args);
        }
    }

    public static Object unregisterContentObserver(Object who, Method method, Object[] args) throws Throwable {
        IContentObserver observer = (IContentObserver) args[0];
//        VContentService.get().unregisterContentObserver(observer);

        VContentManager.get().unregisterContentObserver(observer);
        return method.invoke(who, args);
    }

    public static Object notifyChange(Object who, Method method, Object[] args) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (args.length >= 6) {
                args[5] = Build.VERSION_CODES.LOLLIPOP_MR1;
            }
        }
        Uri uri = (Uri) args[0];
        if (isAppUri(uri)) {
            IContentObserver observer = (IContentObserver) args[1];
            boolean observerWantsSelfNotifications = (boolean) args[2];
            boolean syncToNetwork;
            if (args[3] instanceof Integer) {
                int flags = (int) args[3];
                syncToNetwork = (flags & ContentResolver.NOTIFY_SYNC_TO_NETWORK) != 0;
            } else {
                syncToNetwork = (boolean) args[3];
            }
            VContentManager.get().notifyChange(uri, observer, observerWantsSelfNotifications, syncToNetwork, VUserHandle.myUserId());
            return 0;
        } else {
            return method.invoke(who, args);
        }
    }
}
