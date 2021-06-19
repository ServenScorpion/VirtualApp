package com.lody.virtual.client.hook.proxies.usb;

import android.content.Context;
import android.os.IInterface;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.helper.utils.ArrayUtils;

import java.lang.reflect.Method;

import mirror.android.hardware.usb.IUsbManager;
import mirror.android.hardware.usb.UsbManager;

public class UsbManagerStub extends BinderInvocationProxy {
    public UsbManagerStub() {
        super(IUsbManager.Stub.asInterface, Context.USB_SERVICE);
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        final IInterface hookedService = getInvocationStub().getProxyInterface();
        android.hardware.usb.UsbManager usbManager = (android.hardware.usb.UsbManager) VirtualCore.get().getContext().getSystemService(Context.USB_SERVICE);
        if (UsbManager.mService != null && hookedService != null){
            Object mService = UsbManager.mService.get(usbManager);
            if (mService != null) {
                if (mService != hookedService) {
                    UsbManager.mService.set(usbManager, hookedService);
                }
            }
        }
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplacePackageNameWithUserId("openDevice"));
        addMethodProxy(new ReplacePackageNameWithUserId("setDevicePackage"));
        addMethodProxy(new ReplacePackageNameWithUserId("setAccessoryPackage"));
        addMethodProxy(new ReplacePackageNameWithUserId("hasDevicePermission"));
        addMethodProxy(new ReplacePackageNameWithUserId("hasAccessoryPermission"));
        addMethodProxy(new ReplacePackageNameWithUserId("requestDevicePermission"));
        addMethodProxy(new ReplacePackageNameWithUserId("requestAccessoryPermission"));
        addMethodProxy(new ReplacePackageNameWithUserId("hasDefaults"));
        addMethodProxy(new ReplacePackageNameWithUserId("clearDefaults"));
    }

    private class ReplacePackageNameWithUserId extends StaticMethodProxy {
        public ReplacePackageNameWithUserId(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int index = ArrayUtils.indexOfLast(args, String.class);
            if (index >= 0) {
                String pkg = (String) args[index];
                if (isAppPkg(pkg)) {
                    args[index] = getHostPkg();
                }
                if ((index + 1) < args.length) {
                    if (args[index + 1] instanceof Integer) {
                        int userId = (int) args[index + 1];
                        if (userId == getAppUserId() && userId != getRealUserId()) {
                            args[index + 1] = getRealUserId();
                        }
                    }
                }
            }
            return super.call(who, method, args);
        }
    }
}
