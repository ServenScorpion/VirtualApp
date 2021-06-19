package com.xdja.zs;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;

@RequiresApi(api = Build.VERSION_CODES.O)
@SuppressLint("OverrideAbstract")
public class NotificationListener extends NotificationListenerService {
    String Tag = "zs_NotificationListener";

    public NotificationListener() {
    }

    private Context mApp = null;
    private PackageManager packageManager = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = getApplicationContext();
        packageManager = mApp.getPackageManager();
    }

    private boolean isInWhiteList(String packageName, boolean currentSpace) {
        return VirtualCore.getConfig().isCanShowNotification(packageName, currentSpace);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(Tag, "onStartCommand");
        if(intent == null) {
            return super.onStartCommand(intent, flags, startId);
        } else {
            String type = intent.getType();

            if(type.equals("cancelAll")) {
                try {
                    cancelAllNotifications();

                    Log.e(Tag, "cancelAll");
                    StatusBarNotification all[] = this.getActiveNotifications();
                    if (all != null) {
                        for (StatusBarNotification item : all) {
                            Notification notification = item.getNotification();
                            Bundle extras = notification.extras;
                            String notificationTitle = extras.getString(Notification.EXTRA_TITLE);

                            if (!item.getPackageName().equals(mApp.getPackageName())) {
                                Log.e(Tag, String.format("int NotificationListener, snooze [Title: %s]", notificationTitle));
                                cancelNotification(item.getKey());
                                snoozeNotification(item.getKey(),10000);
                            }
                        }
                    }
                }catch (Exception e) {
                    //不做处理
                }
            }


            return super.onStartCommand(intent, flags, startId);
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        Log.e(Tag, "onListenerConnected");
    }

    private boolean isSystemApp(String pkgName) {
        boolean ret = false;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isThirdApp: ");
            stringBuilder.append(pkgName);
            Log.d(Tag, stringBuilder.toString());
            if ((packageManager.getPackageInfo(pkgName, 0).applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                ret = true;
            }
            Log.d(Tag,"ret:" + ret);
            return ret;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        Notification notification = sbn.getNotification();
        String packageName = sbn.getPackageName();
        Bundle extras = notification.extras;
        String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
//        int notificationIcon = extras.getInt(Notification.EXTRA_SMALL_ICON);
//        Bitmap notificationLargeIcon = ((Bitmap)extras.getParcelable(Notification.EXTRA_LARGE_ICON));
        CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

        Log.e(Tag, String.format("onNotificationPosted [Title: %s] [Text: %s] [SubText: %s] [pkgName %s]",
                notificationTitle, notificationText, notificationSubText, packageName));

        if(mApp.getPackageName().equals(packageName)) {
            Log.e(Tag, String.format("\tthis notify cames from inside, ignore"));

            return;
        }
        boolean currentSpace = currentSpace();
        Log.e(Tag, "currentSpace "+currentSpace);

        if (isInWhiteList(packageName, currentSpace)) {

            Log.d(Tag, "[packageName]" + " in white list");
            return;
        }

        if(isSystemApp(packageName) && currentSpace) {
            Log.e(Tag, "\tdelete this notify " + sbn.getKey());
            cancelNotification(sbn.getKey());
            snoozeNotification(sbn.getKey(),10000);
            return;
        }

        if(currentSpace()) {
            Log.e(Tag, "\tsnooze this notify " + sbn.getKey());
            cancelNotification(sbn.getKey());
            snoozeNotification(sbn.getKey(),10000);
        }
    }

    boolean currentSpace() {
       return BoxProvider.isCurrentSpace();
    }

    public static void clearAllNotifications() {
        Log.e("zs_NotificationListener", "clearAllNotifications");

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(VirtualCore.get().getHostPkg(), NotificationListener.class.getName()));
        intent.setType("cancelAll");

        VirtualCore.get().getContext().startService(intent);
    }
}