// IWaterMark.aidl
package com.xdja.zs;

// Declare any non-default types here with import statements
import com.xdja.zs.WaterMarkInfo;

interface IWaterMark {
    /**
         * 设置水印信息
         */
        void setWaterMark(in WaterMarkInfo waterMark);

        /**
         * 获取水印信息
         */
        WaterMarkInfo getWaterMark();
}
