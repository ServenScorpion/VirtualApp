package com.lody.virtual.client.hook.proxies.dev_identifiers_policy;

import android.annotation.TargetApi;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;

import mirror.android.os.IDeviceIdentifiersPolicyService;

@TargetApi(29)
public class DeviceIdentifiersPolicyServiceHub extends BinderInvocationProxy {

    public DeviceIdentifiersPolicyServiceHub() {
        super(IDeviceIdentifiersPolicyService.Stub.asInterface, "device_identifiers");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getSerialForPackage"));
    }

}
