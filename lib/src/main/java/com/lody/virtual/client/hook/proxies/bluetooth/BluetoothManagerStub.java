package com.lody.virtual.client.hook.proxies.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.util.Log;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.xdja.zs.VAppPermissionManager;
import com.lody.virtual.helper.utils.marks.FakeDeviceMark;

import java.lang.reflect.Method;

import mirror.android.bluetooth.IBluetoothManager;

/**
 * @see android.bluetooth.BluetoothManager
 */
public class BluetoothManagerStub extends BinderInvocationProxy {
    private static final String TAG = "Test" + BluetoothManagerStub.class.getSimpleName();
    public static final String SERVICE_NAME = Build.VERSION.SDK_INT >= 17 ?
            "bluetooth_manager" :
            "bluetooth";

    public BluetoothManagerStub() {
        super(IBluetoothManager.Stub.asInterface, SERVICE_NAME);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new BluetoothMethodProxy("registerAdapter"));
        addMethodProxy(new BluetoothMethodProxy("unregisterAdapter"));
        addMethodProxy(new BluetoothMethodProxy("registerStateChangeCallback"));
        addMethodProxy(new BluetoothMethodProxy("unregisterStateChangeCallback"));
        addMethodProxy(new BluetoothMethodProxy("isEnabled"));
        addMethodProxy(new BluetoothMethodProxy("enable"));
        addMethodProxy(new BluetoothMethodProxy("enableNoAutoConnect"));
        addMethodProxy(new BluetoothMethodProxy("disable"));
        addMethodProxy(new BluetoothMethodProxy("getState"));
        addMethodProxy(new BluetoothMethodProxy("getBluetoothGatt"));
        addMethodProxy(new BluetoothMethodProxy("bindBluetoothProfileService"));
        addMethodProxy(new BluetoothMethodProxy("unbindBluetoothProfileService"));
        addMethodProxy(new BluetoothMethodProxy("getAddress"));
        addMethodProxy(new BluetoothMethodProxy("getName"));
        addMethodProxy(new BluetoothMethodProxy("isBleScanAlwaysAvailable"));
        addMethodProxy(new BluetoothMethodProxy("updateBleAppCount"));
        addMethodProxy(new BluetoothMethodProxy("isBleAppPresent"));
    }

    @FakeDeviceMark("fake MAC")
    private static class BluetoothMethodProxy extends ReplaceLastPkgMethodProxy {
        public BluetoothMethodProxy(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String methodName = getMethodName();
            Log.e(TAG, methodName + " calling");
            boolean bluetoothEnable = VAppPermissionManager.get().getAppPermissionEnable(getAppPkg(), VAppPermissionManager.PROHIBIT_BLUETOOTH);
            switch (methodName) {
                case "isEnabled":
                case "enable":
                case "enableNoAutoConnect":
                case "disable":
                case "bindBluetoothProfileService":
                case "isBleScanAlwaysAvailable":
                case "isBleAppPresent":
                    return bluetoothEnable ? false : super.call(who, method, args);
                case "getState":
                    return bluetoothEnable ? BluetoothAdapter.STATE_OFF : super.call(who, method, args);
                case "getAddress":
                case "getName":
                    return bluetoothEnable ? "" : super.call(who, method, args);
                case "registerAdapter":
                case "unregisterAdapter":
                case "registerStateChangeCallback":
                case "unregisterStateChangeCallback":
                case "getBluetoothGatt":
                case "updateBleAppCount":
                default:
                    return bluetoothEnable ? null : super.call(who, method, args);
            }
        }
    }
}
