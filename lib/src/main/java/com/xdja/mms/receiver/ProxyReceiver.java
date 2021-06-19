
package com.xdja.mms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.BroadcastIntentData;
import com.lody.virtual.server.am.VActivityManagerService;

class ProxyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent target) {
        target.setPackage(InstallerSetting.MESSAGING_PKG);
        target.setComponent(null);
        VLog.i("ActivityManager", "proxy onReceive intent %s", target.getAction());
        Intent intent = ComponentUtils.redirectBroadcastIntent(target, 0,
                BroadcastIntentData.TYPE_FROM_SYSTEM | BroadcastIntentData.TYPE_FROM_INTENT_SENDER);
        VirtualCore.get().getContext().sendBroadcast(intent);
    }
}
