package com.lody.virtual.client.hook.proxies.network;

import android.annotation.TargetApi;
import android.os.Build;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceUidMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;

import java.lang.reflect.Method;

import mirror.android.os.INetworkManagementService;

@TargetApi(Build.VERSION_CODES.M)
public class
NetworkManagementStub extends BinderInvocationProxy {

	public NetworkManagementStub() {
		super(INetworkManagementService.Stub.asInterface, "network_management");
	}

	@Override
	protected void onBindMethods() {
		super.onBindMethods();
		addMethodProxy(new ReplaceUidMethodProxy("setUidCleartextNetworkPolicy", 0));
		addMethodProxy(new ReplaceUidMethodProxy("setUidMeteredNetworkBlacklist", 0));
        addMethodProxy(new ReplaceUidMethodProxy("setUidMeteredNetworkWhitelist", 0));
        addMethodProxy(new StaticMethodProxy("getNetworkStatsUidDetail"){
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                int uid = (int)args[0];
                if(uid == getVUid()){
                    args[0] = getRealUid();
                }
                return super.call(who, method, args);
            }
        });
	}
}
