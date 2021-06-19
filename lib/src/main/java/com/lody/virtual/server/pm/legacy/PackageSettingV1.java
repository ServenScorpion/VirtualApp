package com.lody.virtual.server.pm.legacy;

import android.os.Parcel;
import android.util.SparseArray;

import com.lody.virtual.server.pm.PackageUserState;


/**
 * @author Lody
 */
public class PackageSettingV1 {
    public String packageName;
    public boolean notCopyApk;
    public int appId;
    public SparseArray<PackageUserState> userState;
    public int flag;

    public void readFromParcel(Parcel in, int version) {
        this.packageName = in.readString();
        in.readString(); // Historical legacy
        in.readString(); // Historical legacy
        this.notCopyApk = in.readByte() != 0;
        this.appId = in.readInt();
        //noinspection unchecked
        this.userState = in.readSparseArray(PackageUserState.class.getClassLoader());
        in.readByte(); // Historical legacy
        if (version > 3) {
            this.flag = in.readInt();
        }
    }
}
