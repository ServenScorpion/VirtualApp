package mirror.com.android.internal.telecom;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class ITelecomService {
    public static Class<?> TYPE = RefClass.load(ITelecomService.class, "com.android.internal.telecom.ITelecomService");

    public static class Stub {
        public static Class<?> TYPE = RefClass.load(ITelecomService.Stub.class, "com.android.internal.telecom.ITelecomService$Stub");
        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}
