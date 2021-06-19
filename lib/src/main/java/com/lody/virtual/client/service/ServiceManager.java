package com.lody.virtual.client.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.IBinder;
import android.text.TextUtils;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.ClientConfig;
import com.lody.virtual.remote.ServiceResult;

import java.util.Map;

/**
 * @author Lody
 */
public class ServiceManager {

    private static final ServiceManager sInstance = new ServiceManager();
    private final Map<ComponentName, ServiceRecord> mServices = new ArrayMap<>();


    private ServiceManager() {
    }

    public static ServiceManager get() {
        return sInstance;
    }

    private ServiceRecord getServiceRecord(ComponentName component) {
        return mServices.get(component);
    }

    private ServiceRecord getOrCreateService(ComponentName componentName, ServiceInfo serviceInfo) {
        ServiceRecord service = getServiceRecord(componentName);
        return service == null ? handleCreateService(serviceInfo) : service;
    }

    private ServiceRecord handleCreateService(ServiceInfo serviceInfo) {
        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.service = VClient.get().createService(serviceInfo, serviceRecord);
        mServices.put(ComponentUtils.toComponentName(serviceInfo), serviceRecord);
        return serviceRecord;
    }

    private boolean isSameAppProcess(ServiceInfo serviceInfo, int userId){
        String processName = ComponentUtils.getProcessName(serviceInfo);
        ClientConfig clientConfig = VClient.get().getClientConfig();
        if (clientConfig == null) {
            //未调用initProcess，ShadowService重启
            VLog.w("ServiceManager", "isSameAppProcess:false, clientConfig=null");
            return false;
        }
        int clientUserId = VUserHandle.getUserId(clientConfig.vuid);
        if (TextUtils.equals(serviceInfo.packageName, clientConfig.packageName)
                && TextUtils.equals(processName, clientConfig.processName) && clientUserId == userId) {
            return true;
        }
        VLog.w("ServiceManager", "isSameAppProcess:false, cur=%s/%s@%d, new=%s/%s@%d",
                clientConfig.packageName, clientConfig.processName, clientUserId, serviceInfo.packageName, processName, userId);
        return false;
    }

    public int onStartCommand(Intent proxyIntent, int flags) {
        if (proxyIntent == null) {
            return Service.START_NOT_STICKY;
        }
        ServiceInfo serviceInfo = proxyIntent.getParcelableExtra("_VA_|_service_info_");
        Intent intent = proxyIntent.getParcelableExtra("_VA_|_intent_");
        int startId = proxyIntent.getIntExtra("_VA_|_start_id_", -1);
        int userId = proxyIntent.getIntExtra("_VA_|_user_id_", -1);
        if (serviceInfo == null || intent == null || startId == -1 || userId == -1) {
            return Service.START_NOT_STICKY;
        }
        if(!isSameAppProcess(serviceInfo, userId)){
            return Service.START_NOT_STICKY;
        }
        ComponentName component = ComponentUtils.toComponentName(serviceInfo);
        ServiceRecord record;
        try {
            record = getOrCreateService(component, serviceInfo);
        } catch (Throwable e) {
            throw new RuntimeException("startService fail: " + intent, e);
        }
        if (record == null || record.service == null) {
            return Service.START_NOT_STICKY;
        }
        intent.setExtrasClassLoader(record.service.getClassLoader());
        boolean restartRedeliverIntent = proxyIntent.getBooleanExtra("EXTRA_RESTART_REDELIVER_INTENT", true);
        if (!restartRedeliverIntent) {
            intent = null;
        }
        int startResult = record.service.onStartCommand(intent, flags, startId);
        if (startResult == Service.START_STICKY
                || startResult == Service.START_REDELIVER_INTENT) {
            restartRedeliverIntent = startResult == Service.START_REDELIVER_INTENT;
            proxyIntent.putExtra("EXTRA_RESTART_REDELIVER_INTENT", restartRedeliverIntent);
            VActivityManager.get().onServiceStartCommand(VUserHandle.myUserId(), startId, serviceInfo, intent);
        }
        return startResult;
    }

    public void onDestroy() {
        if (this.mServices.size() > 0) {
            for (ServiceRecord record : mServices.values()) {
                Service service = record.service;
                if (service != null) {
                    service.onDestroy();
                }
            }
        }
        this.mServices.clear();
    }

    public IBinder onBind(Intent proxyIntent) {
        Intent intent = proxyIntent.getParcelableExtra("_VA_|_intent_");
        ServiceInfo serviceInfo = proxyIntent.getParcelableExtra("_VA_|_service_info_");
        int userId = proxyIntent.getIntExtra("_VA_|_user_id_", -1);
        if (intent == null || serviceInfo == null || userId == -1) {
            return null;
        }
        if(!isSameAppProcess(serviceInfo, userId)){
            return null;
        }

        ComponentName component = ComponentUtils.toComponentName(serviceInfo);
        ServiceRecord record;
        try {
            record = getOrCreateService(component, serviceInfo);
        } catch (Throwable e) {
            //throw new RuntimeException("bindService fail: " + intent, e);
            record = null;
        }
        if (record == null) {
            return null;
        }
        intent.setExtrasClassLoader(record.service.getClassLoader());
        record.increaseConnectionCount(intent);
        if (record.hasBinder(intent)) {
            IBinder iBinder = record.getBinder(intent);
            if (record.shouldRebind(intent)) {
                record.service.onRebind(intent);
            }
            return iBinder;
        }
        IBinder binder = record.service.onBind(intent);
        record.setBinder(intent, binder);
        return binder;
    }

    public void onUnbind(Intent proxyIntent) {
        Intent intent = proxyIntent.getParcelableExtra("_VA_|_intent_");
        ServiceInfo serviceInfo = proxyIntent.getParcelableExtra("_VA_|_service_info_");
        if (intent == null || serviceInfo == null) {
            return;
        }
        ComponentName component = ComponentUtils.toComponentName(serviceInfo);
        ServiceRecord record = getServiceRecord(component);
        if (record == null) {
            return;
        }
        ServiceResult res = VActivityManager.get().onServiceUnBind(VUserHandle.myUserId(), component);
        if(res.restart) {
            if (!VirtualCore.getConfig().IsServiceCanRestart(serviceInfo)) {
                res.restart = false;
            }
        }
        boolean destroy = res.startId == 0 || res.restart;
        if (destroy || record.decreaseConnectionCount(intent)) {
            boolean rebind = record.service.onUnbind(intent);
            if (destroy) {
                record.service.onDestroy();
                mServices.remove(component);
                VActivityManager.get().onServiceDestroyed(VUserHandle.myUserId(), component);
                if (res.restart) {
                    ServiceRecord sr = getOrCreateService(component, serviceInfo);
                    sr.service.onStartCommand(null, 0, res.startId);
                }
                return;
            }
            record.setShouldRebind(intent, rebind);
        }
    }

    public void onLowMemory() {
        for (ServiceRecord record : mServices.values()) {
            Service service = record.service;
            if (service != null) {
                try {
                    service.onLowMemory();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        for (ServiceRecord record : mServices.values()) {
            Service service = record.service;
            if (service != null) {
                try {
                    service.onConfigurationChanged(newConfig);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onTrimMemory(int level) {
        for (ServiceRecord record : mServices.values()) {
            Service service = record.service;
            if (service != null) {
                try {
                    service.onTrimMemory(level);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doStopServiceOnMainThread(ComponentName component) {
        ServiceRecord r = mServices.get(component);
        if (r != null) {
            r.service.onDestroy();
            mServices.remove(component);
        }
    }

    public void stopService(final ComponentName component) {
        VirtualRuntime.getUIHandler().post(new Runnable() {
            public void run() {
                doStopServiceOnMainThread(component);
            }
        });

    }

}
