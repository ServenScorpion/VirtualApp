package mirror.android.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefMethod;

public class PendingIntentJBMR2 {
    public static Class Class = RefClass.load(PendingIntentJBMR2.class, PendingIntent.class);
    @MethodParams({IBinder.class})
    public static RefConstructor<PendingIntent> ctor;
    public static RefMethod<Intent> getIntent;
}