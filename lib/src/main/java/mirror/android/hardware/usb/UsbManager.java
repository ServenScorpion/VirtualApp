package mirror.android.hardware.usb;

import mirror.RefClass;
import mirror.RefObject;

public class UsbManager {
    public static Class<?> TYPE = RefClass.load(UsbManager.class, android.hardware.usb.UsbManager.class);

    public static RefObject<Object> mService;
}
