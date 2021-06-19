
package com.xdja.mms;

import android.app.IntentService;
import android.content.Intent;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.helper.utils.VLog;

public class NoConfirmationSmsSendService extends IntentService {

    public NoConfirmationSmsSendService() {
        // Class name will be the thread name.
        super(NoConfirmationSmsSendService.class.getName());
        // Intent should be redelivered if the process gets killed before completing the job.
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        intent.setComponent(null);
        intent.setPackage(InstallerSetting.MESSAGING_PKG);
        VLog.i("ActivityManager", "proxy service intent %s", intent);
        VActivityManager.get().startService(0, intent);
    }

}
