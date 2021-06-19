package com.lody.virtual.server.notification;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VNotificationManager;

/**
 * @author 247321543
 */
@SuppressWarnings("deprecation")
class NotificationCompatCompatV14 extends NotificationCompat {
    private final RemoteViewsFixer mRemoteViewsFixer;

    NotificationCompatCompatV14() {
        super();
        mRemoteViewsFixer = new RemoteViewsFixer(this);
    }

    private RemoteViewsFixer getRemoteViewsFixer() {
        return mRemoteViewsFixer;
    }

    @Override
    public VNotificationManager.Result dealNotification(int id, Notification notification, final String packageName, int userId) {
        Context appContext = getAppContext(packageName);
        if (appContext == null) {
            return VNotificationManager.Result.NONE;
        }
        if (VClient.get().isAppUseOutsideAPK() && VirtualCore.get().isOutsideInstalled(packageName)) {
            if(notification.icon != 0) {
                getNotificationFixer().fixIconImage(appContext.getResources(), notification.contentView, false, notification);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getNotificationFixer().fixIconImage(appContext.getResources(), notification.bigContentView, false, notification);
                }
                notification.icon = getHostContext().getApplicationInfo().icon;
            }
            return VNotificationManager.Result.USE_OLD;
        }
        remakeRemoteViews(id, notification, appContext, notification);
        if (notification.icon != 0) {
            notification.icon = getHostContext().getApplicationInfo().icon;
        }
        return VNotificationManager.Result.USE_OLD;
    }

    protected void remakeRemoteViews(int id, Notification notification, Context appContext, Notification result) {
        if (notification.tickerView != null) {

            if (isSystemLayout(notification.tickerView)) {
                //系统布局
                getNotificationFixer().fixRemoteViewActions(appContext, false, notification.tickerView);
                if(result != notification) {
                    result.tickerView = notification.tickerView;
                }
            } else {
                //把通知栏的内容提前绘制好，再展示
                result.tickerView = getRemoteViewsFixer().makeRemoteViews(id + ":tickerView", appContext,
                        notification.tickerView, false, false);
            }
        }
        if (notification.contentView != null) {
            if (isSystemLayout(notification.contentView)) {
                boolean hasIconBitmap = getNotificationFixer().fixRemoteViewActions(appContext, false, notification.contentView);
                getNotificationFixer().fixIconImage(appContext.getResources(), notification.contentView, hasIconBitmap, notification);
                if(result != notification) {
                    result.contentView = notification.contentView;
                }
            } else {
                result.contentView = getRemoteViewsFixer().makeRemoteViews(id + ":contentView", appContext,
                        notification.contentView, false, true);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (notification.bigContentView != null) {
                if (isSystemLayout(notification.bigContentView)) {
                    getNotificationFixer().fixRemoteViewActions(appContext, false, notification.bigContentView);
                    if(result != notification) {
                        result.bigContentView = notification.bigContentView;
                    }
                } else {
                    result.bigContentView = getRemoteViewsFixer().makeRemoteViews(id + ":bigContentView", appContext,
                            notification.bigContentView, true, true);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (notification.headsUpContentView != null) {
                if (isSystemLayout(notification.headsUpContentView)) {
                    boolean hasIconBitmap = getNotificationFixer().fixRemoteViewActions(appContext, false, notification.headsUpContentView);
                    getNotificationFixer().fixIconImage(appContext.getResources(), notification.contentView, hasIconBitmap, notification);
                    if(result != notification) {
                        result.headsUpContentView = notification.headsUpContentView;
                    }
                } else {
                    result.headsUpContentView = getRemoteViewsFixer().makeRemoteViews(id + ":headsUpContentView", appContext,
                            notification.headsUpContentView, false, false);
                }
            }
        }
    }

    Context getAppContext(final String packageName) {
        Context context = null;
        try {
            context = getHostContext().createPackageContext(packageName,
                    Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
        } catch (PackageManager.NameNotFoundException e) {
           e.printStackTrace();
        }
        return context;
    }

}
