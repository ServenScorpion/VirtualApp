package com.lody.virtual.client.stub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.compat.IntentCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.BroadcastIntentData;
import com.lody.virtual.remote.IntentSenderData;
import com.lody.virtual.remote.IntentSenderExtData;

/**
 * @author Lody
 */

public class ShadowPendingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setExtrasClassLoader(IntentSenderExtData.class.getClassLoader());
        Intent finalIntent = ComponentUtils.getIntentForIntentSender(intent);
        int userId = ComponentUtils.getUserIdForIntentSender(intent);
        if (finalIntent == null || userId == -1) {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException("targetIntent = null");
            }
            return;
        }
        ComponentUtils.clearVAData(intent);
        if (intent.getExtras() != null) {
            try {
                finalIntent.putExtras(intent.getExtras());
            } catch (Throwable e) {
                //unknown
            }
        }
        IntentSenderExtData ext = intent.getParcelableExtra("_VA_|_ext_");
        if (ext != null && ext.sender != null) {
            IntentSenderData data = VActivityManager.get().getIntentSender(ext.sender);
            Intent fillIn = ext.fillIn;
            if (fillIn != null) {
                finalIntent.fillIn(fillIn, data.flags);
            }
            int flagsMask = ext.flagsMask;
            int flagsValues = ext.flagsValues;
            flagsMask &= ~IntentCompat.IMMUTABLE_FLAGS;
            flagsValues &= flagsMask;
            finalIntent.setFlags((finalIntent.getFlags() & ~flagsMask) | flagsValues);
        }
        int resultCode = getResultCode();
        String resultData = getResultData();
        Bundle result = getResultExtras(true);
        VLog.d("BroadcastSystem", "ShadowPendingReceiver::sendBroadcast:resultCode=%d, resultData=%s, %s", resultCode, resultData, finalIntent);
        Intent redirectIntent = ComponentUtils.redirectBroadcastIntent(finalIntent, userId, BroadcastIntentData.TYPE_FROM_INTENT_SENDER);
        redirectIntent.putExtra("_VA_|_hasResult_", true);
        redirectIntent.putExtra("_VA_|_resultCode_", resultCode);
        redirectIntent.putExtra("_VA_|_resultData_", resultData);
        redirectIntent.putExtra("_VA_|_resultExtras_", result);
        context.sendBroadcast(redirectIntent);
    }
}
