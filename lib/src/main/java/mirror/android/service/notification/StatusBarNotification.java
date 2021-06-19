package mirror.android.service.notification;

import android.annotation.TargetApi;
import android.os.Build;

import mirror.RefClass;
import mirror.RefObject;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class StatusBarNotification {
    public static Class<?> TYPE = RefClass.load(StatusBarNotification.class, android.service.notification.StatusBarNotification.class);
    public static RefObject<Integer> id;
    public static RefObject<String> pkg;
    public static RefObject<String> tag;
    public static RefObject<String> opPkg;
}
