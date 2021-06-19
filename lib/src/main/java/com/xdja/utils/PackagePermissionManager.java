package com.xdja.utils;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.xdja.zs.VAppPermissionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date 19-4-19 15
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class PackagePermissionManager {

    static final String TAG = PackagePermissionManager.class.getName();

    static private final String ANTON_DIALER = "com.xdja.dialer";
    //keep alive
    private static ArrayList<String> mKeepLiveList = new ArrayList<>();
    //can't uninstall
    private static ArrayList<String> mNoUnInstallList =  new ArrayList<>();

    public static ArrayList<String> getKeepLiveList(){
        if(!mKeepLiveList.contains(ANTON_DIALER))
            mKeepLiveList.add(ANTON_DIALER);
        return mKeepLiveList;
    }
    public static void setKeepLiveList(@NonNull ArrayList<String> list){
        synchronized (mKeepLiveList){
            mKeepLiveList = list;
        }
    }
    public static void addKeepLiveList(@NonNull String packagename){
        synchronized (mKeepLiveList){
            if(!mKeepLiveList.contains(packagename))
                mKeepLiveList.add(packagename);
        }
    }

    public static void removeKeepLiveList(@NonNull String packagename){
        synchronized (mKeepLiveList){
            if(!mKeepLiveList.contains(packagename))
                mKeepLiveList.remove(packagename);
        }
    }

    public static boolean isKeepLiveApp(@NonNull String pkg){
        return getKeepLiveList().contains(pkg);
    }
    public static ArrayList<String> getProtectUninstallList(){
        return mNoUnInstallList;
    }
    public static void setProtectUninstallList(@NonNull ArrayList<String> list){
        synchronized (mNoUnInstallList){
            mNoUnInstallList = list;
        }
    }
    public static boolean isProtectUninstallApp(@NonNull String pkg){
        return mNoUnInstallList.contains(pkg);
    }

    /**
     * 控制安全域内安装源接口
     *
     * @param bundle
     */
    public static void setEnableInstallationSource(Bundle bundle) {
        ArrayList<String> apps = bundle.getStringArrayList("installationSourceList");
        Log.e(TAG,"setEnableInstallationSource "+apps);
        List<String> list = new ArrayList<>(apps);
        try {
            VAppPermissionManager.get().getService().setEnableInstallationSource(list);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public static ArrayList<String> getEnableInstallationSource(){
        ArrayList<String> list = new ArrayList<>();
        try {
            list.addAll(VAppPermissionManager.get().getService().getEnableInstallationSource());
            Log.e(TAG,"EnabledInstallationSource "+list);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return list;
    }
    public static void setInstallSourceSignature(Bundle bundle) {
        ArrayList<String> apps = bundle.getStringArrayList("installSourceSignature");
        Log.e(TAG,"setInstallSourceSignature "+apps);
        List<String> list = new ArrayList<>(apps);
        try {
            VAppPermissionManager.get().getService().setInstallSourceSignature(list);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public static ArrayList<String> getInstallSourceSignature(){
        ArrayList<String> list = new ArrayList<>();
        try {
            list.addAll(VAppPermissionManager.get().getService().getInstallSourceSignature());
            Log.e(TAG,"getInstallSourceSignature "+list);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return list;
    }
}