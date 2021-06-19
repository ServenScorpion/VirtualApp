package com.lody.virtual.client.env;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;

import mirror.android.ddm.DdmHandleAppName;
import mirror.android.ddm.DdmHandleAppNameJBMR1;
import mirror.dalvik.system.VMRuntime;

/**
 * @author Lody
 * <p>
 * <p/>
 * Runtime Environment for App.
 */
public class VirtualRuntime {

    private static final Handler sUIHandler = new Handler(Looper.getMainLooper());

    private static String sInitialPackageName;
    private static String sProcessName;

    public static Handler getUIHandler() {
        return sUIHandler;
    }

    public static String getProcessName() {
        return sProcessName;
    }

    public static String getInitialPackageName() {
        return sInitialPackageName;
    }

    public static void setupRuntime(String processName, ApplicationInfo appInfo) {
        if (sProcessName != null) {
            return;
        }
        sInitialPackageName = appInfo.packageName;
        sProcessName = processName;
        mirror.android.os.Process.setArgV0.call(processName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DdmHandleAppNameJBMR1.setAppName.call(processName, 0);
        } else {
            DdmHandleAppName.setAppName.call(processName);
        }
    }

    public static <T> T crash(Throwable e) throws RuntimeException {
        e.printStackTrace();
        throw new RuntimeException("transact remote server failed", e);
    }

    public static boolean is64bit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Process.is64Bit();
        }
        return VMRuntime.is64Bit.call(VMRuntime.getRuntime.call());

    }

    public static void exit() {
        VLog.d(VirtualRuntime.class.getSimpleName(), "Exit process : %s (%s).", getProcessName(), VirtualCore.get().getProcessName());
        Process.killProcess(android.os.Process.myPid());
    }


    public static boolean isArt() {
        return System.getProperty("java.vm.version").startsWith("2");
    }
}
