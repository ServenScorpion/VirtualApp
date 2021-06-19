package com.lody.virtual.client.hook.base;

import android.os.IBinder;
import android.os.IInterface;

import com.lody.virtual.helper.utils.VLog;

import mirror.RefStaticMethod;
import mirror.android.os.ServiceManager;

/**
 * @author Paulo Costa
 *
 * @see MethodInvocationProxy
 */
public abstract class BinderInvocationProxy extends MethodInvocationProxy<BinderInvocationStub> {

	protected String mServiceName;

	public BinderInvocationProxy(IInterface stub, String serviceName) {
		this(new BinderInvocationStub(stub), serviceName);
	}

	public BinderInvocationProxy(RefStaticMethod<IInterface> asInterfaceMethod, String serviceName) {
		this(new BinderInvocationStub(asInterfaceMethod, getService(serviceName)),
                serviceName);
	}

	public BinderInvocationProxy(Class<?> stubClass, String serviceName) {
		this(new BinderInvocationStub(stubClass, getService(serviceName)),
                serviceName);
	}

	private static IBinder getService(String serviceName){
	    return ServiceManager.getService.call(serviceName);
    }

	public BinderInvocationProxy(BinderInvocationStub hookDelegate, String serviceName) {
		super(hookDelegate);
		if(hookDelegate.getBaseInterface() == null){
            VLog.d("BinderInvocationProxy", "Unable to build HookDelegate: %s.", serviceName);
        }
		this.mServiceName = serviceName;
	}

	@Override
	public void inject() throws Throwable {
		getInvocationStub().replaceService(mServiceName);
	}

	@Override
	public boolean isEnvBad() {
		IBinder binder = ServiceManager.getService.call(mServiceName);
		return binder != null && getInvocationStub() != binder;
	}
}
