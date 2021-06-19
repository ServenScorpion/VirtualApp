package com.lody.virtual.remote;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceResult implements Parcelable {
    public boolean died;
    public int startId;
    public int clientCount;
    public boolean restart;

    public ServiceResult() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.died ? (byte) 1 : (byte) 0);
        dest.writeInt(this.startId);
        dest.writeInt(this.clientCount);
        dest.writeByte(this.restart ? (byte) 1 : (byte) 0);
    }

    protected ServiceResult(Parcel in) {
        this.died = in.readByte() != 0;
        this.startId = in.readInt();
        this.clientCount = in.readInt();
        this.restart = in.readByte() != 0;
    }

    public static final Creator<ServiceResult> CREATOR = new Creator<ServiceResult>() {
        @Override
        public ServiceResult createFromParcel(Parcel source) {
            return new ServiceResult(source);
        }

        @Override
        public ServiceResult[] newArray(int size) {
            return new ServiceResult[size];
        }
    };
}
