package com.lody.virtual.client.stub;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;


/**
 * @author Lody
 */
public class KeepAliveService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(!VirtualCore.getConfig().isHideForegroundNotification()) {
            HiddenForeNotification.bindForeground(this);
        }
    }

    @Override
    public void onDestroy() {
        if(!VirtualCore.getConfig().isHideForegroundNotification()) {
            HiddenForeNotification.hideForeground(this);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
