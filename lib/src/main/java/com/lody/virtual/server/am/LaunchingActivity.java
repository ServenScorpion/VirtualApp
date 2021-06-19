package com.lody.virtual.server.am;

import android.content.ComponentName;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LaunchingActivity {

    public ComponentName componentName;
    public List<PendingNewIntent> pendingNewIntents = new CopyOnWriteArrayList<>();

    public LaunchingActivity(ComponentName componentName) {
        this.componentName = componentName;
    }

    public boolean Match(ComponentName componentName) {
        if (componentName == null || this.componentName == null)
            return false;
        return this.componentName.equals(componentName);
    }

}