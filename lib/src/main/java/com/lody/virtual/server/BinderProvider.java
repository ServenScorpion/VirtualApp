package com.lody.virtual.server;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.client.stub.KeepAliveService;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.compat.NotificationChannelCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.accounts.VAccountManagerService;
import com.lody.virtual.server.am.BroadcastSystem;
import com.lody.virtual.server.am.VActivityManagerService;
import com.lody.virtual.server.content.VContentService;
import com.lody.virtual.server.device.VDeviceManagerService;
import com.lody.virtual.server.interfaces.IServiceFetcher;
import com.lody.virtual.server.job.VJobSchedulerService;
import com.lody.virtual.server.location.VirtualLocationService;
import com.lody.virtual.server.notification.VNotificationManagerService;
import com.lody.virtual.server.pm.VAppManagerService;
import com.lody.virtual.server.pm.VPackageManagerService;
import com.lody.virtual.server.pm.VUserManagerService;
import com.lody.virtual.server.vs.VirtualStorageService;

import com.xdja.activitycounter.ActivityCounterService;
import com.xdja.zs.InstallerSettingService;
import com.xdja.zs.VSafekeyManagerService;
import com.xdja.zs.VServiceKeepAliveService;
import com.xdja.zs.VWaterMarkService;
import com.xdja.zs.controllerService;

import com.xdja.zs.VAppPermissionManagerService;

import java.util.List;

/**
 * @author Lody
 */
public final class BinderProvider extends ContentProvider {
    private static final String TAG = "BinderProvider";

    private final ServiceFetcher mServiceFetcher = new ServiceFetcher();
    private static boolean sInitialized = false;

    @Override
    public boolean onCreate() {
        return init();
    }

    public static boolean isInitialized() {
        return sInitialized;
    }

    private boolean init() {
        if (sInitialized) {
            return false;
        }
        Context context = getContext();
        if (context != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannelCompat.checkOrCreateGroup(context, NotificationChannelCompat.GROUP_DAEMON, "daemon");
                NotificationChannelCompat.checkOrCreateGroup(context, NotificationChannelCompat.GROUP_SYSTEM, "system");
                NotificationChannelCompat.checkOrCreateGroup(context, NotificationChannelCompat.GROUP_APP, "app");
                NotificationChannelCompat.checkOrCreateGroup(context, NotificationChannelCompat.GROUP_PHONE, "phone");
                NotificationChannelCompat.checkOrCreateChannel(context, NotificationChannelCompat.DAEMON_ID, "daemon");
                NotificationChannelCompat.checkOrCreateChannel(context, NotificationChannelCompat.SYSTEM_ID, "system");
                NotificationChannelCompat.checkOrCreateChannel(context, NotificationChannelCompat.DEFAULT_ID, "default");
                NotificationChannelCompat.checkOrCreateChannel(context, NotificationChannelCompat.LIGHT_ID, "light");
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && VirtualCore.get().getTargetSdkVersion() >= Build.VERSION_CODES.O
                        && !VirtualCore.getConfig().isHideForegroundNotification()) {
                    context.startForegroundService(new Intent(context, KeepAliveService.class));
                } else {
                    context.startService(new Intent(context, KeepAliveService.class));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!VirtualCore.get().isStartup()) {
            return false;
        }

        addService(ServiceManagerNative.INSTALLERSETTING, InstallerSettingService.get());
        VPackageManagerService.systemReady();
        addService(ServiceManagerNative.PACKAGE, VPackageManagerService.get());
        addService(ServiceManagerNative.ACTIVITY, VActivityManagerService.get());
        addService(ServiceManagerNative.USER, VUserManagerService.get());
        VServiceKeepAliveService.systemReady();
        addService(ServiceManagerNative.KEEPALIVE, VServiceKeepAliveService.get());
        VAppManagerService.systemReady();
        addService(ServiceManagerNative.APP, VAppManagerService.get());
        BroadcastSystem.attach(VActivityManagerService.get(), VAppManagerService.get());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addService(ServiceManagerNative.JOB, VJobSchedulerService.get());
        }
        VNotificationManagerService.systemReady(context);
        addService(ServiceManagerNative.NOTIFICATION, VNotificationManagerService.get());
        VAppManagerService.get().scanApps();
        VAccountManagerService.systemReady();
        VContentService.systemReady();
        addService(ServiceManagerNative.ACCOUNT, VAccountManagerService.get());
        addService(ServiceManagerNative.CONTENT, VContentService.get());
        addService(ServiceManagerNative.VS, VirtualStorageService.get());
        addService(ServiceManagerNative.DEVICE, VDeviceManagerService.get());
        addService(ServiceManagerNative.VIRTUAL_LOC, VirtualLocationService.get());

        /* Start Changed by XDJA */
        VSafekeyManagerService.systemReady(context);
        addService(ServiceManagerNative.SAFEKEY, VSafekeyManagerService.get());
        addService(ServiceManagerNative.CONTROLLER, controllerService.get());
        VAppPermissionManagerService.systemReady();
        addService(ServiceManagerNative.APPPERMISSION, VAppPermissionManagerService.get());
        VWaterMarkService.systemReady();
        addService(ServiceManagerNative.WATERMARK, VWaterMarkService.get());
//        VSafekeyCkmsManagerService.systemReady(context);
//        addService(ServiceManagerNative.CKMSSAFEKEY, VSafekeyCkmsManagerService.get());
        /* End Changed by XDJA */
        addService(ServiceManagerNative.FLOATICONBALL, ActivityCounterService.get());
        VActivityManagerService.systemReady();
        sInitialized = true;
        if(context != null) {
            //下面2个清理，确保在无任何app启动之前执行，防止误清理
            if (VirtualCore.getConfig().isClearInvalidTask()) {
                //清理无效的task
                clearOldTask(context);
            }
            if (VirtualCore.getConfig().isClearInvalidProcess()) {
                //清理无效的其他进程
                clearOldProcess(context);
            }
        }
        //xdja
        VirtualCore.get().preLaunchApp();
        VirtualCore.getConfig().onPreLunchApp();
        return true;
    }

    private void clearOldTask(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> list = null;
            try {
                list = am.getAppTasks();
            } catch (Throwable e) {
                VLog.w(TAG, "getAppTasks failed\n%s", VLog.getStackTraceString(e));
            }
            if (list != null) {
                for (ActivityManager.AppTask task : list) {
                    ActivityManager.RecentTaskInfo taskInfo = null;
                    try {
                        taskInfo = task.getTaskInfo();
                    } catch (Throwable e) {
                        VLog.w(TAG, "getTaskInfo failed\n%s", VLog.getStackTraceString(e));
                    }
                    if (taskInfo == null) {
                        continue;
                    }
                    if (taskInfo.baseIntent != null && taskInfo.baseIntent.getComponent() != null) {
                        ComponentName cmp = taskInfo.baseIntent.getComponent();
                        if (cmp != null) {
                            if (context.getPackageName().equals(cmp.getPackageName())
                                    && StubManifest.isStubActivity(cmp.getClassName())) {
                                try {
                                    VLog.w(TAG, "clear %s", ComponentUtils.getAppComponent(taskInfo.baseIntent));
                                    //app组件
                                    task.finishAndRemoveTask();
                                } catch (Throwable ignore) {

                                }
//                            } else {
//                                VLog.w(TAG, "ignore task=%s", cmp);
                            }
                        }
//                    } else {
//                        VLog.w(TAG, "unknown=%s", taskInfo.baseIntent);
                    }
                }
            }
        }
    }

    private void clearOldProcess(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcessInfos) {
            //判断是否是app进程
            if (VActivityManagerService.parseVPid(runningAppProcessInfo.processName) >= 0) {
                try {
                    VLog.d(TAG, "kill old process %d", runningAppProcessInfo.pid);
                    Process.killProcess(runningAppProcessInfo.pid);
                } catch (Throwable e) {
                    //ignore
                }
            }
        }
    }

    private void addService(String name, IBinder service) {
        ServiceCache.addService(name, service);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!sInitialized) {
            init();
        }
        if ("@".equals(method)) {
            Bundle bundle = new Bundle();
            BundleCompat.putBinder(bundle, "_VA_|_binder_", mServiceFetcher);
            return bundle;
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private class ServiceFetcher extends IServiceFetcher.Stub {
        @Override
        public IBinder getService(String name) throws RemoteException {
            if (name != null) {
                return ServiceCache.getService(name);
            }
            return null;
        }

        @Override
        public void addService(String name, IBinder service) throws RemoteException {
            if (name != null && service != null) {
                ServiceCache.addService(name, service);
            }
        }

        @Override
        public void removeService(String name) throws RemoteException {
            if (name != null) {
                ServiceCache.removeService(name);
            }
        }
    }
}
