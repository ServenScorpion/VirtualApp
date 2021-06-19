package com.lody.virtual.client.hook.proxies.telephony;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.annotations.Inject;

import mirror.com.android.internal.telephony.IHwTelephony;

/**
 * @author Lody
 * @see android.telephony.TelephonyManager
 */
@Inject(MethodProxies.class)
public class HwTelephonyStub extends BinderInvocationProxy {

	public HwTelephonyStub() {
		super(IHwTelephony.Stub.TYPE, "phone_huawei");
	}

	@Override
	protected void onBindMethods() {
        addMethodProxy(new GetUniqueDeviceId());
	}

    private static class GetUniqueDeviceId extends MethodProxies.GetDeviceId{
        @Override
        public String getMethodName() {
            return "getUniqueDeviceId";
        }
    }

}
