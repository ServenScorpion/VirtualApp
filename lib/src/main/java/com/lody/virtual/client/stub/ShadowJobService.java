package com.lody.virtual.client.stub;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.job.IJobService;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.VLog;
import com.xdja.zs.BoxProvider;

/**
 * @author Lody
 * <p>
 * This service running on the Server process.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ShadowJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParams) {
        if (BoxProvider.isCurrentSpace()) {
            VLog.d("kk-test", "startJob:"+jobParams.getJobId());
            ShadowJobWorkService.startJob(ShadowJobService.this, jobParams);
        } else {
            VLog.d("kk-test", "cancelJob:"+jobParams.getJobId());
            ShadowJobWorkService.cancelJob(ShadowJobService.this, jobParams);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParams) {
        VLog.d("kk-test", "onStopJob:"+jobParams.getJobId());
        ShadowJobWorkService.stopJob(ShadowJobService.this, jobParams);
        return true;
    }
}
