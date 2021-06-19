package com.xdja.zs;

import android.util.Log;

import com.xdja.zs.IWaterMark;


public class VWaterMarkService extends IWaterMark.Stub {
    private static final String TAG = VWaterMarkService.class.getSimpleName();
    private static VWaterMarkService sInstance;
    private static WaterMarkInfo waterMarkInfo;

    public static void systemReady() {
        sInstance = new VWaterMarkService();
    }

    public static VWaterMarkService get() {
        return sInstance;
    }

    public void setWaterMark(WaterMarkInfo waterMark) {
        Log.e(TAG, "set water mark");
        if (waterMark == null) {
            Log.e(TAG, "set water mark params is null return");
        }
        waterMarkInfo = waterMark;
    }

    public WaterMarkInfo getWaterMark() {
        Log.e(TAG, "get water mark: " + (waterMarkInfo == null ? "is null" : waterMarkInfo.toString()));
        return waterMarkInfo;
    }
}
