package com.lody.virtual.client.hook.proxies.accessibility;

import android.content.Context;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;

import java.lang.reflect.Method;

import mirror.android.view.accessibility.IAccessibilityManager;

/**
 * @author Lody
 */
public class AccessibilityManagerStub extends BinderInvocationProxy {

    public AccessibilityManagerStub() {
        super(IAccessibilityManager.Stub.TYPE, Context.ACCESSIBILITY_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceLastUserIdProxy("addClient"));
        addMethodProxy(new ReplaceLastUserIdProxy("sendAccessibilityEvent"));
        addMethodProxy(new ReplaceLastUserIdProxy("getInstalledAccessibilityServiceList"));
        addMethodProxy(new ReplaceLastUserIdProxy("getEnabledAccessibilityServiceList"));
        addMethodProxy(new ReplaceLastUserIdProxy("getWindowToken"));
        addMethodProxy(new ReplaceLastUserIdProxy("interrupt"));
        addMethodProxy(new ReplaceLastUserIdProxy("addAccessibilityInteractionConnection"));
    }

    private static class ReplaceLastUserIdProxy extends StaticMethodProxy {

        public ReplaceLastUserIdProxy(String name) {
            super(name);
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            int index = args.length - 1;
            if (index >= 0 && args[index] instanceof Integer) {
                args[index] = 0;
            }
            return super.beforeCall(who, method, args);
        }
    }
}
