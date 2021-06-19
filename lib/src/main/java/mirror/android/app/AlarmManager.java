package mirror.android.app;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefInt;
import mirror.RefObject;

public class AlarmManager {
    public static Class<?> TYPE = RefClass.load(AlarmManager.class, android.app.AlarmManager.class);
    public static RefInt mTargetSdkVersion;
    public static RefObject<IInterface> mService;
}
