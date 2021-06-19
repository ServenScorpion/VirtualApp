package mirror.android.content.pm;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;
import mirror.android.app.ISearchManager;

/**
 * @author Lody
 */

public class IShortcutService {
    public static Class<?> TYPE = RefClass.load(IShortcutService.class, "android.content.pm.IShortcutService");

    public static final class Stub {
        public static Class<?> TYPE = RefClass.load(Stub.class, "android.content.pm.IShortcutService$Stub");

        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}
