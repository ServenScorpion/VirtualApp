package com.lody.virtual;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.InstallOptions;
import com.lody.virtual.remote.InstallResult;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Lody
 */
public class GmsSupport {
    private static final String TAG = GmsSupport.class.getSimpleName();
    private static final HashSet<String> GOOGLE_APP = new HashSet<>();
    private static final HashSet<String> GOOGLE_SERVICE = new HashSet<>();
    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GSF_PKG = "com.google.android.gsf";
    public static final String VENDING_PKG = "com.android.vending";

    static {
        GOOGLE_APP.add(VENDING_PKG);
        GOOGLE_APP.add("com.google.android.play.games");
        GOOGLE_APP.add("com.google.android.wearable.app");
        GOOGLE_APP.add("com.google.android.wearable.app.cn");

        // GMS must install at first
        GOOGLE_SERVICE.add(GMS_PKG);
        GOOGLE_SERVICE.add(GSF_PKG);
        GOOGLE_SERVICE.add("com.google.android.gsf.login");
        GOOGLE_SERVICE.add("com.google.android.backuptransport");
        GOOGLE_SERVICE.add("com.google.android.backup");
        GOOGLE_SERVICE.add("com.google.android.configupdater");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.contacts");
        GOOGLE_SERVICE.add("com.google.android.feedback");
        GOOGLE_SERVICE.add("com.google.android.onetimeinitializer");
        GOOGLE_SERVICE.add("com.google.android.partnersetup");
        GOOGLE_SERVICE.add("com.google.android.setupwizard");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.calendar");
    }

    public static boolean isGoogleFrameworkInstalled() {
        return VirtualCore.get().isAppInstalled(GMS_PKG);
    }

    public static boolean isGoogleService(String packageName) {
        return GOOGLE_SERVICE.contains(packageName);
    }

    public static boolean isGoogleAppOrService(String str) {
        return GOOGLE_APP.contains(str) || GOOGLE_SERVICE.contains(str);
    }

    public static boolean isOutsideGoogleFrameworkExist() {
        return VirtualCore.get().isOutsideInstalled(GMS_PKG);
    }

    private static void installPackages(Set<String> list, int userId) {
        VirtualCore core = VirtualCore.get();
        for (String packageName : list) {
            if (core.isAppInstalledAsUser(userId, packageName)) {
                continue;
            }
            ApplicationInfo info;
            try {
                info = VirtualCore.get().getUnHookPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // Ignore
                continue;
            }
            if (userId == 0) {
                InstallOptions options = InstallOptions.makeOptions(true, false, InstallOptions.UpdateStrategy.FORCE_UPDATE);
                InstallResult result = core.installPackageSync(info.sourceDir, options);
                if (result.isSuccess) {
                    VLog.w(TAG, "install gms pkg success:" + info.packageName);
                } else {
                    VLog.w(TAG, "install gms pkg fail:" + info.packageName + ",error : " + result.error);
                }
            } else {
                core.installPackageAsUser(userId, packageName);
            }
        }
    }

    public static void installGApps(int userId) {
        installPackages(GOOGLE_SERVICE, userId);
        installPackages(GOOGLE_APP, userId);
    }

    public static void remove(String packageName) {
        GOOGLE_SERVICE.remove(packageName);
        GOOGLE_APP.remove(packageName);
    }

    public static boolean isInstalledGoogleService() {
        return VirtualCore.get().isAppInstalled(GMS_PKG);
    }

}