package com.lody.virtual.client.hook.proxies.appops;

import android.annotation.TargetApi;
import android.os.Build;

import com.lody.virtual.client.hook.annotations.Inject;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;

import mirror.com.android.internal.app.ISmtOpsService;

/**
 * @author Lody
 * <p>
 * Fuck the AppOpsService.
 * @see android.app.AppOpsManager
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@Inject(MethodProxies.class)
public class SmtOpsManagerStub extends BinderInvocationProxy {

    public SmtOpsManagerStub() {
        super(ISmtOpsService.Stub.asInterface, "smtops");
    }

}
