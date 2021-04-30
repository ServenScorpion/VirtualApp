package io.virtualapp.home.models;

import android.content.Context;

import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.VDeviceConfig;

public class DeviceData extends SettingsData {
    public DeviceData(Context context, InstalledAppInfo installedAppInfo, int userId) {
        super(context, installedAppInfo, userId);
    }

    public boolean isMocking() {
        return VDeviceManager.get().isEnable(userId);
    }
}
