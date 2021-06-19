package mirror.android.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefMethod;

public class PendingIntentO {
    public static Class Class = RefClass.load(PendingIntentO.class, PendingIntent.class);
    @MethodParams({IBinder.class, Object.class})
    public static RefConstructor<PendingIntent> ctor;
}