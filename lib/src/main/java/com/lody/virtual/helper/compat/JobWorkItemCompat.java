package com.lody.virtual.helper.compat;

import android.annotation.TargetApi;
import android.app.job.JobWorkItem;
import android.content.Intent;
import android.os.Build;

import com.lody.virtual.helper.utils.ComponentUtils;

@TargetApi(Build.VERSION_CODES.O)
public class JobWorkItemCompat {

    public static JobWorkItem redirect(JobWorkItem item, String pkg) {
        if (item != null) {
            Intent target = mirror.android.app.job.JobWorkItem.getIntent.call(item);
            if (target.hasExtra("_VA_|_intent_")) {
                return item;
            }
            // TODO: is it work?
            Intent intent = ComponentUtils.redirectIntentSender(
                    ActivityManagerCompat.INTENT_SENDER_SERVICE, pkg, target);

            JobWorkItem workItem = (JobWorkItem) mirror.android.app.job.JobWorkItem.ctor.newInstance(intent);
            int wordId = mirror.android.app.job.JobWorkItem.mWorkId.get(item);
            mirror.android.app.job.JobWorkItem.mWorkId.set(workItem, wordId);

            Object obj = mirror.android.app.job.JobWorkItem.mGrants.get(item);
            mirror.android.app.job.JobWorkItem.mGrants.set(workItem, obj);

            int deliveryCount = mirror.android.app.job.JobWorkItem.mDeliveryCount.get(item);
            mirror.android.app.job.JobWorkItem.mDeliveryCount.set(workItem, deliveryCount);
            return workItem;
        }
        return null;
    }
}
