package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.helper.utils.IInterfaceUtils;

public class VWaterMarkManager {
    private static final VWaterMarkManager sInstance = new VWaterMarkManager();
    IWaterMark mService;

    public static VWaterMarkManager get() {
        return sInstance;
    }

    private Object getRemoteInterface() {
        return IWaterMark.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.WATERMARK));
    }

    public IWaterMark getService() {

        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IWaterMark.class, binder);
            }
        }
        return mService;
    }

    /**
     * 设置水印信息
     *
     * @param waterMark 水印信息
     */
    public void setWaterMark(WaterMarkInfo waterMark) {
        try {
            getService().setWaterMark(waterMark);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 获取水印信息
     *
     * @return 水印信息
     */
    public WaterMarkInfo getWaterMark() {
        try {
            return getService().getWaterMark();
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }
}
