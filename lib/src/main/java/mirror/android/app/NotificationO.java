package mirror.android.app;


import android.app.Notification;

import mirror.RefClass;
import mirror.RefObject;

public class NotificationO {
    public static Class<?> TYPE = RefClass.load(NotificationO.class, Notification.class);
    public static RefObject<String> mChannelId;
    public static RefObject<String> mGroupKey;
}