package com.lody.virtual.client.stub;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.compat.IntentCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.remote.IntentSenderData;
import com.lody.virtual.remote.IntentSenderExtData;

/**
 * @author Lody
 */

public class ShadowPendingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        Intent intent = getIntent();
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
        if ("android.nfc.action.NDEF_DISCOVERED".equals(intent.getAction())
                || "android.nfc.action.TAG_DISCOVERED".equals(intent.getAction())
                | "android.nfc.action.TECH_DISCOVERED".equals(intent.getAction())) {
            if (intent.getData() != null) {
                finalIntent.setDataAndType(intent.getData(), intent.getType());
            }
            if (intent.getCategories() != null) {
                for (String g : intent.getCategories()) {
                    finalIntent.addCategory(g);
                }
            }
            if (intent.getAction() != null) {
                finalIntent.setAction(intent.getAction());
            }
        }
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
            ActivityInfo info = VirtualCore.get().resolveActivityInfo(intent, data.userId);
            int res = VActivityManager.get().startActivity(finalIntent, info, ext.resultTo, ext.options, ext.resultWho, ext.requestCode, data.userId);
            if (res != 0 && ext.resultTo != null && ext.requestCode > 0) {
                VActivityManager.get().sendCancelActivityResult(ext.resultTo, ext.resultWho, ext.requestCode);
            }
        } else {
            VActivityManager.get().startActivity(finalIntent, userId);
        }
    }
}
