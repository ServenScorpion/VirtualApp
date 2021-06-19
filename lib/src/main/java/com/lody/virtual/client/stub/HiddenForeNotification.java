package com.lody.virtual.client.stub;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.IBinder;

import com.lody.virtual.R;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.NotificationChannelCompat;
import com.lody.virtual.helper.utils.VLog;

public class HiddenForeNotification extends Service {
    private static final int ID = 2781;

    public static void hideForeground(Service service) {
        service.stopForeground(true);
        if (VERSION.SDK_INT <= 24) {
            service.stopService(new Intent(service, HiddenForeNotification.class));
        }
    }

    public static void bindForeground(Service service) {
        service.startForeground(ID, VirtualCore.getConfig().getForegroundNotification());
        if (VERSION.SDK_INT <= 24) {
            service.startService(new Intent(service, HiddenForeNotification.class));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(VirtualCore.getConfig().isHideForegroundNotification()) {
            startForeground();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!VirtualCore.getConfig().isHideForegroundNotification()) {
            try {
                startForeground();
                stopForeground(true);
                stopSelf();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return START_NOT_STICKY;
    }

    private void startForeground(){
        startForeground(ID, VirtualCore.getConfig().getForegroundNotification());
    }

    @Override
    public void onDestroy() {
        if(VirtualCore.getConfig().isHideForegroundNotification()) {
            stopForeground(true);
        }
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}