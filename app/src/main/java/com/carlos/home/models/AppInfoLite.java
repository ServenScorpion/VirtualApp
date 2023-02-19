package com.carlos.home.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

/**
 * @author LodyChen
 */

public class AppInfoLite implements Parcelable {
    public static final Parcelable.Creator<AppInfoLite> CREATOR = new Parcelable.Creator<AppInfoLite>() {
        public AppInfoLite createFromParcel(Parcel source) {
            return new AppInfoLite(source);
        }

        public AppInfoLite[] newArray(int size) {
            return new AppInfoLite[size];
        }
    };
    public boolean dynamic;
    public String label;
    public String packageName;
    public String path;
    public String[] requestedPermissions;
    public int targetSdkVersion;
    public int multiNumber;

    public AppInfoLite(AppInfo appInfo,int itemMultiNumber) {
        this(appInfo.packageName, appInfo.path, String.valueOf(appInfo.name), appInfo.cloneMode, appInfo.targetSdkVersion, appInfo.requestedPermissions,itemMultiNumber);
    }

    public AppInfoLite(String packageName2, String path2, String label2, boolean dynamic2, int targetSdkVersion2, String[] requestedPermissions2,int itemMultiNumber) {
        this.packageName = packageName2;
        this.path = path2;
        this.label = label2;
        this.dynamic = dynamic2;
        this.targetSdkVersion = targetSdkVersion2;
        this.requestedPermissions = requestedPermissions2;
        this.multiNumber = itemMultiNumber;
    }

    public AppInfoLite(String packageName2, String path2, String label2, boolean dynamic2, String[] requestedPermissions2) {
        this.packageName = packageName2;
        this.path = path2;
        this.label = label2;
        this.dynamic = dynamic2;
        this.requestedPermissions = requestedPermissions2;
    }

    public Uri getUri() {
        if (!this.dynamic) {
            return Uri.fromFile(new File(this.path));
        }
        return Uri.parse("package:" + this.packageName);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.path);
        dest.writeString(this.label);
        dest.writeByte(this.dynamic ? (byte) 1 : 0);
        dest.writeInt(this.targetSdkVersion);
        dest.writeStringArray(this.requestedPermissions);
        dest.writeInt(this.multiNumber);
    }

    protected AppInfoLite(Parcel in) {
        this.packageName = in.readString();
        this.path = in.readString();
        this.label = in.readString();
        this.dynamic = in.readByte() != 0;
        this.targetSdkVersion = in.readInt();
        this.requestedPermissions = in.createStringArray();
        this.multiNumber = in.readInt();
    }
}