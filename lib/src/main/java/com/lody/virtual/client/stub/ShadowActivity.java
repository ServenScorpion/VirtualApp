package com.lody.virtual.client.stub;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.text.TextUtils;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.hook.proxies.am.HCallbackStub;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.StubActivityRecord;

import mirror.android.app.ActivityThread;
import mirror.android.os.BaseBundle;
import mirror.android.os.BundleICS;

/**
 * @author Lody
 */
public abstract class ShadowActivity extends Activity {

    private Object activityThread;
    private Instrumentation mInstrumentation;
    private ShadowInstrumentation mShadowInstrumentation;

    public ShadowActivity() {
        activityThread = VirtualCore.mainThread();
        mInstrumentation = ActivityThread.mInstrumentation.get(activityThread);
        if (mShadowInstrumentation == null) {
            mShadowInstrumentation = new ShadowInstrumentation(mInstrumentation);
        }
        ActivityThread.mInstrumentation.set(activityThread, mShadowInstrumentation);

    }

    private void restoreInstrumentation() {
        ActivityThread.mInstrumentation.set(activityThread, mInstrumentation);
    }

    private static class ShadowInstrumentation extends Instrumentation {
        private Instrumentation mBase;

        public ShadowInstrumentation(Instrumentation mBase) {
            this.mBase = mBase;
        }

        @Override
        public void callActivityOnCreate(Activity activity, Bundle icicle) {
            if (icicle != null) {
                clearParcelledData(icicle);
            }
            mBase.callActivityOnCreate(activity, icicle);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
            if (icicle != null) {
                clearParcelledData(icicle);
            }
            mBase.callActivityOnCreate(activity, icicle, persistentState);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The savedInstanceState's classLoader is not exist.
        super.onCreate(null);
        if (savedInstanceState != null) {
            clearParcelledData(savedInstanceState);
        }
        restoreInstrumentation();
        finish();
        // It seems that we have conflict with the other Android-Plugin-Framework.
        Intent stubIntent = getIntent();
        // Try to acquire the actually component information.
        StubActivityRecord r = new StubActivityRecord(stubIntent);
        if (r.intent != null) {
            if (TextUtils.equals(r.info.processName, VirtualRuntime.getProcessName()) && r.userId == VUserHandle.myUserId()) {
                // Retry to inject the HCallback to instead of the exist one.
                InvocationStubManager.getInstance().checkEnv(HCallbackStub.class);
                Intent intent = r.intent;
                intent.setExtrasClassLoader(VClient.get().getCurrentApplication().getClassLoader());
                startActivity(intent);
            } else {
                // Start the target Activity in other process.
                VActivityManager.get().startActivity(r.intent, r.userId);
            }
        }
    }

    private static void clearParcelledData(Bundle savedInstanceState) {
        Parcel newData = Parcel.obtain();
        newData.writeInt(0);
        newData.setDataPosition(0);
        Parcel data = null;
        if (BaseBundle.TYPE != null) {
            data = BaseBundle.mParcelledData.get(savedInstanceState);
            BaseBundle.mParcelledData.set(savedInstanceState, newData);
        } else if (BundleICS.TYPE != null) {
            data = BundleICS.mParcelledData.get(savedInstanceState);
            BundleICS.mParcelledData.set(savedInstanceState, newData);
        }
        if (data != null) {
            data.recycle();
        }
    }

    public static class P0 extends ShadowActivity {
    }

    public static class P1 extends ShadowActivity {
    }

    public static class P2 extends ShadowActivity {
    }

    public static class P3 extends ShadowActivity {
    }

    public static class P4 extends ShadowActivity {
    }

    public static class P5 extends ShadowActivity {
    }

    public static class P6 extends ShadowActivity {
    }

    public static class P7 extends ShadowActivity {
    }

    public static class P8 extends ShadowActivity {
    }

    public static class P9 extends ShadowActivity {
    }

    public static class P10 extends ShadowActivity {
    }

    public static class P11 extends ShadowActivity {
    }

    public static class P12 extends ShadowActivity {
    }

    public static class P13 extends ShadowActivity {
    }

    public static class P14 extends ShadowActivity {
    }

    public static class P15 extends ShadowActivity {
    }

    public static class P16 extends ShadowActivity {
    }

    public static class P17 extends ShadowActivity {
    }

    public static class P18 extends ShadowActivity {
    }

    public static class P19 extends ShadowActivity {
    }

    public static class P20 extends ShadowActivity {
    }

    public static class P21 extends ShadowActivity {
    }

    public static class P22 extends ShadowActivity {
    }

    public static class P23 extends ShadowActivity {
    }

    public static class P24 extends ShadowActivity {
    }

    public static class P25 extends ShadowActivity {
    }

    public static class P26 extends ShadowActivity {
    }

    public static class P27 extends ShadowActivity {
    }

    public static class P28 extends ShadowActivity {
    }

    public static class P29 extends ShadowActivity {
    }

    public static class P30 extends ShadowActivity {
    }

    public static class P31 extends ShadowActivity {
    }

    public static class P32 extends ShadowActivity {
    }

    public static class P33 extends ShadowActivity {
    }

    public static class P34 extends ShadowActivity {
    }

    public static class P35 extends ShadowActivity {
    }

    public static class P36 extends ShadowActivity {
    }

    public static class P37 extends ShadowActivity {
    }

    public static class P38 extends ShadowActivity {
    }

    public static class P39 extends ShadowActivity {
    }

    public static class P40 extends ShadowActivity {
    }

    public static class P41 extends ShadowActivity {
    }

    public static class P42 extends ShadowActivity {
    }

    public static class P43 extends ShadowActivity {
    }

    public static class P44 extends ShadowActivity {
    }

    public static class P45 extends ShadowActivity {
    }

    public static class P46 extends ShadowActivity {
    }

    public static class P47 extends ShadowActivity {
    }

    public static class P48 extends ShadowActivity {
    }

    public static class P49 extends ShadowActivity {
    }

    public static class P50 extends ShadowActivity {
    }

    public static class P51 extends ShadowActivity {
    }

    public static class P52 extends ShadowActivity {
    }

    public static class P53 extends ShadowActivity {
    }

    public static class P54 extends ShadowActivity {
    }

    public static class P55 extends ShadowActivity {
    }

    public static class P56 extends ShadowActivity {
    }

    public static class P57 extends ShadowActivity {
    }

    public static class P58 extends ShadowActivity {
    }

    public static class P59 extends ShadowActivity {
    }

    public static class P60 extends ShadowActivity {
    }

    public static class P61 extends ShadowActivity {
    }

    public static class P62 extends ShadowActivity {
    }

    public static class P63 extends ShadowActivity {
    }

    public static class P64 extends ShadowActivity {
    }

    public static class P65 extends ShadowActivity {
    }

    public static class P66 extends ShadowActivity {
    }

    public static class P67 extends ShadowActivity {
    }

    public static class P68 extends ShadowActivity {
    }

    public static class P69 extends ShadowActivity {
    }

    public static class P70 extends ShadowActivity {
    }

    public static class P71 extends ShadowActivity {
    }

    public static class P72 extends ShadowActivity {
    }

    public static class P73 extends ShadowActivity {
    }

    public static class P74 extends ShadowActivity {
    }

    public static class P75 extends ShadowActivity {
    }

    public static class P76 extends ShadowActivity {
    }

    public static class P77 extends ShadowActivity {
    }

    public static class P78 extends ShadowActivity {
    }

    public static class P79 extends ShadowActivity {
    }

    public static class P80 extends ShadowActivity {
    }

    public static class P81 extends ShadowActivity {
    }

    public static class P82 extends ShadowActivity {
    }

    public static class P83 extends ShadowActivity {
    }

    public static class P84 extends ShadowActivity {
    }

    public static class P85 extends ShadowActivity {
    }

    public static class P86 extends ShadowActivity {
    }

    public static class P87 extends ShadowActivity {
    }

    public static class P88 extends ShadowActivity {
    }

    public static class P89 extends ShadowActivity {
    }

    public static class P90 extends ShadowActivity {
    }

    public static class P91 extends ShadowActivity {
    }

    public static class P92 extends ShadowActivity {
    }

    public static class P93 extends ShadowActivity {
    }

    public static class P94 extends ShadowActivity {
    }

    public static class P95 extends ShadowActivity {
    }

    public static class P96 extends ShadowActivity {
    }

    public static class P97 extends ShadowActivity {
    }

    public static class P98 extends ShadowActivity {
    }

    public static class P99 extends ShadowActivity {
    }


}
