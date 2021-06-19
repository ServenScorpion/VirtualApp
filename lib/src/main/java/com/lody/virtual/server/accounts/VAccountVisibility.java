package com.lody.virtual.server.accounts;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class VAccountVisibility implements Parcelable {
    public String name;
    public String type;
    public int userId;
    public Map<String, Integer> visibility;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.type);
        dest.writeInt(this.userId);
        dest.writeInt(this.visibility.size());
        for (Map.Entry<String, Integer> entry : this.visibility.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeValue(entry.getValue());
        }
    }

    public VAccountVisibility() {
    }

    public VAccountVisibility(int i, Account account, Map<String, Integer> map) {
        this.userId = i;
        this.name = account.name;
        this.type = account.type;
        this.visibility = new HashMap<>();
        if (map != null) {
            this.visibility.putAll(map);
        }
    }

    protected VAccountVisibility(Parcel in) {
        this.name = in.readString();
        this.type = in.readString();
        this.userId = in.readInt();
        int visibilitySize = in.readInt();
        this.visibility = new HashMap<String, Integer>(visibilitySize);
        for (int i = 0; i < visibilitySize; i++) {
            String key = in.readString();
            Integer value = (Integer) in.readValue(Integer.class.getClassLoader());
            this.visibility.put(key, value);
        }
    }

    public static final Parcelable.Creator<VAccountVisibility> CREATOR = new Parcelable.Creator<VAccountVisibility>() {
        @Override
        public VAccountVisibility createFromParcel(Parcel source) {
            return new VAccountVisibility(source);
        }

        @Override
        public VAccountVisibility[] newArray(int size) {
            return new VAccountVisibility[size];
        }
    };
}
