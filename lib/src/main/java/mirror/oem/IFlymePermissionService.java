package mirror.oem;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class IFlymePermissionService {
    public static Class<?> TYPE = RefClass.load(IFlymePermissionService.class, "meizu.security.IFlymePermissionService");

    public static class Stub {
        public static Class<?> TYPE = RefClass.load(IFlymePermissionService.Stub.class, "meizu.security.IFlymePermissionService$Stub");
        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}
