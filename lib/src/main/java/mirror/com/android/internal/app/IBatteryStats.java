package mirror.com.android.internal.app;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class IBatteryStats {
    public static Class<?> TYPE = RefClass.load(IBatteryStats.class, "com.android.internal.app.IBatteryStats");

    public static class Stub {
        public static Class<?> TYPE = RefClass.load(IBatteryStats.Stub.class, "com.android.internal.app.IBatteryStats$Stub");
        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}
