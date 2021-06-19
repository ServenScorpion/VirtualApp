package com.lody.virtual.client.hook.secondary;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;

import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.helper.compat.BuildCompat;

import mirror.android.app.IServiceConnectionO;

/**
 * @author Lody
 */

public class ServiceConnectionDelegate extends IServiceConnection.Stub {
    private final static ArrayMap<IBinder, ServiceConnectionDelegate> DELEGATE_MAP = new ArrayMap<>();
    private IServiceConnection mConn;
    private ComponentName targetComponent;

    private ServiceConnectionDelegate(IServiceConnection mConn, ComponentName targetComponent) {
        this.mConn = mConn;
        this.targetComponent = targetComponent;
    }

    public static ServiceConnectionDelegate getDelegate(IServiceConnection conn) {
        if (conn instanceof ServiceConnectionDelegate) {
            return (ServiceConnectionDelegate) conn;
        }
        return DELEGATE_MAP.get(conn.asBinder());
    }

    public static ServiceConnectionDelegate getOrCreateDelegate(IServiceConnection conn, ComponentName targetComponent) {
        if (conn instanceof ServiceConnectionDelegate) {
            return (ServiceConnectionDelegate) conn;
        }
        final IBinder binder = conn.asBinder();
        ServiceConnectionDelegate delegate = DELEGATE_MAP.get(binder);
        if (delegate == null) {
            try {
                binder.linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        DELEGATE_MAP.remove(binder);
                        binder.unlinkToDeath(this, 0);
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            delegate = new ServiceConnectionDelegate(conn, targetComponent);
            DELEGATE_MAP.put(binder, delegate);
        }
        return delegate;
    }

    @Override
    public void connected(ComponentName name, IBinder service) throws RemoteException {
        connected(name, service, false);
    }

    public void connected(ComponentName name, IBinder service, boolean dead) throws RemoteException {
        if (BuildCompat.isOreo()) {
            IServiceConnectionO.connected.call(mConn, targetComponent, service, dead);
        } else {
            mConn.connected(name, service);
        }
    }
}
