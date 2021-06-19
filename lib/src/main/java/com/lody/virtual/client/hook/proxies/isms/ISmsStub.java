package com.lody.virtual.client.hook.proxies.isms;

import android.net.Uri;
import android.os.Build;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceSpecPkgMethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.helper.utils.ComponentUtils;

import java.lang.reflect.Method;

import mirror.com.android.internal.telephony.ISms;

/**
 * @author Lody
 */

public class ISmsStub extends BinderInvocationProxy {

    public ISmsStub() {
        super(ISms.Stub.asInterface, "isms");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("getAllMessagesFromIccEfForSubscriber", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("updateMessageOnIccEfForSubscriber", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("copyMessageToIccEfForSubscriber", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendDataForSubscriber", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendDataForSubscriberWithSelfPermissions", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendTextForSubscriber", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendTextForSubscriberWithSelfPermissions", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendMultipartTextForSubscriber", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendStoredText", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendStoredMultipartText", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxy("createAppSpecificSmsToken", 1));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("getAllMessagesFromIccEf"));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("getAllMessagesFromIccEfForSubscriber", 1));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("updateMessageOnIccEf"));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("updateMessageOnIccEfForSubscriber", 1));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("copyMessageToIccEf"));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("copyMessageToIccEfForSubscriber", 1));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("sendData"));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendDataForSubscriber", 1));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("sendText"));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendTextForSubscriber", 1));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("sendMultipartText"));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendMultipartTextForSubscriber", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendStoredText", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendStoredMultipartText", 1));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("getAllMessagesFromIccEf"));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("updateMessageOnIccEf"));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("copyMessageToIccEf"));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("sendData"));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("sendText"));
            addMethodProxy(new ReplaceCallingPkgMethodProxyEx("sendMultipartText"));
        }

        //9.0
        if (Build.VERSION.SDK_INT >= 28) {
            addMethodProxy(new ReplaceSpecPkgMethodProxy("sendTextForSubscriberWithOptions", 1));
            addMethodProxy(new ReplaceSpecPkgMethodProxy("sendMultipartTextForSubscriberWithOptions", 1));
        }
    }

    static class ReplaceCallingPkgMethodProxyEx extends ReplaceCallingPkgMethodProxy{
        public ReplaceCallingPkgMethodProxyEx(String name) {
            super(name);
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            int index = MethodParameterUtils.getIndex(args, Uri.class);
            if (index != -1) {
                Uri uri = (Uri) args[index];
                args[index] = ComponentUtils.processOutsideUri(getAppUserId(), false, uri);
            }
            return super.beforeCall(who, method, args);
        }
    }

    static class ReplaceSpecPkgMethodProxyEx extends ReplaceSpecPkgMethodProxy {
        public ReplaceSpecPkgMethodProxyEx(String name, int index) {
            super(name, index);
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            int index = MethodParameterUtils.getIndex(args, Uri.class);
            if (index != -1) {
                Uri uri = (Uri) args[index];
                args[index] = ComponentUtils.processOutsideUri(getAppUserId(), false, uri);
            }
            return super.beforeCall(who, method, args);
        }
    }
}
