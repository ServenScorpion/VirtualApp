package com.lody.virtual.server.content;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncStatusObserver;
import android.content.PeriodicSync;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncRequest;
import android.content.SyncStatusInfo;
import android.database.IContentObserver;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VBinder;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.server.interfaces.IContentService;

import java.util.ArrayList;
import java.util.List;

public final class VContentService extends IContentService.Stub {
    private static final String TAG = "ContentService";
    private static final VContentService sInstance = new VContentService();
    private Context mContext;
    private final ObserverNode mRootNode = new ObserverNode("");
    private SyncManager mSyncManager = null;
    private final Object mSyncManagerLock = new Object();

    public static VContentService get() {
        return sInstance;
    }

    private SyncManager getSyncManager() {

        synchronized (mSyncManagerLock) {
            try {
                // Try to create the SyncManager, return null if it fails (e.g. the disk is full).
                if (mSyncManager == null) mSyncManager = new SyncManager(mContext);
            } catch (SQLiteException e) {
                Log.e(TAG, "Can't create SyncManager", e);
            }
            return mSyncManager;
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            // The content service only throws security exceptions, so let's
            // log all others.
            if (!(e instanceof SecurityException)) {
                e.printStackTrace();
            }
            throw e;
        }
    }

    private VContentService() {
        mContext = VirtualCore.get().getContext();
    }

    public static void systemReady() {
        get().getSyncManager();
    }

    /**
     * Register a content observer tied to a specific user's view of the provider.
     *
     * @param VUserHandle the user whose view of the provider is to be observed.  May be
     *                    the calling user without requiring any permission, otherwise the caller needs to
     *                    hold the INTERACT_ACROSS_USERS_FULL permission.  Pseudousers USER_ALL and
     *                    USER_CURRENT are properly handled; all other pseudousers are forbidden.
     */
    @Override
    public void registerContentObserver(Uri uri, boolean notifyForDescendants,
                                        IContentObserver observer, int VUserHandle) {
        if (observer == null || uri == null) {
            throw new IllegalArgumentException("You must pass a valid uri and observer");
        }

        synchronized (mRootNode) {
            mRootNode.addObserverLocked(uri, observer, notifyForDescendants, mRootNode,
                    VBinder.getCallingUid(), VBinder.getCallingPid(), VUserHandle);
        }
    }

    public void registerContentObserver(Uri uri, boolean notifyForDescendants,
                                        IContentObserver observer) {
        registerContentObserver(uri, notifyForDescendants, observer,
                VUserHandle.getCallingUserId());
    }

    public void unregisterContentObserver(IContentObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("You must pass a valid observer");
        }
        synchronized (mRootNode) {
            mRootNode.removeObserverLocked(observer);
        }
    }

    /**
     * Notify observers of a particular user's view of the provider.
     *
     * @param VUserHandle the user whose view of the provider is to be notified.  May be
     *                    the calling user without requiring any permission, otherwise the caller needs to
     *                    hold the INTERACT_ACROSS_USERS_FULL permission.  Pseudousers USER_ALL and
     *                    USER_CURRENT are properly interpreted; no other pseudousers are allowed.
     */
    @Override
    public void notifyChange(Uri uri, IContentObserver observer,
                             boolean observerWantsSelfNotifications, boolean syncToNetwork,
                             int VUserHandle) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Notifying update of " + uri + " for user " + VUserHandle
                    + " from observer " + observer + ", syncToNetwork " + syncToNetwork);
        }

        final int uid = VBinder.getCallingUid();
        // This makes it so that future permission checks will be in the context of this
        // process rather than the caller's process. We will restore this before returning.
        long identityToken = clearCallingIdentity();
        try {
            ArrayList<ObserverCall> calls = new ArrayList<ObserverCall>();
            synchronized (mRootNode) {
                mRootNode.collectObserversLocked(uri, 0, observer, observerWantsSelfNotifications,
                        VUserHandle, calls);
            }
            final int numCalls = calls.size();
            for (int i = 0; i < numCalls; i++) {
                ObserverCall oc = calls.get(i);
                try {
                    oc.mObserver.onChange(oc.mSelfChange, uri, VUserHandle);
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "Notified " + oc.mObserver + " of " + "update at " + uri);
                    }
                } catch (RemoteException ex) {
                    synchronized (mRootNode) {
                        Log.w(TAG, "Found dead observer, removing");
                        IBinder binder = oc.mObserver.asBinder();
                        final ArrayList<ObserverNode.ObserverEntry> list
                                = oc.mNode.mObservers;
                        int numList = list.size();
                        for (int j = 0; j < numList; j++) {
                            ObserverNode.ObserverEntry oe = list.get(j);
                            if (oe.observer.asBinder() == binder) {
                                list.remove(j);
                                j--;
                                numList--;
                            }
                        }
                    }
                }
            }
            if (syncToNetwork) {
                SyncManager syncManager = getSyncManager();
                if (syncManager != null) {
                    syncManager.scheduleLocalSync(null /* all accounts */, VUserHandle, uid,
                            uri.getAuthority());
                }
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void notifyChange(Uri uri, IContentObserver observer,
                             boolean observerWantsSelfNotifications, boolean syncToNetwork) {
        notifyChange(uri, observer, observerWantsSelfNotifications, syncToNetwork,
                VUserHandle.getCallingUserId());
    }

    /**
     * Hide this class since it is not part of api,
     * but current unittest framework requires it to be public
     *
     * @hide
     */
    public static final class ObserverCall {
        final ObserverNode mNode;
        final IContentObserver mObserver;
        final boolean mSelfChange;

        ObserverCall(ObserverNode node, IContentObserver observer, boolean selfChange) {
            mNode = node;
            mObserver = observer;
            mSelfChange = selfChange;
        }
    }

    @Override
    public void requestSync(Account account, String authority, Bundle extras) {
        ContentResolver.validateSyncExtrasBundle(extras);
        int userId = VUserHandle.getCallingUserId();
        int uId = VBinder.getCallingUid();

        // This makes it so that future permission checks will be in the context of this
        // process rather than the caller's process. We will restore this before returning.
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.scheduleSync(account, userId, uId, authority, extras,
                        0 /* no delay */, 0 /* no delay */,
                        false /* onlyThoseWithUnkownSyncableState */);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    /**
     * Request a sync with a generic {@link android.content.SyncRequest} object. This will be
     * either:
     * periodic OR one-off sync.
     * and
     * anonymous OR provider sync.
     * Depending on the request, we enqueue to suit in the SyncManager.
     *
     * @param request The request object. Validation of this object is done by its builder.
     */
    @Override
    public void sync(SyncRequest request) {
        Bundle extras = mirror.android.content.SyncRequest.mExtras.get(request);
        long flextime = mirror.android.content.SyncRequest.mSyncFlexTimeSecs.get(request);
        long runAtTime = mirror.android.content.SyncRequest.mSyncRunTimeSecs.get(request);
        int userId = VUserHandle.getCallingUserId();
        int uId = VBinder.getCallingUid();

        // This makes it so that future permission checks will be in the context of this
        // process rather than the caller's process. We will restore this before returning.
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                // Sync Adapter registered with the system - old API.
                final Account account = mirror.android.content.SyncRequest.mAccountToSync.get(request);
                final String provider = mirror.android.content.SyncRequest.mAuthority.get(request);
                if (mirror.android.content.SyncRequest.mIsPeriodic.get(request)) {
                    if (runAtTime < 60) {
                        VLog.w(TAG, "Requested poll frequency of " + runAtTime
                                + " seconds being rounded up to 60 seconds.");
                        runAtTime = 60;
                    }
                    PeriodicSync syncToAdd =
                            new PeriodicSync(account, provider, extras, runAtTime);
                    mirror.android.content.PeriodicSync.flexTime.set(syncToAdd, flextime);
                    getSyncManager().getSyncStorageEngine().addPeriodicSync(syncToAdd, userId);
                } else {
                    long beforeRuntimeMillis = (flextime) * 1000;
                    long runtimeMillis = runAtTime * 1000;
                    syncManager.scheduleSync(
                            account, userId, uId, provider, extras,
                            beforeRuntimeMillis, runtimeMillis,
                            false /* onlyThoseWithUnknownSyncableState */);
                }
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    /**
     * Clear all scheduled sync operations that match the uri and cancel the active sync
     * if they match the authority and account, if they are present.
     *
     * @param account   filter the pending and active syncs to cancel using this account
     * @param authority filter the pending and active syncs to cancel using this authority
     */
    @Override
    public void cancelSync(Account account, String authority) {
        if (authority != null && authority.length() == 0) {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
        int userId = VUserHandle.getCallingUserId();
        // This makes it so that future permission checks will be in the context of this
        // process rather than the caller's process. We will restore this before returning.
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.clearScheduledSyncOperations(account, userId, authority);
                syncManager.cancelActiveSync(account, userId, authority);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    /**
     * Get information about the SyncAdapters that are known to the system.
     *
     * @return an array of SyncAdapters that have registered with the system
     */
    @Override
    public SyncAdapterType[] getSyncAdapterTypes() {
        // This makes it so that future permission checks will be in the context of this
        // process rather than the caller's process. We will restore this before returning.
        final int userId = VUserHandle.getCallingUserId();
        final long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            return syncManager.getSyncAdapterTypes();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    @Override
    public boolean getSyncAutomatically(Account account, String providerName) {

        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getSyncAutomatically(
                        account, userId, providerName);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
        return false;
    }

    @Override
    public void setSyncAutomatically(Account account, String providerName, boolean sync) {
        if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setSyncAutomatically(
                        account, userId, providerName, sync);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    /**
     * Old API. Schedule periodic sync with default flex time.
     */
    @Override
    public void addPeriodicSync(Account account, String authority, Bundle extras,
                                long pollFrequency) {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must not be empty.");
        }
        int userId = VUserHandle.getCallingUserId();
        if (pollFrequency < 60) {
            VLog.w(TAG, "Requested poll frequency of " + pollFrequency
                    + " seconds being rounded up to 60 seconds.");
            pollFrequency = 60;
        }

        long identityToken = clearCallingIdentity();
        try {
            // Add default flex time to this sync.
            PeriodicSync syncToAdd =
                    new PeriodicSync(account, authority, extras,
                            pollFrequency);
            mirror.android.content.PeriodicSync.flexTime.set(syncToAdd, SyncStorageEngine.calculateDefaultFlexTime(pollFrequency));
            getSyncManager().getSyncStorageEngine().addPeriodicSync(syncToAdd, userId);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    @Override
    public void removePeriodicSync(Account account, String authority, Bundle extras) {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }

        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            PeriodicSync syncToRemove = new PeriodicSync(account, authority, extras,
                    0 /* Not read for removal */);
            getSyncManager().getSyncStorageEngine().removePeriodicSync(syncToRemove, userId);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }


    @Override
    public List<PeriodicSync> getPeriodicSyncs(Account account, String providerName) {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            return getSyncManager().getSyncStorageEngine().getPeriodicSyncs(
                    account, userId, providerName);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public int getIsSyncable(Account account, String providerName) {
        int userId = VUserHandle.getCallingUserId();

        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getIsSyncable(
                        account, userId, providerName);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
        return -1;
    }

    @Override
    public void setIsSyncable(Account account, String providerName, int syncable) {
        if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setIsSyncable(
                        account, userId, providerName, syncable);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    @Override
    public boolean getMasterSyncAutomatically() {

        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getMasterSyncAutomatically(userId);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
        return false;
    }

    @Override
    public void setMasterSyncAutomatically(boolean flag) {

        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setMasterSyncAutomatically(flag, userId);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean isSyncActive(Account account, String authority) {
        int userId = VUserHandle.getCallingUserId();

        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().isSyncActive(
                        account, userId, authority);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
        return false;
    }

    public List<SyncInfo> getCurrentSyncs() {

        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            List<VSyncInfo> vList = getSyncManager().getSyncStorageEngine().getCurrentSyncsCopy(userId);
            List<SyncInfo> list = new ArrayList<>(vList.size());
            for (VSyncInfo info : vList) {
                list.add(info.toSyncInfo());
            }
            return list;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public SyncStatusInfo getSyncStatus(Account account, String authority) {
        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getStatusByAccountAndAuthority(
                        account, userId, authority);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
        return null;
    }

    public boolean isSyncPending(Account account, String authority) {

        int userId = VUserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().isSyncPending(account, userId, authority);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
        return false;
    }

    public void addStatusChangeListener(int mask, ISyncStatusObserver callback) {
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null && callback != null) {
                syncManager.getSyncStorageEngine().addStatusChangeListener(mask, callback);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver callback) {
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null && callback != null) {
                syncManager.getSyncStorageEngine().removeStatusChangeListener(callback);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    /**
     * Hide this class since it is not part of api,
     * but current unittest framework requires it to be public
     *
     * @hide
     */
    public static final class ObserverNode {
        private class ObserverEntry implements IBinder.DeathRecipient {
            public final IContentObserver observer;
            public final int uid;
            public final int pid;
            public final boolean notifyForDescendants;
            private final int userHandle;
            private final Object observersLock;

            public ObserverEntry(IContentObserver o, boolean n, Object observersLock,
                                 int _uid, int _pid, int _userHandle) {
                this.observersLock = observersLock;
                observer = o;
                uid = _uid;
                pid = _pid;
                userHandle = _userHandle;
                notifyForDescendants = n;
                try {
                    observer.asBinder().linkToDeath(this, 0);
                } catch (RemoteException e) {
                    binderDied();
                }
            }

            public void binderDied() {
                synchronized (observersLock) {
                    removeObserverLocked(observer);
                }
            }
        }

        public static final int INSERT_TYPE = 0;
        public static final int UPDATE_TYPE = 1;
        public static final int DELETE_TYPE = 2;

        private String mName;
        private ArrayList<ObserverNode> mChildren = new ArrayList<ObserverNode>();
        private ArrayList<ObserverEntry> mObservers = new ArrayList<ObserverEntry>();

        public ObserverNode(String name) {
            mName = name;
        }

        private String getUriSegment(Uri uri, int index) {
            if (uri != null) {
                if (index == 0) {
                    return uri.getAuthority();
                } else {
                    return uri.getPathSegments().get(index - 1);
                }
            } else {
                return null;
            }
        }

        private int countUriSegments(Uri uri) {
            if (uri == null) {
                return 0;
            }
            return uri.getPathSegments().size() + 1;
        }

        // Invariant:  VUserHandle is either a hard user number or is USER_ALL
        public void addObserverLocked(Uri uri, IContentObserver observer,
                                      boolean notifyForDescendants, Object observersLock,
                                      int uid, int pid, int VUserHandle) {
            addObserverLocked(uri, 0, observer, notifyForDescendants, observersLock,
                    uid, pid, VUserHandle);
        }

        private void addObserverLocked(Uri uri, int index, IContentObserver observer,
                                       boolean notifyForDescendants, Object observersLock,
                                       int uid, int pid, int VUserHandle) {
            // If this is the leaf node add the observer
            if (index == countUriSegments(uri)) {
                mObservers.add(new ObserverEntry(observer, notifyForDescendants, observersLock,
                        uid, pid, VUserHandle));
                return;
            }

            // Look to see if the proper child already exists
            String segment = getUriSegment(uri, index);
            if (segment == null) {
                throw new IllegalArgumentException("Invalid Uri (" + uri + ") used for observer");
            }
            int N = mChildren.size();
            for (int i = 0; i < N; i++) {
                ObserverNode node = mChildren.get(i);
                if (node.mName.equals(segment)) {
                    node.addObserverLocked(uri, index + 1, observer, notifyForDescendants,
                            observersLock, uid, pid, VUserHandle);
                    return;
                }
            }

            // No child found, create one
            ObserverNode node = new ObserverNode(segment);
            mChildren.add(node);
            node.addObserverLocked(uri, index + 1, observer, notifyForDescendants,
                    observersLock, uid, pid, VUserHandle);
        }

        public boolean removeObserverLocked(IContentObserver observer) {
            int size = mChildren.size();
            for (int i = 0; i < size; i++) {
                boolean empty = mChildren.get(i).removeObserverLocked(observer);
                if (empty) {
                    mChildren.remove(i);
                    i--;
                    size--;
                }
            }

            IBinder observerBinder = observer.asBinder();
            size = mObservers.size();
            for (int i = 0; i < size; i++) {
                ObserverEntry entry = mObservers.get(i);
                if (entry.observer.asBinder() == observerBinder) {
                    mObservers.remove(i);
                    // We no longer need to listen for death notifications. Remove it.
                    observerBinder.unlinkToDeath(entry, 0);
                    break;
                }
            }

            if (mChildren.size() == 0 && mObservers.size() == 0) {
                return true;
            }
            return false;
        }

        private void collectMyObserversLocked(boolean leaf, IContentObserver observer,
                                              boolean observerWantsSelfNotifications, int targetUserHandle,
                                              ArrayList<ObserverCall> calls) {
            int N = mObservers.size();
            IBinder observerBinder = observer == null ? null : observer.asBinder();
            for (int i = 0; i < N; i++) {
                ObserverEntry entry = mObservers.get(i);

                // Don't notify the observer if it sent the notification and isn't interested
                // in self notifications
                boolean selfChange = (entry.observer.asBinder() == observerBinder);
                if (selfChange && !observerWantsSelfNotifications) {
                    continue;
                }

                // Does this observer match the target user?
                if (targetUserHandle == VUserHandle.USER_ALL
                        || entry.userHandle == VUserHandle.USER_ALL
                        || targetUserHandle == entry.userHandle) {
                    // Make sure the observer is interested in the notification
                    if (leaf || entry.notifyForDescendants) {
                        calls.add(new ObserverCall(this, entry.observer, selfChange));
                    }
                }
            }
        }

        /**
         * targetUserHandle is either a hard user handle or is USER_ALL
         */
        public void collectObserversLocked(Uri uri, int index, IContentObserver observer,
                                           boolean observerWantsSelfNotifications, int targetUserHandle,
                                           ArrayList<ObserverCall> calls) {
            String segment = null;
            int segmentCount = countUriSegments(uri);
            if (index >= segmentCount) {
                // This is the leaf node, notify all observers
                collectMyObserversLocked(true, observer, observerWantsSelfNotifications,
                        targetUserHandle, calls);
            } else {
                segment = getUriSegment(uri, index);
                // Notify any observers at this level who are interested in descendants
                collectMyObserversLocked(false, observer, observerWantsSelfNotifications,
                        targetUserHandle, calls);
            }

            int N = mChildren.size();
            for (int i = 0; i < N; i++) {
                ObserverNode node = mChildren.get(i);
                if (segment == null || node.mName.equals(segment)) {
                    // We found the child,
                    node.collectObserversLocked(uri, index + 1,
                            observer, observerWantsSelfNotifications, targetUserHandle, calls);
                    if (segment != null) {
                        break;
                    }
                }
            }
        }
    }
}
