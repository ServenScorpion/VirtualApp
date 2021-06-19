package com.lody.virtual.server.interfaces;

import android.app.IServiceConnection;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.lody.virtual.remote.ClientConfig;

import com.lody.virtual.remote.AppRunningProcessInfo;
import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.remote.BadgerInfo;
import com.lody.virtual.remote.IntentSenderData;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.remote.VParceledListSlice;
import com.lody.virtual.remote.ServiceResult;

/**
 * @author Lody
 */
interface IActivityManager{

    ClientConfig initProcess(String packageName, String processName, int userId, int type);

    void appDoneExecuting(in String packageName, int userId);

    int getFreeStubCount();

    int checkPermission(boolean is64bit, String permission, int pid, int uid);

    int getSystemPid();

    int getSystemUid();

    int getUidByPid(int pid);

    boolean isAppProcess(String processName);

    boolean isAppRunning(String packageName, int userId, boolean foreground);

    boolean isAppPid(int pid);

    String getAppProcessName(int pid);

    java.util.List<String> getProcessPkgList(int pid);

    void killAllApps();

    void killAppByPkg(String pkg, int userId);

    void killApplicationProcess(String processName, int vuid);

    void dump();

    String getInitialPackage(int pid);

    int startActivities(in Intent[] intents,in  String[] resolvedTypes,in  IBinder token,in  Bundle options, int userId);

    int startActivity(in Intent intent,in  ActivityInfo info,in  IBinder resultTo,in  Bundle options, String resultWho, int requestCode, int userId);

    boolean finishActivityAffinity(int userId, in IBinder token);

    void onActivityCreated(in IBinder record, in IBinder token, int taskId);

    void onActivityResumed(int userId,in  IBinder token);

    boolean onActivityDestroyed(int userId,in  IBinder token);

    void onActivityFinish(int userId, in IBinder token);

    ComponentName getActivityClassForToken(int userId,in  IBinder token);

    String getCallingPackage(int userId,in  IBinder token);

    int getCallingUidByPid(int pid);

    ComponentName getCallingActivity(int userId,in  IBinder token);

    AppTaskInfo getTaskInfo(int taskId);

    String getPackageForToken(int userId,in  IBinder token);

    VParceledListSlice getServices(int maxNum, int flags, int userId);

    IBinder acquireProviderClient(int userId,in  ProviderInfo info);

    void addOrUpdateIntentSender(in IntentSenderData sender, int userId);

    void removeIntentSender(in IBinder token);

    IntentSenderData getIntentSender(in IBinder token);

    void processRestarted(String packageName, String processName, int userId);

    void notifyBadgerChange(in BadgerInfo info);

    void setAppInactive(String packageName, boolean idle, int userId);

    boolean isAppInactive(String packageName, int userId);

    ComponentName startService(int userId, in Intent service);

    void stopService(int appUserId, in ServiceInfo serviceInfo);

    void unbindService(int userId, in IBinder token);

    Intent bindService(int userId, in Intent intent, in ServiceInfo serviceInfo, in IBinder binder, in int flags);

    void onServiceStartCommand(int userId, int startId, in ServiceInfo serviceInfo, in Intent intent);

    int onServiceStop(int userId, in ComponentName component, int targetStartId);

    void onServiceDestroyed(int userId, in ComponentName component);

    ServiceResult onServiceUnBind(int userId, in ComponentName component);

    void setServiceForeground(in ComponentName component, int userId, int id, String tag, boolean cancel);

    void handleDownloadCompleteIntent(in Intent intent);

    int getRunningAppMemorySize(String packageName, int userId);

    void closeAllLongSocket(String packageName, int userId);

    void broadcastFinish(in PendingResultData res, int userId);

    List<AppRunningProcessInfo> getRunningAppProcesses(String packageName, int userId);

    boolean includeExcludeFromRecentsFlag(in IBinder token);

    void onBackHome();

    long getLastBackHomeTime();

    void finishAllActivities();
}
