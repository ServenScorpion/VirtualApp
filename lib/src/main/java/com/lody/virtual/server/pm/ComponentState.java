package com.lody.virtual.server.pm;

import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Lody
 */
public class ComponentState implements Parcelable {

    public int state = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.state);
    }

    public ComponentState() {
    }

    protected ComponentState(Parcel in) {
        this.state = in.readInt();
    }

    public static final Creator<ComponentState> CREATOR = new Creator<ComponentState>() {
        @Override
        public ComponentState createFromParcel(Parcel source) {
            return new ComponentState(source);
        }

        @Override
        public ComponentState[] newArray(int size) {
            return new ComponentState[size];
        }
    };
}
