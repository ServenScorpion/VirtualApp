package mirror.android.hardware.usb;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class IUsbManager {
    public static Class<?> TYPE = RefClass.load(IUsbManager.class, "android.hardware.usb.IUsbManager");

    public static class Stub {
        public static Class<?> TYPE = RefClass.load(IUsbManager.Stub.class, "android.hardware.usb.IUsbManager$Stub");

        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}
