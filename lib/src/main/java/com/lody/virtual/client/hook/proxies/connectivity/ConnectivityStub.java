package com.lody.virtual.client.hook.proxies.connectivity;

import android.content.Context;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ResultStaticMethodProxy;

import mirror.android.net.IConnectivityManager;

/**
 * @author legency
 * @see android.net.ConnectivityManager
 */
public class ConnectivityStub extends BinderInvocationProxy {

    public ConnectivityStub() {
        super(IConnectivityManager.Stub.asInterface, Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ResultStaticMethodProxy("isTetheringSupported", true));
        addMethodProxy(new MethodProxies.PrepareVpn());
    }
}
