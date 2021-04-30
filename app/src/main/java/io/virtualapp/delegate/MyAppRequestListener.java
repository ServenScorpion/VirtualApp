package io.virtualapp.delegate;

import android.content.Context;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.InstallOptions;

/**
 * @author LodyChen
 */

public class MyAppRequestListener implements VirtualCore.AppRequestListener {

    private final Context context;

    public MyAppRequestListener(Context context) {
        this.context = context;
    }

    @Override
    public void onRequestInstall(String path) {
        info("Start installing: " + path);
        InstallOptions options = InstallOptions.makeOptions(false);
        VirtualCore.get().installPackage(path, options, res -> {
            if (res.isSuccess) {
                info("Install " + res.packageName + " success.");
                boolean success = VActivityManager.get().launchApp(0, res.packageName);
                info("launch app " + (success ? "success." : "fail."));
            } else {
                info("Install " + res.packageName + " fail, reason: " + res.error);
            }
        });
    }

    private static void info(String msg) {
        VLog.e("AppInstaller", msg);
    }

    @Override
    public void onRequestUninstall(String pkg) {
        Toast.makeText(context, "Intercept uninstall request: " + pkg, Toast.LENGTH_SHORT).show();

    }
}
