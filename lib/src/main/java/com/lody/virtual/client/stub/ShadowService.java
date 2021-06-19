package com.lody.virtual.client.stub;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Process;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.service.ServiceManager;
import com.lody.virtual.helper.utils.VLog;

/**
 * @author Lody
 */
public abstract class ShadowService extends Service {
    private static final ServiceManager sServiceManager = ServiceManager.get();

    private boolean checkProcessStatus() {
        if (VClient.get().getClientConfig() == null) {
            stopSelf();
            VLog.w("ServiceManager", "checkProcessStatus:false, clientConfig=null");
            //Process.killProcess(Process.myPid());
            //杀死会让reject init process错误出现概率变高
            return false;
        }
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(!checkProcessStatus()){
            return null;
        }
        return sServiceManager.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!checkProcessStatus()){
            return START_NOT_STICKY;
        }
        try {
            sServiceManager.onStartCommand(intent, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(!checkProcessStatus()){
            return false;
        }
        sServiceManager.onUnbind(intent);
        return false;
    }

    @Override
    public void onLowMemory() {
        sServiceManager.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        sServiceManager.onTrimMemory(level);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        sServiceManager.onConfigurationChanged(newConfig);
    }


    @Override
    public void onDestroy() {
        sServiceManager.onDestroy();
    }

    public static class P0 extends ShadowService {
    }

    public static class P1 extends ShadowService {
    }

    public static class P2 extends ShadowService {
    }

    public static class P3 extends ShadowService {
    }

    public static class P4 extends ShadowService {
    }

    public static class P5 extends ShadowService {
    }

    public static class P6 extends ShadowService {
    }

    public static class P7 extends ShadowService {
    }

    public static class P8 extends ShadowService {
    }

    public static class P9 extends ShadowService {
    }

    public static class P10 extends ShadowService {
    }

    public static class P11 extends ShadowService {
    }

    public static class P12 extends ShadowService {
    }

    public static class P13 extends ShadowService {
    }

    public static class P14 extends ShadowService {
    }

    public static class P15 extends ShadowService {
    }

    public static class P16 extends ShadowService {
    }

    public static class P17 extends ShadowService {
    }

    public static class P18 extends ShadowService {
    }

    public static class P19 extends ShadowService {
    }

    public static class P20 extends ShadowService {
    }

    public static class P21 extends ShadowService {
    }

    public static class P22 extends ShadowService {
    }

    public static class P23 extends ShadowService {
    }

    public static class P24 extends ShadowService {
    }

    public static class P25 extends ShadowService {
    }

    public static class P26 extends ShadowService {
    }

    public static class P27 extends ShadowService {
    }

    public static class P28 extends ShadowService {
    }

    public static class P29 extends ShadowService {
    }

    public static class P30 extends ShadowService {
    }

    public static class P31 extends ShadowService {
    }

    public static class P32 extends ShadowService {
    }

    public static class P33 extends ShadowService {
    }

    public static class P34 extends ShadowService {
    }

    public static class P35 extends ShadowService {
    }

    public static class P36 extends ShadowService {
    }

    public static class P37 extends ShadowService {
    }

    public static class P38 extends ShadowService {
    }

    public static class P39 extends ShadowService {
    }

    public static class P40 extends ShadowService {
    }

    public static class P41 extends ShadowService {
    }

    public static class P42 extends ShadowService {
    }

    public static class P43 extends ShadowService {
    }

    public static class P44 extends ShadowService {
    }

    public static class P45 extends ShadowService {
    }

    public static class P46 extends ShadowService {
    }

    public static class P47 extends ShadowService {
    }

    public static class P48 extends ShadowService {
    }

    public static class P49 extends ShadowService {
    }

    public static class P50 extends ShadowService {
    }

    public static class P51 extends ShadowService {
    }

    public static class P52 extends ShadowService {
    }

    public static class P53 extends ShadowService {
    }

    public static class P54 extends ShadowService {
    }

    public static class P55 extends ShadowService {
    }

    public static class P56 extends ShadowService {
    }

    public static class P57 extends ShadowService {
    }

    public static class P58 extends ShadowService {
    }

    public static class P59 extends ShadowService {
    }

    public static class P60 extends ShadowService {
    }

    public static class P61 extends ShadowService {
    }

    public static class P62 extends ShadowService {
    }

    public static class P63 extends ShadowService {
    }

    public static class P64 extends ShadowService {
    }

    public static class P65 extends ShadowService {
    }

    public static class P66 extends ShadowService {
    }

    public static class P67 extends ShadowService {
    }

    public static class P68 extends ShadowService {
    }

    public static class P69 extends ShadowService {
    }

    public static class P70 extends ShadowService {
    }

    public static class P71 extends ShadowService {
    }

    public static class P72 extends ShadowService {
    }

    public static class P73 extends ShadowService {
    }

    public static class P74 extends ShadowService {
    }

    public static class P75 extends ShadowService {
    }

    public static class P76 extends ShadowService {
    }

    public static class P77 extends ShadowService {
    }

    public static class P78 extends ShadowService {
    }

    public static class P79 extends ShadowService {
    }

    public static class P80 extends ShadowService {
    }

    public static class P81 extends ShadowService {
    }

    public static class P82 extends ShadowService {
    }

    public static class P83 extends ShadowService {
    }

    public static class P84 extends ShadowService {
    }

    public static class P85 extends ShadowService {
    }

    public static class P86 extends ShadowService {
    }

    public static class P87 extends ShadowService {
    }

    public static class P88 extends ShadowService {
    }

    public static class P89 extends ShadowService {
    }

    public static class P90 extends ShadowService {
    }

    public static class P91 extends ShadowService {
    }

    public static class P92 extends ShadowService {
    }

    public static class P93 extends ShadowService {
    }

    public static class P94 extends ShadowService {
    }

    public static class P95 extends ShadowService {
    }

    public static class P96 extends ShadowService {
    }

    public static class P97 extends ShadowService {
    }

    public static class P98 extends ShadowService {
    }

    public static class P99 extends ShadowService {
    }

}
