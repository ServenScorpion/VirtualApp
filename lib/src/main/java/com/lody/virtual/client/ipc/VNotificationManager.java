package com.lody.virtual.client.ipc;

import android.app.Notification;
import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.server.interfaces.INotificationManager;
import com.lody.virtual.server.notification.NotificationCompat;

import com.xdja.zs.INotificationCallback;

/**
 * Fake notification manager
 */
public class VNotificationManager {
    /**
     * 无法处理notification
     */
    public static final int MODE_NONE = 0;
    /**
     * 不替换notification对象
     */
    public static final int MODE_USE_OLD = 1;
    /**
     * 需要替换notification对象
     */
    public static final int MODE_REPLACED = 2;

    private static final VNotificationManager sInstance = new VNotificationManager();
    private final NotificationCompat mNotificationCompat;
    private INotificationManager mService;

    public INotificationManager getService() {
        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (VNotificationManager.class) {
                final Object pmBinder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(INotificationManager.class, pmBinder);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return INotificationManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.NOTIFICATION));
    }

    private VNotificationManager() {
        mNotificationCompat = NotificationCompat.create();
    }

    public static VNotificationManager get() {
        return sInstance;
    }

    public Result dealNotification(int id, Notification notification, String packageName, int userId) {
        if (notification == null) return Result.USE_OLD;
        if(VirtualCore.get().getHostPkg().equals(packageName)){
            return Result.USE_OLD;
        }
        return mNotificationCompat.dealNotification(id, notification, packageName, userId);
    }

    public int dealNotificationId(int id, String packageName, String tag, int userId) {
        try {
            return getService().dealNotificationId(id, packageName, tag, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return id;
    }

    public String dealNotificationTag(int id, String packageName, String tag, int userId) {
        try {
            return getService().dealNotificationTag(id, packageName, tag, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return tag;
    }

    public boolean areNotificationsEnabledForPackage(String packageName, int userId) {
        try {
            return getService().areNotificationsEnabledForPackage(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void setNotificationsEnabledForPackage(String packageName, boolean enable, int userId) {
        try {
            getService().setNotificationsEnabledForPackage(packageName, enable, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void addNotification(int id, String tag, String packageName, int userId) {
        try {
            getService().addNotification(id, tag, packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelAllNotification(String packageName, int userId) {
        try {
            getService().cancelAllNotification(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelNotification(String packageName, String tag, int id, int userId) {
        try {
            getService().cancelNotification(packageName, tag, id, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void registerCallback(INotificationCallback iNotificationCallback) {
        try {
            getService().registerCallback(iNotificationCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean checkNotificationTag(String tag, String packageName, int userId){
        try {
            return getService().checkNotificationTag(tag, packageName, userId);
        } catch (RemoteException e) {
           return false;
        }
    }

    public boolean checkNotificationChannel(String id, String packageName, int userId){
        try {
            return getService().checkNotificationChannel(id, packageName, userId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean checkNotificationGroup(String id, String packageName, int userId){
        try {
            return getService().checkNotificationGroup(id, packageName, userId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public String dealNotificationChannel(String id, String packageName, int userId){
        try {
            return getService().dealNotificationChannel(id, packageName, userId);
        } catch (RemoteException e) {
            return id;
        }
    }

    public String dealNotificationGroup(String id, String packageName, int userId){
        if (id == null) {
            return null;
        }
        try {
            return getService().dealNotificationGroup(id, packageName, userId);
        } catch (RemoteException e) {
            return id;
        }
    }

    public String getRealNotificationTag(String tag, String packageName, int userId){
        try {
            return getService().getRealNotificationTag(tag, packageName, userId);
        } catch (RemoteException e) {
            return tag;
        }
    }

    public String getRealNotificationChannel(String tag, String packageName, int userId){
        try {
            return getService().getRealNotificationChannel(tag, packageName, userId);
        } catch (RemoteException e) {
            return tag;
        }
    }

    public String getRealNotificationGroup(String tag, String packageName, int userId){
        try {
            return getService().getRealNotificationGroup(tag, packageName, userId);
        } catch (RemoteException e) {
            return tag;
        }
    }

    public static class Result {
        public static final Result NONE = new Result(MODE_NONE);
        public static final Result USE_OLD = new Result(MODE_USE_OLD);
        public int mode;
        public Notification notification;

        public Result(int mode) {
            this.mode = mode;
        }
    }
}
