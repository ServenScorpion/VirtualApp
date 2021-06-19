package mirror.android.content;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefObject;
import mirror.RefStaticObject;

public class ClipboardManagerOreo {
    public static Class<?> TYPE = RefClass.load(ClipboardManagerOreo.class, android.content.ClipboardManager.class);
    public static RefObject<IInterface> mService;
    public static RefStaticObject<IInterface> sService;
}
