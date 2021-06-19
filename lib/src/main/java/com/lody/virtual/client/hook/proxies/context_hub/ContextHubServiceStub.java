package com.lody.virtual.client.hook.proxies.context_hub;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ResultStaticMethodProxy;
import com.lody.virtual.helper.compat.BuildCompat;

import mirror.android.hardware.location.IContextHubService;

public class ContextHubServiceStub extends BinderInvocationProxy {

    public ContextHubServiceStub() {
        super(IContextHubService.Stub.asInterface, getServiceName());
    }

    private static String getServiceName() {
        return BuildCompat.isOreo() ? "contexthub" : "contexthub_service";
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ResultStaticMethodProxy("registerCallback", 0));
        addMethodProxy(new ResultStaticMethodProxy("getContextHubInfo", null));
        addMethodProxy(new ResultStaticMethodProxy("getContextHubHandles",new int[]{}));
    }
}