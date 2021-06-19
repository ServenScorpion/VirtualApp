package com.lody.virtual.server.interfaces;

import android.app.job.JobInfo;
import com.lody.virtual.remote.VJobWorkItem;
import android.os.Parcelable;
import android.os.RemoteException;

import java.util.List;

/**
 * @author Lody
 */
interface IJobService{

    int schedule(int uid,in  JobInfo job);

    void cancel(int uid, int jobId);

    void cancelAll(int uid);

    List<JobInfo> getAllPendingJobs(int uid);

    JobInfo getPendingJob(int uid, int jobId);

    int enqueue(int uid,in JobInfo job, in VJobWorkItem workItem);
}
