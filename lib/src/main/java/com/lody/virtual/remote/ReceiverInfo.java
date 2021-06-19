package com.lody.virtual.remote;

import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * @author Lody
 */

public class ReceiverInfo implements Parcelable {
    public ActivityInfo info;
    public List<IntentFilter> filters;


    public ReceiverInfo(ActivityInfo receiverInfo, List<IntentFilter> filters) {
        this.info = receiverInfo;
        this.filters = filters;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.info, flags);
        dest.writeTypedList(this.filters);
    }

    protected ReceiverInfo(Parcel in) {
        this.info = in.readParcelable(ActivityInfo.class.getClassLoader());
        this.filters = in.createTypedArrayList(IntentFilter.CREATOR);
    }

    public static final Creator<ReceiverInfo> CREATOR = new Creator<ReceiverInfo>() {
        @Override
        public ReceiverInfo createFromParcel(Parcel source) {
            return new ReceiverInfo(source);
        }

        @Override
        public ReceiverInfo[] newArray(int size) {
            return new ReceiverInfo[size];
        }
    };
}
