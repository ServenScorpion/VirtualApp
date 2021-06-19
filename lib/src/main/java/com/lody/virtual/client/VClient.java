package com.lody.virtual.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.core.CrashHandler;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.delegate.AppInstrumentation;
import com.lody.virtual.client.hook.providers.ProviderHook;
import com.lody.virtual.client.hook.proxies.am.HCallbackStub;
import com.lody.virtual.client.hook.secondary.ProxyServiceFactory;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.ipc.VirtualStorageManager;
import com.lody.virtual.client.service.ServiceManager;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;
import com.lody.virtual.helper.compat.StorageManagerCompat;
import com.lody.virtual.helper.compat.StrictModeCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.ClientConfig;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.remote.VDeviceConfig;
import com.lody.virtual.server.pm.PackageSetting;
import com.xdja.activitycounter.ActivityCounterManager;
import com.xdja.zs.VAppPermissionManager;
import com.xdja.zs.controllerManager;
import com.xdja.zs.exceptionRecorder;

import java.io.File;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mirror.android.app.ActivityManagerNative;
import mirror.android.app.ActivityThread;
import mirror.android.app.ActivityThreadNMR1;
import mirror.android.app.ActivityThreadQ;
import mirror.android.app.ContextImpl;
import mirror.android.app.ContextImplKitkat;
import mirror.android.app.IActivityManager;
import mirror.android.app.LoadedApk;
import mirror.android.app.LoadedApkICS;
import mirror.android.app.LoadedApkKitkat;
import mirror.android.content.ContentProviderHolderOreo;
import mirror.android.content.res.CompatibilityInfo;
import mirror.android.providers.Settings;
import mirror.android.renderscript.RenderScriptCacheDir;
import mirror.android.security.net.config.NetworkSecurityConfigProvider;
import mirror.android.view.CompatibilityInfoHolder;
import mirror.android.view.DisplayAdjustments;
import mirror.android.view.HardwareRenderer;
import mirror.android.view.RenderScript;
import mirror.android.view.ThreadedRenderer;
import mirror.com.android.internal.content.ReferrerIntent;
import mirror.dalvik.system.VMRuntime;
import mirror.java.lang.ThreadGroupN;
import mirror.oem.HwApiCacheManagerEx;
import mirror.oem.HwFrameworkFactory;

import static com.lody.virtual.client.core.VirtualCore.getConfig;
import static com.lody.virtual.os.VUserHandle.getUserId;
import static com.lody.virtual.remote.InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK;

/**
 * @author Lody
 */

public final class VClient extends IVClient.Stub {

    private static final int NEW_INTENT = 11;
    private static final int RECEIVER = 12;
    private static final int FINISH_ACTIVITY = 13;

    private static final String TAG = VClient.class.getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private static final VClient gClient = new VClient();

    private final H mH = new H();
    private Instrumentation mInstrumentation = AppInstrumentation.getDefault();
    private ClientConfig clientConfig;
    private AppBindData mBoundApplication;
    private Application mInitialApplication;
    private CrashHandler crashHandler;
    private InstalledAppInfo mAppInfo;
    private int mTargetSdkVersion;
    private ConditionVariable mBindingApplicationLock;
    private boolean mEnvironmentPrepared = false;
    private int systemPid;

    public InstalledAppInfo getAppInfo() {
        return mAppInfo;
    }

    public static VClient get() {
        return gClient;
    }

    public boolean isEnvironmentPrepared() {
        return mEnvironmentPrepared;
    }

    public boolean isAppUseOutsideAPK() {
        InstalledAppInfo appInfo = getAppInfo();
        return appInfo != null && appInfo.appMode == MODE_APP_USE_OUTSIDE_APK;
    }

    public VDeviceConfig getDeviceConfig() {
        return VDeviceManager.get().getDeviceConfig(getUserId(getVUid()));
    }

    public Application getCurrentApplication() {
        return mInitialApplication;
    }

    public String getCurrentPackage() {
        return mBoundApplication != null ?
                mBoundApplication.appInfo.packageName : VPackageManager.get().getNameForUid(getVUid());
    }

    public ApplicationInfo getCurrentApplicationInfo() {
        return mBoundApplication != null ? mBoundApplication.appInfo : null;
    }

    public int getCurrentTargetSdkVersion() {
        return mTargetSdkVersion == 0 ?
                VirtualCore.get().getTargetSdkVersion()
                : mTargetSdkVersion;
    }

    public CrashHandler getCrashHandler() {
        return crashHandler;
    }

    public void setCrashHandler(CrashHandler crashHandler) {
        this.crashHandler = crashHandler;
    }

    public int getSystemPid() {
        return systemPid;
    }

    public int getVUid() {
        if (clientConfig == null) {
            return 0;
        }
        return clientConfig.vuid;
    }

    /**
     * $Px
     * 0-99
     */
    public int getVpid() {
        if (clientConfig == null) {
            return 0;
        }
        return clientConfig.vpid;
    }

    public int getBaseVUid() {
        if (clientConfig == null) {
            return 0;
        }
        return VUserHandle.getAppId(clientConfig.vuid);
    }

    public int getCallingVUid() {
        return VActivityManager.get().getCallingUid();
    }

    public ClassLoader getClassLoader(ApplicationInfo appInfo) {
        Context context = createPackageContext(appInfo);
        return context.getClassLoader();
    }

    private void sendMessage(int what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        mH.sendMessage(msg);
    }

    @Override
    public IBinder getAppThread() {
        return ActivityThread.getApplicationThread.call(VirtualCore.mainThread());
    }

    @Override
    public IBinder getToken() {
        if (clientConfig == null) {
            return null;
        }
        return clientConfig.token;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    @Override
    public boolean isAppRunning() {
        return mBoundApplication != null;
    }
    //xdja
    int countOfActivity = 0;
    @Override
    public boolean isAppForeground(){
        return countOfActivity > 0;
    }
    public void initProcess(ClientConfig clientConfig) {
        if (this.clientConfig != null) {
            throw new RuntimeException("reject init process: " + clientConfig.processName + ", this process is : " + this.clientConfig.processName);
        }
        this.clientConfig = clientConfig;
    }

    private void handleNewIntent(NewIntentData data) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent = ReferrerIntent.ctor.newInstance(data.intent, data.creator);
        } else {
            intent = data.intent;
        }
        if (ActivityThread.performNewIntents != null) {
            ActivityThread.performNewIntents.call(
                    VirtualCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent)
            );
        } else if (ActivityThreadNMR1.performNewIntents != null){
            ActivityThreadNMR1.performNewIntents.call(
                    VirtualCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent),
                    true);
        } else if(ActivityThreadQ.handleNewIntent != null){
            ActivityThreadQ.handleNewIntent.call(VirtualCore.mainThread(), data.token, Collections.singletonList(intent));
        }
        if("com.tencent.mm".equals(getCurrentPackage())){
            //xdja 修复微信按2次返回键退出
            //第二次是因为是在顶层activity，微信用了Process.kill导致重启。
            if(intent.getComponent() != null && intent.getComponent().getClassName().endsWith(".ui.LauncherUI")){
                if(intent.getBooleanExtra("can_finish", false)){
                    Intent home = new Intent(Intent.ACTION_MAIN);
                    home.addCategory(Intent.CATEGORY_HOME);
                    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        VirtualCore.get().getContext().startActivity(home);
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
    }

    public void bindApplication(final String packageName, final String processName) {
        if (clientConfig == null) {
            throw new RuntimeException("Unrecorded process: " + processName);
        }
        if (isAppRunning()) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            if (mBindingApplicationLock != null) {
                mBindingApplicationLock.block();
                mBindingApplicationLock = null;
            } else {
                mBindingApplicationLock = new ConditionVariable();
            }
            VirtualRuntime.getUIHandler().post(new Runnable() {
                @Override
                public void run() {
                    bindApplicationNoCheck(packageName, processName);
                    ConditionVariable lock = mBindingApplicationLock;
                    mBindingApplicationLock = null;
                    if (lock != null) {
                        lock.open();
                    }
                }
            });
            if (mBindingApplicationLock != null) {
                mBindingApplicationLock.block();
            }
        } else {
            bindApplicationNoCheck(packageName, processName);
        }
    }


    private void bindApplicationNoCheck(String packageName, String processName) {
        if (isAppRunning()) {
            return;
        }
        if (processName == null) {
            processName = packageName;
        }
        systemPid = VActivityManager.get().getSystemPid();
        try {
            setupUncaughtHandler();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        final int userId = getUserId(getVUid());
        try {
            fixInstalledProviders();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        VDeviceConfig deviceConfig = getDeviceConfig();
        VDeviceManager.get().applyBuildProp(deviceConfig);
        final boolean isSubRemote = VirtualCore.get().isPluginEngine();
        // Fix: com.loafwallet
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if("com.loafwallet".equals(packageName)) {
                try {
                    KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                    keyStore.load(null);
                    Enumeration<String> aliases = keyStore.aliases();
                    while (aliases.hasMoreElements()) {
                        String entry = aliases.nextElement();
                        VLog.w(TAG, "remove entry: " + entry);
                        keyStore.deleteEntry(entry);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        ActivityThread.mInitialApplication.set(
                VirtualCore.mainThread(),
                null
        );
        AppBindData data = new AppBindData();
        InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(packageName, 0);
        if (info == null) {
            new Exception("app not exist").printStackTrace();
            Process.killProcess(0);
            System.exit(0);
        }
        mAppInfo = info;
        data.appInfo = VPackageManager.get().getApplicationInfo(packageName, 0, userId);

        data.processName = processName;
        data.providers = VPackageManager.get().queryContentProviders(processName, getVUid(), PackageManager.GET_META_DATA);
        mTargetSdkVersion = data.appInfo.targetSdkVersion;
        VLog.i(TAG, "Binding application %s (%s [%d])", data.appInfo.packageName, data.processName, Process.myPid());
        mBoundApplication = data;
        VirtualRuntime.setupRuntime(data.processName, data.appInfo);
        if (VirtualCore.get().isPluginEngine()) {
            File apkFile = new File(info.getApkPath());
            File libDir = new File(data.appInfo.nativeLibraryDir);
            if (!apkFile.exists()) {
                VirtualCore.get().requestCopyPackage64(packageName);
            }
            if (!libDir.exists()) {
                NativeLibraryHelperCompat.copyNativeBinaries(apkFile, libDir);
            }
        }
        int targetSdkVersion = data.appInfo.targetSdkVersion;
        if (targetSdkVersion < Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy newPolicy = new StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy()).permitNetwork().build();
            StrictMode.setThreadPolicy(newPolicy);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (VirtualCore.get().getTargetSdkVersion() >= Build.VERSION_CODES.N
                    && targetSdkVersion < Build.VERSION_CODES.N) {
                StrictModeCompat.disableDeathOnFileUriExposure();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && targetSdkVersion < Build.VERSION_CODES.LOLLIPOP) {
            mirror.android.os.Message.updateCheckRecycle.call(targetSdkVersion);
        }
        AlarmManager alarmManager = (AlarmManager) VirtualCore.get().getContext().getSystemService(Context.ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (mirror.android.app.AlarmManager.mTargetSdkVersion != null) {
                try {
                    mirror.android.app.AlarmManager.mTargetSdkVersion.set(alarmManager, targetSdkVersion);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //tmp dir
        File tmpDir;
        if (isSubRemote) {
            tmpDir = new File(VEnvironment.getDataUserPackageDirectory64(userId, info.packageName), "cache");
        } else {
            tmpDir = new File(VEnvironment.getDataUserPackageDirectory(userId, info.packageName), "cache");
        }
        if(!tmpDir.exists()){
            tmpDir.mkdirs();
        }
        System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());

        fixForEmui10();

        if (getConfig().isEnableIORedirect()) {
            if (VirtualCore.get().isIORelocateWork()) {
                startIORelocater(info, isSubRemote);
            } else {
                VLog.w(TAG, "IO Relocate verify fail.");
            }
        }
        NativeEngine.launchEngine();
        mEnvironmentPrepared = true;
        Object mainThread = VirtualCore.mainThread();
        NativeEngine.startDexOverride();
        initDataStorage(isSubRemote, userId, packageName);
        Context context = createPackageContext(data.appInfo);
        File codeCacheDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            codeCacheDir = context.getCodeCacheDir();
        } else {
            codeCacheDir = context.getCacheDir();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (HardwareRenderer.setupDiskCache != null) {
                HardwareRenderer.setupDiskCache.call(codeCacheDir);
            }
        } else {
            if (ThreadedRenderer.setupDiskCache != null) {
                ThreadedRenderer.setupDiskCache.call(codeCacheDir);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (RenderScriptCacheDir.setupDiskCache != null) {
                RenderScriptCacheDir.setupDiskCache.call(codeCacheDir);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (RenderScript.setupDiskCache != null) {
                RenderScript.setupDiskCache.call(codeCacheDir);
            }
        }
        mBoundApplication.info = ContextImpl.mPackageInfo.get(context);
        ApplicationInfo applicationInfo = LoadedApk.mApplicationInfo.get(mBoundApplication.info);
        if(applicationInfo.nativeLibraryDir == null){
            Log.w("kk-test", "applicationInfo.nativeLibraryDir = null,"+context.getApplicationInfo().nativeLibraryDir, new Exception());
        }
        applicationInfo.nativeLibraryDir = data.appInfo.nativeLibraryDir;
        Object thread = VirtualCore.mainThread();
        Object boundApp = mirror.android.app.ActivityThread.mBoundApplication.get(thread);
        mirror.android.app.ActivityThread.AppBindData.appInfo.set(boundApp, data.appInfo);
        mirror.android.app.ActivityThread.AppBindData.processName.set(boundApp, data.processName);
        mirror.android.app.ActivityThread.AppBindData.instrumentationName.set(
                boundApp,
                new ComponentName(data.appInfo.packageName, Instrumentation.class.getName())
        );
        mirror.android.app.ActivityThread.AppBindData.info.set(boundApp, data.info);
        ActivityThread.AppBindData.providers.set(boundApp, data.providers);
        if (LoadedApk.mSecurityViolation != null) {
            LoadedApk.mSecurityViolation.set(mBoundApplication.info, false);
        }
        VMRuntime.setTargetSdkVersion.call(VMRuntime.getRuntime.call(), data.appInfo.targetSdkVersion);
        Configuration configuration = context.getResources().getConfiguration();
        boolean is64Bit = VirtualRuntime.is64bit();
        if (!isSubRemote && info.flag == PackageSetting.FLAG_RUN_BOTH_32BIT_64BIT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<String> supportAbiList = new LinkedList<>();
            for (String abi : Build.SUPPORTED_ABIS) {
                if(is64Bit) {
                    if (NativeLibraryHelperCompat.is64bitAbi(abi)) {
                        supportAbiList.add(abi);
                    }
                } else {
                    if (NativeLibraryHelperCompat.is32bitAbi(abi)) {
                        supportAbiList.add(abi);
                    }
                }
            }
            String[] supportAbis = supportAbiList.toArray(new String[0]);
            Reflect.on(Build.class).set("SUPPORTED_ABIS", supportAbis);
        }
        Object compatInfo = null;
        if (CompatibilityInfo.ctor != null) {
            compatInfo = CompatibilityInfo.ctor.newInstance(data.appInfo, configuration.screenLayout, configuration.smallestScreenWidthDp, false);
        }
        if (CompatibilityInfo.ctorLG != null) {
            compatInfo = CompatibilityInfo.ctorLG.newInstance(data.appInfo, configuration.screenLayout, configuration.smallestScreenWidthDp, false, 0);
        }

        if (compatInfo != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    DisplayAdjustments.setCompatibilityInfo.call(ContextImplKitkat.mDisplayAdjustments.get(context), compatInfo);
                }
                DisplayAdjustments.setCompatibilityInfo.call(LoadedApkKitkat.mDisplayAdjustments.get(mBoundApplication.info), compatInfo);
            } else {
                CompatibilityInfoHolder.set.call(LoadedApkICS.mCompatibilityInfo.get(mBoundApplication.info), compatInfo);
            }
        }
		//ssl适配
		if (NetworkSecurityConfigProvider.install != null) {
            Security.removeProvider("AndroidNSSP");
            NetworkSecurityConfigProvider.install.call(context);
        }

        if(data.appInfo != null && "com.tencent.mm".equals(data.appInfo.packageName)
                && "com.tencent.mm".equals(data.appInfo.processName)){
            ClassLoader originClassLoader = context.getClassLoader();
            fixWeChatTinker(context, data.appInfo, originClassLoader);
        }

        VirtualCore.get().getAppCallback().beforeStartApplication(packageName, processName, context);

        try {
            //TODO reset?
//            if(LoadedApk.mApplication != null) {
//                LoadedApk.mApplication.set(data.info, null);
//            }
            mInitialApplication = LoadedApk.makeApplication.call(data.info, false, null);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to makeApplication", e);
        }
        Log.e("kk", data.info+" mInitialApplication set  " + LoadedApk.mApplication.get(data.info));
        mirror.android.app.ActivityThread.mInitialApplication.set(mainThread, mInitialApplication);
        ContextFixer.fixContext(mInitialApplication);
        if (Build.VERSION.SDK_INT >= 24 && "com.tencent.mm:recovery".equals(processName)) {
            fixWeChatRecovery(mInitialApplication);
        }
        if (GmsSupport.VENDING_PKG.equals(packageName)) {
            try {
                context.getSharedPreferences("vending_preferences", 0)
                        .edit()
                        .putBoolean("notify_updates", false)
                        .putBoolean("notify_updates_completion", false)
                        .apply();
                context.getSharedPreferences("finsky", 0)
                        .edit()
                        .putBoolean("auto_update_enabled", false)
                        .apply();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        /*
         * Support Atlas plugin framework
         * see:
         * https://github.com/alibaba/atlas/blob/master/atlas-core/src/main/java/android/taobao/atlas/bridge/BridgeApplicationDelegate.java
         */
        List<ProviderInfo> providers = ActivityThread.AppBindData.providers.get(boundApp);
        if (providers != null && !providers.isEmpty()) {
            installContentProviders(mInitialApplication, providers);
        }
        VirtualCore.get().getAppCallback().beforeApplicationCreate(packageName, processName, mInitialApplication);
        try {
            mInstrumentation.callApplicationOnCreate(mInitialApplication);
            InvocationStubManager.getInstance().checkEnv(HCallbackStub.class);
            Application createdApp = ActivityThread.mInitialApplication.get(mainThread);
            if (createdApp != null) {
                if (TextUtils.equals(VirtualCore.get().getHostPkg(), createdApp.getPackageName())) {
                    VLog.w("kk", "mInitialApplication is host!!");
                    ActivityThread.mInitialApplication.set(mainThread, mInitialApplication);
                    //reset mInitialApplication
                } else {
                    mInitialApplication = createdApp;
                }
            }
            //reset
            if(LoadedApk.mApplication != null) {
                Application application = LoadedApk.mApplication.get(data.info);
                if (application != null && TextUtils.equals(VirtualCore.get().getHostPkg(), application.getPackageName())) {
                    VLog.w("kk", "LoadedApk's mApplication is host!");
                    LoadedApk.mApplication.set(data.info, mInitialApplication);
                }
            }
        } catch (Exception e) {
            if (!mInstrumentation.onException(mInitialApplication, e)) {
                throw new RuntimeException("Unable to create application " + data.appInfo.name + ": " + e.toString(), e);
            }
        }
        mInitialApplication.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) { }
            @Override
            public void onActivityStarted(Activity activity) {
                ActivityCounterManager.get().activityCountAdd(activity.getPackageName(),activity.getClass().getName(), android.os.Process.myPid());
                countOfActivity++;
            }
            @Override
            public void onActivityResumed(Activity activity) {
                //检测截屏权限
                boolean screenShort = VAppPermissionManager.get().getAppPermissionEnable(
                        activity.getPackageName(), VAppPermissionManager.PROHIBIT_SCREEN_SHORT_RECORDER);
                Log.e(TAG, "screenShort: " + screenShort);
                if (screenShort) {
                    activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE
                            , WindowManager.LayoutParams.FLAG_SECURE);
                }
            }
            @Override
            public void onActivityPaused(Activity activity) { }
            @Override
            public void onActivityStopped(Activity activity) {
                ActivityCounterManager.get().activityCountReduce(activity.getPackageName(),activity.getClass().getName(),android.os.Process.myPid());
                countOfActivity--;
            }
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) { }
            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
        VirtualCore.get().getAppCallback().afterApplicationCreate(packageName, processName, mInitialApplication);
        VActivityManager.get().appDoneExecuting(info.packageName);

        //xdja
       /* context.getCacheDir();
        List<InstalledAppInfo> modules = VirtualCore.get().getInstalledApps(0);
        for (InstalledAppInfo module : modules) {
            String libPath = VEnvironment.getAppLibDirectory(module.packageName).getAbsolutePath();
            LoadModules.loadModule(module.getApkPath(), module.getOdexFile().getParent(), libPath, mInitialApplication);
        }*/
    }

    private void initDataStorage(boolean is64bit, int userId, String pkg) {
        // ensure dir created
        if (is64bit) {
            VEnvironment.getDataUserPackageDirectory64(userId, pkg);
            VEnvironment.getDeDataUserPackageDirectory64(userId, pkg);
        } else {
            VEnvironment.getDataUserPackageDirectory(userId, pkg);
            VEnvironment.getDeDataUserPackageDirectory(userId, pkg);
        }
    }


    private void fixForEmui10() {
        if (BuildCompat.isQ() && BuildCompat.isEMUI()) {
            if (HwApiCacheManagerEx.getDefault != null && HwApiCacheManagerEx.mPkg != null) {
                HwApiCacheManagerEx.mPkg.set(HwApiCacheManagerEx.getDefault.call(), VirtualCore.get().getPM());
            } else if (HwFrameworkFactory.getHwApiCacheManagerEx != null) {
                Object hwmgr = HwFrameworkFactory.getHwApiCacheManagerEx.call();
                if (hwmgr != null) {
                    try {
                        Reflect.on(hwmgr).call("apiPreCache", VirtualCore.get().getPM());
                    } catch (Throwable e) {
                        //ignore
                    }
                }
            }
        }
    }

    private void fixWeChatRecovery(Application app) {
        try {
            Field field = app.getClassLoader().loadClass("com.tencent.recovery.Recovery").getField("context");
            field.setAccessible(true);
            if (field.get(null) != null) {
                return;
            }
            field.set(null, app.getBaseContext());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void fixWeChatTinker(Context context, ApplicationInfo applicationInfo, ClassLoader appClassLoader)
    {
        String dataDir = applicationInfo.dataDir;
        File tinker = new File(dataDir, "tinker");
        if(tinker.exists()){
            Log.e("wxd", " deleteWechatTinker " + tinker.getPath());
            FileUtils.deleteDir(tinker);
        }
        File tinker_temp = new File(dataDir, "tinker_temp");
        if(tinker_temp.exists()){
            Log.e("wxd", " deleteWechatTinker " + tinker_temp.getPath());
            FileUtils.deleteDir(tinker_temp);
        }
        File tinker_server = new File(dataDir, "tinker_server");
        if(tinker_server.exists()){
            Log.e("wxd", " deleteWechatTinker " + tinker_server.getPath());
            FileUtils.deleteDir(tinker_server);
        }
    }

    private void setupUncaughtHandler() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        ThreadGroup newRoot = new RootThreadGroup(root);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            final List<ThreadGroup> groups = mirror.java.lang.ThreadGroup.groups.get(root);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (groups) {
                List<ThreadGroup> newGroups = new ArrayList<>(groups);
                newGroups.remove(newRoot);
                mirror.java.lang.ThreadGroup.groups.set(newRoot, newGroups);
                groups.clear();
                groups.add(newRoot);
                mirror.java.lang.ThreadGroup.groups.set(root, groups);
                for (ThreadGroup group : newGroups) {
                    if (group == newRoot) {
                        continue;
                    }
                    mirror.java.lang.ThreadGroup.parent.set(group, newRoot);
                }
            }
        } else {
            final ThreadGroup[] groups = ThreadGroupN.groups.get(root);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (groups) {
                ThreadGroup[] newGroups = groups.clone();
                ThreadGroupN.groups.set(newRoot, newGroups);
                ThreadGroupN.groups.set(root, new ThreadGroup[]{newRoot});
                for (Object group : newGroups) {
                    if (group == null) {
                        continue;
                    }
                    if (group == newRoot) {
                        continue;
                    }
                    ThreadGroupN.parent.set(group, newRoot);
                }
                ThreadGroupN.ngroups.set(root, 1);
            }
        }
    }


    @SuppressLint("SdCardPath")
    private void startIORelocater(InstalledAppInfo info, boolean is64bit) {
        String packageName = info.packageName;
        int userId = VUserHandle.myUserId();
        String dataDir, de_dataDir, libPath;
        if (is64bit) {
            dataDir = VEnvironment.getDataUserPackageDirectory64(userId, packageName).getPath();
            de_dataDir = VEnvironment.getDeDataUserPackageDirectory64(userId, packageName).getPath();
            libPath = VEnvironment.getAppLibDirectory64(packageName).getAbsolutePath();
        } else {
            dataDir = VEnvironment.getDataUserPackageDirectory(userId, packageName).getPath();
            de_dataDir = VEnvironment.getDeDataUserPackageDirectory(userId, packageName).getPath();
            libPath = VEnvironment.getAppLibDirectory(packageName).getAbsolutePath();
        }
        VDeviceConfig deviceConfig = getDeviceConfig();
        if (deviceConfig.enable) {
            File wifiMacAddressFile = getDeviceConfig().getWifiFile(userId, is64bit);
            if (wifiMacAddressFile != null && wifiMacAddressFile.exists()) {
                String wifiMacAddressPath = wifiMacAddressFile.getPath();
                NativeEngine.redirectFile("/sys/class/net/wlan0/address", wifiMacAddressPath);
                NativeEngine.redirectFile("/sys/class/net/eth0/address", wifiMacAddressPath);
                NativeEngine.redirectFile("/sys/class/net/wifi/address", wifiMacAddressPath);
            }
        }
        LinuxCompat.forgeProcDriver(is64bit);
        forbidHost();
        boolean autoFixPath = userId > 0;//防止多开的应用写死路径
        String cache = new File(dataDir, "cache").getAbsolutePath();
        NativeEngine.redirectDirectory("/tmp/", cache);
        // /data/data/{packageName}/ -> /data/data/va/.../data/user/{userId}/{packageName}/
        NativeEngine.redirectDirectory("/data/data/" + packageName, dataDir);
        // /data/user/{userId}/{packageName}/ -> /data/data/va/.../data/user/{userId}/{packageName}/
        NativeEngine.redirectDirectory("/data/user/" + userId + "/" + packageName, dataDir);
        if(autoFixPath) {
            // /data/user/0/{packageName}/ -> /data/data/va/.../data/user/{userId}/{packageName}/
            NativeEngine.redirectDirectory("/data/user/0/" + packageName, dataDir);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NativeEngine.redirectDirectory("/data/user_de/" + userId + "/" + packageName, de_dataDir);
            if(autoFixPath) {
                // /data/user_de/0/{packageName}/ -> /data/data/va/.../data/user_de/{userId}/{packageName}/
                NativeEngine.redirectDirectory("/data/user_de/0/" + packageName, de_dataDir);
            }
        }
        SettingConfig.AppLibConfig appLibConfig = getConfig().getAppLibConfig(packageName);

        if (appLibConfig == SettingConfig.AppLibConfig.UseRealLib) {
            if (info.appMode != MODE_APP_USE_OUTSIDE_APK
                    || !VirtualCore.get().isOutsideInstalled(info.packageName)) {
                appLibConfig = SettingConfig.AppLibConfig.UseOwnLib;
            }
        }
        NativeEngine.whitelist(libPath);
        if (appLibConfig == SettingConfig.AppLibConfig.UseOwnLib) {
            NativeEngine.redirectDirectory("/data/data/" + packageName + "/lib/", libPath);
            NativeEngine.redirectDirectory("/data/user/" + userId + "/" + packageName + "/lib/", libPath);
            if (autoFixPath) {
                // /data/user_de/0/{packageName}/lib -> /data/data/va/.../data/user_de/{userId}/{packageName}/lib
                NativeEngine.redirectDirectory("/data/user/0/" + packageName + "/lib/", libPath);
            }
        } else {
            NativeEngine.whitelist("/data/user/" + userId + "/" + packageName + "/lib/");
        }
        // /data/data/va/.../data/user/{userId}/{packageName}/lib
        File userLibDir = VEnvironment.getUserAppLibDirectory(userId, packageName);
        //libPath=/data/data/va/.../data/app/{packageName}/lib
        //改为link，防止其他进程读取这个lib目录，报找不到文件错误
        try {
            if(userLibDir.exists() && !FileUtils.isSymlink(userLibDir)){
                FileUtils.deleteDir(userLibDir);
            }
            if(!userLibDir.exists()) {
                FileUtils.createSymlink(libPath, userLibDir.getPath());
            }
        } catch (Exception e) {
            NativeEngine.redirectDirectory(userLibDir.getPath(), libPath);
        }

        //xdja safekey adapter
        String subPathData = "/Android/data/" + info.packageName;
        String prefix = "/emulated/" + VUserHandle.realUserId() + "/";
        File[] efd = VEnvironment.getTFRoots();
        for (File f : efd) {
            if (f == null)
                continue;
            String filename = f.getAbsolutePath();
            if(filename.contains(prefix))
                continue;
            String tfRoot = VEnvironment.getTFRoot(f.getAbsolutePath()).getAbsolutePath();
            NativeEngine.redirectDirectory(tfRoot+subPathData
                    ,VEnvironment.getTFVirtualRoot(tfRoot,subPathData).getAbsolutePath());
        }

        VirtualStorageManager vsManager = VirtualStorageManager.get();
        //xdja
        vsManager.setVirtualStorage(info.packageName, userId, VEnvironment.getExternalStorageDirectory(userId).getAbsolutePath());
        String vsPath = vsManager.getVirtualStorage(info.packageName, userId);
        boolean enable = vsManager.isVirtualStorageEnable(info.packageName, userId);
        if (enable && vsPath != null) {
            File vsDirectory = new File(vsPath);
            if (vsDirectory.exists() || vsDirectory.mkdirs()) {
                HashSet<String> mountPoints = getMountPoints();
                for (String mountPoint : mountPoints) {
                    //xdja
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        try {
                            if (Environment.isExternalStorageRemovable(new File(mountPoint))) {
                                continue;
                            }
                        } catch (IllegalArgumentException e) {
                            VLog.d(TAG, e.toString());
                        }
                    }
                    NativeEngine.redirectDirectory(mountPoint, vsPath);
                }

            }
        }

        //xdja 放开异常记录路径
        NativeEngine.whitelist(exceptionRecorder.getExceptionRecordPath());
        if (VAppPermissionManager.get().getEncryptConfig() != null) {
            NativeEngine.nativeConfigEncryptPkgName(VAppPermissionManager.get().getEncryptConfig());
        }
        NativeEngine.enableIORedirect();
        if (controllerManager.getNetworkState()) {
            NativeEngine.nativeConfigNetworkState(controllerManager.getNetworkState());
            NativeEngine.nativeConfigWhiteOrBlack(controllerManager.isWhiteList());
            NativeEngine.nativeConfigNetStrategy(controllerManager.get().getIpStrategy(), 1);
            NativeEngine.nativeConfigNetStrategy(controllerManager.get().getDomainStrategy(), 2);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (controllerManager.get().getDomainStrategy() != null
                            && controllerManager.get().getDomainStrategy().length > 0) {
                        NativeEngine.nativeConfigDomainToIp();
                    }
                }
            }).start();
        }
    }

    private void forbidHost() {
        final List<String> hostProcesses;
        if (StubManifest.PACKAGE_NAME_64BIT != null) {
            hostProcesses = Arrays.asList(StubManifest.PACKAGE_NAME,
                    StubManifest.PACKAGE_NAME_64BIT,
                    StubManifest.PACKAGE_NAME + ":x",
                    StubManifest.PACKAGE_NAME_64BIT + ":x");
        } else {
            hostProcesses = Arrays.asList(StubManifest.PACKAGE_NAME,
                    StubManifest.PACKAGE_NAME + ":x");
        }
        ActivityManager am = (ActivityManager) VirtualCore.get().getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.pid == Process.myPid()) {
                continue;
            }
            if (info.uid != VirtualCore.get().myUid()) {
                continue;
            }
            if(hostProcesses.contains(info.processName)){
                //is host
                NativeEngine.forbid("/proc/" + info.pid, false);//直接不允许读取host的任何proc信息
                NativeEngine.forbid("/proc/" + info.pid + "/maps", true);
                NativeEngine.forbid("/proc/" + info.pid + "/cmdline", true);
            }
        }
    }

    @SuppressLint("SdCardPath")
    private HashSet<String> getMountPoints() {
        HashSet<String> mountPoints = new HashSet<>(3);
        mountPoints.add("/mnt/sdcard/");
        mountPoints.add("/sdcard/");
        mountPoints.add("/storage/emulated/" + VUserHandle.realUserId() +"/");
        String[] points = StorageManagerCompat.getAllPoints(VirtualCore.get().getContext());
        if (points != null) {
            Collections.addAll(mountPoints, points);
        }
        return mountPoints;

    }

    private Context createPackageContext(ApplicationInfo appInfo) {
        try {
            final String packageName = appInfo.packageName;
            Context hostContext = VirtualCore.get().getContext();
            Context appContext = hostContext.createPackageContext(packageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            if (appContext != null) {
                if (appContext.getApplicationInfo().nativeLibraryDir == null) {
                    VLog.w(TAG, "fix nativeLibraryDir");
                    appContext.getApplicationInfo().nativeLibraryDir = appInfo.nativeLibraryDir;
                }
                if (appContext.getApplicationInfo().sharedLibraryFiles == null && appInfo.sharedLibraryFiles != null) {
                    VLog.w(TAG, "fix sharedLibraryFiles");
                    appContext.getApplicationInfo().sharedLibraryFiles = appInfo.sharedLibraryFiles;
                }
            }
            return appContext;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
        throw new RuntimeException();
    }

    private void installContentProviders(Context app, List<ProviderInfo> providers) {
        long origId = Binder.clearCallingIdentity();
        Object mainThread = VirtualCore.mainThread();
        try {
            for (ProviderInfo cpi : providers) {
                try {
                    ActivityThread.installProvider(mainThread, app, cpi, null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    @Override
    public IBinder acquireProviderClient(ProviderInfo info) {
        if (!isAppRunning()) {
            VClient.get().bindApplication(info.packageName, info.processName);
        }
        if (VClient.get().getCurrentApplication() == null) {
            return null;
        }
        IInterface provider = null;
        String authority = ComponentUtils.getFirstAuthority(info);
        ContentResolver resolver = VirtualCore.get().getContext().getContentResolver();
        ContentProviderClient client = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                client = resolver.acquireUnstableContentProviderClient(authority);
            } else {
                client = resolver.acquireContentProviderClient(authority);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (client != null) {
            provider = mirror.android.content.ContentProviderClient.mContentProvider.get(client);
            client.release();
        }
        IBinder binder = provider != null ? provider.asBinder() : null;
        if (binder != null) {
            return binder;
        }
        return null;
    }

    private void fixInstalledProviders() {
        clearSettingProvider();
        //noinspection unchecked
        Map<Object, Object> clientMap = ActivityThread.mProviderMap.get(VirtualCore.mainThread());
        for (Map.Entry<Object, Object> e : clientMap.entrySet()) {
            Object clientRecord = e.getValue();
            if (BuildCompat.isOreo()) {
                IInterface provider = ActivityThread.ProviderClientRecordJB.mProvider.get(clientRecord);
                Object holder = ActivityThread.ProviderClientRecordJB.mHolder.get(clientRecord);
                if (holder == null) {
                    continue;
                }
                ProviderInfo info = ContentProviderHolderOreo.info.get(holder);
                String name = ComponentUtils.getFirstAuthority(info);
                if (name != null && !name.startsWith(StubManifest.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, name, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    ContentProviderHolderOreo.provider.set(holder, provider);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                IInterface provider = ActivityThread.ProviderClientRecordJB.mProvider.get(clientRecord);
                Object holder = ActivityThread.ProviderClientRecordJB.mHolder.get(clientRecord);
                if (holder == null) {
                    continue;
                }
                ProviderInfo info = IActivityManager.ContentProviderHolder.info.get(holder);
                String name = ComponentUtils.getFirstAuthority(info);
                if (name != null && !name.startsWith(StubManifest.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, name, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
            } else {
                String authority = ActivityThread.ProviderClientRecord.mName.get(clientRecord);
                IInterface provider = ActivityThread.ProviderClientRecord.mProvider.get(clientRecord);
                if (provider != null && !authority.startsWith(StubManifest.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, authority, provider);
                    ActivityThread.ProviderClientRecord.mProvider.set(clientRecord, provider);
                }
            }
        }
    }

    public void clearSettingProvider() {
        Object cache;
        cache = Settings.System.sNameValueCache.get();
        if (cache != null) {
            clearContentProvider(cache);
        }
        cache = Settings.Secure.sNameValueCache.get();
        if (cache != null) {
            clearContentProvider(cache);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && Settings.Global.TYPE != null) {
            cache = Settings.Global.sNameValueCache.get();
            if (cache != null) {
                clearContentProvider(cache);
            }
        }
    }

    private static void clearContentProvider(Object cache) {
        if (BuildCompat.isOreo()) {
            Object holder = Settings.NameValueCacheOreo.mProviderHolder.get(cache);
            if (holder != null) {
                Settings.ContentProviderHolder.mContentProvider.set(holder, null);
            }
        } else {
            Settings.NameValueCache.mContentProvider.set(cache, null);
        }
    }

    @Override
    public void finishActivity(IBinder token) {
        sendMessage(FINISH_ACTIVITY, token);
    }

    @Override
    public void closeAllLongSocket() throws RemoteException {
        NativeEngine.nativeCloseAllSocket();
    }

    @Override
    public void scheduleNewIntent(String creator, IBinder token, Intent intent) {
        NewIntentData data = new NewIntentData();
        data.creator = creator;
        data.token = token;
        data.intent = intent;
        sendMessage(NEW_INTENT, data);
    }

    @Override
    public void scheduleReceiver(String processName, ComponentName component, Intent intent, PendingResultData pendingResult) {
        ReceiverData receiverData = new ReceiverData();
        receiverData.pendingResult = pendingResult;
        receiverData.intent = intent;
        receiverData.component = component;
        receiverData.processName = processName;
        receiverData.stacktrace = new Exception();
        sendMessage(RECEIVER, receiverData);
    }

    private void handleReceiver(ReceiverData data) {
        if (!isAppRunning()) {
            bindApplication(data.component.getPackageName(), data.processName);
        }
        BroadcastReceiver.PendingResult result = data.pendingResult.build();
        try {
            Context context = mInitialApplication.getBaseContext();
            Context receiverContext = ContextImpl.getReceiverRestrictedContext.call(context);
            String className = data.component.getClassName();
            ClassLoader classLoader = LoadedApk.getClassLoader.call(mBoundApplication.info);
            BroadcastReceiver receiver = (BroadcastReceiver) classLoader.loadClass(className).newInstance();
            mirror.android.content.BroadcastReceiver.setPendingResult.call(receiver, result);
            data.intent.setExtrasClassLoader(classLoader);
            if (data.intent.getComponent() == null) {
                data.intent.setComponent(data.component);
            }
            receiver.onReceive(receiverContext, data.intent);
            if (mirror.android.content.BroadcastReceiver.getPendingResult.call(receiver) != null) {
                result.finish();
            }
        } catch (Exception e) {
            data.stacktrace.printStackTrace();
            throw new RuntimeException(
                    "Unable to start receiver " + data.component
                            + ": " + e.toString(), e);
        }
        VActivityManager.get().broadcastFinish(data.pendingResult);
    }

    public ClassLoader getClassLoader() {
        return LoadedApk.getClassLoader.call(mBoundApplication.info);
    }

    public Service createService(ServiceInfo info, IBinder token) {
        if (!isAppRunning()) {
            bindApplication(info.packageName, info.processName);
        }
        ClassLoader classLoader = LoadedApk.getClassLoader.call(mBoundApplication.info);
        Service service;
        try {
            service = (Service) classLoader.loadClass(info.name).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to instantiate service " + info.name
                            + ": " + e.toString(), e);
        }
        try {
            Context context = VirtualCore.get().getContext().createPackageContext(
                    info.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            ContextImpl.setOuterContext.call(context, service);
            mirror.android.app.Service.attach.call(
                    service,
                    context,
                    VirtualCore.mainThread(),
                    info.name,
                    token,
                    mInitialApplication,
                    ActivityManagerNative.getDefault.call()
            );
            ContextFixer.fixContext(service);
            service.onCreate();
            return service;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to create service " + info.name
                            + ": " + e.toString(), e);
        }
    }

    @Override
    public IBinder createProxyService(ComponentName component, IBinder binder) {
        return ProxyServiceFactory.getProxyService(getCurrentApplication(), component, binder);
    }

    @Override
    public String getDebugInfo() {
        return VirtualRuntime.getProcessName();
    }

    @Override
    public void stopService(ComponentName component) {
        ServiceManager.get().stopService(component);
    }

    private static class RootThreadGroup extends ThreadGroup {

        RootThreadGroup(ThreadGroup parent) {
            super(parent, "VA");
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {

            VLog.e(TAG, "uncaughtException !!!!!!");
            //将异常记录在本地
            exceptionRecorder.recordException(e);

            Thread.UncaughtExceptionHandler threadHandler = null;
            try {
                //当前线程的handler
                threadHandler = Reflect.on(t).get("uncaughtExceptionHandler");
            } catch (Throwable ignore) {

            }
            if(threadHandler == null){
                //应用进程的Thread.class的ClassLoader是系统classloader，所以直接用静态方法
                threadHandler = Thread.getDefaultUncaughtExceptionHandler();
            }
            Thread.UncaughtExceptionHandler handler;
            if (threadHandler != null) {
                handler = threadHandler;
            } else {
                handler = VClient.gClient.crashHandler;
            }
            boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
            //要考虑下面几个情况：
            //1.defHandler.uncaughtException里面自己杀死当前进程，如果是top activity，则会无限重启
            //2.defHandler.uncaughtException里面没有杀死当前进程
            if(isMainThread){
                //如果是activity是最上层，可能会不断重启activity，或者保留一个白色无效的activity
                //返回主界面
                VirtualCore.get().gotoBackHome();
            }
            if (handler != null) {
                handler.uncaughtException(t, e);
            }

            //如果上面方法退出进程，则下面不会执行
            //主进程异常后，是无法响应后续事件，只能杀死
            if (isMainThread) {
                System.exit(0);
            }
        }
    }

    private final class NewIntentData {
        String creator;
        IBinder token;
        Intent intent;
    }

    private final class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }

    private final class ReceiverData {
        PendingResultData pendingResult;
        Intent intent;
        ComponentName component;
        String processName;
        Throwable stacktrace;
    }


    @SuppressLint("HandlerLeak")
    private class H extends Handler {

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NEW_INTENT: {
                    handleNewIntent((NewIntentData) msg.obj);
                    break;
                }
                case RECEIVER: {
                    handleReceiver((ReceiverData) msg.obj);
                    break;
                }
                case FINISH_ACTIVITY: {
                    VActivityManager.get().finishActivity((IBinder) msg.obj);
                    break;
                }
            }
        }
    }
}
