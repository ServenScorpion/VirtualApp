package com.lody.virtual.remote;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.os.VEnvironment;

import java.io.File;

/**
 * @author Lody
 */
public final class InstalledAppInfo implements Parcelable {

    public static final int MODE_APP_COPY_APK = 0;
    public static final int MODE_APP_USE_OUTSIDE_APK = 1;

    public String packageName;
    public int appMode;
    public int flag;
    public int appId;

    public InstalledAppInfo(String packageName, int appMode, int flags,int appId) {
        this.packageName = packageName;
        this.appMode = appMode;
        this.flag = flags;
        this.appId = appId;
    }

    public String getApkPath() {
        return getApkPath(VirtualCore.get().isPluginEngine());
    }

    public String getApkPath(boolean is64bit) {
        if (appMode == MODE_APP_USE_OUTSIDE_APK) {
            try {
                ApplicationInfo info = VirtualCore.get().getUnHookPackageManager().getApplicationInfo(packageName, 0);
                return info.publicSourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        if (is64bit) {
            return VEnvironment.getPackageResourcePath64(packageName).getPath();
        } else {
            return VEnvironment.getPackageResourcePath(packageName).getPath();
        }
    }

    public String getOdexPath() {
        return getOdexFile().getPath();
    }

    public String getOdexPath(boolean is64Bit) {
        return getOdexFile(is64Bit).getPath();
    }

    public File getOdexFile() {
        return getOdexFile(VirtualCore.get().isPluginEngine());
    }

    public File getOdexFile(boolean is64Bit) {
        if (is64Bit) {
            return VEnvironment.getOdexFile64(packageName);
        }
        return VEnvironment.getOdexFile(packageName);
    }

    public ApplicationInfo getApplicationInfo(int userId) {
        return VPackageManager.get().getApplicationInfo(packageName, 0, userId);
    }

    public boolean isRunOn64BitProcess() {
        return false;
        /*switch (flag) {
            case FLAG_RUN_32BIT:
                return false;
            case FLAG_RUN_64BIT:
                return true;
            case FLAG_RUN_BOTH_32BIT_64BIT:
                return VirtualCore.getConfig().getPluginEnginePackageName() != null;
            default:
                return false;
        }*/
    }

    public PackageInfo getPackageInfo(int userId) {
        return VPackageManager.get().getPackageInfo(packageName, 0, userId);
    }

    public int[] getInstalledUsers() {
        return VirtualCore.get().getPackageInstalledUsers(packageName);
    }

    public boolean isLaunched(int userId) {
        return VirtualCore.get().isPackageLaunched(userId, packageName);
    }

    public boolean isRunning64Process(){
        //TODO flag
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeInt(this.appMode);
        dest.writeInt(this.flag);
        dest.writeInt(this.appId);
    }

    protected InstalledAppInfo(Parcel in) {
        this.packageName = in.readString();
        this.appMode = in.readInt();
        this.flag = in.readInt();
        this.appId = in.readInt();
    }

    public static final Creator<InstalledAppInfo> CREATOR = new Creator<InstalledAppInfo>() {
        @Override
        public InstalledAppInfo createFromParcel(Parcel source) {
            return new InstalledAppInfo(source);
        }

        @Override
        public InstalledAppInfo[] newArray(int size) {
            return new InstalledAppInfo[size];
        }
    };
}
