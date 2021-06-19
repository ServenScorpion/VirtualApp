package com.lody.virtual.client.ipc;



import android.accounts.Account;
import android.content.ISyncStatusObserver;
import android.content.PeriodicSync;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncRequest;
import android.content.SyncStatusInfo;
import android.database.IContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;

import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.server.interfaces.IContentService;

import java.util.List;

/**
 * @author Lody
 */

public class VContentManager {

    private static final VContentManager sInstance = new VContentManager();

    public static VContentManager get() {
        return sInstance;
    }

    private IContentService mService;

    public IContentService getService() {
        if (!IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IContentService.class, binder);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return IContentService.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.CONTENT));
    }


    public void unregisterContentObserver(IContentObserver observer) throws RemoteException {
        getService().unregisterContentObserver(observer);
    }

    public void registerContentObserver(Uri uri, boolean notifyForDescendants, IContentObserver observer, int userHandle) throws RemoteException {
        getService().registerContentObserver(uri, notifyForDescendants, observer, userHandle);
    }

    public void notifyChange(Uri uri, IContentObserver observer, boolean observerWantsSelfNotifications, boolean syncToNetwork, int userHandle) throws RemoteException {
        getService().notifyChange(uri, observer, observerWantsSelfNotifications, syncToNetwork, userHandle);
    }

    public void requestSync(Account account, String authority, Bundle extras) throws RemoteException {
        getService().requestSync(account, authority, extras);
    }

    public void sync(SyncRequest request) throws RemoteException {
        getService().sync(request);
    }

    public void cancelSync(Account account, String authority) throws RemoteException {
        getService().cancelSync(account, authority);
    }

    public boolean getSyncAutomatically(Account account, String providerName) throws RemoteException {
        return getService().getSyncAutomatically(account, providerName);
    }

    public void setSyncAutomatically(Account account, String providerName, boolean sync) throws RemoteException {
        getService().setSyncAutomatically(account, providerName, sync);
    }

    public List<PeriodicSync> getPeriodicSyncs(Account account, String providerName) throws RemoteException {
        return getService().getPeriodicSyncs(account, providerName);
    }

    public void addPeriodicSync(Account account, String providerName, Bundle extras, long pollFrequency) throws RemoteException {
        getService().addPeriodicSync(account, providerName, extras, pollFrequency);
    }

    public void removePeriodicSync(Account account, String providerName, Bundle extras) throws RemoteException {
        getService().removePeriodicSync(account, providerName, extras);
    }

    public int getIsSyncable(Account account, String providerName) throws RemoteException {
        return getService().getIsSyncable(account, providerName);
    }

    public void setIsSyncable(Account account, String providerName, int syncable) throws RemoteException {
        getService().setIsSyncable(account, providerName, syncable);
    }

    public void setMasterSyncAutomatically(boolean flag) throws RemoteException {
        getService().setMasterSyncAutomatically(flag);
    }

    public boolean getMasterSyncAutomatically() throws RemoteException {
        return getService().getMasterSyncAutomatically();
    }

    public boolean isSyncActive(Account account, String authority) throws RemoteException {
        return getService().isSyncActive(account, authority);
    }

    public List<SyncInfo> getCurrentSyncs() throws RemoteException {
        return getService().getCurrentSyncs();
    }

    public SyncAdapterType[] getSyncAdapterTypes() throws RemoteException {
        return getService().getSyncAdapterTypes();
    }

    public SyncStatusInfo getSyncStatus(Account account, String authority) throws RemoteException {
        return getService().getSyncStatus(account, authority);
    }

    public boolean isSyncPending(Account account, String authority) throws RemoteException {
        return getService().isSyncPending(account, authority);
    }

    public void addStatusChangeListener(int mask, ISyncStatusObserver callback) throws RemoteException {
        getService().addStatusChangeListener(mask, callback);
    }

    public void removeStatusChangeListener(ISyncStatusObserver callback) throws RemoteException {
        getService().removeStatusChangeListener(callback);
    }
}
