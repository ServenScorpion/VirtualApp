package com.lody.virtual.server.am;

import android.content.Intent;

public class PendingNewIntent {
    public int userId;
    public ActivityRecord sourceRecord;
    public Intent intent;

    public PendingNewIntent(int userId, ActivityRecord sourceRecord, Intent intent) {
        this.userId = userId;
        this.sourceRecord = sourceRecord;
        this.intent = intent;
    }

}