package com.lody.virtual.remote;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class IntentSenderData implements Parcelable {

    /**
     * packageName
     */
    public String creator;
    public IBinder token;
    public Intent intent;
    public int flags;
    public int type;
    public int userId;

    public IntentSenderData(String creator, IBinder token, Intent intent, int flags, int type, int userId) {
        this.creator = creator;
        this.token = token;
        this.intent = intent;
        this.flags = flags;
        this.type = type;
        this.userId = userId;
    }

    public PendingIntent getPendingIntent() {
        return readPendingIntent(token);
    }


    public static PendingIntent readPendingIntent(IBinder binder) {
        Parcel parcel = Parcel.obtain();
        parcel.writeStrongBinder(binder);
        parcel.setDataPosition(0);
        try {
            return PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.creator);
        dest.writeStrongBinder(token);
        dest.writeParcelable(this.intent, flags);
        dest.writeInt(this.flags);
        dest.writeInt(this.type);
        dest.writeInt(this.userId);
    }

    protected IntentSenderData(Parcel in) {
        this.creator = in.readString();
        this.token = in.readStrongBinder();
        this.intent = in.readParcelable(Intent.class.getClassLoader());
        this.flags = in.readInt();
        this.type = in.readInt();
        this.userId = in.readInt();
    }

    public static final Creator<IntentSenderData> CREATOR = new Creator<IntentSenderData>() {
        @Override
        public IntentSenderData createFromParcel(Parcel source) {
            return new IntentSenderData(source);
        }

        @Override
        public IntentSenderData[] newArray(int size) {
            return new IntentSenderData[size];
        }
    };

    public void replace(IntentSenderData other) {
        this.creator = other.creator;
        this.token = other.token;
        this.intent = other.intent;
        this.flags = other.flags;
        this.type = other.type;
        this.userId = other.userId;
    }
}