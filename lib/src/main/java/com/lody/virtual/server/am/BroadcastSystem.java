package com.lody.virtual.server.am;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.BroadcastIntentData;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.server.pm.PackageSetting;
import com.lody.virtual.server.pm.VAppManagerService;
import com.lody.virtual.server.pm.parser.VPackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mirror.android.app.ContextImpl;
import mirror.android.app.LoadedApkHuaWei;
import mirror.android.rms.HwSysResImplP;
import mirror.android.rms.resource.ReceiverResourceLP;
import mirror.android.rms.resource.ReceiverResourceM;
import mirror.android.rms.resource.ReceiverResourceN;
import mirror.android.rms.resource.ReceiverResourceO;

import static android.content.Intent.FLAG_RECEIVER_REGISTERED_ONLY;

/**
 * @author Lody
 */

public class BroadcastSystem {

    static final String TAG = BroadcastSystem.class.getSimpleName();
    /**
     * MUST < 10000.
     */
    private static final int BROADCAST_TIME_OUT = 8500;
    private static BroadcastSystem gDefault;

    private final Map<String, Boolean> mReceiverStatus = new ArrayMap<>();
    private final ArrayMap<String, List<StaticBroadcastReceiver>> mReceivers = new ArrayMap<>();
    private final Map<String, BroadcastRecord> mBroadcastRecords = new HashMap<>();
    private final Context mContext;
    private final StaticScheduler mScheduler;
    private final TimeoutHandler mTimeoutHandler;
    private final VActivityManagerService mAMS;
    private final VAppManagerService mApp;

    private final HandlerThread mWorkThread;
    private final HandlerThread mTimeoutThread;

    private BroadcastSystem(Context context, VActivityManagerService ams, VAppManagerService app) {
        this.mContext = context;
        this.mApp = app;
        this.mAMS = ams;
        mWorkThread = new HandlerThread("_VA_ams_bs_work");
        mTimeoutThread = new HandlerThread("_VA_ams_bs_timeout");
        mWorkThread.start();
        mTimeoutThread.start();
        mScheduler = new StaticScheduler(mWorkThread.getLooper());
        mTimeoutHandler = new TimeoutHandler(mTimeoutThread.getLooper());
        fuckHuaWeiVerifier();
    }

    /**
     * FIX ISSUE #171:
     * java.lang.AssertionError: Register too many Broadcast Receivers
     * at android.app.LoadedApk.checkRecevierRegisteredLeakLocked(LoadedApk.java:772)
     * at android.app.LoadedApk.getReceiverDispatcher(LoadedApk.java:800)
     * at android.app.ContextImpl.registerReceiverInternal(ContextImpl.java:1329)
     * at android.app.ContextImpl.registerReceiver(ContextImpl.java:1309)
     * at com.lody.virtual.server.am.BroadcastSystem.startApp(BroadcastSystem.java:54)
     * at com.lody.virtual.server.pm.VAppManagerService.install(VAppManagerService.java:193)
     * at com.lody.virtual.server.pm.VAppManagerService.preloadAllApps(VAppManagerService.java:98)
     * at com.lody.virtual.server.pm.VAppManagerService.systemReady(VAppManagerService.java:70)
     * at com.lody.virtual.server.BinderProvider.onCreate(BinderProvider.java:42)
     */
    private void fuckHuaWeiVerifier() {
        if (LoadedApkHuaWei.mReceiverResource != null) {
            Object packageInfo = ContextImpl.mPackageInfo.get(mContext);
            if (packageInfo != null) {
                Object receiverResource = LoadedApkHuaWei.mReceiverResource.get(packageInfo);
                if (receiverResource != null) {
                    if (BuildCompat.isPie() || BuildCompat.isOreo()) {
                        //AMS进程判断, 非白名单每进程最多1000个receiver对象
                        //最差情况，一个月应用100个静态广播接收者，va里面能装10个这样的，多开同一个应用还是按一个计算
                        if (HwSysResImplP.mWhiteListMap != null) {
                            Map<Integer, ArrayList<String>> whiteMap = HwSysResImplP.mWhiteListMap.get(receiverResource);
                            ArrayList<String> list = whiteMap.get(0);
                            if (null == list) {
                                list = new ArrayList<>();
                                whiteMap.put(0, list);
                            }
                            list.add(mContext.getPackageName());
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (ReceiverResourceN.mWhiteList != null) {
                            List<String> whiteList = ReceiverResourceN.mWhiteList.get(receiverResource);
                            List<String> newWhiteList = new ArrayList<>();
                            // Add our package name to the white list.
                            newWhiteList.add(mContext.getPackageName());
                            if (whiteList != null) {
                                newWhiteList.addAll(whiteList);
                            }
                            ReceiverResourceN.mWhiteList.set(receiverResource, newWhiteList);
                        }
                    } else {
                        if (ReceiverResourceM.mWhiteList != null) {
                            String[] whiteList = ReceiverResourceM.mWhiteList.get(receiverResource);
                            List<String> newWhiteList = new LinkedList<>();
                            Collections.addAll(newWhiteList, whiteList);
                            // Add our package name to the white list.
                            newWhiteList.add(mContext.getPackageName());
                            ReceiverResourceM.mWhiteList.set(receiverResource, newWhiteList.toArray(new String[newWhiteList.size()]));
                        } else if (ReceiverResourceLP.mResourceConfig != null) {
                            // Just clear the ResourceConfig.
                            ReceiverResourceLP.mResourceConfig.set(receiverResource, null);
                        }
                    }
                }
            }
        }
    }

    public static void attach(VActivityManagerService ams, VAppManagerService app) {
        if (gDefault != null) {
            throw new IllegalStateException();
        }
        gDefault = new BroadcastSystem(VirtualCore.get().getContext(), ams, app);
    }

    public static BroadcastSystem get() {
        return gDefault;
    }

    public void startApp(VPackage p) {
        Boolean status;
        synchronized (mReceiverStatus) {
            status = mReceiverStatus.get(p.packageName);
        }
        if(status != null){
            stopApp(p.packageName);
        }
        synchronized (mReceiverStatus) {
            mReceiverStatus.put(p.packageName, true);
        }
        VLog.d(TAG, "startApp:%s,version=%s/%d", p.packageName, p.mVersionName, p.mVersionCode);
        PackageSetting setting = (PackageSetting) p.mExtras;
        //微信有60多个静态receiver,华为低版本是每进程500个receiver对象，高版本是每进程1000个对象
        List<StaticBroadcastReceiver> receivers = mReceivers.get(p.packageName);
        if (receivers == null) {
            receivers = new ArrayList<>();
            mReceivers.put(p.packageName, receivers);
        }
        for (VPackage.ActivityComponent receiver : p.receivers) {
            ActivityInfo info = receiver.info;
            String componentAction = ComponentUtils.getComponentAction(info);
            IntentFilter componentFilter = new IntentFilter(componentAction);
            StaticBroadcastReceiver r = new StaticBroadcastReceiver(setting.appId, info);
            mContext.registerReceiver(r, componentFilter, null, mScheduler);
            receivers.add(r);
            Log.v("kk", "registerReceiver:" + info.name + ",action=" + componentAction);
            for (VPackage.ActivityIntentInfo ci : receiver.intents) {
                IntentFilter cloneFilter = new IntentFilter(ci.filter);
                SpecialComponentList.protectIntentFilter(cloneFilter);
                mContext.registerReceiver(r, cloneFilter, null, mScheduler);
            }
        }
    }

    public void stopApp(String packageName) {
        Boolean status;
        synchronized (mReceiverStatus) {
            status = mReceiverStatus.remove(packageName);
        }
        if(status == null || !status){
            return;
        }
        synchronized (mBroadcastRecords) {
            Iterator<Map.Entry<String, BroadcastRecord>> iterator = mBroadcastRecords.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, BroadcastRecord> entry = iterator.next();
                BroadcastRecord record = entry.getValue();
                if (record.receiverInfo.packageName.equals(packageName)) {
                    record.pendingResult.finish();
                    iterator.remove();
                }
            }
        }
        synchronized (mReceivers) {
            List<StaticBroadcastReceiver> receivers = mReceivers.get(packageName);
            if (receivers != null) {
                for (int i = receivers.size() - 1; i >= 0; i--) {
                    StaticBroadcastReceiver r = receivers.get(i);
                    try {
                        mContext.unregisterReceiver(r);
                    }catch (Throwable e){
                        //ignore
                    }
                    receivers.remove(r);
                }
            }
            mReceivers.remove(packageName);
        }
    }

    void broadcastFinish(PendingResultData res, int userId) {
        BroadcastRecord record;
        synchronized (mBroadcastRecords) {
            record = mBroadcastRecords.remove(res.getKey());
        }
        if (record == null) {
            VLog.e(TAG, "Unable to find the BroadcastRecord: [%s@%d]", res.getKey(), userId);
            return;
        } else {
            VLog.v(TAG, "broadcastFinish token: [%s] %s", record.receiverInfo.name, res.getKey());
        }
        mTimeoutHandler.removeMessages(BROADCAST_TIME_OUT, res.getKey());
        if(!record.timeout) {
            record.finished = true;
            res.finish();
        }
    }

    void broadcastSent(int vuid, ActivityInfo receiverInfo, PendingResultData res, Intent intent) {
        VLog.v(TAG, "broadcastSent token: [%s@%s] %s", receiverInfo.name, res.getKey(), intent.getAction());
        BroadcastRecord record = new BroadcastRecord(vuid, receiverInfo, res);
        record.action = intent.getAction();
        synchronized (mBroadcastRecords) {
            mBroadcastRecords.put(res.getKey(), record);
        }
        Message msg = new Message();
        msg.obj = res.getKey();
        mTimeoutHandler.sendMessageDelayed(msg, BROADCAST_TIME_OUT);
    }

    private static final class StaticScheduler extends Handler {
        public StaticScheduler(Looper looper) {
            super(looper);
        }
    }

    private static final class BroadcastRecord {
        int vuid;
        ActivityInfo receiverInfo;
        PendingResultData pendingResult;
        String action;
        boolean finished;
        boolean timeout;

        BroadcastRecord(int vuid, ActivityInfo receiverInfo, PendingResultData pendingResult) {
            this.vuid = vuid;
            this.receiverInfo = receiverInfo;
            this.pendingResult = pendingResult;
        }
    }

    private final class TimeoutHandler extends Handler {
        public TimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String key = (String) msg.obj;
            BroadcastRecord r;
            synchronized (mBroadcastRecords) {
                r = mBroadcastRecords.remove(key);
            }
            if (r != null) {
                if(!r.finished) {
                    r.timeout = true;
                    VLog.w(TAG, "Broadcast timeout, cancel to dispatch it [%s@%d] %s", r.receiverInfo.name, r.vuid, r.action);
                    r.pendingResult.finish();
                }
            }
        }
    }

    private final class StaticBroadcastReceiver extends BroadcastReceiver {
        private static final int FLAG_RECEIVER_INCLUDE_BACKGROUND = 0x01000000;
        private static final int FLAG_RECEIVER_EXCLUDE_BACKGROUND = 0x00800000;
        private static final int FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT = 0x04000000;
        private int appId;
        private ActivityInfo info;
        private ComponentName componentName;

        private StaticBroadcastReceiver(int appId, ActivityInfo info) {
            this.appId = appId;
            this.info = info;
            this.componentName = ComponentUtils.toComponentName(info);
        }

        private boolean isBackgroundAction(String action) {
            //8.0 下面广播是无法通过静态广播接收
            return Intent.ACTION_PACKAGE_ADDED.equals(action)
                    || Intent.ACTION_PACKAGE_REPLACED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mApp.isBooting()) {
                return;
            }
            if ((intent.getFlags() & FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT) != 0) {
                VLog.w(TAG, "StaticBroadcastReceiver:%s ignore by FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT:%s", info.name, intent.getAction());
                return;
            }
            if ((intent.getFlags() & FLAG_RECEIVER_REGISTERED_ONLY) != 0 || isInitialStickyBroadcast()) {
                VLog.w(TAG, "StaticBroadcastReceiver:%s ignore by FLAG_RECEIVER_REGISTERED_ONLY:%s", info.name, intent.getAction());
                return;
            }
            String targetPackage = intent.getStringExtra("_VA_|_privilege_pkg_");
            if(!TextUtils.isEmpty(targetPackage) && !TextUtils.equals(info.packageName, targetPackage)){
                VLog.w(TAG, "StaticBroadcastReceiver:%s ignore by targetPackage:%s", info.packageName, intent.getAction());
                return;
            }
            BroadcastIntentData data = null;
            if (intent.hasExtra("_VA_|_data_")) {
                intent.setExtrasClassLoader(BroadcastIntentData.class.getClassLoader());
                try {
                    data = intent.getParcelableExtra("_VA_|_data_");
                } catch (Throwable e) {
                    // ignore
                }
            }
            //如果应用停止，允许广播唤醒应用
            //1.系统发送的广播
            //ShadowPendingReceiver
            //
            if (data == null) {
                //系统
                intent.setPackage(null);
                data = new BroadcastIntentData(VUserHandle.USER_ALL, intent, null, BroadcastIntentData.TYPE_FROM_SYSTEM);
            } else {
                //广播本身的限制条件过滤
                if(data.intent.getPackage() != null && !TextUtils.equals(info.packageName, data.intent.getPackage())){
                    //该广播是指定包名
                    VLog.d(TAG, "StaticBroadcastReceiver:%s ignore by package. %s", info.packageName, data.intent.getPackage());
                    return;
                }
                if(data.intent.getComponent() != null && !componentName.equals(data.intent.getComponent())){
                    //该广播是指定组件名
                    VLog.d(TAG, "StaticBroadcastReceiver:ignore by component. %s", data.intent.getAction());
                    return;
                }
                //8.0的静态广播限制
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.O) {
                    //非系统发送的广播
                    if (InstallerSetting.privApps.contains(info.packageName)//provider
                            || (data.isFromSystem() && !isBackgroundAction(data.intent.getAction()))) {
                        // 允许系统应用接收隐式广播
                        // 允许来自服务进程的广播（除了应用安装/卸载/替换广播
                        // MDM在InstallerSetting.privApps名单，允许被收到全部静态广播
                    } else if (data.intent.getComponent() == null
                            && data.intent.getPackage() == null
                            && ((data.intent.getFlags() & FLAG_RECEIVER_INCLUDE_BACKGROUND) == 0)) {
                        //该广播未指定组件/应用
                        VLog.d(TAG, "StaticBroadcastReceiver:package and component is null or FLAG_RECEIVER_INCLUDE_BACKGROUND %s", data.intent.getAction());
                        return;
                    }
                }
            }
            VLog.d(TAG, "StaticBroadcastReceiver:[%s] onReceive:%s", info.name, data.intent.getAction());
            mAMS.scheduleStaticBroadcast(data, this.appId, info, data.type, goAsync());
        }
    }

}