package com.lody.virtual.remote;

import android.annotation.TargetApi;
import android.app.job.JobWorkItem;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

@TargetApi(Build.VERSION_CODES.O)
public class VJobWorkItem implements Parcelable {
    private JobWorkItem item;

    public JobWorkItem get() {
        return item;
    }

    public void set(JobWorkItem item) {
        this.item = item;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.item, flags);
    }

    public VJobWorkItem() {
    }

    public VJobWorkItem(JobWorkItem item) {
        this.item = item;
    }

    protected VJobWorkItem(Parcel in) {
        this.item = in.readParcelable(JobWorkItem.class.getClassLoader());
    }

    public static final Parcelable.Creator<VJobWorkItem> CREATOR = new Parcelable.Creator<VJobWorkItem>() {
        @Override
        public VJobWorkItem createFromParcel(Parcel source) {
            return new VJobWorkItem(source);
        }

        @Override
        public VJobWorkItem[] newArray(int size) {
            return new VJobWorkItem[size];
        }
    };
}
