package com.lody.virtual.client.hook.delegate;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.fixer.ActivityFixer;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.proxies.am.HCallbackStub;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.os.VUserHandle;
import com.xdja.zs.IUiCallback;

import java.lang.reflect.Field;

import mirror.android.app.ActivityThread;

/**
 * @author Lody
 */
public final class AppInstrumentation extends InstrumentationDelegate implements IInjector {

    private static final String TAG = AppInstrumentation.class.getSimpleName();

    private static AppInstrumentation gDefault;

    private AppInstrumentation(Instrumentation base) {
        super(base);
    }

    public static AppInstrumentation getDefault() {
        if (gDefault == null) {
            synchronized (AppInstrumentation.class) {
                if (gDefault == null) {
                    gDefault = create();
                }
            }
        }
        return gDefault;
    }

    private static AppInstrumentation create() {
        Instrumentation instrumentation = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
        if (instrumentation instanceof AppInstrumentation) {
            return (AppInstrumentation) instrumentation;
        }
        return new AppInstrumentation(instrumentation);
    }


    @Override
    public void inject() {
        base = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
        ActivityThread.mInstrumentation.set(VirtualCore.mainThread(), this);
    }

    @Override
    public boolean isEnvBad() {
        return !checkInstrumentation(ActivityThread.mInstrumentation.get(VirtualCore.mainThread()));
    }

    private boolean checkInstrumentation(Instrumentation instrumentation) {
        if (instrumentation instanceof AppInstrumentation) {
            return true;
        }
        Class<?> clazz = instrumentation.getClass();
        if (Instrumentation.class.equals(clazz)) {
            return false;
        }
        do {
            Field[] fields = clazz.getDeclaredFields();
            if (fields != null) {
                for (Field field : fields) {
                    if (Instrumentation.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object obj;
                        try {
                            obj = field.get(instrumentation);
                        } catch (IllegalAccessException e) {
                            return false;
                        }
                        if ((obj instanceof AppInstrumentation)) {
                            return true;
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while (!Instrumentation.class.equals(clazz));
        return false;
    }

    private void checkActivityCallback() {
        InvocationStubManager.getInstance().checkEnv(HCallbackStub.class);
        InvocationStubManager.getInstance().checkEnv(AppInstrumentation.class);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        checkActivityCallback();
        ContextFixer.fixContext(activity);
        ActivityFixer.fixActivity(activity);
        ActivityInfo info = mirror.android.app.Activity.mActivityInfo.get(activity);
        if (info != null) {
            if (info.theme != 0) {
                activity.setTheme(info.theme);
            }
            if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    && info.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                if (activity.getRequestedOrientation() != info.screenOrientation) {
                    ActivityManagerCompat.setActivityOrientation(activity, info.screenOrientation);
                    boolean needWait;
                    //set orientation
                    Configuration configuration = activity.getResources().getConfiguration();
                    if (isOrientationLandscape(info.screenOrientation)) {
                        needWait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE;
                        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
                    } else {
                        needWait = configuration.orientation != Configuration.ORIENTATION_PORTRAIT;
                        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
                    }
                    if (needWait) {
                        try {
                            Thread.sleep(800);
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
            }
        }
        super.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        Intent intent = activity.getIntent();
        if (intent != null) {
            Bundle bundle = intent.getBundleExtra("_VA_|_sender_");
            if (bundle != null) {
                IBinder callbackToken = BundleCompat.getBinder(bundle, "_VA_|_ui_callback_");
                IUiCallback callback = IUiCallback.Stub.asInterface(callbackToken);
                if (callback != null) {
                    try {
                        callback.onAppOpened(VClient.get().getCurrentPackage(), VUserHandle.myUserId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                intent.removeExtra("_VA_|_sender_");
            }
        }
    }

    private boolean isOrientationLandscape(int requestedOrientation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        } else {
            return (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    }


    @Override
    public void callActivityOnDestroy(Activity activity) {
        super.callActivityOnDestroy(activity);
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        super.callActivityOnPause(activity);
    }


    @Override
    public void callApplicationOnCreate(Application app) {
        checkActivityCallback();
        super.callApplicationOnCreate(app);
    }

    @Override
    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Activity activity, Intent intent, int i, Bundle bundle) throws Throwable {
        return super.execStartActivity(context, iBinder, iBinder2, activity, intent, i, bundle);
    }

    @Override
    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, String str, Intent intent, int i, Bundle bundle) throws Throwable {
        return super.execStartActivity(context, iBinder, iBinder2, str, intent, i, bundle);
    }

    @Override
    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Fragment fragment, Intent intent, int i) throws Throwable {
        return super.execStartActivity(context, iBinder, iBinder2, fragment, intent, i);
    }

    @Override
    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Activity activity, Intent intent, int i) throws Throwable {
        return super.execStartActivity(context, iBinder, iBinder2, activity, intent, i);
    }

    @Override
    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Fragment fragment, Intent intent, int i, Bundle bundle) throws Throwable {
        return super.execStartActivity(context, iBinder, iBinder2, fragment, intent, i, bundle);
    }

    @Override
    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Activity activity, Intent intent, int i, Bundle bundle, UserHandle userHandle) throws Throwable {
        return super.execStartActivity(context, iBinder, iBinder2, activity, intent, i, bundle, userHandle);
    }
	
	public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return super.newActivity(cl, className, intent);
        } catch (ClassNotFoundException e) {
            return root.newActivity(cl, className, intent);
        }
    }
}
