package com.lody.virtual.remote;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingRuleInfo implements Parcelable {
    public int rule;
    public String word;
    public boolean regex;
    private transient Pattern pattern;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettingRuleInfo that = (SettingRuleInfo) o;
        return rule == that.rule &&
                regex == that.regex &&
                TextUtils.equals(word, that.word);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{rule, word, regex});
    }

    public boolean matches(String packageName) {
        if(!regex){
            return TextUtils.equals(packageName, word);
        }
        try {
            if (pattern == null) {
                pattern = Pattern.compile(word);
            }
            Matcher m = pattern.matcher(packageName);
            return m.matches();
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.rule);
        dest.writeString(this.word);
        dest.writeByte(this.regex ? (byte) 1 : (byte) 0);
    }

    public SettingRuleInfo() {
    }

    public SettingRuleInfo(int rule, String word, boolean regex) {
        this.rule = rule;
        this.word = word;
        this.regex = regex;
    }

    protected SettingRuleInfo(Parcel in) {
        this.rule = in.readInt();
        this.word = in.readString();
        this.regex = in.readByte() != 0;
    }

    public static final Parcelable.Creator<SettingRuleInfo> CREATOR = new Parcelable.Creator<SettingRuleInfo>() {
        @Override
        public SettingRuleInfo createFromParcel(Parcel source) {
            return new SettingRuleInfo(source);
        }

        @Override
        public SettingRuleInfo[] newArray(int size) {
            return new SettingRuleInfo[size];
        }
    };
}
