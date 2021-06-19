package com.lody.virtual.client.hook.delegate;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.lody.virtual.helper.Keep;
import com.lody.virtual.helper.MultiAvoidRecursive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author Lody
 */
@Keep
public class InstrumentationDelegate extends Instrumentation {

    protected Instrumentation base;
    protected Instrumentation root;
    private MultiAvoidRecursive avoidRecursive = new MultiAvoidRecursive(20);

    public InstrumentationDelegate(Instrumentation base) {
        this.base = base;
        this.root = base;
    }

    @Override
    public void onCreate(Bundle arguments) {
        root.onCreate(arguments);
    }

    @Override
    public void start() {
        root.start();
    }

    @Override
    public void onStart() {
        root.onStart();
    }

    @Override
    public boolean onException(Object obj, Throwable e) {
        return root.onException(obj, e);
    }

    @Override
    public void sendStatus(int resultCode, Bundle results) {
        root.sendStatus(resultCode, results);
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        root.finish(resultCode, results);
    }

    @Override
    public void setAutomaticPerformanceSnapshots() {
        root.setAutomaticPerformanceSnapshots();
    }

    @Override
    public void startPerformanceSnapshot() {
        root.startPerformanceSnapshot();
    }

    @Override
    public void endPerformanceSnapshot() {
        root.endPerformanceSnapshot();
    }

    @Override
    public void onDestroy() {
        root.onDestroy();
    }

    @Override
    public Context getContext() {
        return root.getContext();
    }

    @Override
    public ComponentName getComponentName() {
        return root.getComponentName();
    }

    @Override
    public Context getTargetContext() {
        return root.getTargetContext();
    }

    @Override
    public boolean isProfiling() {
        return root.isProfiling();
    }

    @Override
    public void startProfiling() {
        root.startProfiling();
    }

    @Override
    public void stopProfiling() {
        root.stopProfiling();
    }

    @Override
    public void setInTouchMode(boolean inTouch) {
        root.setInTouchMode(inTouch);
    }

    @Override
    public void waitForIdle(Runnable recipient) {
        root.waitForIdle(recipient);
    }

    @Override
    public void waitForIdleSync() {
        root.waitForIdleSync();
    }

    @Override
    public void runOnMainSync(Runnable runner) {
        root.runOnMainSync(runner);
    }

    @Override
    public Activity startActivitySync(Intent intent) {
        return root.startActivitySync(intent);
    }

    @Override
    public void addMonitor(ActivityMonitor monitor) {
        root.addMonitor(monitor);
    }

    @Override
    public ActivityMonitor addMonitor(IntentFilter filter, ActivityResult result, boolean block) {
        return root.addMonitor(filter, result, block);
    }

    @Override
    public ActivityMonitor addMonitor(String cls, ActivityResult result, boolean block) {
        return root.addMonitor(cls, result, block);
    }

    @Override
    public boolean checkMonitorHit(ActivityMonitor monitor, int minHits) {
        return root.checkMonitorHit(monitor, minHits);
    }

    @Override
    public Activity waitForMonitor(ActivityMonitor monitor) {
        return root.waitForMonitor(monitor);
    }

    @Override
    public Activity waitForMonitorWithTimeout(ActivityMonitor monitor, long timeOut) {
        return root.waitForMonitorWithTimeout(monitor, timeOut);
    }

    @Override
    public void removeMonitor(ActivityMonitor monitor) {
        root.removeMonitor(monitor);
    }

    @Override
    public boolean invokeMenuActionSync(Activity targetActivity, int id, int flag) {
        return root.invokeMenuActionSync(targetActivity, id, flag);
    }

    @Override
    public boolean invokeContextMenuAction(Activity targetActivity, int id, int flag) {
        return root.invokeContextMenuAction(targetActivity, id, flag);
    }

    @Override
    public void sendStringSync(String text) {
        root.sendStringSync(text);
    }

    @Override
    public void sendKeySync(KeyEvent event) {
        root.sendKeySync(event);
    }

    @Override
    public void sendKeyDownUpSync(int key) {
        root.sendKeyDownUpSync(key);
    }

    @Override
    public void sendCharacterSync(int keyCode) {
        root.sendCharacterSync(keyCode);
    }

    @Override
    public void sendPointerSync(MotionEvent event) {
        root.sendPointerSync(event);
    }

    @Override
    public void sendTrackballEventSync(MotionEvent event) {
        root.sendTrackballEventSync(event);
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            if (avoidRecursive.beginCall(0)) {
                return base.newApplication(cl, className, context);
            } else {
                return root.newApplication(cl, className, context);
            }
        } finally {
            avoidRecursive.finishCall(0);
        }
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        try {
            if (avoidRecursive.beginCall(1)) {
                base.callApplicationOnCreate(app);
            } else {
                root.callApplicationOnCreate(app);
            }
        } finally {
            avoidRecursive.finishCall(1);
        }
    }

    @Override
    public Activity newActivity(Class<?> clazz, Context context, IBinder token, Application application, Intent intent,
                                ActivityInfo info, CharSequence title, Activity parent, String id, Object lastNonConfigurationInstance)
            throws InstantiationException, IllegalAccessException {
        try {
            if (avoidRecursive.beginCall(2)) {
                return base.newActivity(clazz, context, token, application, intent, info, title, parent, id,
                        lastNonConfigurationInstance);
            } else {
                return root.newActivity(clazz, context, token, application, intent, info, title, parent, id,
                        lastNonConfigurationInstance);
            }
        } finally {
            avoidRecursive.finishCall(2);
        }
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            if (avoidRecursive.beginCall(3)) {
                return base.newActivity(cl, className, intent);
            } else {
                return root.newActivity(cl, className, intent);
            }
        } finally {
            avoidRecursive.finishCall(3);
        }
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        try {
            if (avoidRecursive.beginCall(4)) {
                base.callActivityOnCreate(activity, icicle);
            } else {
                root.callActivityOnCreate(activity, icicle);
            }
        } finally {
            avoidRecursive.finishCall(4);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        try {
            if (avoidRecursive.beginCall(5)) {
                base.callActivityOnCreate(activity, icicle, persistentState);
            } else {
                root.callActivityOnCreate(activity, icicle, persistentState);
            }
        } finally {
            avoidRecursive.finishCall(5);
        }
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        try {
            if (avoidRecursive.beginCall(6)) {
                base.callActivityOnDestroy(activity);
            } else {
                root.callActivityOnDestroy(activity);
            }
        } finally {
            avoidRecursive.finishCall(6);
        }
    }

    @Override
    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState) {
        try {
            if (avoidRecursive.beginCall(7)) {
                base.callActivityOnRestoreInstanceState(activity, savedInstanceState);
            } else {
                root.callActivityOnRestoreInstanceState(activity, savedInstanceState);
            }
        } finally {
            avoidRecursive.finishCall(7);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState,
                                                   PersistableBundle persistentState) {
        try {
            if (avoidRecursive.beginCall(8)) {
                base.callActivityOnRestoreInstanceState(activity, savedInstanceState, persistentState);
            } else {
                root.callActivityOnRestoreInstanceState(activity, savedInstanceState, persistentState);
            }
        } finally {
            avoidRecursive.finishCall(8);
        }
    }

    @Override
    public void callActivityOnPostCreate(Activity activity, Bundle icicle) {
        try {
            if (avoidRecursive.beginCall(9)) {
                base.callActivityOnPostCreate(activity, icicle);
            } else {
                root.callActivityOnPostCreate(activity, icicle);
            }
        } finally {
            avoidRecursive.finishCall(9);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callActivityOnPostCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        try {
            if (avoidRecursive.beginCall(10)) {
                base.callActivityOnPostCreate(activity, icicle, persistentState);
            } else {
                root.callActivityOnPostCreate(activity, icicle, persistentState);
            }
        } finally {
            avoidRecursive.finishCall(10);
        }
    }

    @Override
    public void callActivityOnNewIntent(Activity activity, Intent intent) {
        try {
            if (avoidRecursive.beginCall(11)) {
                base.callActivityOnNewIntent(activity, intent);
            } else {
                root.callActivityOnNewIntent(activity, intent);
            }
        } finally {
            avoidRecursive.finishCall(11);
        }
    }


    @Override
    public void callActivityOnStart(Activity activity) {
        try {
            if (avoidRecursive.beginCall(12)) {
                base.callActivityOnStart(activity);
            } else {
                root.callActivityOnStart(activity);
            }
        } finally {
            avoidRecursive.finishCall(12);
        }
    }

    @Override
    public void callActivityOnRestart(Activity activity) {
        try {
            if (avoidRecursive.beginCall(13)) {
                base.callActivityOnRestart(activity);
            } else {
                root.callActivityOnRestart(activity);
            }
        } finally {
            avoidRecursive.finishCall(13);
        }
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        try {
            if (avoidRecursive.beginCall(14)) {
                base.callActivityOnResume(activity);
            } else {
                root.callActivityOnResume(activity);
            }
        } finally {
            avoidRecursive.finishCall(14);
        }
    }

    @Override
    public void callActivityOnStop(Activity activity) {
        try {
            if (avoidRecursive.beginCall(15)) {
                base.callActivityOnStop(activity);
            } else {
                root.callActivityOnStop(activity);
            }
        } finally {
            avoidRecursive.finishCall(15);
        }
    }

    @Override
    public void callActivityOnSaveInstanceState(Activity activity, Bundle outState) {
        try {
            if (avoidRecursive.beginCall(16)) {
                base.callActivityOnSaveInstanceState(activity, outState);
            } else {
                root.callActivityOnSaveInstanceState(activity, outState);
            }
        } finally {
            avoidRecursive.finishCall(16);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callActivityOnSaveInstanceState(Activity activity, Bundle outState,
                                                PersistableBundle outPersistentState) {
        try {
            if (avoidRecursive.beginCall(17)) {
                base.callActivityOnSaveInstanceState(activity, outState, outPersistentState);
            } else {
                root.callActivityOnSaveInstanceState(activity, outState, outPersistentState);
            }
        } finally {
            avoidRecursive.finishCall(17);
        }
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        try {
            if (avoidRecursive.beginCall(18)) {
                base.callActivityOnPause(activity);
            } else {
                root.callActivityOnPause(activity);
            }
        } finally {
            avoidRecursive.finishCall(18);
        }
    }

    @Override
    public void callActivityOnUserLeaving(Activity activity) {
        try {
            if (avoidRecursive.beginCall(19)) {
                base.callActivityOnUserLeaving(activity);
            } else {
                root.callActivityOnUserLeaving(activity);
            }
        } finally {
            avoidRecursive.finishCall(19);
        }
    }

    @Override
    public Bundle getAllocCounts() {
        return root.getAllocCounts();
    }

    @Override
    public Bundle getBinderCounts() {
        return root.getBinderCounts();
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public UiAutomation getUiAutomation() {
        return root.getUiAutomation();
    }

    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Activity activity, Intent intent, int i, Bundle bundle) throws Throwable {
        try {
            if (avoidRecursive.beginCall(20)) {
                return (ActivityResult) findDeclaredMethod(base, "execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, Integer.TYPE, Bundle.class).invoke(base, new Object[]{context, iBinder, iBinder2, activity, intent, i, bundle});
            } else {
                return (ActivityResult) findDeclaredMethod(root, "execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, Integer.TYPE, Bundle.class).invoke(base, new Object[]{context, iBinder, iBinder2, activity, intent, i, bundle});
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            avoidRecursive.finishCall(20);
        }
    }

    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, String str, Intent intent, int i, Bundle bundle) throws Throwable {
        try {
            if (avoidRecursive.beginCall(21)) {
                return (ActivityResult) findDeclaredMethod(base, "execStartActivity", Context.class, IBinder.class, IBinder.class, String.class, Intent.class, Integer.TYPE, Bundle.class).invoke(base, new Object[]{context, iBinder, iBinder2, str, intent, i, bundle});
            } else {
                return (ActivityResult) findDeclaredMethod(root, "execStartActivity", Context.class, IBinder.class, IBinder.class, String.class, Intent.class, Integer.TYPE, Bundle.class).invoke(base, new Object[]{context, iBinder, iBinder2, str, intent, i, bundle});
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            avoidRecursive.finishCall(21);
        }
    }

    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Fragment fragment, Intent intent, int i) throws Throwable {
        try {
            if (avoidRecursive.beginCall(22)) {
                return (ActivityResult) findDeclaredMethod(base, "execStartActivity", Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class, Integer.TYPE).invoke(base, new Object[]{context, iBinder, iBinder2, fragment, intent, i});
            } else {
                return (ActivityResult) findDeclaredMethod(root, "execStartActivity", Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class, Integer.TYPE).invoke(base, new Object[]{context, iBinder, iBinder2, fragment, intent, i});
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            avoidRecursive.finishCall(22);
        }
    }

    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Activity activity, Intent intent, int i) throws Throwable {
        try {
            if (avoidRecursive.beginCall(23)) {
                return (ActivityResult) findDeclaredMethod(base, "execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, Integer.TYPE).invoke(base, new Object[]{context, iBinder, iBinder2, activity, intent, i});
            } else {
                return (ActivityResult) findDeclaredMethod(root, "execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, Integer.TYPE).invoke(base, new Object[]{context, iBinder, iBinder2, activity, intent, i});
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            avoidRecursive.finishCall(23);
        }

    }

    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Fragment fragment, Intent intent, int i, Bundle bundle) throws Throwable {
        try {
            if (avoidRecursive.beginCall(24)) {
                return (ActivityResult) findDeclaredMethod(base, "execStartActivity", Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class, Integer.TYPE, Bundle.class).invoke(base, new Object[]{context, iBinder, iBinder2, fragment, intent, i, bundle});
            } else {
                return (ActivityResult) findDeclaredMethod(root, "execStartActivity", Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class, Integer.TYPE, Bundle.class).invoke(base, new Object[]{context, iBinder, iBinder2, fragment, intent, i, bundle});
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            avoidRecursive.finishCall(24);
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public ActivityResult execStartActivity(Context context, IBinder iBinder, IBinder iBinder2, Activity activity, Intent intent, int i, Bundle bundle, UserHandle userHandle) throws Throwable {
        try {
            if (avoidRecursive.beginCall(25)) {
                return (ActivityResult) findDeclaredMethod(base, "execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, Integer.TYPE, Bundle.class, UserHandle.class).invoke(base, new Object[]{context, iBinder, iBinder2, activity, intent, i, bundle, userHandle});
            } else {
                return (ActivityResult) findDeclaredMethod(root, "execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, Integer.TYPE, Bundle.class, UserHandle.class).invoke(base, new Object[]{context, iBinder, iBinder2, activity, intent, i, bundle, userHandle});
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            avoidRecursive.finishCall(25);
        }
    }

    private static Method findDeclaredMethod(Object obj, String name, Class<?>... args) throws NoSuchMethodException {
        Class<?> cls = obj.getClass();
        while (cls != null) {
            try {
                Method method = cls.getDeclaredMethod(name, args);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method;
            } catch (NoSuchMethodException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Method " + name + " with parameters " + Arrays.asList(args) + " not found in " + obj.getClass());
    }
}
