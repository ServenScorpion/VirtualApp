package com.xdja.zs;

import android.content.ClipData;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;
import com.xdja.utils.SignatureVerify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by geyao on 2018/1/22.
 * 应用能力
 */

public class VAppPermissionManagerService extends IAppPermission.Stub {
    private static final String TAG = VAppPermissionManagerService.class.getSimpleName();

    /**
     * 权限信息缓存map
     */
    private static Map<String, Boolean> functionMaps;

    /**
     * 缓存透明加解密配置信息
     */
    private static Set<String> encryptConfig;
    /**
     * 权限监听
     */
    private IAppPermissionCallback iAppPermissionCallback;
    /**
     * 缓存剪切板信息
     */
    private static ClipData clipDataCache;
    /**
     * 缓存剪切板变化监听
     */
    private static IOnPrimaryClipChangedListener pcListener;

    /**
     * 缓存是否允许安装第三方应用
     */
    private static boolean isAllowThirdAppInstallion;

    public static void systemReady() {
        functionMaps = new HashMap<>();
        encryptConfig = new HashSet<>();
    }

    private static final Singleton<VAppPermissionManagerService> sService = new Singleton<VAppPermissionManagerService>() {
        @Override
        protected VAppPermissionManagerService create() {
            return new VAppPermissionManagerService();
        }
    };

    public static VAppPermissionManagerService get() {
        return sService.get();
    }

    /**
     * 是否是支持的权限
     *
     * @param permissionName 权限名称
     * @return 是否是支持的权限
     */
    @Override
    public boolean isSupportPermission(String permissionName) {
        List<String> list = Arrays.asList(VAppPermissionManager.permissions);
        return list.contains(permissionName);
    }

    @Override
    public boolean isSupportEncrypt(String packageName) throws RemoteException {
        return false;
    }

    /**
     * 清除权限数据
     */
    @Override
    public void clearPermissionData() {
        functionMaps.clear();
    }

    /**
     * 设置应用权限开关
     *
     * @param packageName      应用包名
     * @param isPermissionOpen 开关状态
     */
    @Override
    public void setAppPermission(String packageName, String appPermissionName, boolean isPermissionOpen) {
        //若策略是关闭网络 则关闭应用进程的socket长链接
        if (VAppPermissionManager.PROHIBIT_NETWORK.equals(appPermissionName) && isPermissionOpen) {
            VLog.e(TAG, "close long socket packageName: " + packageName);
            VActivityManager.get().closeAllLongSocket(packageName, 0);
        }else if(VAppPermissionManager.PROHIBIT_WATER_MARK.equalsIgnoreCase(appPermissionName)){
            //后台于终端策略默认值不统一，后台为启用水印功能 而终端为 禁用水印功能
            functionMaps.put(buildKey(packageName, appPermissionName),!isPermissionOpen);
            return;
        } else if(VAppPermissionManager.ALLOW_DATA_ENCRYPT.equals(appPermissionName)) {
            if (isPermissionOpen) {
                encryptConfig.add(packageName);
            } else {
                encryptConfig.remove(packageName);
            }
            return;
        }
        functionMaps.put(buildKey(packageName, appPermissionName), isPermissionOpen);
    }

    /**
     * 获取应用权限开关状态
     *
     * @param packageName 应用包名
     * @return 应用权限开关状态
     */
    @Override
    public boolean getAppPermissionEnable(String packageName, String appPermissionName) {
        if (VAppPermissionManager.ALLOW_DATA_ENCRYPT.equals(appPermissionName)) {
            return encryptConfig.contains(packageName);
        }
        Boolean aBoolean = functionMaps.get(buildKey(packageName, appPermissionName));
        if (aBoolean == null) {
            VLog.e(TAG, "result is null return false");
            return false;
        }
        VLog.e(TAG, "result: " + aBoolean);
        return aBoolean;
    }

    /**
     * 注册监听
     *
     * @param iAppPermissionCallback 监听
     */
    @Override
    public void registerCallback(IAppPermissionCallback iAppPermissionCallback) {
        if (iAppPermissionCallback == null) {
            this.iAppPermissionCallback = null;
            return;
        }
        this.iAppPermissionCallback = iAppPermissionCallback;
    }

    /**
     * 解除监听注册
     */
    @Override
    public void unregisterCallback() {
        iAppPermissionCallback = null;
    }

    /**
     * 权限拦截触发回调
     *
     * @param appPackageName 应用包名
     * @param permissionName 权限名称
     */
    @Override
    public void interceptorTriggerCallback(String appPackageName, String permissionName) throws RemoteException {
        if (iAppPermissionCallback == null) {
            VLog.d(TAG, "callback is null");
            return;
        }
        iAppPermissionCallback.onPermissionTrigger(appPackageName, permissionName);
    }

    /**
     * 缓存剪切板信息
     *
     * @param clipData 剪切板信息
     */
    @Override
    public void cacheClipData(ClipData clipData) {
        clipDataCache = clipData;
    }

    /**
     * 获取剪切板信息
     *
     * @return 剪切板信息
     */
    @Override
    public ClipData getClipData() {
        return clipDataCache;
    }

    /**
     * 缓存剪切板数据改变监听
     *
     * @param listener 监听
     */
    @Override
    public void cachePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
        pcListener = listener;
    }

    /**
     * 获取剪切板数据改变监听
     */
    @Override
    public void callPrimaryClipChangedListener() throws RemoteException {
        if (pcListener == null) {
            return;
        }
        pcListener.dispatchPrimaryClipChanged();
    }

    /**
     * 移除剪切板数据改变监听
     */
    @Override
    public void removePrimaryClipChangedListener() {
        pcListener = null;
    }

    @Override
    public boolean getLocationEnable(String packageName) throws RemoteException {
        return false;
    }

    /**
     * 设置安装第三方应用状态
     * @hide 方法过期，
     * @param isEnable 是否可安装 true:允许安装第三方应用 false:不允许安装第三方应用
     */
    @Override
    public void setThirdAppInstallationEnable(boolean isEnable) {
        isAllowThirdAppInstallion = isEnable;
    }

    /**
     * 获取是否可以安装第三方应用状态
     * @hide
     * @return 是否可以安装第三方应用状态 true:可以安装第三方应用 false:不可以安装第三方应用
     */
    @Override
    public boolean getThirdAppInstallationEnable() {
        return isAllowThirdAppInstallion;
    }

    /**
     * 获取透明加解密配置
     */
    @Override
    public String[] getEncryptConfig() {
        if (encryptConfig.size() == 0) {
            return null;
        }
        return encryptConfig.toArray(new String[encryptConfig.size()]);
    }

    private static ArrayList<String> EnabledInstallationSource = new ArrayList<>();
    public void setEnableInstallationSource(List<String> list) {
        Log.e(TAG,"setEnableInstallationSource "+list);
        synchronized (EnabledInstallationSource){
            if (list != null && !list.isEmpty()) {
                EnabledInstallationSource.clear();
                EnabledInstallationSource.addAll(list);
                SignatureVerify.isEnable = true;
            } else {
                EnabledInstallationSource.clear();
                SignatureVerify.isEnable = false;
            }
        }
    }
    public ArrayList<String> getEnableInstallationSource(){
        Log.e(TAG,"EnabledInstallationSource "+EnabledInstallationSource);
        return EnabledInstallationSource;
    }
    /**
     * 安全域内安装源签名设置
     */
    private static ArrayList<String> InstallSourceSignature = new ArrayList<>();
    public void setInstallSourceSignature(List<String> list){
        Log.e(TAG,"setInstallSourceSignature "+list);
        synchronized (InstallSourceSignature){
            if (list != null && !list.isEmpty()) {
                InstallSourceSignature.clear();
                InstallSourceSignature.addAll(list);
            } else {
                InstallSourceSignature.clear();
            }
        }
    }
    public ArrayList<String> getInstallSourceSignature(){
        return InstallSourceSignature;
    }

    /**
     * 构建缓存权限map的key
     *
     * @param packageName       应用包名
     * @param appPermissionName 权限名称
     * @return 生成的key
     */
    private String buildKey(String packageName, String appPermissionName) {
        return packageName + "," + appPermissionName;
    }
}
