package com.lody.virtual.server.device;

import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.remote.VDeviceConfig;
import com.lody.virtual.server.interfaces.IDeviceManager;

/**
 * @author Lody
 */
public class VDeviceManagerService extends IDeviceManager.Stub {

    private static final VDeviceManagerService sInstance = new VDeviceManagerService();
    final SparseArray<VDeviceConfig> mDeviceConfigs = new SparseArray<>();
    private DeviceInfoPersistenceLayer mPersistenceLayer = new DeviceInfoPersistenceLayer(this);

    public static VDeviceManagerService get() {
        return sInstance;
    }


    private VDeviceManagerService() {
        mPersistenceLayer.read();
        for (int i = 0; i < mDeviceConfigs.size(); i++) {
            VDeviceConfig info = mDeviceConfigs.valueAt(i);
            VDeviceConfig.addToPool(info);
        }
    }


    @Override
    public VDeviceConfig getDeviceConfig(int userId) {
        VDeviceConfig info;
        synchronized (mDeviceConfigs) {
            info = mDeviceConfigs.get(userId);
            if (info == null) {
                info = VDeviceConfig.random();
                mDeviceConfigs.put(userId, info);
                mPersistenceLayer.save();
            }
        }
        return info;
    }

    @Override
    public void updateDeviceConfig(int userId, VDeviceConfig config) {
        synchronized (mDeviceConfigs) {
            if (config != null) {
                mDeviceConfigs.put(userId, config);
                mPersistenceLayer.save();
            }
        }
    }

    @Override
    public boolean isEnable(int userId) {
        return getDeviceConfig(userId).enable;
    }

    @Override
    public void setEnable(int userId, boolean enable) {
        synchronized (mDeviceConfigs) {
            VDeviceConfig info = mDeviceConfigs.get(userId);
            if (info == null) {
                info = VDeviceConfig.random();
                mDeviceConfigs.put(userId, info);
            }
            info.enable = enable;
            mPersistenceLayer.save();
        }
    }

}
