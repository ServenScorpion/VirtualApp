package mirror.android.content;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefObject;

/**
 * @author Lody
 */
public class IntentSender {
    public static Class TYPE = RefClass.load(IntentSender.class, android.content.IntentSender.class);
    public static RefObject<IInterface> mTarget;
}
