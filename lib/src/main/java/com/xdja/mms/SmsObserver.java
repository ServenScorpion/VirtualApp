package com.xdja.mms;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

/**
 * sms 短信
 * mms 彩信
 */
public class SmsObserver {
    private static final String TAG = SmsObserver.class.getSimpleName();

    private static SmsObserver sSmsObserver = new SmsObserver();

    private static SmsObserver getInstance() {
        return sSmsObserver;
    }

    private Context getContext() {
        return VirtualCore.get().getContext();
    }

    private synchronized static Handler getAsyncHandler() {
        if (sAsyncHandlerThread == null) {
            sAsyncHandlerThread = new HandlerThread("sms_async_handler_thread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sAsyncHandlerThread.start();
            sAsyncHandler = new Handler(sAsyncHandlerThread.getLooper());
        }
        return sAsyncHandler;
    }

    private static HandlerThread sAsyncHandlerThread;
    private static Handler sAsyncHandler;

    public static void observe() {
        Log.d(TAG, "Observe mms log");
        getInstance().observeInner();
    }

    private void observeInner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Context context = getContext();
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(SmsConstants.DEFAULT_SMS_APP_SETTING), true, new ContentObserver(getAsyncHandler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    onDefaultPackageChange(context);
                }
            });
            onDefaultPackageChange(context);
        }
    }

    private String lastPackageName;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void onDefaultPackageChange(Context context) {
        SmsManager smsManager = SmsManager.getDefault();
        String pkg = Telephony.Sms.getDefaultSmsPackage(context);
        if (VirtualCore.get().getHostPkg().equals(pkg)) {
            if(!TextUtils.equals(lastPackageName, pkg)) {
                try {
                    if (mirror.android.telephony.SmsManager.setAutoPersisting != null) {
                        mirror.android.telephony.SmsManager.setAutoPersisting.call(smsManager, false);
                        VLog.i(TAG, "setAutoPersisting=false");
                    }
                } catch (Throwable e) {
                    VLog.e(TAG, "setAutoPersisting", e);
                }
            }
        } else {
            VLog.i(TAG, "onDefaultPackageChange:pkg=%s", pkg);
        }
        lastPackageName = pkg;
    }

}