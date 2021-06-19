package com.lody.virtual.server.interfaces;

import android.os.RemoteException;
import java.util.List;
import com.lody.virtual.remote.VDeviceConfig;

/**
 * @author Lody
 */
interface IDeviceManager{

    VDeviceConfig getDeviceConfig(int userId);

    void updateDeviceConfig(int userId,in  VDeviceConfig config);

    boolean isEnable(int userId);

    void setEnable(int userId, boolean enable);

}
