package mirror.android.net.wifi;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefObject;
import mirror.RefStaticObject;

public class WifiManager {
    public static Class<?> TYPE = RefClass.load(WifiManager.class, android.net.wifi.WifiManager.class);
    public static RefObject<IInterface> mService;
    public static RefStaticObject<IInterface> sService;
}
