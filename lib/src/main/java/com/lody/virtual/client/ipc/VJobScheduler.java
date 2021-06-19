package com.lody.virtual.client.ipc;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobWorkItem;
import android.os.Build;
import android.os.RemoteException;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.remote.VJobWorkItem;
import com.lody.virtual.server.interfaces.IJobService;

import java.util.List;

/**
 * @author Lody
 */

public class VJobScheduler {

    private static final VJobScheduler sInstance = new VJobScheduler();

    public static VJobScheduler get() {
        return sInstance;
    }

    private IJobService mService;

    public IJobService getService() {
        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IJobService.class, binder);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return IJobService.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.JOB));
    }
    public int schedule(JobInfo job) {
        try {
            return getService().schedule(VClient.get().getVUid(), job);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public List<JobInfo> getAllPendingJobs() {
        try {
            return getService().getAllPendingJobs(VClient.get().getVUid());
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void cancelAll() {
        try {
            getService().cancelAll(VClient.get().getVUid());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancel(int jobId) {
        try {
            getService().cancel(VClient.get().getVUid(), jobId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public JobInfo getPendingJob(int jobId) {
        try {
            return getService().getPendingJob(VClient.get().getVUid(), jobId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public int enqueue(JobInfo job, JobWorkItem workItem) {
        if (workItem == null) return -1;
        if (BuildCompat.isOreo()) {
            try {
                return getService().enqueue(VClient.get().getVUid(), job, new VJobWorkItem(workItem));
            } catch (RemoteException e) {
                return VirtualRuntime.crash(e);
            }
        }
        return -1;
    }
}
