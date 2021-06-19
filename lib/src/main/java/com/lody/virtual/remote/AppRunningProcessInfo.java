package com.lody.virtual.remote;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.lody.virtual.client.core.VirtualCore;

import java.util.ArrayList;
import java.util.List;

public class AppRunningProcessInfo implements Parcelable {
    public int pid;
    public int vuid;
    public int vpid;
    public String packageName;
    public String processName;
    public final List<String> pkgList = new ArrayList<>();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.pid);
        dest.writeInt(this.vuid);
        dest.writeString(this.packageName);
        dest.writeString(this.processName);
        dest.writeStringList(this.pkgList);
    }

    public AppRunningProcessInfo() {
    }

    @SuppressLint("DefaultLocale")
    public String getRealProcessName(){
        return String.format("%s:p%d", VirtualCore.get().getHostPkg(), vpid);
    }

    protected AppRunningProcessInfo(Parcel in) {
        this.pid = in.readInt();
        this.vuid = in.readInt();
        this.packageName = in.readString();
        this.processName = in.readString();
        List<String> list = in.createStringArrayList();
        if(list != null) {
            this.pkgList.addAll(list);
        }
    }

    public static final Parcelable.Creator<AppRunningProcessInfo> CREATOR = new Parcelable.Creator<AppRunningProcessInfo>() {
        @Override
        public AppRunningProcessInfo createFromParcel(Parcel source) {
            return new AppRunningProcessInfo(source);
        }

        @Override
        public AppRunningProcessInfo[] newArray(int size) {
            return new AppRunningProcessInfo[size];
        }
    };

    @Override
    public String toString() {
        return "AppRunningProcess{" +
                "pid=" + pid +
                ", vuid=" + vuid +
                ", vpid=" + vpid +
                ", packageName='" + packageName + '\'' +
                ", processName='" + processName + '\'' +
                ", pkgList=" + pkgList +
                '}';
    }
}
