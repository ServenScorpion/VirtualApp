package mirror.android.app;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.List;

import mirror.RefClass;
import mirror.RefObject;

@TargetApi(Build.VERSION_CODES.O)
public class NotificationChannel {
    public static Class<?> TYPE = RefClass.load(NotificationChannel.class, android.app.NotificationChannel.class);

    public static RefObject<String> mId;

}