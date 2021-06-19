package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.helper.utils.IInterfaceUtils;

import java.util.List;

/**
 * @Date 19-11-20 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class InstallerSettingManager {

    private static final InstallerSettingManager sInstance = new InstallerSettingManager();
    IInstallerSetting mService;

    private Object getRemoteInterface() {
        return IInstallerSetting.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.INSTALLERSETTING));
    }

    public IInstallerSetting getService() {

        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IInstallerSetting.class, binder);
            }
        }
        return mService;
    }

    public static InstallerSettingManager get() {
        return sInstance;
    }


    public  List<String> getSystemApps() {
        try {
            return getService().getSystemApps();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setSystemApps(List<String> list) {
        try {
            getService().setSystemApps(list);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void addSystemApp(String packagename){
        try {
            getService().addSystemApp(packagename);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void removeSystemApp(String packagename) {
        try {
            getService().removeSystemApp(packagename);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isSystemApp(String pkg) {
        try {
            return getService().isSystemApp(pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }
}
