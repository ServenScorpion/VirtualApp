package com.xdja.zs;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;


/**
 * @Date 19-6-27 11
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class ProviderInfoUtil {

    private static final String TAG = ProviderInfoUtil.class.getName();
    private static final String PROVIDER_KEY = "com.xdja.engine.provider";
    private static final String SAFETY_PACKAGE_NAME = VirtualCore.get().getHostPkg();

    public static String  getProviderInfo(@NonNull Context context){
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appinfo =  pm.getApplicationInfo(SAFETY_PACKAGE_NAME, PackageManager.GET_META_DATA);
            if(appinfo==null)
                return null;
            String URI = appinfo.metaData.getString(PROVIDER_KEY);
            Log.e(TAG,"URI "+ URI + " package:" + SAFETY_PACKAGE_NAME);
            return URI;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}