package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.helper.utils.IInterfaceUtils;

public class VServiceKeepAliveManager {

    public static final int ACTION_ADD = 1;
    public static final int ACTION_DEL = 2;
    public static final int ACTION_TEMP_ADD = 3;
    public static final int ACTION_TEMP_DEL = 4;

    private static final VServiceKeepAliveManager sInstance = new VServiceKeepAliveManager();
    IServiceKeepAlive mService;

    private Object getRemoteInterface() {
        return IServiceKeepAlive.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.KEEPALIVE));
    }

    public IServiceKeepAlive getService() {
        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IServiceKeepAlive.class, binder);
            }
        }
        return mService;
    }

    public static VServiceKeepAliveManager get() {
        return sInstance;
    }

    public void scheduleUpdateKeepAliveList(String pkgName, int action) {
        try {
            get().getService().scheduleUpdateKeepAliveList(pkgName, action);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void scheduleRunKeepAliveService(String pkgName, int userId) {
        try {
            get().getService().scheduleRunKeepAliveService(pkgName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean inKeepAliveServiceList(String pkgName) {
        try {
            return get().getService().inKeepAliveServiceList(pkgName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }
}
