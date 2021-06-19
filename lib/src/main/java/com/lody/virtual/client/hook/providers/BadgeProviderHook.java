package com.lody.virtual.client.hook.providers;

import android.os.Bundle;
import android.util.Log;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.hook.base.MethodBox;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.BadgerInfo;

import java.lang.reflect.InvocationTargetException;

public class BadgeProviderHook extends ExternalProviderHook {
    public BadgeProviderHook(Object base) {
        super(base);
    }

    @Override
    public Bundle call(MethodBox methodBox, String method, String arg, Bundle extras) throws InvocationTargetException {
        if("change_badge".equals(method)){
            BadgerInfo info = new BadgerInfo();
            info.userId = VUserHandle.myUserId();
            info.packageName = extras.getString("package");
            info.className = extras.getString("class");
            info.badgerCount = extras.getInt("badgenumber");
            Log.e("wxd", " change_badge " + " useeId : " + info.userId + " package : " + info.packageName + " count : " + info.badgerCount);
            VActivityManager.get().notifyBadgerChange(info);
            Bundle out = new Bundle();
            out.putBoolean("success", true);
            return out;
        } else if ("setAppBadgeCount".equals(method)) {
            BadgerInfo info = new BadgerInfo();
            info.userId = VUserHandle.myUserId();
            info.packageName = VClient.get().getCurrentPackage();
            info.badgerCount = extras.getInt("app_badge_count");
            //xdja 只需要使用change_badge即可
            //VActivityManager.get().notifyBadgerChange(info);
            Bundle out = new Bundle();
            out.putBoolean("success", true);
            return out;
        }
        return super.call(methodBox, method, arg, extras);
    }
}
