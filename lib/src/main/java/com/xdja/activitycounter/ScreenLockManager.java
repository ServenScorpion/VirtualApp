package com.xdja.activitycounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
/**
 * @Date 19-03-20 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class ScreenLockManager extends BaseCounterManager{

    private String TAG = "ScreenLockManager";

    private static boolean isScreenOn = false;

    final int UNLOCK = 0; // 亮屏到前台
    final int LOCK = 1; //  手机灭屏
    final int SHOW = 2; //  到前台
    final int HIDE = 3; //  到后台
    final int INCALL = 4; //来电页面到前台

    public ScreenLockManager(){

        IntentFilter slFilter = new IntentFilter();
        slFilter.addAction(Intent.ACTION_SCREEN_OFF);
        VirtualCore.get().getContext().registerReceiver(new ScreenLockReceiver(),slFilter);
    }

    @Override
    synchronized void changeState(int mode, boolean on,String name) {
        Log.e(TAG,"name " + name);
        //安通拨号，微信，安通＋,安全通话, 闹钟
        if (VirtualCore.getConfig().isFloatOnLockScreen(name)){
            Log.e(TAG,"Incall Activity " + name);
            screenLock(4);
            return;
        }
        Log.e(TAG,"isScreenOn " + isScreenOn+", name="+name);
        if(!isScreenOn  && on){
            screenLock(UNLOCK);
            isScreenOn = true; // 进入安全盒后清除锁屏状态
        }else {
            screenLock(on?SHOW:HIDE);
        }
    }

    private void screenLock(int on) {
        Log.e(TAG,"screenlock " + on);
        if(mForegroundInterface==null){
            Log.e(TAG,"ScreenLock callback is null! ");
            return;
        }
        try {
            mForegroundInterface.screenChanged(on);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    class ScreenLockReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.e(TAG,"onReceive " + intent.getAction());
           if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
               //快速锁定解锁，锁屏广播发送的慢
               isScreenOn = false;
               PowerManager pm =(PowerManager)context.getSystemService(Context.POWER_SERVICE);
               if(!pm.isScreenOn()){ //如果灭屏了通知锁屏状态
                   screenLock(LOCK);
               }else{ //如果没有锁屏（很快解锁了）主动调用生命周期相关的状态切换，//不记录状态
                   changeState(ActivityCounterService.ADD,ActivityCounterService.get().getFroundCount()>0,null);
               }

            }
        }
    }
}
