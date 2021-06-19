package com.lody.virtual.remote;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Lody
 */
public class BroadcastIntentData implements Parcelable {
    /** va内部，根据config允许唤醒app */
    public static final int TYPE_APP = 0;
    /** va的服务进程/主进程，看x进程的判断和config允许唤醒app */
    public static final int TYPE_FROM_SYSTEM = 1;
    /** 来自intent sender，允许唤醒app */
    public static final int TYPE_FROM_INTENT_SENDER = 2;

    public int userId;
    public Intent intent;
    public String targetPackage;
    public int type;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.userId);
        dest.writeParcelable(this.intent, flags);
        dest.writeString(this.targetPackage);
        dest.writeByte((byte) type);
    }

    public BroadcastIntentData(int userId, Intent intent, String targetPackage, int type) {
        this.userId = userId;
        this.intent = intent;
        this.targetPackage = targetPackage;
        this.type = type;
    }

    public BroadcastIntentData(Parcel in) {
        this.userId = in.readInt();
        this.intent = in.readParcelable(Intent.class.getClassLoader());
        this.targetPackage = in.readString();
        this.type = in.readByte();
    }

    public static final Parcelable.Creator<BroadcastIntentData> CREATOR = new Parcelable.Creator<BroadcastIntentData>() {
        @Override
        public BroadcastIntentData createFromParcel(Parcel source) {
            return new BroadcastIntentData(source);
        }

        @Override
        public BroadcastIntentData[] newArray(int size) {
            return new BroadcastIntentData[size];
        }
    };

    public boolean isFromSystem(){
        return (type & TYPE_FROM_SYSTEM) == TYPE_FROM_SYSTEM;
    }


    public boolean isFromIntentSender(){
        return (type & TYPE_FROM_INTENT_SENDER) == TYPE_FROM_INTENT_SENDER;
    }

    @Override
    public String toString() {
        return "BroadcastIntentData{" +
                "userId=" + userId +
                ", intent=" + intent +
                ", targetPackage='" + targetPackage + '\'' +
                ", type=" + type +
                '}';
    }
}
