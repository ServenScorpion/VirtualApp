package com.xdja.zs;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * @Date 28-4-19
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class MobileInfoUtil {
    /**
     * 获取手机IMEI
     *
     * @param context
     * @return
     */
    public static final String getIMEI(Context context) {
        try {
            //实例化TelephonyManager对象
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            //获取IMEI号
            String imei = telephonyManager.getDeviceId();
            //在次做个验证，也不是什么时候都能获取到的啊
            if (imei == null) {
                imei = "";
            }
            return imei;
        } catch (Exception e) {
//            e.printStackTrace();
            return "";
        }
    }
}
