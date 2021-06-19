package com.xdja.activitycounter;

import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;

/**
 * @Date 18-11-28 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class ActivityCounterManager {
    private static final ActivityCounterManager sInstance = new ActivityCounterManager();
    public static ActivityCounterManager get() { return sInstance; }

    private  IActivityCounterService mRemote;
    public  IActivityCounterService getRemote() {
        if (mRemote == null ||
                (!mRemote.asBinder().isBinderAlive() && !VirtualCore.get().isVAppProcess())) {
            synchronized (ActivityCounterManager.class) {
                Object remote = getStubInterface();
                mRemote = LocalProxyUtils.genProxy( IActivityCounterService.class, remote);
            }
        }
        return mRemote;
    }
    private Object getStubInterface() {
        return  IActivityCounterService.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.FLOATICONBALL));
    }
    public void activityCountAdd(String pkg, String name, int pid ){
        try {
            getRemote().activityCountAdd(pkg,name,pid);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
    public void activityCountReduce(String pkg,String name,int pid){
        try {
            getRemote().activityCountReduce(pkg,name,pid);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
    public void cleanProcess(int pid){
        try{
            getRemote().cleanProcess(pid);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
    public void cleanPackage(String pkg){
        try{
            getRemote().cleanPackage(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
    public boolean isForeGroundApp(String pkg){
        try {
            return getRemote().isForeGroundApp(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
        return false;
    }public boolean isForeGround(){
        try {
            return getRemote().isForeGround();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
        return false;
    }
    public void registerCallback(IForegroundInterface fibCallback) {
        try {
            getRemote().registerCallback(fibCallback);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void unregisterCallback() {
        try {
            getRemote().unregisterCallback();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
}
