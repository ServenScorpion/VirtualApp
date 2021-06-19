package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.helper.utils.IInterfaceUtils;


public class VSafekeyCkmsManager {

    private static final String TAG = "VSafekeyCkmsManager";
    private static final VSafekeyCkmsManager sInstance = new VSafekeyCkmsManager();
    IVSafekeyCkmsManager mService;

    private Object getRemoteInterface() {
        return IVSafekeyCkmsManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.CKMSSAFEKEY));
    }

    public IVSafekeyCkmsManager getService() {

        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IVSafekeyCkmsManager.class, binder);
            }
        }
        return mService;
    }

    public static VSafekeyCkmsManager get() {
        return sInstance;
    }


    public static byte[] ckmsencryptKey(byte[] key, int keylen) {
        try {
            return get().getService().ckmsencryptKey(key,keylen);
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    public static byte[] ckmsdecrypeKey(byte[] seckey,int seckeylen) {
        try {
            return get().getService().ckmsdecryptKey(seckey,seckeylen);
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    public void registerCallback(IVSKeyCallback ivsKeyCallback) {
        try {
            getService().registerCallback(ivsKeyCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void unregisterCallback() {
        try {
            getService().unregisterCallback();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
