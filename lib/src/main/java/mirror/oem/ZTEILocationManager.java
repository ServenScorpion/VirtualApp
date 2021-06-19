package mirror.oem;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefObject;

public class ZTEILocationManager {
    public static Class<?> TYPE = RefClass.load(ZTEILocationManager.class, "com.zte.security.ZTEILocationManager");

    public static RefObject<IInterface> mILocationManager;

}
