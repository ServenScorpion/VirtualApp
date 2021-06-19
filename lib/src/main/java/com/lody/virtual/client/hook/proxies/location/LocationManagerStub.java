package com.lody.virtual.client.hook.proxies.location;

import android.content.Context;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;

import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.annotations.Inject;
import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.base.MethodInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.ReflectException;

import com.xdja.zs.VAppPermissionManager;

import java.lang.reflect.Method;

import mirror.android.location.ILocationManager;
import mirror.android.os.ServiceManager;

/**
 * @author Lody
 * @see android.location.LocationManager
 */
@Inject(MethodProxies.class)
public class LocationManagerStub extends MethodInvocationProxy<BinderInvocationStub> {
    public LocationManagerStub() {
        super(new BinderInvocationStub(getInterface()));
    }

    private static IInterface getInterface() {
        IBinder base = ServiceManager.getService.call(Context.LOCATION_SERVICE);
        if (base instanceof Binder) {
            try {
                return Reflect.on(base).get("mILocationManager");
            } catch (ReflectException e) {
                e.printStackTrace();
            }
        }
        return ILocationManager.Stub.asInterface.call(base);
    }

    @Override
    public void inject() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        Object base = mirror.android.location.LocationManager.mService.get(locationManager);
        if (base instanceof Binder) {
            Reflect.on(base).set("mILocationManager", getInvocationStub().getProxyInterface());
        }
        mirror.android.location.LocationManager.mService.set(locationManager, getInvocationStub().getProxyInterface());
        getInvocationStub().replaceService(Context.LOCATION_SERVICE);
    }

    @Override
    public boolean isEnvBad() {
        return false;
    }


    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addMethodProxy(new ReplaceLastPkgMethodProxy("addTestProvider"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("removeTestProvider"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("setTestProviderLocation"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("clearTestProviderLocation"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("setTestProviderEnabled"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("clearTestProviderEnabled"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("setTestProviderStatus"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("clearTestProviderStatus"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("addGpsMeasurementListener", true));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("addGpsNavigationMessageListener", true));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeGpsMeasurementListener", 0));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeGpsNavigationMessageListener", 0));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("requestGeofence", 0));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeGeofence", 0));
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            addMethodProxy(new MethodProxies.GetLastKnownLocation());
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("addProximityAlert", 0));
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            addMethodProxy(new MethodProxies.RequestLocationUpdatesPI());
            addMethodProxy(new MethodProxies.RemoveUpdatesPI());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            addMethodProxy(new MethodProxies.RequestLocationUpdates());
            addMethodProxy(new MethodProxies.RemoveUpdates());
        }

        addMethodProxy(new MethodProxies.IsProviderEnabled());
        addMethodProxy(new MethodProxies.GetBestProvider());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            addMethodProxy(new MethodProxies.GetLastLocation());
            addMethodProxy(new MethodProxies.AddGpsStatusListener());
            addMethodProxy(new MethodProxies.RemoveGpsStatusListener());
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("addNmeaListener", 0));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeNmeaListener", 0));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addMethodProxy(new MethodProxies.RegisterGnssStatusCallback());
            addMethodProxy(new MethodProxies.UnregisterGnssStatusCallback());
        }


        addMethodProxy(new MethodProxies.getAllProviders());
        addMethodProxy(new MethodProxies.getProviderProperties());
        addMethodProxy(new MethodProxies.sendExtraCommand());
        addMethodProxy(new MethodProxies.locationCallbackFinished());
    }

    private static class FakeReplaceLastPkgMethodProxy extends ReplaceLastPkgMethodProxy {
        private Object mDefValue;

        private FakeReplaceLastPkgMethodProxy(String name, Object def) {
            super(name);
            mDefValue = def;
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            boolean appPermissionEnable = VAppPermissionManager.get().getLocationEnable(getAppPkg());
            if (appPermissionEnable) {
                Log.e("geyao_LocationManStub", method.getName() + " return");
                return 0;
            }
            if (isFakeLocationEnable()) {
                return mDefValue;
            }
            return super.call(who, method, args);
        }
    }
}
