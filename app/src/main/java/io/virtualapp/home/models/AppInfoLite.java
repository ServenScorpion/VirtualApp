package io.virtualapp.home.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author LodyChen
 */

public class AppInfoLite implements Parcelable {

    public String packageName;
    public String path;
    public String label;
    public boolean notCopyApk;
    public int targetSdkVersion;
    public String[] requestedPermissions;

    public AppInfoLite(AppInfo appInfo) {
        this(appInfo.packageName, appInfo.path, String.valueOf(appInfo.name), appInfo.cloneMode,
                appInfo.targetSdkVersion, appInfo.requestedPermissions);
    }

    public AppInfoLite(String packageName, String path, String label, boolean notCopyApk, int targetSdkVersion, String[] requestedPermissions) {
        this.packageName = packageName;
        this.path = path;
        this.label = label;
        this.notCopyApk = notCopyApk;
        this.targetSdkVersion = targetSdkVersion;
        this.requestedPermissions = requestedPermissions;
    }

    public AppInfoLite(String packageName, String path, String label, boolean notCopyApk, String[] requestedPermissions) {
        this.packageName = packageName;
        this.path = path;
        this.label = label;
        this.notCopyApk = notCopyApk;
        this.requestedPermissions = requestedPermissions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.path);
        dest.writeString(this.label);
        dest.writeByte(this.notCopyApk ? (byte) 1 : (byte) 0);
        dest.writeInt(this.targetSdkVersion);
        dest.writeStringArray(this.requestedPermissions);
    }

    protected AppInfoLite(Parcel in) {
        this.packageName = in.readString();
        this.path = in.readString();
        this.label = in.readString();
        this.notCopyApk = in.readByte() != 0;
        this.targetSdkVersion = in.readInt();
        this.requestedPermissions = in.createStringArray();
    }

    public static final Creator<AppInfoLite> CREATOR = new Creator<AppInfoLite>() {
        @Override
        public AppInfoLite createFromParcel(Parcel source) {
            return new AppInfoLite(source);
        }

        @Override
        public AppInfoLite[] newArray(int size) {
            return new AppInfoLite[size];
        }
    };
}
