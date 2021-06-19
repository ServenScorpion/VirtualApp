package com.lody.virtual.client.hook.proxies.dropbox;

import android.content.Context;
import android.os.DropBoxManager;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ResultStaticMethodProxy;

import mirror.com.android.internal.os.IDropBoxManagerService;

/**
 * @author Lody
 */
public class DropBoxManagerStub extends BinderInvocationProxy {
    public DropBoxManagerStub() {
        super(IDropBoxManagerService.Stub.asInterface, Context.DROPBOX_SERVICE);
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        DropBoxManager dm = (DropBoxManager) VirtualCore.get().getContext().getSystemService(Context.DROPBOX_SERVICE);
        try {
            mirror.android.os.DropBoxManager.mService.set(dm, getInvocationStub().getProxyInterface());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ResultStaticMethodProxy("getNextEntry", null));
    }
}
