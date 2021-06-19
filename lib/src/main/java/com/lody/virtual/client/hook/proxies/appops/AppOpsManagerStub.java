package com.lody.virtual.client.hook.proxies.appops;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.annotations.Inject;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;

import mirror.com.android.internal.app.IAppOpsService;

/**
 * @author Lody
 * <p>
 * Fuck the AppOpsService.
 * @see android.app.AppOpsManager
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@Inject(MethodProxies.class)
public class AppOpsManagerStub extends BinderInvocationProxy {

    public AppOpsManagerStub() {
        super(IAppOpsService.Stub.asInterface, Context.APP_OPS_SERVICE);
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        if (mirror.android.app.AppOpsManager.mService != null) {
            AppOpsManager appOpsManager = (AppOpsManager) VirtualCore.get().getContext().getSystemService(Context.APP_OPS_SERVICE);
            try {
                mirror.android.app.AppOpsManager.mService.set(appOpsManager, getInvocationStub().getProxyInterface());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
    }

}
