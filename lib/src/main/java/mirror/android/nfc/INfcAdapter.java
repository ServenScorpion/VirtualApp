package mirror.android.nfc;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class INfcAdapter {
    public static Class<?> TYPE = RefClass.load(INfcAdapter.class, "android.nfc.INfcAdapter");

    public static class Stub {
        public static Class<?> TYPE = RefClass.load(INfcAdapter.Stub.class, "android.nfc.INfcAdapter$Stub");
        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}
