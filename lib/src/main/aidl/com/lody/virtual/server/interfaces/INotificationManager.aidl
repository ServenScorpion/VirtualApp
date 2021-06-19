package com.lody.virtual.server.interfaces;

import android.os.RemoteException;
import com.xdja.zs.INotificationCallback;

/**
 * @author Lody
 */
interface INotificationManager {

    int dealNotificationId(int id, String packageName, String tag, int userId);

    String dealNotificationTag(int id, String packageName, String tag, int userId);

    boolean areNotificationsEnabledForPackage(String packageName, int userId);

    void setNotificationsEnabledForPackage(String packageName, boolean enable, int userId);

    void addNotification(int id, String tag, String packageName, int userId);

    void cancelAllNotification(String packageName, int userId);

    void cancelNotification(String pkg, String tag, int id, int userId);

    void registerCallback(INotificationCallback iNotificationCallback);

    boolean checkNotificationTag(String tag, String packageName, int userId);

    boolean checkNotificationChannel(String id, String packageName, int userId);

    boolean checkNotificationGroup(String id, String packageName, int userId);

    String dealNotificationChannel(String id, String packageName, int userId);

    String dealNotificationGroup(String id, String packageName, int userId);

    String getRealNotificationTag(String tag, String packageName, int userId);

    String getRealNotificationChannel(String id, String packageName, int userId);

    String getRealNotificationGroup(String id, String packageName, int userId);
}