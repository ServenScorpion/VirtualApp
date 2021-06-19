package com.lody.virtual.client.hook.proxies.system;

import com.android.internal.widget.ILockSettings;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;

import mirror.android.os.ServiceManager;

/**
 * @author Lody
 */
public class LockSettingsStub extends BinderInvocationProxy {

    private static final String SERVICE_NAME = "lock_settings";

    public LockSettingsStub() {
        super(new EmptyLockSettings(), SERVICE_NAME);
    }

    @Override
    public void inject() throws Throwable {
        if (ServiceManager.checkService.call(SERVICE_NAME) == null) {
            super.inject();
        }
    }

    static class EmptyLockSettings extends ILockSettings.Stub {

        @Override
        public void setRecoverySecretTypes(int[] secretTypes) {
        }

        @Override
        public int[] getRecoverySecretTypes() {
            return new int[0];
        }
    }
}