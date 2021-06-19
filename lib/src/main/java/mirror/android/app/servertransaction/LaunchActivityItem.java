package mirror.android.app.servertransaction;


import android.content.Intent;
import android.content.pm.ActivityInfo;

import mirror.RefClass;
import mirror.RefObject;

public class LaunchActivityItem {
    public static Class<?> TYPE = RefClass.load(LaunchActivityItem.class, "android.app.servertransaction.LaunchActivityItem");
    public static RefObject<ActivityInfo> mInfo;
    public static RefObject<Intent> mIntent;
}
