package mirror.android.os;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class IDeviceIdentifiersPolicyService {
    public static Class<?> TYPE = RefClass.load(IDeviceIdentifiersPolicyService.class, "android.os.IDeviceIdentifiersPolicyService");

    public static class Stub {
        public static Class<?> TYPE = RefClass.load(Stub.class, "android.os.IDeviceIdentifiersPolicyService$Stub");
        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}
