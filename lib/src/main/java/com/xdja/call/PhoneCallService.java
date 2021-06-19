package com.xdja.call;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.telecom.InCallService;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.M)
public class PhoneCallService extends InCallService {

    private IBinder dialerBinder = null;
    private String TAG = "PhoneCallService";

    @Override
    public void onCreate() {
        Log.i(TAG, " PhoneCallService onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, " PhoneCallService onStartCommand");
        bindDialerService();
        return super.onStartCommand(intent, flags, startId);
    }

    public void bindDialerService(){
        Intent serviceIntent = new Intent();
        serviceIntent.setAction("android.telecom.InCallService");
        serviceIntent.putExtra("_VA_|_user_id_", 0);
        serviceIntent.setComponent(new ComponentName("com.xdja.dialer","com.xdja.incallui.InCallServiceImpl"));
        bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, " PhoneCallService onBind " + intent.toString());
        if (dialerBinder != null && dialerBinder.isBinderAlive()) {
            Log.i(TAG, " return dialerBinder");
            return dialerBinder;
        }
        Log.i(TAG, " return error dialerBinder");
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, " PhoneCallService onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, " PhoneCallService onUnbind");
        return super.onUnbind(intent);
    }

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            dialerBinder = service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected");
            unbindService(conn);
            stopSelf();
        }
    };
}
