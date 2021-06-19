package mirror.android.app;

import android.os.IBinder;

import java.util.List;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;

/**
 * @author Swift Gans
 */

public class ActivityThreadQ {
    public static Class<?> Class = RefClass.load(ActivityThreadQ.class, "android.app.ActivityThread");
    @MethodParams({IBinder.class, List.class})
    public static RefMethod<Void> handleNewIntent;
}
