package com.lody.virtual.client.hook.proxies.nfc;

import android.content.Context;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.helper.compat.BuildCompat;

import mirror.android.nfc.INfcAdapter;

/**
 * @see android.nfc.NfcAdapter
 * @see android.nfc.NfcManager
 */
public class NfcAdapterStub extends BinderInvocationProxy {

    public NfcAdapterStub() {
        super(INfcAdapter.Stub.asInterface, Context.NFC_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceLastPkgMethodProxy("getNfcAdapterExtrasInterface"));
        if(BuildCompat.isPie()){
            addMethodProxy(new ReplaceLastPkgMethodProxy("getNfcDtaInterface"));
        }
    }
}
