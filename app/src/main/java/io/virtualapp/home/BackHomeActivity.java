package io.virtualapp.home;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;


/**
 * @author LodyChen
 */
public class BackHomeActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean existTask = false;
        ComponentName homeActivity = new ComponentName(this, HomeActivity.class);
        for (ActivityManager.RunningTaskInfo info : am.getRunningTasks(Integer.MAX_VALUE)) {
            if (info.baseActivity != null && info.baseActivity.equals(homeActivity)) {
                am.moveTaskToFront(info.id, 0);
                existTask = true;
                break;
            }
            if (info.topActivity != null && info.topActivity.equals(homeActivity)) {
                am.moveTaskToFront(info.id, 0);
                existTask = true;
                break;
            }
        }
        if (!existTask) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
        finish();
    }
}
