// IVClient.aidl
package com.lody.virtual.client;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.content.Intent;

import com.lody.virtual.remote.PendingResultData;

interface IVClient {
    void scheduleReceiver(in String processName, in ComponentName component, in Intent intent, in PendingResultData resultData);
    void scheduleNewIntent(in String creator, in IBinder token, in Intent intent);
    void finishActivity(in IBinder token);
    void closeAllLongSocket();
    void clearSettingProvider();
    IBinder createProxyService(in ComponentName component, in IBinder binder);
    IBinder acquireProviderClient(in ProviderInfo info);
    IBinder getAppThread();
    IBinder getToken();
    boolean isAppRunning();
    boolean isAppForeground();
    String getDebugInfo();
    void stopService(in ComponentName component);
}