package mirror.android.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;

public class Instrumentation {
    public static Class<?> TYPE = RefClass.load(Instrumentation.class, android.app.Instrumentation.class);

    @MethodParams({Context.class, IBinder.class, IBinder.class, Activity.class,
            Intent.class, int.class, Bundle.class})
    public static RefMethod<android.app.Instrumentation.ActivityResult> execStartActivity;
}
