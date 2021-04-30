package io.virtualapp.home;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.lody.virtual.helper.compat.PermissionCompat;

import io.virtualapp.R;

/**
 * @author LodyChen
 */
@TargetApi(Build.VERSION_CODES.M)
public class PermissionRequestActivity extends Activity {

    private static final int REQUEST_PERMISSION_CODE = 995;
    private static final String EXTRA_PERMISSIONS = "extra.permission";
    private static final String EXTRA_APP_NAME = "extra.app_name";
    private static final String EXTRA_USER_ID = "extra.user_id";
    private static final String EXTRA_PACKAGE_NAME = "extra.package_name";
    private int userId;
    private String appName;
    private String packageName;

    public static void requestPermission(@NonNull Activity activity, @NonNull String[] permissions, @NonNull String appName, int userId, @NonNull String packageName, int requestCode) {
        Intent intent = new Intent(activity, PermissionRequestActivity.class);
        intent.putExtra(EXTRA_PERMISSIONS, permissions);
        intent.putExtra(EXTRA_APP_NAME, appName);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(EXTRA_USER_ID, userId);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(0, 0);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);
        appName = intent.getStringExtra(EXTRA_APP_NAME);
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        userId = intent.getIntExtra(EXTRA_USER_ID, -1);
        requestPermissions(permissions, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionCompat.isRequestGranted(grantResults)) {
            Intent data = new Intent();
            data.putExtra("pkg", packageName);
            data.putExtra("user_id", userId);
            setResult(RESULT_OK, data);
        } else {
            runOnUiThread(() -> Toast.makeText(this, getString(R.string.start_app_failed, appName),
                    Toast.LENGTH_SHORT).show());
        }
        finish();
    }

}
