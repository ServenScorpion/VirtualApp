package com.lody.virtual.client.hook.proxies.am;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.AvoidRecursive;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.StubActivityRecord;

import java.util.List;

import mirror.android.app.ActivityManagerNative;
import mirror.android.app.ActivityThread;
import mirror.android.app.ClientTransactionHandler;
import mirror.android.app.IActivityManager;
import mirror.android.app.servertransaction.ActivityResultItem;
import mirror.android.app.servertransaction.ClientTransaction;
import mirror.android.app.servertransaction.LaunchActivityItem;
import mirror.android.app.servertransaction.TopResumedActivityChangeItem;

/**
 * @author Lody
 * @see Handler.Callback
 */
public class HCallbackStub implements Handler.Callback, IInjector {


    private static final int LAUNCH_ACTIVITY;
    private static final int EXECUTE_TRANSACTION;
    private static final int SCHEDULE_CRASH = ActivityThread.H.SCHEDULE_CRASH.get();

    static {
        LAUNCH_ACTIVITY = BuildCompat.isPie() ? -1 : ActivityThread.H.LAUNCH_ACTIVITY.get();
        EXECUTE_TRANSACTION = BuildCompat.isPie() ? ActivityThread.H.EXECUTE_TRANSACTION.get() : -1;
    }

    private static final String TAG = HCallbackStub.class.getSimpleName();
    private static final HCallbackStub sCallback = new HCallbackStub();

    private final AvoidRecursive mAvoidRecurisve = new AvoidRecursive();


    private Handler.Callback otherCallback;

    private HCallbackStub() {
    }

    public static HCallbackStub getDefault() {
        return sCallback;
    }

    private static Handler getH() {
        return ActivityThread.mH.get(VirtualCore.mainThread());
    }

    private static Handler.Callback getHCallback() {
        try {
            Handler handler = getH();
            return mirror.android.os.Handler.mCallback.get(handler);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (mAvoidRecurisve.beginCall()) {
            try {
                if (LAUNCH_ACTIVITY == msg.what) {
                    if (!handleLaunchActivity(msg, msg.obj)) {
                        return true;
                    }
                } else if (BuildCompat.isPie() && EXECUTE_TRANSACTION == msg.what) {
                    if (!handleExecuteTransaction(msg)) {
                        return true;
                    }
                } else if (SCHEDULE_CRASH == msg.what) {
                    String crashReason = (String) msg.obj;
                    new RemoteException(crashReason).printStackTrace();
                    return true;
                }
                if (otherCallback != null) {
                    return otherCallback.handleMessage(msg);
                }
            } finally {
                mAvoidRecurisve.finishCall();
            }
        }
        return false;
    }

    private boolean handleExecuteTransaction(Message msg) {
        Object transaction = msg.obj;
        IBinder token = ClientTransaction.mActivityToken.get(transaction);
        Object r = ClientTransactionHandler.getActivityClient.call(VirtualCore.mainThread(), token);
        if (r == null) {
            List<Object> activityCallbacks = ClientTransaction.mActivityCallbacks.get(transaction);
            if (activityCallbacks == null || activityCallbacks.isEmpty()) {
                return true;
            }
            Object item = activityCallbacks.get(0);
            if (item.getClass() == LaunchActivityItem.TYPE) {
               return handleLaunchActivity(msg, item);
            } else if(item.getClass() == ActivityResultItem.TYPE){
                if(handleActivityResult(msg, item)){
                    return false;
                }
            }
            return true;
        } else if (BuildCompat.isQ()) {
            List<Object> activityCallbacks = ClientTransaction.mActivityCallbacks.get(transaction);
            if (activityCallbacks != null && !activityCallbacks.isEmpty()) {
                Object item = activityCallbacks.get(0);
                if (item.getClass() == TopResumedActivityChangeItem.TYPE) {
                    if (TopResumedActivityChangeItem.mOnTop.get(item) == ActivityThread.ActivityClientRecord.isTopResumedActivity.get(r)) {
                        VLog.e("HCallbackStub", "Activity top position already set to onTop=" + TopResumedActivityChangeItem.mOnTop.get(item));
                        return false;
                    }
                } else if(item.getClass() == ActivityResultItem.TYPE){
                    if(handleActivityResult(msg, item)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean handleActivityResult(Message msg, Object r){
        //TODO
        return false;
    }

    private boolean handleLaunchActivity(Message msg, Object r) {
        Intent stubIntent;
        if (BuildCompat.isPie()) {
            stubIntent = LaunchActivityItem.mIntent.get(r);
        } else {
            stubIntent = ActivityThread.ActivityClientRecord.intent.get(r);
        }
        StubActivityRecord saveInstance = new StubActivityRecord(stubIntent);
        if (saveInstance.intent == null) {
            return true;
        }
        Intent intent = saveInstance.intent;
        IBinder token;
        if (BuildCompat.isPie()) {
            token = ClientTransaction.mActivityToken.get(msg.obj);
        } else {
            token = ActivityThread.ActivityClientRecord.token.get(r);
        }
        ActivityInfo info = saveInstance.info;
        if (info == null) {
            return true;
        }
        if (VClient.get().getClientConfig() == null) {
            InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
            if (installedAppInfo == null) {
                return true;
            }
            VActivityManager.get().processRestarted(info.packageName, info.processName, saveInstance.userId);
            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
            return false;
        }
        if (!VClient.get().isAppRunning()) {
            VClient.get().bindApplication(info.packageName, info.processName);
            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
            return false;
        }
        int taskId = IActivityManager.getTaskForActivity.call(
                ActivityManagerNative.getDefault.call(),
                token,
                false
        );
        if (info.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            try {
                IActivityManager.setRequestedOrientation.call(ActivityManagerNative.getDefault.call(),
                        token, info.screenOrientation);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        VActivityManager.get().onActivityCreate(saveInstance.virtualToken, token, taskId);
        ClassLoader appClassLoader = VClient.get().getClassLoader(info.applicationInfo);
        intent.setExtrasClassLoader(appClassLoader);
        if (BuildCompat.isPie()) {
            LaunchActivityItem.mIntent.set(r, intent);
            LaunchActivityItem.mInfo.set(r, info);
        } else {
            ActivityThread.ActivityClientRecord.intent.set(r, intent);
            ActivityThread.ActivityClientRecord.activityInfo.set(r, info);
        }
        return true;
    }

    @Override
    public void inject() {
        otherCallback = getHCallback();
        mirror.android.os.Handler.mCallback.set(getH(), this);
    }

    @Override
    public boolean isEnvBad() {
        Handler.Callback callback = getHCallback();
        boolean envBad = callback != this;
        if (callback != null && envBad) {
            VLog.d(TAG, "HCallback has bad, other callback = " + callback);
        }
        return envBad;
    }

}
