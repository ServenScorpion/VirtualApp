package com.lody.virtual.server.content;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncAdapter;
import android.content.ISyncContext;
import android.content.ISyncStatusObserver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.ServiceConnection;
import android.content.SyncAdapterType;
import android.content.SyncResult;
import android.content.SyncStatusInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;

import com.lody.virtual.client.env.Constants;
import com.lody.virtual.helper.compat.ContentResolverCompat;
import com.lody.virtual.os.BackgroundThread;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;
import com.lody.virtual.server.accounts.AccountAndUser;
import com.lody.virtual.server.accounts.VAccountManagerService;
import com.lody.virtual.server.am.VActivityManagerService;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @hide
 */
public class SyncManager {
    private static final String TAG = "SyncManager";

    /**
     * Delay a sync due to local changes this long. In milliseconds
     */
    private static final long LOCAL_SYNC_DELAY;

    /**
     * If a sync takes longer than this and the sync queue is not empty then we will
     * cancel it and add it back to the end of the sync queue. In milliseconds.
     */
    private static final long MAX_TIME_PER_SYNC;

    static {
        final boolean isLargeRAM = true;
        int defaultMaxInitSyncs = isLargeRAM ? 5 : 2;
        int defaultMaxRegularSyncs = isLargeRAM ? 2 : 1;
        MAX_SIMULTANEOUS_INITIALIZATION_SYNCS = defaultMaxInitSyncs;
        MAX_SIMULTANEOUS_REGULAR_SYNCS = defaultMaxRegularSyncs;
        LOCAL_SYNC_DELAY = 30 * 1000 /* 30 seconds */;
        MAX_TIME_PER_SYNC = 5 * 60 * 1000 /* 5 minutes */;
        SYNC_NOTIFICATION_DELAY = 30 * 1000 /* 30 seconds */;
    }

    private static final long SYNC_NOTIFICATION_DELAY;

    /**
     * When retrying a sync for the first time use this delay. After that
     * the retry time will double until it reached MAX_SYNC_RETRY_TIME.
     * In milliseconds.
     */
    private static final long INITIAL_SYNC_RETRY_TIME_IN_MS = 30 * 1000; // 30 seconds

    /**
     * Default the max sync retry time to this value.
     */
    private static final long DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS = 60 * 60; // one hour

    /**
     * How long to wait before retrying a sync that failed due to one already being in progress.
     */
    private static final int DELAY_RETRY_SYNC_IN_PROGRESS_IN_SECONDS = 10;

    private static final int INITIALIZATION_UNBIND_DELAY_MS = 5000;

    private static final String SYNC_WAKE_LOCK_PREFIX = "*sync*";
    private static final String HANDLE_SYNC_ALARM_WAKE_LOCK = "SyncManagerHandleSyncAlarm";
    private static final String SYNC_LOOP_WAKE_LOCK = "SyncLoopWakeLock";

    private static final int MAX_SIMULTANEOUS_REGULAR_SYNCS;
    private static final int MAX_SIMULTANEOUS_INITIALIZATION_SYNCS;

    private Context mContext;

    private static final AccountAndUser[] INITIAL_ACCOUNTS_ARRAY = new AccountAndUser[0];

    // TODO: add better locking around mRunningAccounts
    private volatile AccountAndUser[] mRunningAccounts = INITIAL_ACCOUNTS_ARRAY;

    volatile private boolean mDataConnectionIsConnected = false;
    volatile private boolean mStorageIsLow = false;

    private AlarmManager mAlarmService = null;

    private SyncStorageEngine mSyncStorageEngine;

    private final SyncQueue mSyncQueue;

    protected final ArrayList<ActiveSyncContext> mActiveSyncContexts = new ArrayList<>();

    private final PendingIntent mSyncAlarmIntent;
    // Synchronized on "this". Instead of using this directly one should instead call
    // its accessor, getConnManager().
    private ConnectivityManager mConnManagerDoNotUseDirectly;

    protected SyncAdaptersCache mSyncAdapters;

    private BroadcastReceiver mStorageIntentReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                        Log.v(TAG, "Internal storage is low.");
                        mStorageIsLow = true;
                        cancelActiveSync(null /* any account */, VUserHandle.USER_ALL,
                                null /* any authority */);
                    } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                        Log.v(TAG, "Internal storage is ok.");
                        mStorageIsLow = false;
                        sendCheckAlarmsMessage();
                    }
                }
            };

    private BroadcastReceiver mBootCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mSyncHandler.onBootCompleted();
        }
    };

    private BroadcastReceiver mBackgroundDataSettingChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getConnectivityManager().getBackgroundDataSetting()) {
                scheduleSync(null /* account */, VUserHandle.USER_ALL,
                        SyncOperation.REASON_BACKGROUND_DATA_SETTINGS_CHANGED,
                        null /* authority */,
                        new Bundle(), 0 /* delay */, 0 /* delay */,
                        false /* onlyThoseWithUnknownSyncableState */);
            }
        }
    };

    private BroadcastReceiver mAccountsUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateRunningAccounts();

            // Kick off sync for everyone, since this was a radical account change
            scheduleSync(null, VUserHandle.USER_ALL, SyncOperation.REASON_ACCOUNTS_UPDATED, null,
                    null, 0 /* no delay */, 0/* no delay */, false);
        }
    };

    private final PowerManager mPowerManager;

    // Use this as a random offset to seed all periodic syncs.
    private int mSyncRandomOffsetMillis;

    private final VUserManager mUserManager;

    private static final long SYNC_ALARM_TIMEOUT_MIN = 30 * 1000; // 30 seconds
    private static final long SYNC_ALARM_TIMEOUT_MAX = 2 * 60 * 60 * 1000; // two hours

    private List<VUserInfo> getAllUsers() {
        return mUserManager.getUsers();
    }

    private boolean containsAccountAndUser(AccountAndUser[] accounts, Account account, int userId) {
        boolean found = false;
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].userId == userId
                    && accounts[i].account.equals(account)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public void updateRunningAccounts() {
        mRunningAccounts = VAccountManagerService.get().getAllAccounts();

        if (mBootCompleted) {
            doDatabaseCleanup();
        }

        for (ActiveSyncContext currentSyncContext : mActiveSyncContexts) {
            if (!containsAccountAndUser(mRunningAccounts,
                    currentSyncContext.mSyncOperation.account,
                    currentSyncContext.mSyncOperation.userId)) {
                Log.d(TAG, "canceling sync since the account is no longer running");
                sendSyncFinishedOrCanceledMessage(currentSyncContext,
                        null /* no result since this is a cancel */);
            }
        }

        // we must do this since we don't bother scheduling alarms when
        // the accounts are not set yet
        sendCheckAlarmsMessage();
    }

    private void doDatabaseCleanup() {
        for (VUserInfo user : mUserManager.getUsers(true)) {
            // Skip any partially created/removed users
            if (user.partial) continue;
            Account[] accountsForUser = VAccountManagerService.get().getAccounts(user.id, null);
            mSyncStorageEngine.doDatabaseCleanup(accountsForUser, user.id);
        }
    }

    private BroadcastReceiver mConnectivityIntentReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final boolean wasConnected = mDataConnectionIsConnected;

                    // don't use the intent to figure out if network is connected, just check
                    // ConnectivityManager directly.
                    mDataConnectionIsConnected = readDataConnectionState();
                    if (mDataConnectionIsConnected) {
                        if (!wasConnected) {
                            Log.v(TAG, "Reconnection detected: clearing all backoffs");
                            synchronized (mSyncQueue) {
                                mSyncStorageEngine.clearAllBackoffsLocked(mSyncQueue);
                            }
                        }
                        sendCheckAlarmsMessage();
                    }
                }
            };

    private boolean readDataConnectionState() {
        NetworkInfo networkInfo = getConnectivityManager().getActiveNetworkInfo();
        return (networkInfo != null) && networkInfo.isConnected();
    }

    private BroadcastReceiver mShutdownIntentReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.w(TAG, "Writing sync state before shutdown...");
                    getSyncStorageEngine().writeAllState();
                }
            };

    private BroadcastReceiver mUserIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final int userId = intent.getIntExtra(Constants.EXTRA_USER_HANDLE, VUserHandle.USER_NULL);
            if (userId == VUserHandle.USER_NULL) return;

            if (Constants.ACTION_USER_REMOVED.equals(action)) {
                onUserRemoved(userId);
            } else if (Constants.ACTION_USER_ADDED.equals(action)) {
                onUserStarting(userId);
            } else if (Constants.ACTION_USER_REMOVED.equals(action)) {
                onUserStopping(userId);
            }
        }
    };

    private static final String ACTION_SYNC_ALARM = "android.content.syncmanager.SYNC_ALARM";
    private final SyncHandler mSyncHandler;

    private volatile boolean mBootCompleted = false;

    private ConnectivityManager getConnectivityManager() {
        synchronized (this) {
            if (mConnManagerDoNotUseDirectly == null) {
                mConnManagerDoNotUseDirectly = (ConnectivityManager) mContext.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
            }
            return mConnManagerDoNotUseDirectly;
        }
    }

    public SyncManager(Context context) {
        // Initialize the SyncStorageEngine first, before registering observers
        // and creating threads and so on; it may fail if the disk is full.
        mContext = context;

        SyncStorageEngine.init(context);
        mSyncStorageEngine = SyncStorageEngine.getSingleton();
        mSyncStorageEngine.setOnSyncRequestListener(new SyncStorageEngine.OnSyncRequestListener() {
            @Override
            public void onSyncRequest(Account account, int userId, int reason, String authority,
                                      Bundle extras) {
                scheduleSync(account, userId, reason, authority, extras,
                        0 /* no delay */,
                        0 /* no delay */,
                        false);
            }
        });

        mSyncAdapters = new SyncAdaptersCache(mContext);
        mSyncAdapters.refreshServiceCache(null);
        mSyncQueue = new SyncQueue(mSyncStorageEngine, mSyncAdapters);

        mSyncHandler = new SyncHandler(BackgroundThread.get().getLooper());

        mSyncAlarmIntent = PendingIntent.getBroadcast(
                mContext, 0 /* ignored */, new Intent(ACTION_SYNC_ALARM), 0);

        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(mConnectivityIntentReceiver, intentFilter);

        intentFilter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        context.registerReceiver(mBootCompletedReceiver, intentFilter);

        intentFilter = new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED);
        context.registerReceiver(mBackgroundDataSettingChanged, intentFilter);

        intentFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        context.registerReceiver(mStorageIntentReceiver, intentFilter);

        intentFilter = new IntentFilter(Intent.ACTION_SHUTDOWN);
        intentFilter.setPriority(100);
        context.registerReceiver(mShutdownIntentReceiver, intentFilter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_USER_REMOVED);
        intentFilter.addAction(Constants.ACTION_USER_ADDED);
        intentFilter.addAction(Constants.ACTION_USER_REMOVED);
        mContext.registerReceiver(
                mUserIntentReceiver, intentFilter);

        context.registerReceiver(new SyncAlarmIntentReceiver(),
                new IntentFilter(ACTION_SYNC_ALARM));
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mUserManager = VUserManager.get();

        mSyncStorageEngine.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, new ISyncStatusObserver.Stub() {
                    @Override
                    public void onStatusChanged(int which) {
                        // force the sync loop to run if the settings change
                        sendCheckAlarmsMessage();
                    }
                });

        // Pick a random second in a day to seed all periodic syncs
        mSyncRandomOffsetMillis = mSyncStorageEngine.getSyncRandomOffset() * 1000;
    }

    /**
     * Return a random value v that satisfies minValue <= v < maxValue. The difference between
     * maxValue and minValue must be less than Integer.MAX_VALUE.
     */
    private long jitterize(long minValue, long maxValue) {
        Random random = new Random(SystemClock.elapsedRealtime());
        long spread = maxValue - minValue;
        if (spread > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("the difference between the maxValue and the "
                    + "minValue must be less than " + Integer.MAX_VALUE);
        }
        return minValue + random.nextInt((int) spread);
    }

    public SyncStorageEngine getSyncStorageEngine() {
        return mSyncStorageEngine;
    }

    public int getIsSyncable(Account account, int userId, String providerName) {
        int isSyncable = mSyncStorageEngine.getIsSyncable(account, userId, providerName);
        VUserInfo userInfo = VUserManager.get().getUserInfo(userId);

        // If it's not a restricted user, return isSyncable
        if (userInfo == null || !userInfo.isRestricted()) return isSyncable;

        // Else check if the sync adapter has opted-in or not
        SyncAdaptersCache.SyncAdapterInfo syncAdapterInfo =
                mSyncAdapters.getServiceInfo(
                        account, providerName);
        if (syncAdapterInfo == null) return isSyncable;

        return 0;
    }

    private void ensureAlarmService() {
        if (mAlarmService == null) {
            mAlarmService = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        }
    }

    /**
     * Initiate a sync. This can start a sync for all providers
     * (pass null to url, set onlyTicklable to false), only those
     * providers that are marked as ticklable (pass null to url,
     * set onlyTicklable to true), or a specific provider (set url
     * to the content url of the provider).
     *
     * <p>If the ContentResolver.SYNC_EXTRAS_UPLOAD boolean in extras is
     * true then initiate a sync that just checks for local changes to send
     * to the server, otherwise initiate a sync that first gets any
     * changes from the server before sending local changes back to
     * the server.
     *
     * <p>If a specific provider is being synced (the url is non-null)
     * then the extras can contain SyncAdapter-specific information
     * to control what gets synced (e.g. which specific feed to sync).
     *
     * <p>You'll start getting callbacks after this.
     *
     * @param requestedAccount                 the account to sync, may be null to signify all accounts
     * @param userId                           the id of the user whose accounts are to be synced. If userId is USER_ALL,
     *                                         then all users' accounts are considered.
     * @param reason                           for sync request. If this is a positive integer, it is the Linux uid
     *                                         assigned to the process that requested the sync. If it's negative, the sync was requested by
     *                                         the SyncManager itself and could be one of the following:
     *                                         {@link SyncOperation#REASON_BACKGROUND_DATA_SETTINGS_CHANGED}
     *                                         {@link SyncOperation#REASON_ACCOUNTS_UPDATED}
     *                                         {@link SyncOperation#REASON_SERVICE_CHANGED}
     *                                         {@link SyncOperation#REASON_PERIODIC}
     *                                         {@link SyncOperation#REASON_IS_SYNCABLE}
     *                                         {@link SyncOperation#REASON_SYNC_AUTO}
     *                                         {@link SyncOperation#REASON_MASTER_SYNC_AUTO}
     *                                         {@link SyncOperation#REASON_USER_START}
     * @param requestedAuthority               the authority to sync, may be null to indicate all authorities
     * @param extras                           a Map of SyncAdapter-specific information to control
     *                                         syncs of a specific provider. Can be null. Is ignored
     *                                         if the url is null.
     * @param beforeRuntimeMillis              milliseconds before runtimeMillis that this sync can run.
     * @param runtimeMillis                    maximum milliseconds in the future to wait before performing sync.
     * @param onlyThoseWithUnkownSyncableState Only sync authorities that have unknown state.
     */
    public void scheduleSync(Account requestedAccount, int userId, int reason,
                             String requestedAuthority, Bundle extras, long beforeRuntimeMillis,
                             long runtimeMillis, boolean onlyThoseWithUnkownSyncableState) {
        final boolean backgroundDataUsageAllowed = !mBootCompleted ||
                getConnectivityManager().getBackgroundDataSetting();

        if (extras == null) {
            extras = new Bundle();
        }
        Log.d(TAG, "one-time sync for: " + requestedAccount + " " + extras.toString() + " "
                + requestedAuthority);
        Boolean expedited = extras.getBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
        if (expedited) {
            runtimeMillis = -1; // this means schedule at the front of the queue
        }

        AccountAndUser[] accounts;
        if (requestedAccount != null && userId != VUserHandle.USER_ALL) {
            accounts = new AccountAndUser[]{new AccountAndUser(requestedAccount, userId)};
        } else {
            // if the accounts aren't configured yet then we can't support an account-less
            // sync request
            accounts = mRunningAccounts;
            if (accounts.length == 0) {
                Log.v(TAG, "scheduleSync: no accounts configured, dropping");
                return;
            }
        }

        final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
        final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        if (manualSync) {
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF, true);
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, true);
        }
        final boolean ignoreSettings =
                extras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, false);

        int source;
        if (uploadOnly) {
            source = SyncStorageEngine.SOURCE_LOCAL;
        } else if (manualSync) {
            source = SyncStorageEngine.SOURCE_USER;
        } else if (requestedAuthority == null) {
            source = SyncStorageEngine.SOURCE_POLL;
        } else {
            // this isn't strictly server, since arbitrary callers can (and do) request
            // a non-forced two-way sync on a specific url
            source = SyncStorageEngine.SOURCE_SERVER;
        }

        for (AccountAndUser account : accounts) {
            // Compile a list of authorities that have sync adapters.
            // For each authority sync each account that matches a sync adapter.
            final HashSet<String> syncableAuthorities = new HashSet<String>();
            for (SyncAdaptersCache.SyncAdapterInfo syncAdapter :
                    mSyncAdapters.getAllServices()) {
                syncableAuthorities.add(syncAdapter.type.authority);
            }

            // if the url was specified then replace the list of authorities
            // with just this authority or clear it if this authority isn't
            // syncable
            if (requestedAuthority != null) {
                final boolean hasSyncAdapter = syncableAuthorities.contains(requestedAuthority);
                syncableAuthorities.clear();
                if (hasSyncAdapter) syncableAuthorities.add(requestedAuthority);
            }

            for (String authority : syncableAuthorities) {
                int isSyncable = getIsSyncable(account.account, account.userId,
                        authority);
                if (isSyncable == 0) {
                    continue;
                }
                final SyncAdaptersCache.SyncAdapterInfo syncAdapterInfo;
                syncAdapterInfo = mSyncAdapters.getServiceInfo(
                        account.account, authority);
                if (syncAdapterInfo == null) {
                    continue;
                }
                final boolean allowParallelSyncs = syncAdapterInfo.type.allowParallelSyncs();
                final boolean isAlwaysSyncable = syncAdapterInfo.type.isAlwaysSyncable();
                if (isSyncable < 0 && isAlwaysSyncable) {
                    mSyncStorageEngine.setIsSyncable(account.account, account.userId, authority, 1);
                    isSyncable = 1;
                }
                if (onlyThoseWithUnkownSyncableState && isSyncable >= 0) {
                    continue;
                }
                if (!syncAdapterInfo.type.supportsUploading() && uploadOnly) {
                    continue;
                }

                // always allow if the isSyncable state is unknown
                boolean syncAllowed =
                        (isSyncable < 0)
                                || ignoreSettings
                                || (backgroundDataUsageAllowed
                                && mSyncStorageEngine.getMasterSyncAutomatically(account.userId)
                                && mSyncStorageEngine.getSyncAutomatically(account.account,
                                account.userId, authority));
                if (!syncAllowed) {
                    Log.d(TAG, "scheduleSync: sync of " + account + ", " + authority
                            + " is not allowed, dropping request");
                    continue;
                }

                Pair<Long, Long> backoff = mSyncStorageEngine
                        .getBackoff(account.account, account.userId, authority);
                long delayUntil = mSyncStorageEngine.getDelayUntilTime(account.account,
                        account.userId, authority);
                final long backoffTime = backoff != null ? backoff.first : 0;
                if (isSyncable < 0) {
                    // Initialisation sync.
                    Bundle newExtras = new Bundle();
                    newExtras.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, true);
                    Log.v(TAG, "schedule initialisation Sync:"
                            + ", delay until " + delayUntil
                            + ", run by " + 0
                            + ", source " + source
                            + ", account " + account
                            + ", authority " + authority
                            + ", extras " + newExtras);
                    scheduleSyncOperation(
                            new SyncOperation(account.account, account.userId, reason, source,
                                    authority, newExtras, 0 /* immediate */, 0 /* No flex time*/,
                                    backoffTime, delayUntil, allowParallelSyncs));
                }
                if (!onlyThoseWithUnkownSyncableState) {
                    Log.v(TAG, "scheduleSync:"
                            + " delay until " + delayUntil
                            + " run by " + runtimeMillis
                            + " flex " + beforeRuntimeMillis
                            + ", source " + source
                            + ", account " + account
                            + ", authority " + authority
                            + ", extras " + extras);
                    scheduleSyncOperation(
                            new SyncOperation(account.account, account.userId, reason, source,
                                    authority, extras, runtimeMillis, beforeRuntimeMillis,
                                    backoffTime, delayUntil, allowParallelSyncs));
                }
            }
        }
    }

    /**
     * Schedule sync based on local changes to a provider. Occurs within interval
     * [LOCAL_SYNC_DELAY, 2*LOCAL_SYNC_DELAY].
     */
    public void scheduleLocalSync(Account account, int userId, int reason, String authority) {
        final Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, true);
        scheduleSync(account, userId, reason, authority, extras,
                LOCAL_SYNC_DELAY /* earliest run time */,
                2 * LOCAL_SYNC_DELAY /* latest sync time. */,
                false /* onlyThoseWithUnkownSyncableState */);
    }

    public SyncAdapterType[] getSyncAdapterTypes() {
        final Collection<SyncAdaptersCache.SyncAdapterInfo> serviceInfos;
        serviceInfos = mSyncAdapters.getAllServices();
        SyncAdapterType[] types = new SyncAdapterType[serviceInfos.size()];
        int i = 0;
        for (SyncAdaptersCache.SyncAdapterInfo serviceInfo : serviceInfos) {
            types[i] = serviceInfo.type;
            ++i;
        }
        return types;
    }

    private void sendSyncAlarmMessage() {
        Log.v(TAG, "sending MESSAGE_SYNC_ALARM");
        mSyncHandler.sendEmptyMessage(SyncHandler.MESSAGE_SYNC_ALARM);
    }

    private void sendCheckAlarmsMessage() {
        Log.v(TAG, "sending MESSAGE_CHECK_ALARMS");
        mSyncHandler.removeMessages(SyncHandler.MESSAGE_CHECK_ALARMS);
        mSyncHandler.sendEmptyMessage(SyncHandler.MESSAGE_CHECK_ALARMS);
    }

    private void sendSyncFinishedOrCanceledMessage(ActiveSyncContext syncContext,
                                                   SyncResult syncResult) {
        Log.v(TAG, "sending MESSAGE_SYNC_FINISHED");
        Message msg = mSyncHandler.obtainMessage();
        msg.what = SyncHandler.MESSAGE_SYNC_FINISHED;
        msg.obj = new SyncHandlerMessagePayload(syncContext, syncResult);
        mSyncHandler.sendMessage(msg);
    }

    private void sendCancelSyncsMessage(final Account account, final int userId,
                                        final String authority) {
        Log.v(TAG, "sending MESSAGE_CANCEL");
        Message msg = mSyncHandler.obtainMessage();
        msg.what = SyncHandler.MESSAGE_CANCEL;
        msg.obj = Pair.create(account, authority);
        msg.arg1 = userId;
        mSyncHandler.sendMessage(msg);
    }

    class SyncHandlerMessagePayload {
        public final ActiveSyncContext activeSyncContext;
        public final SyncResult syncResult;

        SyncHandlerMessagePayload(ActiveSyncContext syncContext, SyncResult syncResult) {
            this.activeSyncContext = syncContext;
            this.syncResult = syncResult;
        }
    }

    class SyncAlarmIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendSyncAlarmMessage();
        }
    }

    private void clearBackoffSetting(SyncOperation op) {
        mSyncStorageEngine.setBackoff(op.account, op.userId, op.authority,
                SyncStorageEngine.NOT_IN_BACKOFF_MODE, SyncStorageEngine.NOT_IN_BACKOFF_MODE);
        synchronized (mSyncQueue) {
            mSyncQueue.onBackoffChanged(op.account, op.userId, op.authority, 0);
        }
    }

    private void increaseBackoffSetting(SyncOperation op) {
        // TODO: Use this function to align it to an already scheduled sync
        //       operation in the specified window
        final long now = SystemClock.elapsedRealtime();

        final Pair<Long, Long> previousSettings =
                mSyncStorageEngine.getBackoff(op.account, op.userId, op.authority);
        long newDelayInMs = -1;
        if (previousSettings != null) {
            // don't increase backoff before current backoff is expired. This will happen for op's
            // with ignoreBackoff set.
            if (now < previousSettings.first) {
                Log.v(TAG, "Still in backoff, do not increase it. "
                        + "Remaining: " + ((previousSettings.first - now) / 1000) + " seconds.");
                return;
            }
            // Subsequent delays are the double of the previous delay
            newDelayInMs = previousSettings.second * 2;
        }
        if (newDelayInMs <= 0) {
            // The initial delay is the jitterized INITIAL_SYNC_RETRY_TIME_IN_MS
            newDelayInMs = jitterize(INITIAL_SYNC_RETRY_TIME_IN_MS,
                    (long) (INITIAL_SYNC_RETRY_TIME_IN_MS * 1.1));
        }

        // Cap the delay
        long maxSyncRetryTimeInSeconds = DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS;
        if (newDelayInMs > maxSyncRetryTimeInSeconds * 1000) {
            newDelayInMs = maxSyncRetryTimeInSeconds * 1000;
        }

        final long backoff = now + newDelayInMs;

        mSyncStorageEngine.setBackoff(op.account, op.userId, op.authority,
                backoff, newDelayInMs);

        op.backoff = backoff;
        op.updateEffectiveRunTime();

        synchronized (mSyncQueue) {
            mSyncQueue.onBackoffChanged(op.account, op.userId, op.authority, backoff);
        }
    }

    private void setDelayUntilTime(SyncOperation op, long delayUntilSeconds) {
        final long delayUntil = delayUntilSeconds * 1000;
        final long absoluteNow = System.currentTimeMillis();
        long newDelayUntilTime;
        if (delayUntil > absoluteNow) {
            newDelayUntilTime = SystemClock.elapsedRealtime() + (delayUntil - absoluteNow);
        } else {
            newDelayUntilTime = 0;
        }
        mSyncStorageEngine
                .setDelayUntilTime(op.account, op.userId, op.authority, newDelayUntilTime);
        synchronized (mSyncQueue) {
            mSyncQueue.onDelayUntilTimeChanged(op.account, op.authority, newDelayUntilTime);
        }
    }

    /**
     * Cancel the active sync if it matches the authority and account.
     *
     * @param account   limit the cancelations to syncs with this account, if non-null
     * @param authority limit the cancelations to syncs with this authority, if non-null
     */
    public void cancelActiveSync(Account account, int userId, String authority) {
        sendCancelSyncsMessage(account, userId, authority);
    }

    /**
     * Create and schedule a SyncOperation.
     *
     * @param syncOperation the SyncOperation to schedule
     */
    public void scheduleSyncOperation(SyncOperation syncOperation) {
        boolean queueChanged;
        synchronized (mSyncQueue) {
            queueChanged = mSyncQueue.add(syncOperation);
        }

        if (queueChanged) {
            Log.v(TAG, "scheduleSyncOperation: enqueued " + syncOperation);
            sendCheckAlarmsMessage();
        } else {
            Log.v(TAG, "scheduleSyncOperation: dropping duplicate sync operation "
                    + syncOperation);
        }
    }

    /**
     * Remove scheduled sync operations.
     *
     * @param account   limit the removals to operations with this account, if non-null
     * @param authority limit the removals to operations with this authority, if non-null
     */
    public void clearScheduledSyncOperations(Account account, int userId, String authority) {
        synchronized (mSyncQueue) {
            mSyncQueue.remove(account, userId, authority);
        }
        mSyncStorageEngine.setBackoff(account, userId, authority,
                SyncStorageEngine.NOT_IN_BACKOFF_MODE, SyncStorageEngine.NOT_IN_BACKOFF_MODE);
    }

    void maybeRescheduleSync(SyncResult syncResult, SyncOperation operation) {
        Log.d(TAG, "encountered error(s) during the sync: " + syncResult + ", " + operation);

        operation = new SyncOperation(operation);

        // The SYNC_EXTRAS_IGNORE_BACKOFF only applies to the first attempt to sync a given
        // request. Retries of the request will always honor the backoff, so clear the
        // flag in case we retry this request.
        if (operation.extras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF, false)) {
            operation.extras.remove(ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF);
        }

        // If this sync aborted because the internal sync loop retried too many times then
        //   don't reschedule. Otherwise we risk getting into a retry loop.
        // If the operation succeeded to some extent then retry immediately.
        // If this was a two-way sync then retry soft errors with an exponential backoff.
        // If this was an upward sync then schedule a two-way sync immediately.
        // Otherwise do not reschedule.
        if (operation.extras.getBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false)) {
            Log.d(TAG, "not retrying sync operation because SYNC_EXTRAS_DO_NOT_RETRY was specified "
                    + operation);
        } else if (operation.extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false)
                && !syncResult.syncAlreadyInProgress) {
            operation.extras.remove(ContentResolver.SYNC_EXTRAS_UPLOAD);
            Log.d(TAG, "retrying sync operation as a two-way sync because an upload-only sync "
                    + "encountered an error: " + operation);
            scheduleSyncOperation(operation);
        } else if (syncResult.tooManyRetries) {
            Log.d(TAG, "not retrying sync operation because it retried too many times: "
                    + operation);
        } else if (syncResult.madeSomeProgress()) {
            Log.d(TAG, "retrying sync operation because even though it had an error "
                    + "it achieved some success");
            scheduleSyncOperation(operation);
        } else if (syncResult.syncAlreadyInProgress) {
            Log.d(TAG, "retrying sync operation that failed because there was already a "
                    + "sync in progress: " + operation);
            scheduleSyncOperation(
                    new SyncOperation(
                            operation.account, operation.userId,
                            operation.reason,
                            operation.syncSource,
                            operation.authority, operation.extras,
                            DELAY_RETRY_SYNC_IN_PROGRESS_IN_SECONDS * 1000, operation.flexTime,
                            operation.backoff, operation.delayUntil, operation.allowParallelSyncs));
        } else if (syncResult.hasSoftError()) {
            Log.d(TAG, "retrying sync operation because it encountered a soft error: "
                    + operation);
            scheduleSyncOperation(operation);
        } else {
            Log.d(TAG, "not retrying sync operation because the error is a hard error: "
                    + operation);
        }
    }

    private void onUserStarting(int userId) {

        mSyncAdapters.refreshServiceCache(null);

        updateRunningAccounts();

        synchronized (mSyncQueue) {
            mSyncQueue.addPendingOperations(userId);
        }

        // Schedule sync for any accounts under started user
        final Account[] accounts = VAccountManagerService.get().getAccounts(userId, null);
        for (Account account : accounts) {
            scheduleSync(account, userId, SyncOperation.REASON_USER_START, null, null,
                    0 /* no delay */, 0 /* No flex */,
                    true /* onlyThoseWithUnknownSyncableState */);
        }

        sendCheckAlarmsMessage();
    }

    private void onUserStopping(int userId) {
        updateRunningAccounts();

        cancelActiveSync(
                null /* any account */,
                userId,
                null /* any authority */);
    }

    private void onUserRemoved(int userId) {
        updateRunningAccounts();

        // Clean up the storage engine database
        mSyncStorageEngine.doDatabaseCleanup(new Account[0], userId);
        synchronized (mSyncQueue) {
            mSyncQueue.removeUser(userId);
        }
    }

    /**
     * @hide
     */
    class ActiveSyncContext extends ISyncContext.Stub
            implements ServiceConnection, IBinder.DeathRecipient {
        final SyncOperation mSyncOperation;
        final long mHistoryRowId;
        ISyncAdapter mSyncAdapter;
        final long mStartTime;
        long mTimeoutStartTime;
        boolean mBound;
        VSyncInfo mSyncInfo;
        boolean mIsLinkedToDeath = false;

        /**
         * Create an ActiveSyncContext for an impending sync and grab the wakelock for that
         * sync adapter. Since this grabs the wakelock you need to be sure to call
         * close() when you are done with this ActiveSyncContext, whether the sync succeeded
         * or not.
         *
         * @param syncOperation the SyncOperation we are about to sync
         * @param historyRowId  the row in which to record the history info for this sync
         */
        public ActiveSyncContext(SyncOperation syncOperation, long historyRowId) {
            super();
            mSyncOperation = syncOperation;
            mHistoryRowId = historyRowId;
            mSyncAdapter = null;
            mStartTime = SystemClock.elapsedRealtime();
            mTimeoutStartTime = mStartTime;
        }

        public void sendHeartbeat() {
            // heartbeats are no longer used
        }

        public void onFinished(SyncResult result) {
            Log.v(TAG, "onFinished: " + this);
            // include "this" in the message so that the handler can ignore it if this
            // ActiveSyncContext is no longer the mActiveSyncContext at message handling
            // time
            sendSyncFinishedOrCanceledMessage(this, result);
        }

        public void toString(StringBuilder sb) {
            sb.append("startTime ").append(mStartTime)
                    .append(", mTimeoutStartTime ").append(mTimeoutStartTime)
                    .append(", mHistoryRowId ").append(mHistoryRowId)
                    .append(", syncOperation ").append(mSyncOperation);
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Message msg = mSyncHandler.obtainMessage();
            msg.what = SyncHandler.MESSAGE_SERVICE_CONNECTED;
            msg.obj = new ServiceConnectionData(this, ISyncAdapter.Stub.asInterface(service));
            mSyncHandler.sendMessage(msg);
        }

        public void onServiceDisconnected(ComponentName name) {
            Message msg = mSyncHandler.obtainMessage();
            msg.what = SyncHandler.MESSAGE_SERVICE_DISCONNECTED;
            msg.obj = new ServiceConnectionData(this, null);
            mSyncHandler.sendMessage(msg);
        }

        boolean bindToSyncAdapter(SyncAdaptersCache.SyncAdapterInfo info, int userId) {
            Log.d(TAG, "bindToSyncAdapter: " + info.serviceInfo + ", connection " + this);
            Intent intent = new Intent();
            intent.setAction("android.content.SyncAdapter");
            intent.setComponent(info.componentName);
            // XXX
//            intent.putExtra(Intent.EXTRA_CLIENT_LABEL,
//                    com.android.internal.R.string.sync_binding_label);
//            intent.putExtra(Intent.EXTRA_CLIENT_INTENT, PendingIntent.getActivityAsUser(
//                    mContext, 0, new Intent(Settings.ACTION_SYNC_SETTINGS), 0,
//                    null, new VUserHandle(userId)));
            mBound = true;
            final boolean bindResult = VActivityManagerService.get().bindServiceAsUser(intent, this,
                    Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND
                            | Context.BIND_ALLOW_OOM_MANAGEMENT,
                    new VUserHandle(mSyncOperation.userId));
            if (!bindResult) {
                mBound = false;
            }
            return bindResult;
        }

        /**
         * Performs the required cleanup, which is the releasing of the wakelock and
         * unbinding from the sync adapter (if actually bound).
         */
        protected void close() {
            Log.d(TAG, "unBindFromSyncAdapter: connection " + this);
            if (mBound) {
                mBound = false;
                mContext.unbindService(this);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        @Override
        public void binderDied() {
            sendSyncFinishedOrCanceledMessage(this, null);
        }
    }


    static String formatTime(long time) {
        Time tobj = new Time();
        tobj.set(time);
        return tobj.format("%Y-%m-%d %H:%M:%S");
    }

    private String getLastFailureMessage(int code) {
        switch (code) {
            case ContentResolverCompat.SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS:
                return "sync already in progress";

            case ContentResolverCompat.SYNC_ERROR_AUTHENTICATION:
                return "authentication error";

            case ContentResolverCompat.SYNC_ERROR_IO:
                return "I/O error";

            case ContentResolverCompat.SYNC_ERROR_PARSE:
                return "parse error";

            case ContentResolverCompat.SYNC_ERROR_CONFLICT:
                return "conflict error";

            case ContentResolverCompat.SYNC_ERROR_TOO_MANY_DELETIONS:
                return "too many deletions error";

            case ContentResolverCompat.SYNC_ERROR_TOO_MANY_RETRIES:
                return "too many retries error";

            case ContentResolverCompat.SYNC_ERROR_INTERNAL:
                return "internal error";

            default:
                return "unknown";
        }
    }

    /**
     * A helper object to keep track of the time we have spent syncing since the last boot
     */
    private class SyncTimeTracker {
        /**
         * True if a sync was in progress on the most recent call to update()
         */
        boolean mLastWasSyncing = false;
        /**
         * Used to track when lastWasSyncing was last set
         */
        long mWhenSyncStarted = 0;
        /**
         * The cumulative time we have spent syncing
         */
        private long mTimeSpentSyncing;

        /**
         * Call to let the tracker know that the sync state may have changed
         */
        public synchronized void update() {
            final boolean isSyncInProgress = !mActiveSyncContexts.isEmpty();
            if (isSyncInProgress == mLastWasSyncing) return;
            final long now = SystemClock.elapsedRealtime();
            if (isSyncInProgress) {
                mWhenSyncStarted = now;
            } else {
                mTimeSpentSyncing += now - mWhenSyncStarted;
            }
            mLastWasSyncing = isSyncInProgress;
        }

        /**
         * Get how long we have been syncing, in ms
         */
        public synchronized long timeSpentSyncing() {
            if (!mLastWasSyncing) return mTimeSpentSyncing;

            final long now = SystemClock.elapsedRealtime();
            return mTimeSpentSyncing + (now - mWhenSyncStarted);
        }
    }

    class ServiceConnectionData {
        public final ActiveSyncContext activeSyncContext;
        public final ISyncAdapter syncAdapter;

        ServiceConnectionData(ActiveSyncContext activeSyncContext, ISyncAdapter syncAdapter) {
            this.activeSyncContext = activeSyncContext;
            this.syncAdapter = syncAdapter;
        }
    }

    /**
     * Handles SyncOperation Messages that are posted to the associated
     * HandlerThread.
     */
    class SyncHandler extends Handler {
        // Messages that can be sent on mHandler
        private static final int MESSAGE_SYNC_FINISHED = 1;
        private static final int MESSAGE_SYNC_ALARM = 2;
        private static final int MESSAGE_CHECK_ALARMS = 3;
        private static final int MESSAGE_SERVICE_CONNECTED = 4;
        private static final int MESSAGE_SERVICE_DISCONNECTED = 5;
        private static final int MESSAGE_CANCEL = 6;

        public final SyncNotificationInfo mSyncNotificationInfo = new SyncNotificationInfo();
        private Long mAlarmScheduleTime = null;
        public final SyncTimeTracker mSyncTimeTracker = new SyncTimeTracker();
        private List<Message> mBootQueue = new ArrayList<>();

        public void onBootCompleted() {
            Log.v(TAG, "Boot completed, clearing boot queue.");
            doDatabaseCleanup();
            synchronized (this) {
                // Dispatch any stashed messages.
                for (Message message : mBootQueue) {
                    sendMessage(message);
                }
                mBootQueue = null;
                mBootCompleted = true;
            }
        }

        /**
         * Stash any messages that come to the handler before boot is complete.
         * {@link #onBootCompleted()} will disable this and dispatch all the messages collected.
         *
         * @param msg Message to dispatch at a later point.
         * @return true if a message was enqueued, false otherwise. This is to avoid losing the
         * message if we manage to acquire the lock but by the time we do boot has completed.
         */
        private boolean tryEnqueueMessageUntilReadyToRun(Message msg) {
            synchronized (this) {
                if (!mBootCompleted) {
                    // Need to copy the message bc looper will recycle it.
                    mBootQueue.add(Message.obtain(msg));
                    return true;
                }
                return false;
            }
        }

        /**
         * Used to keep track of whether a sync notification is active and who it is for.
         */
        class SyncNotificationInfo {
            // true iff the notification manager has been asked to send the notification
            public boolean isActive = false;

            // Set when we transition from not running a sync to running a sync, and cleared on
            // the opposite transition.
            public Long startTime = null;

            public void toString(StringBuilder sb) {
                sb.append("isActive ").append(isActive).append(", startTime ").append(startTime);
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                toString(sb);
                return sb.toString();
            }
        }

        public SyncHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (tryEnqueueMessageUntilReadyToRun(msg)) {
                return;
            }

            long earliestFuturePollTime = Long.MAX_VALUE;
            long nextPendingSyncTime = Long.MAX_VALUE;
            // Setting the value here instead of a method because we want the dumpsys logs
            // to have the most recent value used.
            try {
                mDataConnectionIsConnected = readDataConnectionState();
                // Always do this first so that we be sure that any periodic syncs that
                // are ready to run have been converted into pending syncs. This allows the
                // logic that considers the next steps to take based on the set of pending syncs
                // to also take into account the periodic syncs.
                earliestFuturePollTime = scheduleReadyPeriodicSyncs();
                switch (msg.what) {
                    case SyncHandler.MESSAGE_CANCEL: {
                        Pair<Account, String> payload = (Pair<Account, String>) msg.obj;
                        Log.d(TAG, "handleSyncHandlerMessage: MESSAGE_SERVICE_CANCEL: "
                                + payload.first + ", " + payload.second);
                        cancelActiveSyncLocked(payload.first, msg.arg1, payload.second);
                        nextPendingSyncTime = maybeStartNextSyncLocked();
                        break;
                    }

                    case SyncHandler.MESSAGE_SYNC_FINISHED:
                        Log.v(TAG, "handleSyncHandlerMessage: MESSAGE_SYNC_FINISHED");
                        SyncHandlerMessagePayload payload = (SyncHandlerMessagePayload) msg.obj;
                        if (!isSyncStillActive(payload.activeSyncContext)) {
                            Log.d(TAG, "handleSyncHandlerMessage: dropping since the "
                                    + "sync is no longer active: "
                                    + payload.activeSyncContext);
                            break;
                        }
                        runSyncFinishedOrCanceledLocked(payload.syncResult, payload.activeSyncContext);

                        // since a sync just finished check if it is time to start a new sync
                        nextPendingSyncTime = maybeStartNextSyncLocked();
                        break;

                    case SyncHandler.MESSAGE_SERVICE_CONNECTED: {
                        ServiceConnectionData msgData = (ServiceConnectionData) msg.obj;
                        Log.d(TAG, "handleSyncHandlerMessage: MESSAGE_SERVICE_CONNECTED: "
                                + msgData.activeSyncContext);
                        // check that this isn't an old message
                        if (isSyncStillActive(msgData.activeSyncContext)) {
                            runBoundToSyncAdapter(msgData.activeSyncContext, msgData.syncAdapter);
                        }
                        break;
                    }

                    case SyncHandler.MESSAGE_SERVICE_DISCONNECTED: {
                        final ActiveSyncContext currentSyncContext =
                                ((ServiceConnectionData) msg.obj).activeSyncContext;
                        Log.d(TAG, "handleSyncHandlerMessage: MESSAGE_SERVICE_DISCONNECTED: "
                                + currentSyncContext);
                        // check that this isn't an old message
                        if (isSyncStillActive(currentSyncContext)) {
                            // cancel the sync if we have a syncadapter, which means one is
                            // outstanding
                            if (currentSyncContext.mSyncAdapter != null) {
                                try {
                                    currentSyncContext.mSyncAdapter.cancelSync(currentSyncContext);
                                } catch (RemoteException e) {
                                    // we don't need to retry this in this case
                                }
                            }

                            // pretend that the sync failed with an IOException,
                            // which is a soft error
                            SyncResult syncResult = new SyncResult();
                            syncResult.stats.numIoExceptions++;
                            runSyncFinishedOrCanceledLocked(syncResult, currentSyncContext);

                            // since a sync just finished check if it is time to start a new sync
                            nextPendingSyncTime = maybeStartNextSyncLocked();
                        }

                        break;
                    }

                    case SyncHandler.MESSAGE_SYNC_ALARM: {
                        boolean isLoggable = true;
                        Log.v(TAG, "handleSyncHandlerMessage: MESSAGE_SYNC_ALARM");
                        mAlarmScheduleTime = null;
                        nextPendingSyncTime = maybeStartNextSyncLocked();
                        break;
                    }

                    case SyncHandler.MESSAGE_CHECK_ALARMS:
                        Log.v(TAG, "handleSyncHandlerMessage: MESSAGE_CHECK_ALARMS");
                        nextPendingSyncTime = maybeStartNextSyncLocked();
                        break;
                }
            } finally {
                manageSyncNotificationLocked();
                manageSyncAlarmLocked(earliestFuturePollTime, nextPendingSyncTime);
                mSyncTimeTracker.update();
            }
        }

        /**
         * Turn any periodic sync operations that are ready to run into pending sync operations.
         *
         * @return the desired start time of the earliest future  periodic sync operation,
         * in milliseconds since boot
         */
        private long scheduleReadyPeriodicSyncs() {
            Log.v(TAG, "scheduleReadyPeriodicSyncs");
            final boolean backgroundDataUsageAllowed =
                    getConnectivityManager().getBackgroundDataSetting();
            long earliestFuturePollTime = Long.MAX_VALUE;
            if (!backgroundDataUsageAllowed) {
                return earliestFuturePollTime;
            }

            AccountAndUser[] accounts = mRunningAccounts;

            final long nowAbsolute = System.currentTimeMillis();
            final long shiftedNowAbsolute = (0 < nowAbsolute - mSyncRandomOffsetMillis)
                    ? (nowAbsolute - mSyncRandomOffsetMillis) : 0;

            ArrayList<Pair<SyncStorageEngine.AuthorityInfo, SyncStatusInfo>> infos = mSyncStorageEngine
                    .getCopyOfAllAuthoritiesWithSyncStatus();
            for (Pair<SyncStorageEngine.AuthorityInfo, SyncStatusInfo> info : infos) {
                final SyncStorageEngine.AuthorityInfo authorityInfo = info.first;
                final SyncStatusInfo status = info.second;
                if (TextUtils.isEmpty(authorityInfo.authority)) {
                    Log.e(TAG, "Got an empty provider string. Skipping: " + authorityInfo);
                    continue;
                }
                // skip the sync if the account of this operation no longer exists
                if (!containsAccountAndUser(
                        accounts, authorityInfo.account, authorityInfo.userId)) {
                    continue;
                }

                if (!mSyncStorageEngine.getMasterSyncAutomatically(authorityInfo.userId)
                        || !mSyncStorageEngine.getSyncAutomatically(
                        authorityInfo.account, authorityInfo.userId,
                        authorityInfo.authority)) {
                    continue;
                }

                if (getIsSyncable(
                        authorityInfo.account, authorityInfo.userId, authorityInfo.authority)
                        == 0) {
                    continue;
                }

                for (int i = 0, N = authorityInfo.periodicSyncs.size(); i < N; i++) {
                    final PeriodicSync sync = authorityInfo.periodicSyncs.get(i);
                    final Bundle extras = sync.extras;
                    final long periodInMillis = sync.period * 1000;
                    final long flexInMillis = mirror.android.content.PeriodicSync.flexTime.get(sync) * 1000;
                    // Skip if the period is invalid.
                    if (periodInMillis <= 0) {
                        continue;
                    }
                    // Find when this periodic sync was last scheduled to run.
                    final long lastPollTimeAbsolute = status.getPeriodicSyncTime(i);
                    long remainingMillis
                            = periodInMillis - (shiftedNowAbsolute % periodInMillis);
                    long timeSinceLastRunMillis
                            = (nowAbsolute - lastPollTimeAbsolute);
                    // Schedule this periodic sync to run early if it's close enough to its next
                    // runtime, and far enough from its last run time.
                    // If we are early, there will still be time remaining in this period.
                    boolean runEarly = remainingMillis <= flexInMillis
                            && timeSinceLastRunMillis > periodInMillis - flexInMillis;
                    Log.v(TAG, "sync: " + i + " for " + authorityInfo.authority + "."
                            + " period: " + (periodInMillis)
                            + " flex: " + (flexInMillis)
                            + " remaining: " + (remainingMillis)
                            + " time_since_last: " + timeSinceLastRunMillis
                            + " last poll absol: " + lastPollTimeAbsolute
                            + " shifted now: " + shiftedNowAbsolute
                            + " run_early: " + runEarly);
                    /*
                     * Sync scheduling strategy: Set the next periodic sync
                     * based on a random offset (in seconds). Also sync right
                     * now if any of the following cases hold and mark it as
                     * having been scheduled
                     * Case 1: This sync is ready to run now.
                     * Case 2: If the lastPollTimeAbsolute is in the
                     * future, sync now and reinitialize. This can happen for
                     * example if the user changed the time, synced and changed
                     * back.
                     * Case 3: If we failed to sync at the last scheduled
                     * time.
                     * Case 4: This sync is close enough to the time that we can schedule it.
                     */
                    if (runEarly // Case 4
                            || remainingMillis == periodInMillis // Case 1
                            || lastPollTimeAbsolute > nowAbsolute // Case 2
                            || timeSinceLastRunMillis >= periodInMillis) { // Case 3
                        // Sync now

                        final Pair<Long, Long> backoff = mSyncStorageEngine.getBackoff(
                                authorityInfo.account, authorityInfo.userId,
                                authorityInfo.authority);
                        final SyncAdaptersCache.SyncAdapterInfo syncAdapterInfo;
                        syncAdapterInfo = mSyncAdapters.getServiceInfo(
                                authorityInfo.account, authorityInfo.authority);
                        if (syncAdapterInfo == null) {
                            continue;
                        }
                        mSyncStorageEngine.setPeriodicSyncTime(authorityInfo.ident,
                                authorityInfo.periodicSyncs.get(i), nowAbsolute);
                        scheduleSyncOperation(
                                new SyncOperation(authorityInfo.account, authorityInfo.userId,
                                        SyncOperation.REASON_PERIODIC,
                                        SyncStorageEngine.SOURCE_PERIODIC,
                                        authorityInfo.authority, extras,
                                        0 /* runtime */, 0 /* flex */,
                                        backoff != null ? backoff.first : 0,
                                        mSyncStorageEngine.getDelayUntilTime(
                                                authorityInfo.account, authorityInfo.userId,
                                                authorityInfo.authority),
                                        syncAdapterInfo.type.allowParallelSyncs()));

                    }
                    // Compute when this periodic sync should next run.
                    long nextPollTimeAbsolute;
                    if (runEarly) {
                        // Add the time remaining so we don't get out of phase.
                        nextPollTimeAbsolute = nowAbsolute + periodInMillis + remainingMillis;
                    } else {
                        nextPollTimeAbsolute = nowAbsolute + remainingMillis;
                    }
                    if (nextPollTimeAbsolute < earliestFuturePollTime) {
                        earliestFuturePollTime = nextPollTimeAbsolute;
                    }
                }
            }

            if (earliestFuturePollTime == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }

            // convert absolute time to elapsed time
            return SystemClock.elapsedRealtime() +
                    ((earliestFuturePollTime < nowAbsolute) ?
                            0 : (earliestFuturePollTime - nowAbsolute));
        }

        private long maybeStartNextSyncLocked() {
            boolean isLoggable = true;
            if (isLoggable) Log.v(TAG, "maybeStartNextSync");

            // If we aren't ready to run (e.g. the data connection is down), get out.
            if (!mDataConnectionIsConnected) {
                if (isLoggable) {
                    Log.v(TAG, "maybeStartNextSync: no data connection, skipping");
                }
                return Long.MAX_VALUE;
            }

            if (mStorageIsLow) {
                if (isLoggable) {
                    Log.v(TAG, "maybeStartNextSync: memory low, skipping");
                }
                return Long.MAX_VALUE;
            }

            // If the accounts aren't known yet then we aren't ready to run. We will be kicked
            // when the account lookup request does complete.
            AccountAndUser[] accounts = mRunningAccounts;
            if (accounts == INITIAL_ACCOUNTS_ARRAY) {
                if (isLoggable) {
                    Log.v(TAG, "maybeStartNextSync: accounts not known, skipping");
                }
                return Long.MAX_VALUE;
            }

            final long now = SystemClock.elapsedRealtime();

            // will be set to the next time that a sync should be considered for running
            long nextReadyToRunTime = Long.MAX_VALUE;

            // order the sync queue, dropping syncs that are not allowed
            ArrayList<SyncOperation> operations = new ArrayList<SyncOperation>();
            synchronized (mSyncQueue) {
                if (isLoggable) {
                    Log.v(TAG, "build the operation array, syncQueue size is "
                            + mSyncQueue.getOperations().size());
                }
                final Iterator<SyncOperation> operationIterator =
                        mSyncQueue.getOperations().iterator();

                final Set<Integer> removedUsers = new HashSet<>();
                while (operationIterator.hasNext()) {
                    final SyncOperation op = operationIterator.next();

                    // Drop the sync if the account of this operation no longer exists.
                    if (!containsAccountAndUser(accounts, op.account, op.userId)) {
                        operationIterator.remove();
                        mSyncStorageEngine.deleteFromPending(op.pendingOperation);
                        if (isLoggable) {
                            Log.v(TAG, "    Dropping sync operation: account doesn't exist.");
                        }
                        continue;
                    }

                    // Drop this sync request if it isn't syncable.
                    int syncableState = getIsSyncable(
                            op.account, op.userId, op.authority);
                    if (syncableState == 0) {
                        operationIterator.remove();
                        mSyncStorageEngine.deleteFromPending(op.pendingOperation);
                        if (isLoggable) {
                            Log.v(TAG, "    Dropping sync operation: isSyncable == 0.");
                        }
                        continue;
                    }

                    // If the user is not running, drop the request.
                    final VUserInfo userInfo = mUserManager.getUserInfo(op.userId);
                    if (userInfo == null) {
                        removedUsers.add(op.userId);
                    }
                    if (isLoggable) {
                        Log.v(TAG, "    Dropping sync operation: user not running.");
                    }
                    // If the next run time is in the future, even given the flexible scheduling,
                    // return the time.
                }
                for (Integer user : removedUsers) {
                    // if it's still removed
                    if (mUserManager.getUserInfo(user) == null) {
                        onUserRemoved(user);
                    }
                }
            }

            // find the next operation to dispatch, if one is ready
            // iterate from the top, keep issuing (while potentially canceling existing syncs)
            // until the quotas are filled.
            // once the quotas are filled iterate once more to find when the next one would be
            // (also considering pre-emption reasons).
            if (isLoggable) Log.v(TAG, "sort the candidate operations, size " + operations.size());
            Collections.sort(operations);
            if (isLoggable) Log.v(TAG, "dispatch all ready sync operations");
            for (int i = 0, N = operations.size(); i < N; i++) {
                final SyncOperation candidate = operations.get(i);
                final boolean candidateIsInitialization = candidate.isInitialization();

                int numInit = 0;
                int numRegular = 0;
                ActiveSyncContext conflict = null;
                ActiveSyncContext longRunning = null;
                ActiveSyncContext toReschedule = null;
                ActiveSyncContext oldestNonExpeditedRegular = null;

                for (ActiveSyncContext activeSyncContext : mActiveSyncContexts) {
                    final SyncOperation activeOp = activeSyncContext.mSyncOperation;
                    if (activeOp.isInitialization()) {
                        numInit++;
                    } else {
                        numRegular++;
                        if (!activeOp.isExpedited()) {
                            if (oldestNonExpeditedRegular == null
                                    || (oldestNonExpeditedRegular.mStartTime
                                    > activeSyncContext.mStartTime)) {
                                oldestNonExpeditedRegular = activeSyncContext;
                            }
                        }
                    }
                    if (activeOp.account.type.equals(candidate.account.type)
                            && activeOp.authority.equals(candidate.authority)
                            && activeOp.userId == candidate.userId
                            && (!activeOp.allowParallelSyncs
                            || activeOp.account.name.equals(candidate.account.name))) {
                        conflict = activeSyncContext;
                        // don't break out since we want to do a full count of the varieties
                    } else {
                        if (candidateIsInitialization == activeOp.isInitialization()
                                && activeSyncContext.mStartTime + MAX_TIME_PER_SYNC < now) {
                            longRunning = activeSyncContext;
                            // don't break out since we want to do a full count of the varieties
                        }
                    }
                }

                if (isLoggable) {
                    Log.v(TAG, "candidate " + (i + 1) + " of " + N + ": " + candidate);
                    Log.v(TAG, "  numActiveInit=" + numInit + ", numActiveRegular=" + numRegular);
                    Log.v(TAG, "  longRunning: " + longRunning);
                    Log.v(TAG, "  conflict: " + conflict);
                    Log.v(TAG, "  oldestNonExpeditedRegular: " + oldestNonExpeditedRegular);
                }

                final boolean roomAvailable = candidateIsInitialization
                        ? numInit < MAX_SIMULTANEOUS_INITIALIZATION_SYNCS
                        : numRegular < MAX_SIMULTANEOUS_REGULAR_SYNCS;

                if (conflict != null) {
                    if (candidateIsInitialization && !conflict.mSyncOperation.isInitialization()
                            && numInit < MAX_SIMULTANEOUS_INITIALIZATION_SYNCS) {
                        toReschedule = conflict;
                        Log.v(TAG, "canceling and rescheduling sync since an initialization "
                                + "takes higher priority, " + conflict);
                    } else if (candidate.expedited && !conflict.mSyncOperation.expedited
                            && (candidateIsInitialization
                            == conflict.mSyncOperation.isInitialization())) {
                        toReschedule = conflict;
                        Log.v(TAG, "canceling and rescheduling sync since an expedited "
                                + "takes higher priority, " + conflict);
                    } else {
                        continue;
                    }
                } else if (roomAvailable) {
                    // dispatch candidate
                } else if (candidate.isExpedited() && oldestNonExpeditedRegular != null
                        && !candidateIsInitialization) {
                    // We found an active, non-expedited regular sync. We also know that the
                    // candidate doesn't conflict with this active sync since conflict
                    // is null. Reschedule the active sync and start the candidate.
                    toReschedule = oldestNonExpeditedRegular;
                    Log.v(TAG, "canceling and rescheduling sync since an expedited is ready to run, "
                            + oldestNonExpeditedRegular);
                } else if (longRunning != null
                        && (candidateIsInitialization
                        == longRunning.mSyncOperation.isInitialization())) {
                    // We found an active, long-running sync. Reschedule the active
                    // sync and start the candidate.
                    toReschedule = longRunning;
                    Log.v(TAG, "canceling and rescheduling sync since it ran roo long, "
                            + longRunning);
                } else {
                    // we were unable to find or make space to run this candidate, go on to
                    // the next one
                    continue;
                }

                if (toReschedule != null) {
                    runSyncFinishedOrCanceledLocked(null, toReschedule);
                    scheduleSyncOperation(toReschedule.mSyncOperation);
                }
                synchronized (mSyncQueue) {
                    mSyncQueue.remove(candidate);
                }
                dispatchSyncOperation(candidate);
            }

            return nextReadyToRunTime;
        }

        private boolean dispatchSyncOperation(SyncOperation op) {
            Log.v(TAG, "dispatchSyncOperation: we are going to sync " + op);
            Log.v(TAG, "num active syncs: " + mActiveSyncContexts.size());
            for (ActiveSyncContext syncContext : mActiveSyncContexts) {
                Log.v(TAG, syncContext.toString());
            }

            // connect to the sync adapter
            final SyncAdaptersCache.SyncAdapterInfo syncAdapterInfo;
            syncAdapterInfo = mSyncAdapters.getServiceInfo(op.account, op.authority);
            if (syncAdapterInfo == null) {
                Log.d(TAG, "can't find a sync adapter for " + op.authority
                        + ", removing settings for it");
                mSyncStorageEngine.removeAuthority(op.account, op.userId, op.authority);
                return false;
            }

            ActiveSyncContext activeSyncContext =
                    new ActiveSyncContext(op, insertStartSyncEvent(op));
            activeSyncContext.mSyncInfo = mSyncStorageEngine.addActiveSync(activeSyncContext);
            mActiveSyncContexts.add(activeSyncContext);
            Log.v(TAG, "dispatchSyncOperation: starting " + activeSyncContext);
            if (!activeSyncContext.bindToSyncAdapter(syncAdapterInfo, op.userId)) {
                Log.e(TAG, "Bind attempt failed to " + syncAdapterInfo);
                closeActiveSyncContext(activeSyncContext);
                return false;
            }

            return true;
        }

        private void runBoundToSyncAdapter(final ActiveSyncContext activeSyncContext,
                                           ISyncAdapter syncAdapter) {
            activeSyncContext.mSyncAdapter = syncAdapter;
            final SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            try {
                activeSyncContext.mIsLinkedToDeath = true;
                syncAdapter.asBinder().linkToDeath(activeSyncContext, 0);

                syncAdapter.startSync(activeSyncContext, syncOperation.authority,
                        syncOperation.account, syncOperation.extras);
            } catch (RemoteException remoteExc) {
                Log.d(TAG, "maybeStartNextSync: caught a RemoteException, rescheduling", remoteExc);
                closeActiveSyncContext(activeSyncContext);
                increaseBackoffSetting(syncOperation);
                scheduleSyncOperation(new SyncOperation(syncOperation));
            } catch (RuntimeException exc) {
                closeActiveSyncContext(activeSyncContext);
                Log.e(TAG, "Caught RuntimeException while starting the sync " + syncOperation, exc);
            }
        }

        private void cancelActiveSyncLocked(Account account, int userId, String authority) {
            ArrayList<ActiveSyncContext> activeSyncs =
                    new ArrayList<>(mActiveSyncContexts);
            for (ActiveSyncContext activeSyncContext : activeSyncs) {
                if (activeSyncContext != null) {
                    // if an account was specified then only cancel the sync if it matches
                    if (account != null) {
                        if (!account.equals(activeSyncContext.mSyncOperation.account)) {
                            continue;
                        }
                    }
                    // if an authority was specified then only cancel the sync if it matches
                    if (authority != null) {
                        if (!authority.equals(activeSyncContext.mSyncOperation.authority)) {
                            continue;
                        }
                    }
                    // check if the userid matches
                    if (userId != VUserHandle.USER_ALL
                            && userId != activeSyncContext.mSyncOperation.userId) {
                        continue;
                    }
                    runSyncFinishedOrCanceledLocked(null /* no result since this is a cancel */,
                            activeSyncContext);
                }
            }
        }

        private void runSyncFinishedOrCanceledLocked(SyncResult syncResult,
                                                     ActiveSyncContext activeSyncContext) {
            if (activeSyncContext.mIsLinkedToDeath) {
                activeSyncContext.mSyncAdapter.asBinder().unlinkToDeath(activeSyncContext, 0);
                activeSyncContext.mIsLinkedToDeath = false;
            }
            closeActiveSyncContext(activeSyncContext);

            final SyncOperation syncOperation = activeSyncContext.mSyncOperation;

            final long elapsedTime = SystemClock.elapsedRealtime() - activeSyncContext.mStartTime;

            String historyMessage;
            int downstreamActivity;
            int upstreamActivity;
            if (syncResult != null) {
                Log.v(TAG, "runSyncFinishedOrCanceled [finished]: "
                        + syncOperation + ", result " + syncResult);

                if (!syncResult.hasError()) {
                    historyMessage = SyncStorageEngine.MESG_SUCCESS;
                    // TODO: set these correctly when the SyncResult is extended to include it
                    downstreamActivity = 0;
                    upstreamActivity = 0;
                    clearBackoffSetting(syncOperation);
                } else {
                    Log.d(TAG, "failed sync operation " + syncOperation + ", " + syncResult);
                    // the operation failed so increase the backoff time
                    if (!syncResult.syncAlreadyInProgress) {
                        increaseBackoffSetting(syncOperation);
                    }
                    // reschedule the sync if so indicated by the syncResult
                    maybeRescheduleSync(syncResult, syncOperation);
                    historyMessage = ContentResolverCompat.syncErrorToString(
                            syncResultToErrorNumber(syncResult));
                    // TODO: set these correctly when the SyncResult is extended to include it
                    downstreamActivity = 0;
                    upstreamActivity = 0;
                }

                setDelayUntilTime(syncOperation, syncResult.delayUntil);
            } else {
                Log.v(TAG, "runSyncFinishedOrCanceled [canceled]: " + syncOperation);
                if (activeSyncContext.mSyncAdapter != null) {
                    try {
                        activeSyncContext.mSyncAdapter.cancelSync(activeSyncContext);
                    } catch (RemoteException e) {
                        // we don't need to retry this in this case
                    }
                }
                historyMessage = SyncStorageEngine.MESG_CANCELED;
                downstreamActivity = 0;
                upstreamActivity = 0;
            }

            stopSyncEvent(activeSyncContext.mHistoryRowId, syncOperation, historyMessage,
                    upstreamActivity, downstreamActivity, elapsedTime);

            // XXX: installHandleTooManyDeletesNotification

            if (syncResult != null && syncResult.fullSyncRequested) {
                scheduleSyncOperation(
                        new SyncOperation(syncOperation.account, syncOperation.userId,
                                syncOperation.reason,
                                syncOperation.syncSource, syncOperation.authority, new Bundle(),
                                0 /* delay */, 0 /* flex */,
                                syncOperation.backoff, syncOperation.delayUntil,
                                syncOperation.allowParallelSyncs));
            }
            // no need to schedule an alarm, as that will be done by our caller.
        }

        private void closeActiveSyncContext(ActiveSyncContext activeSyncContext) {
            activeSyncContext.close();
            mActiveSyncContexts.remove(activeSyncContext);
            mSyncStorageEngine.removeActiveSync(activeSyncContext.mSyncInfo,
                    activeSyncContext.mSyncOperation.userId);
        }

        /**
         * Convert the error-containing SyncResult into the Sync.History error number. Since
         * the SyncResult may indicate multiple errors at once, this method just returns the
         * most "serious" error.
         *
         * @param syncResult the SyncResult from which to read
         * @return the most "serious" error set in the SyncResult
         * @throws IllegalStateException if the SyncResult does not indicate any errors.
         *                               If SyncResult.error() is true then it is safe to call this.
         */
        private int syncResultToErrorNumber(SyncResult syncResult) {
            if (syncResult.syncAlreadyInProgress)
                return ContentResolverCompat.SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS;
            if (syncResult.stats.numAuthExceptions > 0)
                return ContentResolverCompat.SYNC_ERROR_AUTHENTICATION;
            if (syncResult.stats.numIoExceptions > 0)
                return ContentResolverCompat.SYNC_ERROR_IO;
            if (syncResult.stats.numParseExceptions > 0)
                return ContentResolverCompat.SYNC_ERROR_PARSE;
            if (syncResult.stats.numConflictDetectedExceptions > 0)
                return ContentResolverCompat.SYNC_ERROR_CONFLICT;
            if (syncResult.tooManyDeletions)
                return ContentResolverCompat.SYNC_ERROR_TOO_MANY_DELETIONS;
            if (syncResult.tooManyRetries)
                return ContentResolverCompat.SYNC_ERROR_TOO_MANY_RETRIES;
            if (syncResult.databaseError)
                return ContentResolverCompat.SYNC_ERROR_INTERNAL;
            throw new IllegalStateException("we are not in an error state, " + syncResult);
        }

        private void manageSyncNotificationLocked() {
            boolean shouldCancel;
            boolean shouldInstall;

            if (mActiveSyncContexts.isEmpty()) {
                mSyncNotificationInfo.startTime = null;

                // we aren't syncing. if the notification is active then remember that we need
                // to cancel it and then clear out the info
                shouldCancel = mSyncNotificationInfo.isActive;
                shouldInstall = false;
            } else {
                // we are syncing
                final long now = SystemClock.elapsedRealtime();
                if (mSyncNotificationInfo.startTime == null) {
                    mSyncNotificationInfo.startTime = now;
                }

                // there are three cases:
                // - the notification is up: do nothing
                // - the notification is not up but it isn't time yet: don't install
                // - the notification is not up and it is time: need to install

                if (mSyncNotificationInfo.isActive) {
                    shouldInstall = shouldCancel = false;
                } else {
                    // it isn't currently up, so there is nothing to cancel
                    shouldCancel = false;

                    final boolean timeToShowNotification =
                            now > mSyncNotificationInfo.startTime + SYNC_NOTIFICATION_DELAY;
                    if (timeToShowNotification) {
                        shouldInstall = true;
                    } else {
                        // show the notification immediately if this is a manual sync
                        shouldInstall = false;
                        for (ActiveSyncContext activeSyncContext : mActiveSyncContexts) {
                            final boolean manualSync = activeSyncContext.mSyncOperation.extras
                                    .getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
                            if (manualSync) {
                                shouldInstall = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (shouldCancel && !shouldInstall) {
                sendSyncStateIntent();
                mSyncNotificationInfo.isActive = false;
            }

            if (shouldInstall) {
                sendSyncStateIntent();
                mSyncNotificationInfo.isActive = true;
            }
        }

        private void manageSyncAlarmLocked(long nextPeriodicEventElapsedTime,
                                           long nextPendingEventElapsedTime) {
            // in each of these cases the sync loop will be kicked, which will cause this
            // method to be called again
            if (!mDataConnectionIsConnected) return;
            if (mStorageIsLow) return;

            // When the status bar notification should be raised
            final long notificationTime =
                    (!mSyncHandler.mSyncNotificationInfo.isActive
                            && mSyncHandler.mSyncNotificationInfo.startTime != null)
                            ? mSyncHandler.mSyncNotificationInfo.startTime + SYNC_NOTIFICATION_DELAY
                            : Long.MAX_VALUE;

            // When we should consider canceling an active sync
            long earliestTimeoutTime = Long.MAX_VALUE;
            for (ActiveSyncContext currentSyncContext : mActiveSyncContexts) {
                final long currentSyncTimeoutTime =
                        currentSyncContext.mTimeoutStartTime + MAX_TIME_PER_SYNC;
                Log.v(TAG, "manageSyncAlarm: active sync, mTimeoutStartTime + MAX is "
                        + currentSyncTimeoutTime);
                if (earliestTimeoutTime > currentSyncTimeoutTime) {
                    earliestTimeoutTime = currentSyncTimeoutTime;
                }
            }

            Log.v(TAG, "manageSyncAlarm: notificationTime is " + notificationTime);

            Log.v(TAG, "manageSyncAlarm: earliestTimeoutTime is " + earliestTimeoutTime);

            Log.v(TAG, "manageSyncAlarm: nextPeriodicEventElapsedTime is "
                    + nextPeriodicEventElapsedTime);
            Log.v(TAG, "manageSyncAlarm: nextPendingEventElapsedTime is "
                    + nextPendingEventElapsedTime);

            long alarmTime = Math.min(notificationTime, earliestTimeoutTime);
            alarmTime = Math.min(alarmTime, nextPeriodicEventElapsedTime);
            alarmTime = Math.min(alarmTime, nextPendingEventElapsedTime);

            // Bound the alarm time.
            final long now = SystemClock.elapsedRealtime();
            if (alarmTime < now + SYNC_ALARM_TIMEOUT_MIN) {
                Log.v(TAG, "manageSyncAlarm: the alarmTime is too small, "
                        + alarmTime + ", setting to " + (now + SYNC_ALARM_TIMEOUT_MIN));
                alarmTime = now + SYNC_ALARM_TIMEOUT_MIN;
            } else if (alarmTime > now + SYNC_ALARM_TIMEOUT_MAX) {
                Log.v(TAG, "manageSyncAlarm: the alarmTime is too large, "
                        + alarmTime + ", setting to " + (now + SYNC_ALARM_TIMEOUT_MIN));
                alarmTime = now + SYNC_ALARM_TIMEOUT_MAX;
            }

            // determine if we need to set or cancel the alarm
            boolean shouldSet = false;
            boolean shouldCancel = false;
            final boolean alarmIsActive = (mAlarmScheduleTime != null) && (now < mAlarmScheduleTime);
            final boolean needAlarm = alarmTime != Long.MAX_VALUE;
            if (needAlarm) {
                // Need the alarm if
                //  - it's currently not set
                //  - if the alarm is set in the past.
                if (!alarmIsActive || alarmTime < mAlarmScheduleTime) {
                    shouldSet = true;
                }
            } else {
                shouldCancel = alarmIsActive;
            }

            // Set or cancel the alarm as directed.
            ensureAlarmService();
            if (shouldSet) {
                Log.v(TAG, "requesting that the alarm manager wake us up at elapsed time "
                        + alarmTime + ", now is " + now + ", " + ((alarmTime - now) / 1000)
                        + " secs from now");
                mAlarmScheduleTime = alarmTime;
                mAlarmService.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTime,
                        mSyncAlarmIntent);
            } else if (shouldCancel) {
                mAlarmScheduleTime = null;
                mAlarmService.cancel(mSyncAlarmIntent);
            }
        }

        private void sendSyncStateIntent() {
            // XXX
        }

        public long insertStartSyncEvent(SyncOperation syncOperation) {
            final int source = syncOperation.syncSource;
            final long now = System.currentTimeMillis();

            return mSyncStorageEngine.insertStartSyncEvent(
                    syncOperation.account, syncOperation.userId, syncOperation.reason,
                    syncOperation.authority,
                    now, source, syncOperation.isInitialization(), syncOperation.extras
            );
        }

        public void stopSyncEvent(long rowId, SyncOperation syncOperation, String resultMessage,
                                  int upstreamActivity, int downstreamActivity, long elapsedTime) {
            mSyncStorageEngine.stopSyncEvent(rowId, elapsedTime,
                    resultMessage, downstreamActivity, upstreamActivity);
        }
    }

    private boolean isSyncStillActive(ActiveSyncContext activeSyncContext) {
        for (ActiveSyncContext sync : mActiveSyncContexts) {
            if (sync == activeSyncContext) {
                return true;
            }
        }
        return false;
    }

}
