package com.lody.virtual.client.hook.proxies.usage;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.ipc.VActivityManager;

import java.lang.reflect.Method;

import mirror.android.app.IUsageStatsManager;

/**
 * Created by caokai on 2017/9/8.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class UsageStatsManagerStub extends BinderInvocationProxy {

    public UsageStatsManagerStub() {
        super(IUsageStatsManager.Stub.asInterface, Context.USAGE_STATS_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceLastPkgMethodProxy("queryUsageStats"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("queryConfigurations"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("queryEvents"));
        addMethodProxy(new StaticMethodProxy("setAppInactive"){
            @Override
            public Object call(Object who, Method method, Object... args) {
                int userId = args.length > 2 ? (int) args[2] : 0;
                VActivityManager.get().setAppInactive((String) args[0], (Boolean)args[1], userId);
                return 0;
            }
        });
        addMethodProxy(new StaticMethodProxy("isAppInactive") {
            @Override
            public Object call(Object who, Method method, Object... args) {
                int userId = args.length > 1 ? (int) args[1] : 0;
                return VActivityManager.get().isAppInactive((String) args[0], userId);
            }
        });
        addMethodProxy(new ReplacePkgAndUserIdMethodProxy("whitelistAppTemporarily"));
    }

    private class ReplacePkgAndUserIdMethodProxy extends ReplaceLastPkgMethodProxy{
        public ReplacePkgAndUserIdMethodProxy(String name) {
            super(name);
        }
        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args[args.length - 1] instanceof Integer) {
                args[args.length - 1] = 0;
            }
            return super.call(who, method, args);
        }
    }

}
