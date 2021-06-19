package mirror.android.os;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefObject;

public class DropBoxManager {
    public static Class<?> TYPE = RefClass.load(DropBoxManager.class, android.os.DropBoxManager.class);
    public static RefObject<IInterface> mService;
}
