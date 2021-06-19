package com.lody.virtual.client.hook.proxies.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.IInterface;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.ResultBinderMethodProxy;
import com.lody.virtual.helper.utils.marks.FakeDeviceMark;
import com.lody.virtual.remote.VDeviceConfig;

import java.lang.reflect.InvocationHandler;
import com.xdja.zs.VAppPermissionManager;

import java.lang.reflect.Method;

import mirror.android.bluetooth.IBluetooth;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

/**
 * @see android.bluetooth.BluetoothManager
 */
public class BluetoothStub extends BinderInvocationProxy {
    private static final String TAG = "Test" + BluetoothStub.class.getSimpleName();

    private final static String SERVER_NAME = Build.VERSION.SDK_INT >= JELLY_BEAN_MR1 ?
            "bluetooth_manager" : "bluetooth";

    public BluetoothStub() {
        super(IBluetooth.Stub.asInterface, SERVER_NAME);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new GetAddress());
        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
            addMethodProxy(new ResultBinderMethodProxy("registerAdapter") {
                @Override
                public InvocationHandler createProxy(final IInterface base) {
                    return new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if ("getAddress".equals(method.getName())) {
                                VDeviceConfig config = getDeviceConfig();
                                if (config.enable) {
                                    String mac = getDeviceConfig().bluetoothMac;
                                    if (!TextUtils.isEmpty(mac)) {
                                        return mac;
                                    }
                                }
                            }
                            return method.invoke(base, args);
                        }
                    };
                }
            });
        }

        //葛垚的代码
        addMethodProxy(new BluetoothMethodProxy("getState"));
        addMethodProxy(new BluetoothMethodProxy("registerCallback"));
        addMethodProxy(new BluetoothMethodProxy("unregisterCallback"));
        addMethodProxy(new BluetoothMethodProxy("enable"));
        addMethodProxy(new BluetoothMethodProxy("enableNoAutoConnect"));
        addMethodProxy(new BluetoothMethodProxy("disable"));
        addMethodProxy(new BluetoothMethodProxy("setName"));
        addMethodProxy(new BluetoothMethodProxy("startDiscovery"));
        addMethodProxy(new BluetoothMethodProxy("cancelDiscovery"));
    }

    @FakeDeviceMark("fake MAC")
    private static class GetAddress extends ReplaceLastPkgMethodProxy {
        public GetAddress() {
            super("getAddress");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            VDeviceConfig config = getDeviceConfig();
            if (config.enable) {
                String mac = getDeviceConfig().bluetoothMac;
                if (!TextUtils.isEmpty(mac)) {
                    return mac;
                }
            }
            return super.call(who, method, args);
        }
    }

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
                case "setName":
                case "setScanMode":
                case "setDiscoverableTimeout":
                case "startDiscovery":
                case "cancelDiscovery":
                case "isDiscovering":
                case "createBond":
                case "createBondOutOfBand":
                case "cancelBondProcess":
                case "removeBond":
                case "isBondingInitiatedLocally":
                case "setRemoteAlias":
                case "fetchRemoteUuids":
                case "sdpSearch":
                case "setPin":
                case "setPasskey":
                case "setPairingConfirmation":
                case "setPhonebookAccessPermission":
                case "setMessageAccessPermission":
                case "setSimAccessPermission":
                case "factoryReset":
                case "isMultiAdvertisementSupported":
                case "isOffloadedFilteringSupported":
                case "isOffloadedScanBatchingSupported":
                case "isActivityAndEnergyReportingSupported":
                case "isLe2MPhySupported":
                case "isLeCodedPhySupported":
                case "isLeExtendedAdvertisingSupported":
                case " isLePeriodicAdvertisingSupported":
                    return bluetoothEnable ? false : super.call(who, method, args);
                case "getState":
                    return bluetoothEnable ? BluetoothAdapter.STATE_OFF : super.call(who, method, args);
                case "getAddress":
                case "getName":
                case "getRemoteName":
                case "getRemoteAlias":
                    return bluetoothEnable ? "" : super.call(who, method, args);
                case "getDiscoverableTimeout":
                case "getDiscoveryEndMillis":
                case "getBondState":
                case "getSupportedProfiles":
                case "getRemoteType":
                case "getRemoteClass":
                case "getPhonebookAccessPermission":
                case "getMessageAccessPermission":
                case "getSimAccessPermission":
                    return bluetoothEnable ? -1 : super.call(who, method, args);
                case "getLeMaximumAdvertisingDataLength":
                    return bluetoothEnable ? 0 : super.call(who, method, args);
                case "getAdapterConnectionState":
                case "getProfileConnectionState":
                case "getConnectionState":
                    return bluetoothEnable ? BluetoothAdapter.STATE_DISCONNECTED : super.call(who, method, args);
                case "getScanMode":
                    return bluetoothEnable ? BluetoothAdapter.SCAN_MODE_NONE : super.call(who, method, args);
                case "getUuids":
                case "getBondedDevices":
                case "getRemoteUuids":
                case "sendConnectionStateChange":
                case "registerCallback":
                case "unregisterCallback":
                case "connectSocket":
                case "createSocketChannel":
                case "reportActivityInfo":
                case "requestActivityInfo":
                case "onLeServiceUp":
                case "onBrEdrDown":
                default:
                    return bluetoothEnable ? null : super.call(who, method, args);
            }
        }
    }
}
