package com.lody.virtual.client.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.R;
import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.VClient;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.delegate.TaskDescriptionDelegate;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.HiddenForeNotification;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.utils.BitmapUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.BroadcastIntentData;
import com.lody.virtual.remote.InstallOptions;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.bit64.V64BitHelper;
import com.lody.virtual.server.interfaces.IAppManager;
import com.lody.virtual.server.interfaces.IPackageObserver;
import com.lody.virtual.server.pm.PrivilegeAppOptimizer;
import com.xdja.activitycounter.ActivityCounterManager;
import com.xdja.call.CallLogObserver;
import com.xdja.call.PhoneCallService;
import com.xdja.mms.SmsObserver;
import com.xdja.utils.PackagePermissionManager;
import com.xdja.zs.IAppPermissionCallback;
import com.xdja.zs.IControllerServiceCallback;
import com.xdja.zs.INotificationCallback;
import com.xdja.zs.IToastCallback;
import com.xdja.zs.IUiCallback;
import com.xdja.zs.IVSCallback;
import com.xdja.zs.IVSKeyCallback;
import com.xdja.zs.exceptionRecorder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import mirror.android.app.ActivityThread;

/**
 * @author Lody
 * @version 3.5
 */
public final class VirtualCore {

    public static final int GET_HIDDEN_APP = 0x00000001;
    private static final String TAG = VirtualCore.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static VirtualCore gCore = new VirtualCore();
    private final int myUid = Process.myUid();
    /**
     * Client Package Manager
     */
    private PackageManager unHookPackageManager;
    /**
     * Host package name
     */
    private String hostPkgName;
    /**
     * ActivityThread instance
     */
    private Object mainThread;
    private Context context;
    /**
     * Main ProcessName
     */
    private String mainProcessName;
    /**
     * Real Process Name
     */
    private String processName;
    private ProcessType processType;
    private boolean isHostPulginApp;
    private IAppManager mService;
    private boolean isStartUp;
    private PackageInfo mHostPkgInfo;
    private ConditionVariable mInitLock;
    private AppCallback mAppCallback;
    private TaskDescriptionDelegate mTaskDescriptionDelegate;
    private SettingConfig mConfig;
    private AppRequestListener mAppRequestListener;

    private VirtualCore() {
    }

    public static SettingConfig getConfig() {
        return get().mConfig;
    }

    public static VirtualCore get() {
        return gCore;
    }

    public static PackageManager getPM() {
        return get().getPackageManager();
    }

    public static Object mainThread() {
        if (BuildConfig.DEBUG) {
            if (get().isMainProcess()) {
                throw new RuntimeException("get ActivityThread on Main Process.");
            }
        }
        return get().mainThread;
    }

    public void gotoBackHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.putExtra("pid", Process.myPid());
        home.putExtra("process", get().getProcessName());
        home.putExtra("userId", VUserHandle.myUserId());
        try {
            getContext().startActivity(home);
        } catch (Throwable ignore) {
        }
    }

    public ConditionVariable getInitLock() {
        return mInitLock;
    }

    public int myUid() {
        return myUid;
    }

    public int myUserId() {
        return VUserHandle.getUserId(myUid);
    }

    public AppCallback getAppCallback() {
        return mAppCallback == null ? AppCallback.EMPTY : mAppCallback;
    }

    /**
     * @see #setAppCallback
     * @deprecated
     */
    public void setComponentDelegate(AppCallback delegate) {
        setAppCallback(delegate);
    }

    public void setAppCallback(AppCallback delegate) {
        this.mAppCallback = delegate;
    }

    public void setCrashHandler(CrashHandler handler) {
        VClient.get().setCrashHandler(handler);
    }

    public TaskDescriptionDelegate getTaskDescriptionDelegate() {
        return mTaskDescriptionDelegate;
    }

    public void setTaskDescriptionDelegate(TaskDescriptionDelegate mTaskDescriptionDelegate) {
        this.mTaskDescriptionDelegate = mTaskDescriptionDelegate;
    }

    public int[] getGids() {
        return mHostPkgInfo.gids;
    }

    public ApplicationInfo getHostApplicationInfo() {
        return mHostPkgInfo.applicationInfo;
    }

    public Context getContext() {
        return context;
    }

    public PackageManager getPackageManager() {
        return context.getPackageManager();
    }

    public boolean isSystemApp() {
        ApplicationInfo applicationInfo = getContext().getApplicationInfo();
        return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public String getHostPkg() {
        return hostPkgName;
    }

    public int getTargetSdkVersion() {
        return context.getApplicationInfo().targetSdkVersion;
    }

    public PackageManager getUnHookPackageManager() {
        return unHookPackageManager;
    }

    public boolean checkSelfPermission(String permission, boolean is64Bit) {
        if (is64Bit) {
            return PackageManager.PERMISSION_GRANTED == unHookPackageManager.checkPermission(permission, StubManifest.PACKAGE_NAME_64BIT);
        } else {
            return PackageManager.PERMISSION_GRANTED == unHookPackageManager.checkPermission(permission, StubManifest.PACKAGE_NAME);
        }
    }

    public void waitStartup() {
        if (mInitLock != null) {
            mInitLock.block();
        }
    }

    public int getUidForSharedUser(String sharedUserName) {
        try {
            return getService().getUidForSharedUser(sharedUserName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    private final BroadcastReceiver mDownloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            VLog.w("DownloadManager", "receive download completed brodcast: " + intent);
            intent.setExtrasClassLoader(BroadcastIntentData.class.getClassLoader());
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                VActivityManager.get().handleDownloadCompleteIntent(intent);
            }
        }
    };

    public void startup(Context context, SettingConfig config) throws Throwable {
        if (!isStartUp) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException("VirtualCore.startup() must called in main thread.");
            }
            mInitLock = new ConditionVariable();
            mConfig = config;
            String packageName = config.getHostPackageName();
            String packageName64 = config.getPluginEnginePackageName();
            Constants.ACTION_SHORTCUT = packageName + Constants.ACTION_SHORTCUT;
            Constants.ACTION_BADGER_CHANGE = packageName + Constants.ACTION_BADGER_CHANGE;

            StubManifest.PACKAGE_NAME = packageName;
            StubManifest.STUB_CP_AUTHORITY = packageName + ".virtual_stub_";
            StubManifest.PROXY_CP_AUTHORITY = packageName + ".provider_proxy";
            StubManifest.PROXY_CP_AUTHORITY_OUTSIDE =  packageName + ".provider_outside";

            if (packageName64 == null) {
                packageName64 = "NO_64BIT";
            }
            StubManifest.PACKAGE_NAME_64BIT = packageName64;
            StubManifest.STUB_CP_AUTHORITY_64BIT = packageName64 + ".virtual_stub_64bit_";
            StubManifest.PROXY_CP_AUTHORITY_64BIT = packageName64 + ".provider_proxy_64bit";

            this.context = context;
            unHookPackageManager = context.getPackageManager();
            mHostPkgInfo = unHookPackageManager.getPackageInfo(packageName, PackageManager.GET_GIDS);

            NativeEngine.bypassHiddenAPIEnforcementPolicyIfNeeded();
            //////////////////////////////
            // Now we can use hidden API//
            //////////////////////////////

            detectProcessType();
            if (isMainProcess()) {
                CallLogObserver.observe();
                SmsObserver.observe();
            }
            if (isServerProcess() || isVAppProcess()) {
                mainThread = ActivityThread.currentActivityThread.call();
            }
            if (isPluginEngine()) {
                VLog.e(TAG, "===========  64Bit Engine(%s) ===========", processType.name());
                if (isVAppProcess()) {
                    getService().asBinder().linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            VLog.e(TAG, "32Bit Engine was dead, kill app process.");
                            Process.killProcess(Process.myPid());
                        }
                    }, 0);
                }
            }
            if (isServerProcess() || is64bitHelperProcess()) {
                VLog.w("DownloadManager", "Listening DownloadManager action  in process: " + processType);
                IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                try {
                    context.registerReceiver(mDownloadCompleteReceiver, filter);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            InvocationStubManager invocationStubManager = InvocationStubManager.getInstance();
            invocationStubManager.init();
            invocationStubManager.injectAll();
            ContextFixer.fixContext(context);
            isStartUp = true;

            //xdja 屏蔽‘不兼容api’对话框    (进一步确保）    暂时屏蔽，观察效果。
            /*try {
                Reflect.on("android.app.ActivityThread").call("currentActivityThread").set("mHiddenApiWarningShown", true);
            } catch (Exception e)
            {
                e.printStackTrace();
            }*/

            mInitLock.open();
        }
    }

    public void waitForEngine() {
        ServiceManagerNative.ensureServerStarted();
    }

    //Add by xdja
    public void preLaunchApp() {
        if (shouldLaunchApp("com.xdja.dialer")) {
            VirtualCore.get().context.startService(new Intent(VirtualCore.get().context, PhoneCallService.class));
        }

    }

    //Add by xdja
    public void sendBootCompleteBC(final String packageName, String action, boolean update) {
        if (shouldLaunchApp(packageName)) {
            PrivilegeAppOptimizer.get().addPrivilegeApp(packageName);
            PrivilegeAppOptimizer.get().performOptimizeAllApps();
            PrivilegeAppOptimizer.get().removePrivilegeApp(packageName);
        }
    }

    //Add by xdja
    public boolean shouldLaunchApp(String pkgName) {
        return (isAppInstalled(pkgName) && !isAppRunning(pkgName, myUserId(), false));
    }

    public boolean isEngineLaunched() {
        if (isPluginEngine()) {
            return true;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String engineProcessName = getEngineProcessName();
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.processName.endsWith(engineProcessName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIORelocateWork() {
        try {
            return getService().isIORelocateWork();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesEx() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = new ArrayList<>(am.getRunningAppProcesses());
        List<ActivityManager.RunningAppProcessInfo> list64 = V64BitHelper.getRunningAppProcess64();
        if (list64 != null) {
            list.addAll(list64);
        }
        return list;
    }

    public List<ActivityManager.RunningTaskInfo> getRunningTasksEx(int maxNum) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = new ArrayList<>(am.getRunningTasks(maxNum));
        List<ActivityManager.RunningTaskInfo> list64 = V64BitHelper.getRunningTasks64(maxNum);
        if (list64 != null) {
            list.addAll(list64);
        }
        return list;
    }


    public List<ActivityManager.RecentTaskInfo> getRecentTasksEx(int maxNum, int flags) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RecentTaskInfo> list = new ArrayList<>(am.getRecentTasks(maxNum, flags));
        List<ActivityManager.RecentTaskInfo> list64 = V64BitHelper.getRecentTasks64(maxNum, flags);
        if (list64 != null) {
            list.addAll(list64);
        }
        return list;
    }

    public List<ActivityManager.RecentTaskInfo> getAppTasksEx() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> list = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            //TODO V64BitHelper.getgetAppTasks64
            list = new ArrayList<>(am.getAppTasks());
            List<ActivityManager.RecentTaskInfo> recentTaskInfoList = new ArrayList<>();
            for (ActivityManager.AppTask task : list) {
                try {
                    ActivityManager.RecentTaskInfo info = task.getTaskInfo();
                    if (info == null) {
                        continue;
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                        //過濾其他App
                        String pkg = ActivityManagerCompat.getPackageName(info);
                        if (!(TextUtils.equals(VirtualCore.getConfig().getHostPackageName(), pkg)
                                || TextUtils.equals(VirtualCore.getConfig().getPluginEnginePackageName(), pkg)
                        )) {
                            continue;
                        }
                    }
                    recentTaskInfoList.add(info);
                }catch (Exception e){
                    continue;
                }
            }
            return recentTaskInfoList;
        } else {
            return getRecentTasksEx(128, ActivityManager.RECENT_IGNORE_UNAVAILABLE | ActivityManager.RECENT_WITH_EXCLUDED);
        }
    }

    public void requestCopyPackage64(String packageName) {
        try {
            getService().requestCopyPackage64(packageName);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public String getEngineProcessName() {
        return context.getString(R.string.engine_process_name);
    }

    public void registerActivityLifecycleCallbacks(Application app){
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }
            @Override
            public void onActivityStarted(Activity activity) {
                ActivityCounterManager.get().activityCountAdd(activity.getPackageName(),activity.getClass().getName(),android.os.Process.myPid());
            }
            @Override
            public void onActivityResumed(Activity activity) {}
            @Override
            public void onActivityPaused(Activity activity) {}
            @Override
            public void onActivityStopped(Activity activity) {
                ActivityCounterManager.get().activityCountReduce(activity.getPackageName(),activity.getClass().getName(),android.os.Process.myPid());
            }
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override
            public void onActivityDestroyed(Activity activity) { }
        });
    }

    public void initialize(VirtualInitializer initializer) {
        if (initializer == null) {
            throw new IllegalStateException("Initializer = NULL");
        }
        Log.d("Vxlib", "version: " + BuildConfig.commit);
        switch (processType) {
            case Main:
                initializer.onMainProcess();
                //启动时清零主进程计数器，有可能在进程崩溃时导致计数器残留计数
                ActivityCounterManager.get().cleanPackage(VirtualCore.get().getHostPkg());

                //设置异常记录
                Thread.setDefaultUncaughtExceptionHandler(new exceptionRecorder.defaulUncaughtExceptionHandler());

                break;
            case VAppClient:
                initializer.onVirtualProcess();
                break;
            case Server:
                initializer.onServerProcess();

                //设置异常记录
                Thread.setDefaultUncaughtExceptionHandler(new exceptionRecorder.defaulUncaughtExceptionHandler());

                break;
            case CHILD:
                initializer.onChildProcess();

                //设置异常记录
                Thread.setDefaultUncaughtExceptionHandler(new exceptionRecorder.defaulUncaughtExceptionHandler());
                
                break;
        }
    }

    private static String getProcessName(Context context) {
        int pid = Process.myPid();
        String processName = null;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.pid == pid) {
                processName = info.processName;
                break;
            }
        }
        if (processName == null) {
            throw new RuntimeException("processName = null");
        }
        return processName;
    }

    private void detectProcessType() {
        // Host package name
        hostPkgName = context.getApplicationInfo().packageName;
        // Main process name
        mainProcessName = context.getApplicationInfo().processName;
        // Current process name
        processName = getProcessName(context);
        isHostPulginApp = StubManifest.isHostPluginPackageName(hostPkgName);
        if (processName.equals(mainProcessName)) {
            processType = ProcessType.Main;
        } else if (processName.endsWith(Constants.SERVER_PROCESS_NAME)) {
            processType = ProcessType.Server;
        } else if (processName.endsWith(Constants.HELPER_PROCESS_NAME)) {
            processType = ProcessType.Helper;
        } else if (VActivityManager.get().isAppProcess(processName)) {
            processType = ProcessType.VAppClient;
        } else {
            processType = ProcessType.CHILD;
        }
    }

    public boolean isPluginEngine() {
        return isHostPulginApp;
    }

    private IAppManager getService() {
        if (!IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object remote = getStubInterface();
                mService = LocalProxyUtils.genProxy(IAppManager.class, remote);
            }
        }
        return mService;
    }

    private Object getStubInterface() {
        return IAppManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.APP));
    }

    /**
     * @return If the current process is used to VA.
     */
    public boolean isVAppProcess() {
        return ProcessType.VAppClient == processType;
    }

    public boolean is64bitHelperProcess() {
        return ProcessType.Helper == processType;
    }

    /**
     * @return If the current process is the main.
     */
    public boolean isMainProcess() {
        return ProcessType.Main == processType;
    }

    /**
     * @return If the current process is the child.
     */
    public boolean isChildProcess() {
        return ProcessType.CHILD == processType;
    }

    /**
     * @return If the current process is the server.
     */
    public boolean isServerProcess() {
        return ProcessType.Server == processType;
    }

    /**
     * @return the <em>actual</em> process name
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * @return the <em>Main</em> process name
     */
    public String getMainProcessName() {
        return mainProcessName;
    }

    /**
     * Optimize the Dalvik-Cache for the specified package.
     *
     * @param pkg package name
     * @throws IOException
     */
    @Deprecated
    public void preOpt(String pkg) throws IOException {

    }

    /**
     * Check if the specified app running in foreground / background?
     *
     * @param packageName package name
     * @param userId      user id
     * @return if the specified app running in foreground / background.
     */
    public boolean isAppRunning(String packageName, int userId, boolean foreground) {
        return VActivityManager.get().isAppRunning(packageName, userId, foreground);
    }

    public InstallResult installPackageSync(String apkPath, InstallOptions options) {
        final ConditionVariable lock = new ConditionVariable();
        final InstallResult[] out = new InstallResult[1];
        installPackage(apkPath, options, new InstallCallback() {
            @Override
            public void onFinish(InstallResult result) {
                out[0] = result;
                lock.open();
            }
        });
        lock.block();
        //Add by xdja
        preLaunchApp();
        return out[0];
    }

    @Deprecated
    public InstallResult installPackage(String apkPath, InstallOptions options) {
        return installPackageSync(apkPath, options);
    }


    public interface InstallCallback {
        void onFinish(InstallResult result);
    }

    public void installPackage(String apkPath, InstallOptions options, final InstallCallback callback) {
        ResultReceiver receiver = new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                resultData.setClassLoader(InstallResult.class.getClassLoader());
                if (callback != null) {
                    InstallResult res = resultData.getParcelable("result");
                    callback.onFinish(res);
                }
            }
        };
        try {
            getService().installPackage(apkPath, options, receiver);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public InstallResult installPackageFromAsset(String asset, InstallOptions options) {
        InputStream inputStream = null;
        try {
            inputStream = getContext().getAssets().open(asset);
            return installPackageFromStream(inputStream, options);
        } catch (Throwable e) {
            InstallResult res = new InstallResult();
            res.error = e.getMessage();
            return res;
        } finally {
            FileUtils.closeQuietly(inputStream);
        }
    }

    public InstallResult installPackageFromStream(InputStream inputStream, InstallOptions options) {
        try {
            File dir = getContext().getCacheDir();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File apkFile = new File(dir, "tmp_" + System.currentTimeMillis() + ".apk");
            FileUtils.writeToFile(inputStream, apkFile);
            InstallResult res = installPackageSync(apkFile.getAbsolutePath(), options);
            apkFile.delete();
            return res;
        } catch (Throwable e) {
            InstallResult res = new InstallResult();
            res.error = e.getMessage();
            return res;
        }
    }

    public void addVisibleOutsidePackage(String pkg) {
        try {
            getService().addVisibleOutsidePackage(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void removeVisibleOutsidePackage(String pkg) {
        try {
            getService().removeVisibleOutsidePackage(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isOutsidePackageVisible(String pkg) {
        try {
            return getService().isOutsidePackageVisible(pkg);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isAppInstalled(String pkg) {
        try {
            return getService().isAppInstalled(pkg);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isPackageLaunchable(String packageName) {
        InstalledAppInfo info = getInstalledAppInfo(packageName, 0);
        return info != null
                && getLaunchIntent(packageName, info.getInstalledUsers()[0]) != null;
    }

    public Intent getLaunchIntent(String packageName, int userId) {
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
            return null;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName,
                ris.get(0).activityInfo.name);
        return intent;
    }


    public boolean createShortcut(int userId, String packageName, OnEmitShortcutListener listener) {
        return createShortcut(userId, packageName, null, listener);
    }

    public boolean createShortcut(int userId, String packageName, Intent splash, OnEmitShortcutListener listener) {
        InstalledAppInfo setting = getInstalledAppInfo(packageName, 0);
        if (setting == null) {
            return false;
        }
        ApplicationInfo appInfo = setting.getApplicationInfo(userId);
        PackageManager pm = context.getPackageManager();
        String name;
        Bitmap icon;
        try {
            CharSequence sequence = appInfo.loadLabel(pm);
            name = sequence.toString();
            icon = BitmapUtils.drawableToBitmap(appInfo.loadIcon(pm));
        } catch (Throwable e) {
            return false;
        }
        if (listener != null) {
            String newName = listener.getName(name);
            if (newName != null) {
                name = newName;
            }
            Bitmap newIcon = listener.getIcon(icon);
            if (newIcon != null) {
                icon = newIcon;
            }
        }
        Intent targetIntent = getLaunchIntent(packageName, userId);
        if (targetIntent == null) {
            return false;
        }
        Intent shortcutIntent = wrapperShortcutIntent(targetIntent, splash, packageName, userId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutInfo likeShortcut;
            likeShortcut = new ShortcutInfo.Builder(getContext(), packageName + "@" + userId)
                    .setLongLabel(name)
                    .setShortLabel(name)
                    .setIcon(Icon.createWithBitmap(icon))
                    .setIntent(shortcutIntent)
                    .build();
            ShortcutManager shortcutManager = getContext().getSystemService(ShortcutManager.class);
            if (shortcutManager != null) {
                try {
                    shortcutManager.requestPinShortcut(likeShortcut,
                            PendingIntent.getActivity(getContext(), packageName.hashCode() + userId, shortcutIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT).getIntentSender());
                } catch (Throwable e) {
                    return false;
                }
            }
        } else {
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapUtils.warrperIcon(icon, 256, 256));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            try {
                context.sendBroadcast(addIntent);
            } catch (Throwable e) {
                return false;
            }
        }
        return true;
    }

    public boolean removeShortcut(int userId, String packageName, Intent splash, OnEmitShortcutListener listener) {
        InstalledAppInfo setting = getInstalledAppInfo(packageName, 0);
        if (setting == null) {
            return false;
        }
        ApplicationInfo appInfo = setting.getApplicationInfo(userId);
        PackageManager pm = context.getPackageManager();
        String name;
        try {
            CharSequence sequence = appInfo.loadLabel(pm);
            name = sequence.toString();
        } catch (Throwable e) {
            return false;
        }
        if (listener != null) {
            String newName = listener.getName(name);
            if (newName != null) {
                name = newName;
            }
        }
        Intent targetIntent = getLaunchIntent(packageName, userId);
        if (targetIntent == null) {
            return false;
        }
        Intent shortcutIntent = wrapperShortcutIntent(targetIntent, splash, packageName, userId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        } else {
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
            addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        }
        return true;
    }

    /**
     * @param intent target activity
     * @param splash loading activity
     * @param userId userId
     */
    public Intent wrapperShortcutIntent(Intent intent, Intent splash, String packageName, int userId) {
        Intent shortcutIntent = new Intent();
        shortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
        shortcutIntent.setAction(Constants.ACTION_SHORTCUT);
        shortcutIntent.setPackage(getHostPkg());
        if (splash != null) {
            shortcutIntent.putExtra("_VA_|_splash_", splash.toUri(0));
        }
        shortcutIntent.putExtra("_VA_|_pkg_", packageName);
        shortcutIntent.putExtra("_VA_|_uri_", intent.toUri(0));
        shortcutIntent.putExtra("_VA_|_user_id_", userId);
        return shortcutIntent;
    }

    public abstract static class UiCallback extends IUiCallback.Stub {

    }

    public void setUiCallback(Intent intent, IUiCallback callback) {
        if (callback != null) {
            Bundle bundle = new Bundle();
            BundleCompat.putBinder(bundle, "_VA_|_ui_callback_", callback.asBinder());
            intent.putExtra("_VA_|_sender_", bundle);
        }
    }

    public abstract static class VSCallback extends IVSCallback.Stub {
    }

    public abstract static class AppPermissionCallback extends IAppPermissionCallback.Stub {
    }
    public abstract static class NotificationCallback extends INotificationCallback.Stub {
    }

    public abstract static class ControllerServiceCallback extends IControllerServiceCallback.Stub{

    }

    public abstract static class VSKeyCallback extends IVSKeyCallback.Stub {

    }

    public abstract static class ToastCallback extends IToastCallback.Stub {

    }

    public InstalledAppInfo getInstalledAppInfo(String pkg, int flags) {
        try {
            return getService().getInstalledAppInfo(pkg, flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getInstalledAppCount() {
        try {
            return getService().getInstalledAppCount();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isStartup() {
        return isStartUp;
    }

    public boolean uninstallPackageAsUser(String pkgName, int userId) {
        try {
            if(PackagePermissionManager.getProtectUninstallList().contains(pkgName))
                return false;
            return getService().uninstallPackageAsUser(pkgName, userId);
        } catch (RemoteException e) {
            // Ignore
        }
        return false;
    }

    public boolean uninstallPackage(String pkgName) {
        try {
            if(PackagePermissionManager.getProtectUninstallList().contains(pkgName))
                return false;
            return getService().uninstallPackage(pkgName);
        } catch (RemoteException e) {
            // Ignore
        }
        return false;
    }

    public Resources getResources(String pkg) throws Resources.NotFoundException {
        InstalledAppInfo installedAppInfo = getInstalledAppInfo(pkg, 0);
        if (installedAppInfo != null) {
            AssetManager assets = mirror.android.content.res.AssetManager.ctor.newInstance();
            mirror.android.content.res.AssetManager.addAssetPath.call(assets, installedAppInfo.getApkPath());
            Resources hostRes = context.getResources();
            return new Resources(assets, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
        }
        throw new Resources.NotFoundException(pkg);
    }

    public synchronized ActivityInfo resolveActivityInfo(Intent intent, int userId) {
        if (SpecialComponentList.shouldBlockIntent(intent)) {
            return null;
        }
        ActivityInfo activityInfo = null;
        if (intent.getComponent() == null) {
            ResolveInfo resolveInfo = VPackageManager.get().resolveIntent(intent, intent.getType(), 0, userId);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                activityInfo = resolveInfo.activityInfo;
                intent.setClassName(activityInfo.packageName, activityInfo.name);
            }
        } else {
            activityInfo = resolveActivityInfo(intent.getComponent(), userId);
        }
        return activityInfo;
    }

    public ActivityInfo resolveActivityInfo(ComponentName componentName, int userId) {
        return VPackageManager.get().getActivityInfo(componentName, 0, userId);
    }

    public ServiceInfo resolveServiceInfo(Intent intent, int userId) {
        if (SpecialComponentList.shouldBlockIntent(intent)) {
            return null;
        }
        ServiceInfo serviceInfo = null;
        ResolveInfo resolveInfo = VPackageManager.get().resolveService(intent, intent.getType(), 0, userId);
        if (resolveInfo != null) {
            serviceInfo = resolveInfo.serviceInfo;
        }
        return serviceInfo;
    }

    public void killApp(String pkg, int userId) {
        VActivityManager.get().killAppByPkg(pkg, userId);
    }

    public void killAllApps() {
        VActivityManager.get().killAllApps();
    }

    public List<InstalledAppInfo> getInstalledApps(int flags) {
        try {
            return getService().getInstalledApps(flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public List<InstalledAppInfo> getInstalledAppsAsUser(int userId, int flags) {
        try {
            return getService().getInstalledAppsAsUser(userId, flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void scanApps() {
        try {
            getService().scanApps();
        } catch (RemoteException e) {
            // Ignore
        }
    }

    public AppRequestListener getAppRequestListener() {
        return mAppRequestListener;
    }

    public void setAppRequestListener(final AppRequestListener listener) {
        this.mAppRequestListener = listener;
    }

    public boolean isPackageLaunched(int userId, String packageName) {
        try {
            return getService().isPackageLaunched(userId, packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void setPackageHidden(int userId, String packageName, boolean hidden) {
        try {
            getService().setPackageHidden(userId, packageName, hidden);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean installPackageAsUser(int userId, String packageName) {
        try {
            return getService().installPackageAsUser(userId, packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isAppInstalledAsUser(int userId, String packageName) {
        try {
            return getService().isAppInstalledAsUser(userId, packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int[] getPackageInstalledUsers(String packageName) {
        try {
            return getService().getPackageInstalledUsers(packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public abstract static class PackageObserver extends IPackageObserver.Stub {
    }

    public void registerObserver(IPackageObserver observer) {
        try {
            getService().registerObserver(observer);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void unregisterObserver(IPackageObserver observer) {
        try {
            getService().unregisterObserver(observer);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isOutsideInstalled(String packageName) {
        if (packageName == null) {
            return false;
        }
        try {
            return unHookPackageManager.getApplicationInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore
        }
        return false;
    }

    public boolean is64BitEngineInstalled() {
        return isOutsideInstalled(StubManifest.PACKAGE_NAME_64BIT);
    }

    public boolean isRun64BitProcess(String packageName) {
        try {
            return getService().isRun64BitProcess(packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean cleanPackageData(String pkg, int userId) {
        try {
            return getService().cleanPackageData(pkg, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void startForeground(){
        if(VirtualCore.getConfig().isHideForegroundNotification()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && VirtualCore.get().getTargetSdkVersion() >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, HiddenForeNotification.class));
            } else {
                context.startService(new Intent(context, HiddenForeNotification.class));
            }
        }
    }

    public void stopForeground() {
        if (VirtualCore.getConfig().isHideForegroundNotification()) {
            context.stopService(new Intent(context, HiddenForeNotification.class));
        }
    }

    public abstract static class Receiver extends BroadcastReceiver{
        @Override
        public final void onReceive(Context context, Intent intent) {
            Bundle extraData = intent.getExtras();
            BroadcastIntentData intentData = null;
            if (extraData != null) {
                extraData.setClassLoader(BroadcastIntentData.class.getClassLoader());
                intentData = extraData.getParcelable("_VA_|_data_");
            }
            int userId;
            if (intentData != null) {
                //内部广播，或者服务进程广播
                intent = intentData.intent;
                userId = intentData.userId;
            } else {
                //系统广播
                userId = VUserHandle.USER_ALL;
                SpecialComponentList.unprotectIntent(intent);
            }
            onReceive(context, intent, userId);
        }

        public abstract void onReceive(Context context, Intent intent, int userId);
    }

    /**
     *
     * @param context 直接用这个context进行unregisterReceiver
     * @param receiver intent的内容会自动处理，和原生一样
     * @param filter 自动处理
     */
    public void registerReceiver(Context context, Receiver receiver, IntentFilter filter){
        SpecialComponentList.protectIntentFilter(filter, true);
        context.registerReceiver(receiver, filter);
    }

    /**
     * Process type
     */
    private enum ProcessType {
        /**
         * Server process
         */
        Server,
        /**
         * Virtual app process
         */
        VAppClient,
        /**
         * Main process
         */
        Main,
        /**
         * Helper process
         */
        Helper,
        /**
         * Child process
         */
        CHILD
    }

    public interface AppRequestListener {
        void onRequestInstall(String path);

        void onRequestUninstall(String pkg);
    }

    public interface OnEmitShortcutListener {
        Bitmap getIcon(Bitmap originIcon);

        String getName(String originName);
    }

    public static abstract class VirtualInitializer {
        public void onMainProcess() {
        }

        public void onVirtualProcess() {
        }

        public void onServerProcess() {
        }

        public void onChildProcess() {
        }

    }
}
