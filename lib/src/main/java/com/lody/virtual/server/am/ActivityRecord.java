package com.lody.virtual.server.am;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Binder;
import android.os.IBinder;

/**
 * @author Lody
 *
 */

/* package */ class ActivityRecord extends Binder {
    public TaskRecord task;
    public ActivityInfo info;
    public ComponentName component;
    public Intent intent;
    public IBinder token;
    public IBinder resultTo;
    public int userId;
    public ProcessRecord process;
    public boolean marked;


    public ActivityRecord(Intent intent, ActivityInfo info, IBinder resultTo, int userId) {
        this.intent = intent;
        this.info = info;
        if (info.targetActivity != null) {
            this.component = new ComponentName(info.packageName, info.targetActivity);
        } else {
            this.component = new ComponentName(info.packageName, info.name);
        }
        this.resultTo = resultTo;
        this.userId = userId;
    }

    public void init(TaskRecord task, ProcessRecord process, IBinder token) {
        this.task = task;
        this.process = process;
        this.token = token;
    }

    public boolean isLaunching() {
        return process == null;
    }
}
