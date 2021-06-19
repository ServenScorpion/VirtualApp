package com.lody.virtual.client.hook.proxies.job;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobWorkItem;
import android.content.Context;
import android.os.Build;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.ipc.VJobScheduler;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.JobWorkItemCompat;

import java.lang.reflect.Method;
import java.util.List;

import mirror.android.app.job.IJobScheduler;
import mirror.android.content.pm.ParceledListSlice;

/**
 * @author Lody
 * @see android.app.job.JobScheduler
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class JobServiceStub extends BinderInvocationProxy {

    public JobServiceStub() {
        super(IJobScheduler.Stub.asInterface, Context.JOB_SCHEDULER_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new schedule());
        addMethodProxy(new getAllPendingJobs());
        addMethodProxy(new cancelAll());
        addMethodProxy(new cancel());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addMethodProxy(new getPendingJob());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addMethodProxy(new enqueue());
        }
    }


    private class schedule extends MethodProxy {

        @Override
        public String getMethodName() {
            return "schedule";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            JobInfo jobInfo = (JobInfo) args[0];
            return VJobScheduler.get().schedule(jobInfo);
        }
    }

    private class getAllPendingJobs extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getAllPendingJobs";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            List res = VJobScheduler.get().getAllPendingJobs();
            if (res == null)
                return null;
            if (BuildCompat.isQ()) {
                return ParceledListSlice.ctorQ.newInstance(res);
            } else {
                return res;
            }
        }
    }

    private class cancelAll extends MethodProxy {

        @Override
        public String getMethodName() {
            return "cancelAll";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            VJobScheduler.get().cancelAll();
            return 0;
        }
    }

    private class cancel extends MethodProxy {

        @Override
        public String getMethodName() {
            return "cancel";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int jobId = (int) args[0];
            VJobScheduler.get().cancel(jobId);
            return 0;
        }
    }

    private class getPendingJob extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPendingJob";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int jobId = (int) args[0];
            return VJobScheduler.get().getPendingJob(jobId);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private class enqueue extends MethodProxy {

        @Override
        public String getMethodName() {
            return "enqueue";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            JobInfo jobInfo = (JobInfo) args[0];
            JobWorkItem workItem = JobWorkItemCompat.redirect((JobWorkItem)args[1], getAppPkg());
            return VJobScheduler.get().enqueue(jobInfo, workItem);
        }
    }
}
