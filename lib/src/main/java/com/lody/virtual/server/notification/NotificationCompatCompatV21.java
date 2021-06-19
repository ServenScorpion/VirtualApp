package com.lody.virtual.server.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VNotificationManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.NotificationChannelCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.os.VEnvironment;

import mirror.android.app.NotificationO;

/**
 * @author 247321543
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
/* package */ class NotificationCompatCompatV21 extends NotificationCompatCompatV14 {

    private static final String TAG = NotificationCompatCompatV21.class.getSimpleName();

    NotificationCompatCompatV21() {
        super();
    }

    @Override
    public VNotificationManager.Result dealNotification(int id, Notification notification, String packageName, int userId) {
        if (notification == null) {
            return VNotificationManager.Result.NONE;
        }
        if(!BuildCompat.isOreo()){
            notification.sound = ComponentUtils.wrapperNotificationSoundUri(notification.sound, userId);
        }
        Context appContext = getAppContext(packageName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (VirtualCore.get().getTargetSdkVersion() >= android.os.Build.VERSION_CODES.O) {
                if (TextUtils.isEmpty(notification.getChannelId())) {
                    if (NotificationO.mChannelId != null) {
                        //安通＋, 安全邮件, 短信, 使用声音和呼吸灯
                        if (packageName.equals("com.xdja.actoma")
                                || packageName.equals("com.xdja.HDSafeEMailClient")
                                || packageName.equals(InstallerSetting.MESSAGING_PKG/*"com.xdja.mms"*/)) {
                            //notification_download_layout
                            boolean replaced = false;
                            if (packageName.equals("com.xdja.actoma") && notification.contentView != null) {
                                String name = null;
                                try {
                                    name = appContext.getResources().getResourceEntryName(notification.contentView.getLayoutId());
                                } catch (Throwable e) {
                                    //ignore
                                }
                                //安通+的下载通知不响铃
                                if ("notification_download_layout".equals(name)) {
                                    replaced = true;
                                    NotificationO.mChannelId.set(notification, NotificationChannelCompat.DEFAULT_ID);
                                }
                            }
                            if(!replaced) {
                                NotificationO.mChannelId.set(notification, NotificationChannelCompat.LIGHT_ID);
                            }
                        } else if (packageName.equals(InstallerSetting.CLOCK_PKG)) {
                            NotificationO.mChannelId.set(notification, NotificationChannelCompat.SYSTEM_ID);
                        } else {
                            NotificationO.mChannelId.set(notification, NotificationChannelCompat.DEFAULT_ID);
                        }
                    }
                } else {
                    String channel = VNotificationManager.get().dealNotificationChannel(notification.getChannelId(), packageName, userId);
                    if (NotificationO.mChannelId != null) {
                        NotificationO.mChannelId.set(notification, channel);
                    }
                }
                if (notification.getGroup() != null) {
                    String group = VNotificationManager.get().dealNotificationGroup(notification.getGroup(), packageName, userId);
                    if (NotificationO.mGroupKey != null) {
                        NotificationO.mGroupKey.set(notification, group);
                    }
                }
            }
        }
        ApplicationInfo host = getHostContext().getApplicationInfo();
        PackageInfo outside = VirtualCore.getConfig().useOutsideResourcesBySameApk(packageName)?getOutSidePackageInfo(packageName):null;
        PackageInfo inside = VPackageManager.get().getPackageInfo(packageName,
                PackageManager.GET_SHARED_LIBRARY_FILES, 0);

        //check outside and inside's version
        boolean isInstalled = outside != null && outside.versionCode == inside.versionCode;

        //修复Icon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getNotificationFixer().fixIcon(notification.getSmallIcon(), appContext, isInstalled);
            getNotificationFixer().fixIcon(notification.getLargeIcon(), appContext, isInstalled);
        } else {
            getNotificationFixer().fixIconImage(appContext.getResources(), notification.contentView, false, notification);
        }
        notification.icon = host.icon;

        //5.0-6.0，通过修改RemoteViews的mApplication的apk路径，就解决通知栏资源的问题
        //Fix RemoteViews
        getNotificationFixer().fixNotificationRemoteViews(appContext, notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInstalled) {
            //7.0之后，SystemUI是通过createApplicationContext获取资源，只能采用4.4之前的静态绘制方法
            VNotificationManager.Result result = new VNotificationManager.Result(VNotificationManager.MODE_REPLACED);
            //克隆对象
            result.notification = notification.clone();
            if(result.notification == null){
                return VNotificationManager.Result.NONE;
            }
            remakeRemoteViews(id, notification, appContext, result.notification);
            if(notification.publicVersion != null){
                result.notification.publicVersion = notification.publicVersion.clone();
                remakeRemoteViews(id, notification.publicVersion, appContext, result.notification.publicVersion);
            }
            if (result.notification.icon != 0) {
                result.notification.icon = getHostContext().getApplicationInfo().icon;
            }
            return result;
        }
        //fix apk path
        ApplicationInfo proxyApplicationInfo;
        if (isInstalled) {
            proxyApplicationInfo = outside.applicationInfo;
        } else {
            proxyApplicationInfo = inside.applicationInfo;
            //base.apk
            proxyApplicationInfo.publicSourceDir = VEnvironment.getPublicResourcePath(packageName).getPath();
        }
        //5.0-6.0的通知栏适配，替换mApplication的apk路径即可
        resolveRemoteViews(notification, proxyApplicationInfo);
        resolveRemoteViews(notification.publicVersion, proxyApplicationInfo);
        return VNotificationManager.Result.USE_OLD;
    }

    private PackageInfo getOutSidePackageInfo(String packageName){
        try {
            return  VirtualCore.get().getUnHookPackageManager().getPackageInfo(packageName, PackageManager.GET_SHARED_LIBRARY_FILES);
        } catch (Throwable e) {
            return null;
        }
    }

    private void resolveRemoteViews(Notification notification, ApplicationInfo proxyApplicationInfo) {
        proxyApplicationInfo.targetSdkVersion = 22;
        if(notification != null) {
            fixApplicationInfo(notification.tickerView, proxyApplicationInfo);
            fixApplicationInfo(notification.contentView, proxyApplicationInfo);
            fixApplicationInfo(notification.bigContentView, proxyApplicationInfo);
            fixApplicationInfo(notification.headsUpContentView, proxyApplicationInfo);
            Bundle bundle = Reflect.on(notification).get("extras");
            if (bundle != null) {
                bundle.putParcelable(EXTRA_BUILDER_APPLICATION_INFO, proxyApplicationInfo);
            }
        }
    }

    private ApplicationInfo getApplicationInfo(Notification notification) {
        ApplicationInfo ai = getApplicationInfo(notification.tickerView);
        if (ai != null) {
            return ai;
        }
        ai = getApplicationInfo(notification.contentView);
        if (ai != null) {
            return ai;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ai = getApplicationInfo(notification.bigContentView);
            if (ai != null) {
                return ai;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ai = getApplicationInfo(notification.headsUpContentView);
            if (ai != null) {
                return ai;
            }
        }
        return null;
    }

    private ApplicationInfo getApplicationInfo(RemoteViews remoteViews) {
        if (remoteViews != null) {
            return mirror.android.widget.RemoteViews.mApplication.get(remoteViews);
        }
        return null;
    }

    private void fixApplicationInfo(RemoteViews remoteViews, ApplicationInfo ai) {
        if (remoteViews != null) {
            mirror.android.widget.RemoteViews.mApplication.set(remoteViews, ai);
        }
    }
}
