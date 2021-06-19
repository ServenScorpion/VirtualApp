package mirror.android.app;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.os.Build;

import java.util.List;

import mirror.RefClass;
import mirror.RefObject;

@TargetApi(Build.VERSION_CODES.O)
public class NotificationChannelGroup {
    public static Class<?> TYPE = RefClass.load(NotificationChannelGroup.class, android.app.NotificationChannelGroup.class);

    public static RefObject<String> mId;

    public static RefObject<List<NotificationChannel>> mChannels;
}