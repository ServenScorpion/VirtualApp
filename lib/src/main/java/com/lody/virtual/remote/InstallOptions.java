package com.lody.virtual.remote;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Lody
 */
public class InstallOptions implements Parcelable {

    /**
     * 通俗的解释：
     * [True]: 以双开的形式安装系统已有的app，版本也随系统的变化而变化
     * [False]: 将apk拷贝到本地私有目录，达到与外部隔离、独立运行的目的
     */
    public boolean useSourceLocationApk = false;
    public boolean notify = true;
    public UpdateStrategy updateStrategy = UpdateStrategy.COMPARE_VERSION;

    public enum UpdateStrategy {
        /**
         * 如果app已经存在，不管版本是否变化，直接中断安装
         */
        TERMINATE_IF_EXIST,
        /**
         * 如果app已经存在，不管版本是否变化，直接覆盖安装
         */
        FORCE_UPDATE,
        /**
         * 如果app已经存在，比较新的安装包和旧的安装包的versionCode，
         * 仅当 新的 > 旧的 时才覆盖安装
         */
        COMPARE_VERSION,
        /**
         * 如果app已经存在，不管版本是否变化，不再继续安装
         * 与 {@link UpdateStrategy#TERMINATE_IF_EXIST}
         * 的区别是本策略的最终返回结果将是安装成功
         */
        IGNORE_NEW_VERSION
    }

    public InstallOptions() {

    }

    public InstallOptions(boolean useSourceLocationApk, boolean notify, UpdateStrategy updateStrategy) {
        this.useSourceLocationApk = useSourceLocationApk;
        this.notify = notify;
        this.updateStrategy = updateStrategy;
    }

    public static InstallOptions makeOptions(boolean useSourceLocationFile, boolean notify, UpdateStrategy updateStrategy) {
        return new InstallOptions(useSourceLocationFile, notify, updateStrategy);
    }

    public static InstallOptions makeOptions(boolean useSourceLocationFile, UpdateStrategy updateStrategy) {
        return new InstallOptions(useSourceLocationFile, true, updateStrategy);
    }

    public static InstallOptions makeOptions(boolean useSourceLocationFile) {
        return new InstallOptions(useSourceLocationFile, true, UpdateStrategy.COMPARE_VERSION);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.useSourceLocationApk ? (byte) 1 : (byte) 0);
        dest.writeByte(this.notify ? (byte) 1 : (byte) 0);
        dest.writeInt(this.updateStrategy == null ? -1 : this.updateStrategy.ordinal());
    }

    protected InstallOptions(Parcel in) {
        this.useSourceLocationApk = in.readByte() != 0;
        this.notify = in.readByte() != 0;
        int tmpUpdateStrategy = in.readInt();
        this.updateStrategy = tmpUpdateStrategy == -1 ? null : UpdateStrategy.values()[tmpUpdateStrategy];
    }

    public static final Parcelable.Creator<InstallOptions> CREATOR = new Parcelable.Creator<InstallOptions>() {
        @Override
        public InstallOptions createFromParcel(Parcel source) {
            return new InstallOptions(source);
        }

        @Override
        public InstallOptions[] newArray(int size) {
            return new InstallOptions[size];
        }
    };
}
