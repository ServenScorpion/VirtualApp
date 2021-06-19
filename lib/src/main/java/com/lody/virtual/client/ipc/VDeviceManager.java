package com.lody.virtual.client.ipc;

import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.ReflectException;
import com.lody.virtual.remote.VDeviceConfig;
import com.lody.virtual.server.interfaces.IDeviceManager;

import java.util.Map;

import mirror.android.os.Build;

/**
 * @author Lody
 */

public class VDeviceManager {

    private static final VDeviceManager sInstance = new VDeviceManager();

    public static VDeviceManager get() {
        return sInstance;
    }

    private IDeviceManager mService;

    public IDeviceManager getService() {
        if (!IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IDeviceManager.class, binder);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return IDeviceManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.DEVICE));
    }


    public VDeviceConfig getDeviceConfig(int userId) {
        try {
            return getService().getDeviceConfig(userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void updateDeviceConfig(int userId, VDeviceConfig config) {
        try {
            getService().updateDeviceConfig(userId, config);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isEnable(int userId) {
        try {
            return getService().isEnable(userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void setEnable(int userId, boolean enable) {
        try {
            getService().setEnable(userId, enable);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void applyBuildProp(VDeviceConfig config) {
        for (Map.Entry<String, String> entry : config.buildProp.entrySet()) {
            try {
                Reflect.on(Build.TYPE).set(entry.getKey(), entry.getValue());
            } catch (ReflectException e) {
                e.printStackTrace();
            }
        }
        if (config.serial != null) {
            Reflect.on(Build.TYPE).set("SERIAL", config.serial);
        }
    }
}
