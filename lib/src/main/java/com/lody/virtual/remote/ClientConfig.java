package com.lody.virtual.remote;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Lody
 */
public class ClientConfig implements Parcelable {
    public boolean is64Bit;
    public int vpid;
    public int vuid;
    public String processName;
    public String packageName;
    public IBinder token;

    public ClientConfig() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(is64Bit ? (byte) 1 : (byte) 0);
        dest.writeInt(vpid);
        dest.writeInt(vuid);
        dest.writeString(processName);
        dest.writeString(packageName);
        dest.writeStrongBinder(token);
    }

    protected ClientConfig(Parcel in) {
        is64Bit = in.readByte() != 0;
        vpid = in.readInt();
        vuid = in.readInt();
        processName = in.readString();
        packageName = in.readString();
        token = in.readStrongBinder();
    }

    public static final Creator<ClientConfig> CREATOR = new Creator<ClientConfig>() {
        @Override
        public ClientConfig createFromParcel(Parcel source) {
            return new ClientConfig(source);
        }

        @Override
        public ClientConfig[] newArray(int size) {
            return new ClientConfig[size];
        }
    };
}
