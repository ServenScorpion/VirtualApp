package com.lody.virtual.client.ipc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Process;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.text.TextUtils;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.hook.secondary.VAContentProviderProxy;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.AppRunningProcessInfo;
import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.remote.BadgerInfo;
import com.lody.virtual.remote.BroadcastIntentData;
import com.lody.virtual.remote.ClientConfig;
import com.lody.virtual.remote.IntentSenderData;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.remote.ServiceResult;
import com.lody.virtual.remote.VParceledListSlice;
import com.lody.virtual.server.bit64.V64BitHelper;
import com.lody.virtual.server.interfaces.IActivityManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import mirror.android.app.ActivityThread;
import mirror.android.content.ContentProviderNative;

/**
 * @author Lody
 */
public class VActivityManager {

    public static final int PROCESS_TYPE_OTHER = 0x0;
    public static final int PROCESS_TYPE_ACTIVITY = 0x1;
    public static final int PROCESS_TYPE_SERVICE = 0x20;
    public static final int PROCESS_TYPE_SERVICE_BIND = 0x21;
    public static final int PROCESS_TYPE_PROVIDER = 0x40;
    public static final int PROCESS_TYPE_RECEIVER = 0x80;
    public static final int PROCESS_TYPE_RESTART = 0x100;
    @IntDef({
            PROCESS_TYPE_OTHER,
            PROCESS_TYPE_ACTIVITY,
            PROCESS_TYPE_SERVICE,
            PROCESS_TYPE_SERVICE_BIND,
            PROCESS_TYPE_PROVIDER,
            PROCESS_TYPE_RECEIVER,
            PROCESS_TYPE_RESTART
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProcessStartType {
    }

    private static final VActivityManager sAM = new VActivityManager();
    private IActivityManager mService;

    public IActivityManager getService() {
        if (!IInterfaceUtils.isAlive(mService)) {
            synchronized (VActivityManager.class) {
                final Object remote = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IActivityManager.class, remote);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return IActivityManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.ACTIVITY));
    }

    public static VActivityManager get() {
        return sAM;
    }

    public int startActivity(Intent intent, ActivityInfo info, IBinder resultTo, Bundle options, String resultWho, int requestCode, int userId) {
        if (info == null) {
            info = VirtualCore.get().resolveActivityInfo(intent, userId);
            if (info == null) {
                return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
            }
        }
        try {
            return getService().startActivity(intent, info, resultTo, options, resultWho, requestCode, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int startActivities(Intent[] intents, String[] resolvedTypes, IBinder token, Bundle options, int userId) {
        try {
            return getService().startActivities(intents, resolvedTypes, token, options, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int startActivity(Intent intent, int userId) {
        if (userId < 0) {
            return ActivityManagerCompat.START_NOT_CURRENT_USER_ACTIVITY;
        }
        ActivityInfo info = VirtualCore.get().resolveActivityInfo(intent, userId);
        if (info == null) {
            return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
        }
        return startActivity(intent, info, null, null, null, 0, userId);
    }

    public void appDoneExecuting(String packageName) {
        try {
            getService().appDoneExecuting(packageName, VUserHandle.myUserId());
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void onActivityCreate(IBinder record, IBinder token, int taskId) {
        try {
            getService().onActivityCreated(record, token, taskId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onActivityResumed(IBinder token) {
        try {
            getService().onActivityResumed(VUserHandle.myUserId(), token);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean onActivityDestroy(IBinder token) {
        try {
            return getService().onActivityDestroyed(VUserHandle.myUserId(), token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public AppTaskInfo getTaskInfo(int taskId) {
        try {
            return getService().getTaskInfo(taskId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public ComponentName getCallingActivity(IBinder token) {
        try {
            return getService().getCallingActivity(VUserHandle.myUserId(), token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public String getCallingPackage(IBinder token) {
        try {
            return getService().getCallingPackage(VUserHandle.myUserId(), token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public String getPackageForToken(IBinder token) {
        try {
            return getService().getPackageForToken(VUserHandle.myUserId(), token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public ComponentName getActivityForToken(IBinder token) {
        try {
            return getService().getActivityClassForToken(VUserHandle.myUserId(), token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }


    public VParceledListSlice getServices(int maxNum, int flags) {
        try {
            return getService().getServices(maxNum, flags, VUserHandle.myUserId());
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void processRestarted(String packageName, String processName, int userId) {
        try {
            getService().processRestarted(packageName, processName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getAppProcessName(int pid) {
        try {
            return getService().getAppProcessName(pid);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public String getInitialPackage(int pid) {
        try {
            return getService().getInitialPackage(pid);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isAppProcess(String processName) {
        try {
            return getService().isAppProcess(processName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getRunningAppMemorySize(String packageName, int userId) {
        try {
            return getService().getRunningAppMemorySize(packageName, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void killAllApps() {
        try {
            getService().killAllApps();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void killApplicationProcess(String procName, int uid) {
        try {
            getService().killApplicationProcess(procName, uid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void killAppByPkg(String pkg, int userId) {
        try {
            getService().killAppByPkg(pkg, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<String> getProcessPkgList(int pid) {
        try {
            return getService().getProcessPkgList(pid);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isAppPid(int pid) {
        try {
            return getService().isAppPid(pid);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getUidByPid(int pid) {
        try {
            return getService().getUidByPid(pid);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getSystemPid() {
        try {
            return getService().getSystemPid();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getSystemUid() {
        try {
            return getService().getSystemUid();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void sendCancelActivityResult(IBinder resultTo, String resultWho, int requestCode) {
        //TODO sendActivityResult好像无效
        sendActivityResult(resultTo, resultWho, requestCode, null, Activity.RESULT_CANCELED);
    }

    public void sendActivityResult(IBinder resultTo, String resultWho, int requestCode, Intent data, int resultCode) {
        Activity activity = findActivityByToken(resultTo);
        if (activity != null) {
            Object mainThread = VirtualCore.mainThread();
            ActivityThread.sendActivityResult.call(mainThread, resultTo, resultWho, requestCode, data, resultCode);
        }
    }

    public void sendActivityResultLocal(IBinder resultTo, String resultWho, int requestCode, Intent data, int resultCode) {
        Activity activity = findActivityByToken(resultTo);
        if (activity != null) {
            try {
                mirror.android.app.Activity.onActivityResult.call(activity, requestCode, resultCode, data);
            }catch (Throwable e){
                VLog.e("ActivityManager", "onActivityResult:\r\n%s", VLog.getStackTraceString(e));
            }
        }
    }

    public IInterface acquireProviderClient(int userId, ProviderInfo info) throws RemoteException {
        return acquireProviderClient(userId, info, 0,0, null);
    }

    public IInterface acquireProviderClient(int userId, ProviderInfo info, int uid, int pid, String pkg) throws RemoteException {
        IBinder binder = getService().acquireProviderClient(userId, info);
        if (binder != null) {
            IInterface contentProvider = ContentProviderNative.asInterface.call(binder);
            if (uid == 0 || pid == 0 || TextUtils.isEmpty(pkg)) {
                return contentProvider;
            }
            return VAContentProviderProxy.wrapper(contentProvider, uid, pid, pkg);
        }
        return null;
    }

    public void addOrUpdateIntentSender(IntentSenderData sender) throws RemoteException {
        getService().addOrUpdateIntentSender(sender, VUserHandle.myUserId());
    }

    public void removeIntentSender(IBinder token) throws RemoteException {
        getService().removeIntentSender(token);
    }

    public IntentSenderData getIntentSender(IBinder token) {
        try {
            return getService().getIntentSender(token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public Activity findActivityByToken(IBinder token) {
        Object r = ActivityThread.mActivities.get(VirtualCore.mainThread()).get(token);
        if (r != null) {
            return ActivityThread.ActivityClientRecord.activity.get(r);
        }
        return null;
    }


    public void finishActivity(IBinder token) {
        Activity activity = findActivityByToken(token);
        if (activity == null) {
            VLog.e("VActivityManager", "finishActivity fail : activity = null");
            return;
        }
        while (true) {
            // We shouldn't use Activity.getParent(),
            // because It may be overwritten.
            Activity parent = mirror.android.app.Activity.mParent.get(activity);
            if (parent == null) {
                break;
            }
            activity = parent;
        }
        // We shouldn't use Activity.isFinishing(),
        // because it may be overwritten.
        int resultCode = mirror.android.app.Activity.mResultCode.get(activity);
        Intent resultData = mirror.android.app.Activity.mResultData.get(activity);
        ActivityManagerCompat.finishActivity(token, resultCode, resultData);
        mirror.android.app.Activity.mFinished.set(activity, true);
    }

    public boolean isAppRunning(String packageName, int userId, boolean foreground) {
        try {
            return getService().isAppRunning(packageName, userId, foreground);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getUid() {
        return VClient.get().getVUid();
    }

    public ClientConfig initProcess(String packageName, String processName, int userId, @ProcessStartType int type) {
        try {
            return getService().initProcess(packageName, processName, userId, type);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void sendBroadcast(Intent intent, int userId) {
        sendBroadcast(intent, userId, BroadcastIntentData.TYPE_FROM_SYSTEM);
    }

    /**
     * @see BroadcastIntentData#TYPE_APP
     * @see BroadcastIntentData#TYPE_FROM_SYSTEM
     * @see BroadcastIntentData#TYPE_FROM_INTENT_SENDER
     */
    public void sendBroadcast(Intent intent, int userId, int flags) {
        Intent newIntent = ComponentUtils.redirectBroadcastIntent(intent, userId, flags);
        if (newIntent != null) {
            VirtualCore.get().getContext().sendBroadcast(newIntent);
        }
    }

    public void notifyBadgerChange(BadgerInfo info) {
        try {
            getService().notifyBadgerChange(info);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public int getCallingUid() {
        try {
            int id = getService().getCallingUidByPid(Process.myPid());
            if (id <= 0) {
                return VClient.get().getVUid();
            }
            return id;
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
        return VClient.get().getVUid();
    }

    public void closeAllLongSocket(String packageName, int userId) {
        try {
            getService().closeAllLongSocket(packageName, userId);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void setAppInactive(String packageName, boolean idle, int userId) {
        try {
            getService().setAppInactive(packageName, idle, userId);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isAppInactive(String packageName, int userId) {
        try {
            return getService().isAppInactive(packageName, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void onBackHome(){
        try {
            getService().onBackHome();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public long getLastBackHomeTime() {
        try {
            return getService().getLastBackHomeTime();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean launchApp(final int userId, String packageName) {
        return launchApp(userId, packageName, true);
    }

    public boolean launchApp(final int userId, final String packageName, boolean preview) {
        if (VirtualCore.get().isRun64BitProcess(packageName)) {
            if (!V64BitHelper.has64BitEngineStartPermission()) {
                return false;
            }
        }
        Context context = VirtualCore.get().getContext();
        VPackageManager pm = VPackageManager.get();
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, userId);

        // Otherwise, try to find a main launcher activity.
        if (ris == null || ris.size() <= 0) {
            // reuse the intent instance
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, userId);
        }
        if (ris == null || ris.size() <= 0) {
            return false;
        }
        final ActivityInfo info = ris.get(0).activityInfo;
        final Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(info.packageName, info.name);
        //1.va的进程初始化500ms
        //2.app的Application初始化，这个要看app
        //3.app的4组件初始化
        if (!preview || VActivityManager.get().isAppRunning(info.packageName, userId, true)) {
            VLog.d("kk", "app's main thread was running.");
            VActivityManager.get().startActivity(intent, userId);
        } else {
            VLog.d("kk", "app's main thread not running.");
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            final VirtualCore.UiCallback callBack = new VirtualCore.UiCallback() {
                private boolean mLaunched;

                @Override
                public void onAppOpened(String packageName, int userId) {
                    VLog.d("WindowPreviewActivity", "onAppOpened:" + packageName);
                    synchronized (this) {
                        mLaunched = true;
                    }
                }

                @Override
                public boolean isLaunched(String packageName, int userId) {
                    synchronized (this) {
                        return mLaunched;
                    }
                }
            };

            VirtualCore.getConfig().startPreviewActivity(userId, info, callBack);
            VirtualCore.get().setUiCallback(intent, callBack);
            final String processName = ComponentUtils.getProcessName(info);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //wait 500ms
                    ClientConfig clientConfig = initProcess(packageName, processName, userId, PROCESS_TYPE_ACTIVITY);
                    if (clientConfig != null) {
                        VActivityManager.get().startActivity(intent, userId);
                        //VActivityManager#startActivity启动速度比WindowPreviewActivity快
                    }
                }
            }).start();
        }
        return true;
    }

    public void onFinishActivity(IBinder token) {
        try {
            getService().onActivityFinish(VUserHandle.myUserId(), token);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public int checkPermission(String permission, int pid, int uid) {
        try {
            return getService().checkPermission(VirtualCore.get().isPluginEngine(), permission, pid, uid);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public ComponentName startService(int userId, Intent service) {
        try {
            return getService().startService(userId, service);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void unbindService(int userId, IBinder token) throws RemoteException {
        getService().unbindService(userId, token);
    }

    public Intent bindService(int userId, Intent intent, ServiceInfo serviceInfo, IBinder binder, int flags) throws RemoteException {
        return getService().bindService(userId, intent, serviceInfo, binder, flags);
    }

    public void onServiceStartCommand(int userId, int startId, ServiceInfo serviceInfo, Intent intent) {
        try {
            getService().onServiceStartCommand(userId, startId, serviceInfo, intent);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void onServiceDestroyed(int userId, ComponentName component) {
        try {
            getService().onServiceDestroyed(userId, component);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public ServiceResult onServiceUnBind(int userId, ComponentName component) {
        try {
            return getService().onServiceUnBind(userId, component);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void stopService(int userId, ServiceInfo serviceInfo) throws RemoteException {
        getService().stopService(userId, serviceInfo);
    }

    public int onServiceStop(int userId, ComponentName component, int targetStartId) throws RemoteException {
        return getService().onServiceStop(userId, component, targetStartId);
    }

    public void handleDownloadCompleteIntent(Intent intent) {
        try {
            getService().handleDownloadCompleteIntent(intent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean finishActivityAffinity(int userId, IBinder token) {
        try {
            return getService().finishActivityAffinity(userId, token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void broadcastFinish(PendingResultData res) {
        try {
            getService().broadcastFinish(res, VUserHandle.myUserId());
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public static String getTypeString(@ProcessStartType int type) {
        switch (type) {

            case PROCESS_TYPE_ACTIVITY:
                return "activity";
            case PROCESS_TYPE_SERVICE:
                return "Service";
            case PROCESS_TYPE_SERVICE_BIND:
                return "BindService";
            case PROCESS_TYPE_PROVIDER:
                return "provider";
            case PROCESS_TYPE_RECEIVER:
                return "receiver";
            case PROCESS_TYPE_RESTART:
                return "restart";
            case PROCESS_TYPE_OTHER:
            default:
                return "other";
        }
    }

    public List<AppRunningProcessInfo> getRunningAppProcesses(String packageName, int userId) {
        try {
            return getService().getRunningAppProcesses(packageName, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void setServiceForeground(ComponentName component, int userId, int id, String tag, boolean cancel){
        try {
            getService().setServiceForeground(component, userId, id, tag, cancel);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean includeExcludeFromRecentsFlag(IBinder token) {
        try {
            return getService().includeExcludeFromRecentsFlag(token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void finishAllActivities(){
        try {
            getService().finishAllActivities();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
}
