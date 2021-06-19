package com.lody.virtual.client.ipc;


import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.server.interfaces.IVirtualStorageService;

/**
 * @author Lody
 */

public class VirtualStorageManager {

    private static final VirtualStorageManager sInstance = new VirtualStorageManager();

    public static VirtualStorageManager get() {
        return sInstance;
    }

    private IVirtualStorageService mService;

    public IVirtualStorageService getService() {
        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IVirtualStorageService.class, binder);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return IVirtualStorageService.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.VS));
    }

    public String getVirtualStorage(String packageName, int userId) {
        try {
            return getService().getVirtualStorage(packageName, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void setVirtualStorageState(String packageName, int userId, boolean enable) {
        try {
            getService().setVirtualStorageState(packageName, userId, enable);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isVirtualStorageEnable(String packageName, int userId) {
        try {
            return getService().isVirtualStorageEnable(packageName, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void setVirtualStorage(String packageName, int userId, String vsPath) {
        try {
            getService().setVirtualStorage(packageName, userId, vsPath);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
}
