package com.lody.virtual.client.hook.proxies.system;

import android.net.wifi.IWifiScanner;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Messenger;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;

import java.util.ArrayList;

import mirror.android.net.wifi.WifiScanner;
import mirror.android.os.ServiceManager;

/**
 * @author Lody
 * <p>
 * This empty implemention of WifiScanner is workaround for run GMS.
 */
public class WifiScannerStub extends BinderInvocationProxy {

    private static final String SERVICE_NAME = "wifiscanner";

    public WifiScannerStub() {
        super(new EmptyWifiScannerImpl(), SERVICE_NAME);
    }

    @Override
    public void inject() throws Throwable {
        if (ServiceManager.checkService.call(SERVICE_NAME) == null) {
            super.inject();
        }
    }

    static class EmptyWifiScannerImpl extends IWifiScanner.Stub {

        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @Override
        public Messenger getMessenger() {
            return new Messenger(mHandler);
        }

        @Override
        public Bundle getAvailableChannels(int band) {
            Bundle bundle = new Bundle();
            bundle.putIntegerArrayList(WifiScanner.GET_AVAILABLE_CHANNELS_EXTRA.get(), new ArrayList<Integer>(0));
            return bundle;
        }
    }
}
