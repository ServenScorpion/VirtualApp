package io.virtualapp.home.models;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.lody.virtual.remote.InstalledAppInfo;

/**
 * @see android.location.Location
 */
public class SettingsData {
    public String packageName;
    public int userId;
    public String name;
    public Drawable icon;

    public SettingsData() {
    }

    public SettingsData(Context context, InstalledAppInfo installedAppInfo, int userId) {
        this.packageName = installedAppInfo == null ? null : installedAppInfo.packageName;
        this.userId = userId;
        if (installedAppInfo != null) {
            loadData(context, installedAppInfo.getApplicationInfo(installedAppInfo.getInstalledUsers()[0]));
        }
    }

    private void loadData(Context context, ApplicationInfo appInfo) {
        if (appInfo == null) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        try {
            CharSequence sequence = appInfo.loadLabel(pm);
            name = sequence.toString();
            icon = appInfo.loadIcon(pm);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
