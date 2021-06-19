package com.lody.virtual.remote;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Lody
 */
public class IntentSenderExtData implements Parcelable {

    public static final IntentSenderExtData EMPTY = new IntentSenderExtData(null, null, null, null, 0, null, 0, 0);

    public IBinder sender;
    public Intent fillIn;
    public IBinder resultTo;
    public String resultWho;
    public int requestCode;
    public Bundle options;
    public int flagsMask;
    public int flagsValues;

    public IntentSenderExtData(IBinder sender, Intent fillIn, IBinder resultTo, String resultWho, int requestCode, Bundle options, int flagsMask, int flagsValues) {
        this.sender = sender;
        this.fillIn = fillIn;
        this.resultTo = resultTo;
        this.resultWho = resultWho;
        this.requestCode = requestCode;
        this.options = options;
        this.flagsMask = flagsMask;
        this.flagsValues = flagsValues;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(sender);
        dest.writeParcelable(this.fillIn, flags);
        dest.writeStrongBinder(resultTo);
        dest.writeString(this.resultWho);
        dest.writeInt(this.requestCode);
        dest.writeBundle(this.options);
        dest.writeInt(this.flagsMask);
        dest.writeInt(this.flagsValues);
    }

    protected IntentSenderExtData(Parcel in) {
        this.sender = in.readStrongBinder();
        this.fillIn = in.readParcelable(Intent.class.getClassLoader());
        this.resultTo = in.readStrongBinder();
        this.resultWho = in.readString();
        this.requestCode = in.readInt();
        this.options = in.readBundle();
        this.flagsMask = in.readInt();
        this.flagsValues = in.readInt();
    }

    public static final Parcelable.Creator<IntentSenderExtData> CREATOR = new Parcelable.Creator<IntentSenderExtData>() {
        @Override
        public IntentSenderExtData createFromParcel(Parcel source) {
            return new IntentSenderExtData(source);
        }

        @Override
        public IntentSenderExtData[] newArray(int size) {
            return new IntentSenderExtData[size];
        }
    };


}
