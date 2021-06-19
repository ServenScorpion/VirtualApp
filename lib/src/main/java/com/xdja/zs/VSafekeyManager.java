package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.helper.utils.IInterfaceUtils;

/**
 * Created by wxudong on 18-1-23.
 */

public class VSafekeyManager {
    private static final VSafekeyManager sInstance = new VSafekeyManager();
    IVSafekey mService;

    private Object getRemoteInterface() {
        return IVSafekey.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.SAFEKEY));
    }

    public IVSafekey getService() {

        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IVSafekey.class, binder);
            }
        }
        return mService;
    }

    public static VSafekeyManager get() {
        return sInstance;
    }

    public boolean checkCardState() {
        try {
            return getService().checkCardState();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public String getCardId() {
        try {
            return getService().getCardId();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getPinTryCount() {
        try {
            return getService().getPinTryCount();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void registerCallback(IVSCallback vsCallback) {
        try {
            getService().registerCallback(vsCallback);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void unregisterCallback() {
        try {
            getService().unregisterCallback();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public int initSafekeyCard() {
        try {
            return getService().initSafekeyCard();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
            return -1;
        }
    }


    public static byte[] encryptKey(byte[] key, int keylen) {
        try {
            return get().getService().encryptKey(key, keylen);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }


    public static byte[] decryptKey(byte[] seckey, int seckeylen) {
        try {
            return get().getService().decryptKey(seckey, seckeylen);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public static byte[] getRandom(int len) {
        try {
            return get().getService().getRandom(len);

        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }
}