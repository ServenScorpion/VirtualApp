package com.lody.virtual.client.hook.base;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.annotations.LogInvocation;
import com.lody.virtual.client.ipc.VirtualLocationManager;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.VDeviceConfig;

import java.lang.reflect.Method;

/**
 * @author Lody
 */
public abstract class MethodProxy {

    private boolean enable = true;
    private LogInvocation.Condition mInvocationLoggingCondition = LogInvocation.Condition.NEVER; // Inherit

    public MethodProxy() {
        LogInvocation loggingAnnotation = getClass().getAnnotation(LogInvocation.class);
        if (loggingAnnotation != null) {
            this.mInvocationLoggingCondition = loggingAnnotation.value();
        }
    }

    public static String getHostPkg() {
        return VirtualCore.get().getHostPkg();
    }

    public static String getAppPkg() {
        return VClient.get().getCurrentPackage();
    }

    protected static Context getHostContext() {
        return VirtualCore.get().getContext();
    }

    protected static boolean isAppProcess() {
        return VirtualCore.get().isVAppProcess();
    }

    protected static boolean isServerProcess() {
        return VirtualCore.get().isServerProcess();
    }

    protected static boolean isMainProcess() {
        return VirtualCore.get().isMainProcess();
    }

    protected static int getVUid() {
        return VClient.get().getVUid();
    }

    public static int getAppUserId() {
        return VUserHandle.getUserId(getVUid());
    }

    public static int getRealUserId() {
        return VUserHandle.realUserId();
    }

    public static void replaceLastUserId(Object[] args) {
        if (getRealUserId() == 0)
            return;
        int pos = -1;
        for (int i = 0;i < args.length; i++) {
            Object o = args[i];
            if (o instanceof Integer && o == Integer.valueOf(0)) {
                pos = i;
            }
        }
        if (pos >= 0) {
            args[pos] = getRealUserId();
        }
    }

    public static void replaceFirstUserId(Object[] args) {
        if (getRealUserId() == 0)
            return;
        for (int i = 0;i < args.length; i++) {
            Object o = args[i];
            if (o == Integer.valueOf(0)) {
                args[i] = getRealUserId();
                return;
            }
        }
    }

    protected static int getBaseVUid() {
        return VClient.get().getBaseVUid();
    }

    protected static int getRealUid() {
        return VirtualCore.get().myUid();
    }

    protected static SettingConfig getConfig() {
        return VirtualCore.getConfig();
    }

    protected static VDeviceConfig getDeviceConfig() {
        return VClient.get().getDeviceConfig();
    }

    protected static boolean isFakeLocationEnable() {
        return VirtualLocationManager.get().getMode(VUserHandle.myUserId(), VClient.get().getCurrentPackage()) != 0;
    }

    public static boolean isVisiblePackage(ApplicationInfo info) {
        return getHostPkg().equals(info.packageName)
                || ComponentUtils.isSystemApp(info)
                || VirtualCore.get().isOutsidePackageVisible(info.packageName);
    }

    public static boolean isVisiblePackage(String packageName) {
        try {
            ApplicationInfo info = VirtualCore.get().getUnHookPackageManager().getApplicationInfo(packageName, 0);
            return isVisiblePackage(info);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isHostIntent(Intent intent) {
        ComponentName component = intent.getComponent();
        if (component != null) {
            String pkg = component.getPackageName();
            SettingConfig config = VirtualCore.getConfig();
            return pkg.equals(config.getHostPackageName()) || pkg.equals(config.getPluginEnginePackageName());
        }
        return false;
    }

    public abstract String getMethodName();

    public boolean beforeCall(Object who, Method method, Object... args) {
        return true;
    }

    public Object call(Object who, Method method, Object... args) throws Throwable {
        return method.invoke(who, args);
    }

    public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
        return result;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public LogInvocation.Condition getInvocationLoggingCondition() {
        return mInvocationLoggingCondition;
    }

    public void setInvocationloggingCondition(LogInvocation.Condition invocationLoggingCondition) {
        mInvocationLoggingCondition = invocationLoggingCondition;
    }

    public boolean isAppPkg(String pkg) {
        return VirtualCore.get().isAppInstalled(pkg);
    }

    protected PackageManager getPM() {
        return VirtualCore.getPM();
    }

    @Override
    public String toString() {
        return "Method : " + getMethodName();
    }
}
