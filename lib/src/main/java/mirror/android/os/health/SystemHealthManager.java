package mirror.android.os.health;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.IInterface;

import mirror.RefClass;
import mirror.RefObject;

@TargetApi(Build.VERSION_CODES.N)
public class SystemHealthManager {
    public static Class<?> TYPE = RefClass.load(SystemHealthManager.class, android.os.health.SystemHealthManager.class);
    public static RefObject<IInterface> mBatteryStats;
}
