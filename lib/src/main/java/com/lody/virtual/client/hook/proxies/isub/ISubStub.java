package com.lody.virtual.client.hook.proxies.isub;

import android.os.Build;
import android.telephony.TelephonyManager;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.ipc.VLocationManager;

import java.lang.reflect.Method;

import mirror.com.android.internal.telephony.ISub;

/**
 * @author Lody
 */
public class ISubStub extends BinderInvocationProxy {

    public ISubStub() {
        super(ISub.Stub.asInterface, "isub");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getAllSubInfoList"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getAllSubInfoCount"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getActiveSubscriptionInfo"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getActiveSubscriptionInfoForIccId"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getActiveSubscriptionInfoForSimSlotIndex"));
        addMethodProxy(new StaticMethodProxy("getActiveSubscriptionInfoList") {
            @Override
            public Object call(Object who, Method method, Object... args) {
                try {
                    return super.call(who, method, args);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        addMethodProxy(new ReplaceLastPkgMethodProxy("getActiveSubInfoCount"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("getSubscriptionProperty"));
        addMethodProxy(new StaticMethodProxy(Build.VERSION.SDK_INT >= 24 ?
                "getSimStateForSlotIdx" : "getSimStateForSubscriber"));
    }
}
