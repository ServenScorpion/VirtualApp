package com.xdja.zs;

import android.content.ClipData;
import android.os.RemoteException;
import android.text.TextUtils;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.helper.utils.VLog;

/**
 * Created by geyao on 2018/1/22.
 */

public class VAppPermissionManager {
    private static final String TAG = VAppPermissionManager.class.getSimpleName();
    /**
     * 禁止对此应用进行截屏or录屏
     */
    public static final String PROHIBIT_SCREEN_SHORT_RECORDER = "禁止对此应用进行截屏,录屏";
    /**
     * 禁止使用网络
     */
    public static final String PROHIBIT_NETWORK = "禁止使用网络";
    /**
     * 禁止使用摄像头
     */
    public static final String PROHIBIT_CAMERA = "禁止使用摄像头";
    /**
     * 禁用应用界面水印功能；默认显示
     */
    //后台于终端策略默认值不统一，后台为启用水印功能 而终端为 禁用水印功能
    public static final String PROHIBIT_WATER_MARK = "启用水印功能";
    /**
     * 禁止调用蓝牙功能
     */
    public static final String PROHIBIT_BLUETOOTH = "禁止调用蓝牙功能";
    /**
     * 禁止使用录音功能
     */
    public static final String PROHIBIT_SOUND_RECORD = "禁止使用录音功能";
    /**
     * 禁止读取位置信息
     */
    public static final String PROHIBIT_LOCATION = "禁止读取位置信息";
    /**
     * 应用数据加解密
     */
    public static final String ALLOW_DATA_ENCRYPT_DECRYPT = "应用数据加解密";
    /**
     * 应用防卸载
     */
    public static final String PROHIBIT_APP_UNINSTALL = "应用防卸载";
    /**
     * 启用安全接入
     */
    public static final String ALLOW_SECURE_ACCESS = "启用安全接入";
    /**
     * 透明加解密
     */
    public static final String ALLOW_DATA_ENCRYPT = "启用数据加密";
    /**
     * 目前支持的权限集合
     */
    public static final String[] permissions = new String[]{
            PROHIBIT_SCREEN_SHORT_RECORDER,//禁止对此应用进行截屏or录屏
            PROHIBIT_NETWORK,//禁止使用网络
            PROHIBIT_CAMERA,//禁止使用摄像头
            PROHIBIT_WATER_MARK,//禁用应用界面水印功能
            PROHIBIT_BLUETOOTH,//禁止调用蓝牙功能
            PROHIBIT_SOUND_RECORD,//禁止使用录音功能
            PROHIBIT_LOCATION,//禁止读取位置信息
            ALLOW_DATA_ENCRYPT_DECRYPT,//应用数据加解密
            PROHIBIT_APP_UNINSTALL,//应用防卸载
            ALLOW_SECURE_ACCESS,//启用安全接入
            ALLOW_DATA_ENCRYPT//启用数据加密
    };

    static private VAppPermissionManager sInstance = new VAppPermissionManager();
    static public VAppPermissionManager get() { return sInstance; }

    private IAppPermission mService;

    private Object getRemoteInterface() {
        return IAppPermission.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.APPPERMISSION));
    }

    public IAppPermission getService() {

        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IAppPermission.class, binder);
            }
        }
        return mService;
    }

    /**
     * 是否是支持的权限
     *
     * @param permissionName 权限名称
     * @return 是否是支持的权限
     */
    public boolean isSupportPermission(String permissionName) {
        VLog.d(TAG, "isSupportPermission permissionName: " + (TextUtils.isEmpty(permissionName) ? "is null" : permissionName));
        try {
            return getService().isSupportPermission(permissionName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    /**
     * 是否支持加解密
     *
     * @param packageName 应用名称
     * @return 是否支持加解密 true:支持 false:不支持
     */
    public boolean isSupportEncrypt(String packageName) {
        VLog.d(TAG, "isSupportEncrypt packageName: " + (TextUtils.isEmpty(packageName) ? "" : packageName));
        return !TextUtils.isEmpty(packageName) && getAppPermissionEnable(packageName, ALLOW_DATA_ENCRYPT_DECRYPT);
    }

    /**
     * 清除权限信息
     */
    public void clearPermissionData() {
        VLog.d(TAG, "clearPermissionData");
        try {
            getService().clearPermissionData();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 设置应用权限
     *
     * @param packageName       应用包名
     * @param appPermissionName 应用权限名称
     * @param isPermissionOpen  权限开关
     */
    public void setAppPermission(String packageName, String appPermissionName, boolean isPermissionOpen) {
        VLog.d(TAG, "setAppPermission packageName: " + (TextUtils.isEmpty(packageName) ? "" : packageName)
                + " appPermissionName: " + (TextUtils.isEmpty(appPermissionName) ? "" : appPermissionName)
                + " isPermissionOpen: " + isPermissionOpen);
        try {
            getService().setAppPermission(packageName, appPermissionName, isPermissionOpen);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 获取应用权限开关状态
     *
     * @param packageName       应用包名
     * @param appPermissionName 应用权限名称
     * @return 权限开关状态
     */
    public boolean getAppPermissionEnable(String packageName, String appPermissionName) {
        VLog.d(TAG, "getAppPermissionEnable packageName: " + (TextUtils.isEmpty(packageName) ? "" : packageName)
                + " appPermissionName: " + (TextUtils.isEmpty(appPermissionName) ? "" : appPermissionName));
        try {
            boolean appPermissionEnable = getService().getAppPermissionEnable(packageName, appPermissionName);
            VLog.d(TAG, "getAppPermissionEnable result: " + appPermissionEnable);
            return appPermissionEnable;
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    /**
     * 注册回调监听
     *
     * @param iAppPermissionCallback 回调监听
     */
    public void registerCallback(IAppPermissionCallback iAppPermissionCallback) {
        VLog.d(TAG, "registerCallback");
        try {
            getService().registerCallback(iAppPermissionCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 接触回调监听注册
     */
    public void unregisterCallback() {
        VLog.d(TAG, "unregisterCallback");
        try {
            getService().unregisterCallback();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 权限拦截触发回调
     *
     * @param appPackageName 应用名称
     * @param permissionName 权限名称
     */
    public void interceptorTriggerCallback(String appPackageName, String permissionName) {
        VLog.d(TAG, "interceptorTriggerCallback appPackageName: " + (TextUtils.isEmpty(appPackageName) ? "" : appPackageName)
                + " permissionName: " + (TextUtils.isEmpty(permissionName) ? "" : permissionName));
        try {
            getService().interceptorTriggerCallback(appPackageName, permissionName);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 缓存剪切板信息
     *
     * @param clipData 剪切板信息
     */
    public void cacheClipData(ClipData clipData) {
        VLog.d(TAG, "cacheClipData");
        try {
            getService().cacheClipData(clipData);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 获取缓存的剪切板信息
     */
    public ClipData getClipData() {
        VLog.d(TAG, "getClipData");
        try {
            return getService().getClipData();
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    /**
     * 缓存剪切板数据改变监听
     *
     * @param listener 监听
     */
    public void cachePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
        VLog.d(TAG, "cachePrimaryClipChangedListener");
        try {
            getService().cachePrimaryClipChangedListener(listener);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 响应剪切板数据改变监听
     */
    public void callPrimaryClipChangedListener() {
        VLog.d(TAG, "callPrimaryClipChangedListener");
        try {
            getService().callPrimaryClipChangedListener();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 移除剪切板数据改变监听
     */
    public void removePrimaryClipChangedListener() {
        VLog.d(TAG, "removePrimaryClipChangedListener");
        try {
            getService().removePrimaryClipChangedListener();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 获取位置拦截开关状态
     *
     * @param packageName 应用包名
     * @return 开关状态 true:开启拦截 false:关闭拦截
     */
    public boolean getLocationEnable(String packageName) {
        VLog.d(TAG, "getLocationEnable packageName: " + (TextUtils.isEmpty(packageName) ? "" : packageName));
        boolean result = !TextUtils.isEmpty(packageName) && getAppPermissionEnable(packageName, PROHIBIT_LOCATION);
        if (result) {
            interceptorTriggerCallback(packageName, PROHIBIT_LOCATION);
        }
        return result;
    }

    /**
     * 设置安装第三方应用状态
     *
     * @param isEnable 是否可安装 true:允许安装第三方应用 false:不允许安装第三方应用
     */
    public void setThirdAppInstallationEnable(boolean isEnable) {
        VLog.d(TAG, "setThirdAppInstallationEnable isEnable: " + isEnable);
        try {
            getService().setThirdAppInstallationEnable(isEnable);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 获取透明加解密配置信息
     */
    public String[] getEncryptConfig() {
        try {
            return getService().getEncryptConfig();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
            return  null;
        }
    }

    /**
     * 获取是否可以安装第三方应用状态
     *
     * @return 是否可以安装第三方应用状态 true:可以安装第三方应用 false:不可以安装第三方应用
     */
    public boolean getThirdAppInstallationEnable() {
        try {
            boolean thirdAppInstallationEnable = getService().getThirdAppInstallationEnable();
            VLog.d(TAG, "getThirdAppInstallationEnable isEnable: " + thirdAppInstallationEnable);
            return thirdAppInstallationEnable;
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }
}
