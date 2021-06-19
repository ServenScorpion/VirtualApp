package com.xdja.zs;

import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.helper.utils.Singleton;

import java.util.ArrayList;
import java.util.List;


/**
 * @Date 19-11-20 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class InstallerSettingService extends  IInstallerSetting.Stub{

    private static final String TAG = InstallerSettingService.class.getSimpleName();
    private static final Singleton<InstallerSettingService> sService = new Singleton<InstallerSettingService>() {
        @Override
        protected InstallerSettingService create() {
            return new InstallerSettingService();
        }
    };

    public static InstallerSettingService get() {
        return sService.get();
    }

    @Override
    public List<String> getSystemApps() throws RemoteException {
        List<String> list =  new ArrayList<>();
        list.addAll(InstallerSetting.systemApps);
        return list;
    }

    @Override
    public void setSystemApps(List<String> list) throws RemoteException {
        synchronized (InstallerSetting.systemApps){
            InstallerSetting.systemApps.addAll(list);
        }
    }

    @Override
    public void addSystemApp(String packagename) throws RemoteException {
        synchronized (InstallerSetting.systemApps){
            InstallerSetting.systemApps.add(packagename);
        }
    }

    @Override
    public void removeSystemApp(String packagename) throws RemoteException {
        synchronized (InstallerSetting.systemApps){
            InstallerSetting.systemApps.remove(packagename);
        }
    }
    @Override
    public boolean isSystemApp(String pkg) throws RemoteException {
        synchronized (InstallerSetting.systemApps) {
            return InstallerSetting.systemApps.contains(pkg);
        }
    }
}
