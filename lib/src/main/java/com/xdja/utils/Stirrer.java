package com.xdja.utils;

import android.content.ContentProviderClient;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.compat.BuildCompat;

import mirror.android.content.ContentProviderClientICS;
import mirror.android.content.ContentProviderClientJB;
import mirror.android.content.ContentProviderClientQ;

public class Stirrer {

    private static final String TAG = "xela-" + new Object() {
    }.getClass().getEnclosingClass().getSimpleName();

    public static void preInit() {
        {
            getConentProvider("media");
        }
    }

    public static void kickOff(String packageName) {
        if (packageName != null && VirtualCore.get().isAppInstalled(packageName)) {
            Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(packageName);
            VActivityManager.get().sendBroadcast(intent, 0);
        } else {
            Log.d(TAG, "failed to kick-off " + packageName);
        }
    }

    public static ContentProviderClient getConentProvider(String authority) {
        ContentProviderClient contentProviderClient = null;
        ProviderInfo info = VPackageManager.get().resolveContentProvider(authority, 0, 0);
        if (info != null) {
            try {
                IInterface provider = VActivityManager.get().acquireProviderClient(0, info);
                if (provider != null) {
                    if (BuildCompat.isQ()) {
                        contentProviderClient = ContentProviderClientQ.ctor.newInstance(VirtualCore.get().getContext().getContentResolver(), provider, authority, true);
                    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        contentProviderClient = ContentProviderClientJB.ctor.newInstance(VirtualCore.get().getContext().getContentResolver(), provider, true);
                    } else {
                        contentProviderClient = ContentProviderClientICS.ctor.newInstance(VirtualCore.get().getContext().getContentResolver(), provider);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "no provider info of \"" + authority + "\" found");
        }

        return contentProviderClient;
    }
}
