package com.lody.virtual.client.hook.proxies.appops;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;

import mirror.oem.IFlymePermissionService;

/**
 * @author kenan
 */
public class FlymePermissionServiceStub extends BinderInvocationProxy {
    public FlymePermissionServiceStub() {
        super(IFlymePermissionService.Stub.TYPE, "flyme_permission");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceCallingPkgMethodProxy("noteIntentOperation"));
    }
}
