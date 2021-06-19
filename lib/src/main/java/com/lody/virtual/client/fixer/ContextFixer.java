package com.lody.virtual.client.fixer;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.IInterface;
import android.os.health.SystemHealthManager;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.proxies.alarm.AlarmManagerStub;
import com.lody.virtual.client.hook.proxies.appops.AppOpsManagerStub;
import com.lody.virtual.client.hook.proxies.battery_stats.BatteryStatsHub;
import com.lody.virtual.client.hook.proxies.dropbox.DropBoxManagerStub;
import com.lody.virtual.client.hook.proxies.graphics.GraphicsStatsStub;
import com.lody.virtual.client.hook.proxies.wifi.WifiManagerStub;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.client.ipc.VLocationManager;
import com.lody.virtual.helper.compat.StrictModeCompat;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

import java.util.concurrent.Executor;

import mirror.android.app.ActivityThread;
import mirror.android.app.ContextImpl;
import mirror.android.app.ContextImplKitkat;
import mirror.android.app.LoadedApk;
import mirror.android.content.ContentResolverJBMR2;

/**
 * @author Lody
 */
public class ContextFixer {

    /**
     * Fuck AppOps
     *
     * @param context Context
     */
    public static void fixContext(Context context) {
        try {
            context.getPackageName();
        } catch (Throwable e) {
            return;
        }
        InvocationStubManager.getInstance().checkEnv(GraphicsStatsStub.class);
        int deep = 0;
        while (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
            deep++;
            if (deep >= 10) {
                return;
            }
        }
        ContextImpl.mPackageManager.set(context, null);
        try {
            context.getPackageManager();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (!VirtualCore.get().isVAppProcess()) {
            return;
        }
        String hostPkg = VirtualCore.get().getHostPkg();
        if (LoadedApk.mApplication != null) {
            //fix LoadedApk's application
            Object info = ContextImpl.mPackageInfo.get(context);
            if (info != null) {
                Application application = LoadedApk.mApplication.get(info);
                if (application != null && hostPkg.equals(application.getPackageName())) {
                    //
                    VLog.w("kk", "application is host!!");
                    Application app = VClient.get().getCurrentApplication();
                    if(app != null) {
                        LoadedApk.mApplication.set(info, app);
                    }
                }
            }
        }

        ContextImpl.mBasePackageName.set(context, hostPkg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContextImplKitkat.mOpPackageName.set(context, hostPkg);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ContentResolverJBMR2.mPackageName.set(context.getContentResolver(), hostPkg);
        }
    }
}
