package mirror.android.app.job;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefInt;
import mirror.RefMethod;
import mirror.RefObject;

/**
 * @author Lody
 */

@TargetApi(Build.VERSION_CODES.O)
public class JobWorkItem {
    public static Class<?> TYPE = RefClass.load(JobWorkItem.class, android.app.job.JobWorkItem.class);

    @MethodParams({Intent.class})
    public static RefConstructor<Object> ctor;

    public static RefMethod<Intent> getIntent;

    public static RefInt mWorkId;

    public static RefObject<Object> mGrants;

    public static RefInt mDeliveryCount;
}
