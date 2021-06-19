package com.lody.virtual.client.hook.providers;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.MethodBox;
import com.lody.virtual.client.stub.ContentProviderProxy;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.client.stub.OutsideProxyContentProvider;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.remote.VDeviceConfig;
import com.xdja.mms.SmsConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 */

public class SettingsProviderHook extends ExternalProviderHook {

    private static final String TAG = SettingsProviderHook.class.getSimpleName();

    private static final int METHOD_GET = 0;
    private static final int METHOD_PUT = 1;

    private static final Map<String, String> PRE_SET_VALUES = new HashMap<>();

    static {
        PRE_SET_VALUES.put("user_setup_complete", "1");
        PRE_SET_VALUES.put("install_non_market_apps", "1");
    }


    public SettingsProviderHook(Object base) {
        super(base);
    }

    private static int getMethodType(String method) {
        if (method.startsWith("GET_")) {
            return METHOD_GET;
        }
        if (method.startsWith("PUT_")) {
            return METHOD_PUT;
        }
        return -1;
    }

    private static boolean isSecureMethod(String method) {
        return method.endsWith("secure");
    }


    @Override
    public Bundle call(MethodBox methodBox, String method, String arg, Bundle extras) throws InvocationTargetException {
        if (!VClient.get().isAppRunning()) {
            return methodBox.call();
        }
        int methodType = getMethodType(method);
        if (METHOD_GET == methodType) {
            String presetValue = PRE_SET_VALUES.get(arg);
            if (presetValue != null) {
                return wrapBundle(arg, presetValue);
            }
            if ("android_id".equals(arg)) {
                VDeviceConfig config = VClient.get().getDeviceConfig();
                if (config.enable && config.androidId != null) {
                    return wrapBundle("android_id", config.androidId);
                }
            } else if (SmsConstants.DEFAULT_SMS_APP_SETTING.equals(arg)) {
                //default sms app
                Bundle res = methodBox.call();
                String pkg = getValue(res, SmsConstants.DEFAULT_SMS_APP_SETTING);
                if (VirtualCore.get().getHostPkg().equals(pkg)) {
                    return wrapBundle(SmsConstants.DEFAULT_SMS_APP_SETTING, InstallerSetting.MESSAGING_PKG);
                }
                return res;
            } else if(Settings.System.RINGTONE.equals(arg)){
                Bundle res = methodBox.call();
                String uriStr = getValue(res, Settings.System.RINGTONE);
                if(!TextUtils.isEmpty(uriStr)) {
                    Uri uri = OutsideProxyContentProvider.toProxyUri(Uri.parse(uriStr));
                    return wrapBundle(Settings.System.RINGTONE, uri.toString());
                }
                return res;
            } else if(Settings.System.NOTIFICATION_SOUND.equals(arg)){
                Bundle res = methodBox.call();
                String uriStr = getValue(res, Settings.System.NOTIFICATION_SOUND);
                if(!TextUtils.isEmpty(uriStr)) {
                    Uri uri = OutsideProxyContentProvider.toProxyUri(Uri.parse(uriStr));
                    return wrapBundle(Settings.System.NOTIFICATION_SOUND, uri.toString());
                }
                return res;
            } else if(Settings.System.ALARM_ALERT.equals(arg)){
                Bundle res = methodBox.call();
                String uriStr = getValue(res, Settings.System.ALARM_ALERT);
                if(!TextUtils.isEmpty(uriStr)) {
                    Uri uri = OutsideProxyContentProvider.toProxyUri(Uri.parse(uriStr));
                    return wrapBundle(Settings.System.ALARM_ALERT, uri.toString());
                }
                return res;
            }
        }
        if (METHOD_PUT == methodType) {
            if (isSecureMethod(method)) {
                return null;
            }
        }
        try {
            return methodBox.call();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof SecurityException) {
                return null;
            }
            throw e;
        }
    }

    private Bundle wrapBundle(String name, String value) {
        Bundle bundle = new Bundle();
        if (Build.VERSION.SDK_INT >= 24) {
            bundle.putString(Settings.NameValueTable.NAME, name);
            bundle.putString(Settings.NameValueTable.VALUE, value);
        } else {
            bundle.putString(name, value);
        }
        return bundle;
    }

    private String getValue(Bundle bundle, String key) {
        if (bundle == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            return bundle.getString(Settings.NameValueTable.VALUE);
        } else {
            return bundle.getString(key);
        }
    }

    @Override
    protected void processArgs(Method method, Object... args) {
        super.processArgs(method, args);
    }
}
