package io.virtualapp.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class PackageUtils {
    public static PackageInfo getApkPackageInfo(PackageManager pm, String path, int flags) {
        try {
            return pm.getPackageArchiveInfo(path, flags);
        } catch (Throwable e) {
            return null;
        }
    }

    public static int getApkVersion(Context cxt, String path) {
        PackageInfo packageInfo = getApkPackageInfo(cxt.getPackageManager(), path, 0);
        if (packageInfo == null) {
            return -1;
        }
        return packageInfo.versionCode;
    }

    public static int getApkVersion(PackageManager pm, String path) {
        PackageInfo packageInfo = getApkPackageInfo(pm, path, 0);
        if (packageInfo == null) {
            return -1;
        }
        return packageInfo.versionCode;
    }

}
