package com.lody.virtual.server.pm;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.remote.InstalledAppInfo;

import static com.lody.virtual.remote.InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK;

/**
 * @author Lody
 */

public class PackageSetting implements Parcelable {

    public static final int FLAG_RUN_32BIT = 0;
    public static final int FLAG_RUN_BOTH_32BIT_64BIT = 1;
    public static final int FLAG_RUN_64BIT = 2;

    public static final int FIRST_V2_VERSION = 5;

    public static final int CURRENT_VERSION = 5;

    private static final PackageUserState DEFAULT_USER_STATE = new PackageUserState();

    public int version;

    public String packageName;
    public int appId;
    public int appMode;
    SparseArray<PackageUserState> userState = new SparseArray<>();
    public int flag;
    public long firstInstallTime;
    public long lastUpdateTime;

    public PackageSetting() {
        version = CURRENT_VERSION;
    }


    public String getApkPath(boolean is64bit) {
        if (appMode == MODE_APP_USE_OUTSIDE_APK) {
            try {
                ApplicationInfo info = VirtualCore.get().getUnHookPackageManager().getApplicationInfo(packageName, 0);
                if(info == null){
                    return null;
                }
                return info.publicSourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
        if (is64bit) {
            return VEnvironment.getPackageResourcePath64(packageName).getPath();
        } else {
            return VEnvironment.getPackageResourcePath(packageName).getPath();
        }
    }

    public InstalledAppInfo getAppInfo() {
        return new InstalledAppInfo(packageName, appMode, flag, appId);
    }

    void removeUser(int userId) {
        userState.delete(userId);
    }

    PackageUserState modifyUserState(int userId) {
        PackageUserState state = userState.get(userId);
        if (state == null) {
            state = new PackageUserState();
            userState.put(userId, state);
        }
        return state;
    }

    void setUserState(int userId, boolean launched, boolean hidden, boolean installed) {
        PackageUserState state = modifyUserState(userId);
        state.launched = launched;
        state.hidden = hidden;
        state.installed = installed;
    }

    public PackageUserState readUserState(int userId) {
        PackageUserState state = userState.get(userId);
        if (state != null) {
            return state;
        }
        return DEFAULT_USER_STATE;
    }

    public boolean isLaunched(int userId) {
        return readUserState(userId).launched;
    }

    public boolean isHidden(int userId) {
        return readUserState(userId).hidden;
    }

    public boolean isInstalled(int userId) {
        return readUserState(userId).installed;
    }

    public void setLaunched(int userId, boolean launched) {
        modifyUserState(userId).launched = launched;
    }

    public void setHidden(int userId, boolean hidden) {
        modifyUserState(userId).hidden = hidden;
    }

    public void setInstalled(int userId, boolean installed) {
        modifyUserState(userId).installed = installed;
    }

    public boolean isRunPluginProcess() {
        if (VirtualCore.getConfig().getPluginEnginePackageName() == null) {
            return false;
        }
        if (VirtualRuntime.is64bit()) {
            return flag == FLAG_RUN_32BIT;
        } else {
            return flag == FLAG_RUN_64BIT;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeInt(this.appId);
        dest.writeInt(this.appMode);
        dest.writeSparseArray((SparseArray) this.userState);
        dest.writeInt(this.flag);
        dest.writeLong(this.firstInstallTime);
        dest.writeLong(this.lastUpdateTime);
    }

    PackageSetting(int version, Parcel in) {
        this.version = version;
        this.packageName = in.readString();
        this.appId = in.readInt();
        this.appMode = in.readInt();
        this.userState = in.readSparseArray(PackageUserState.class.getClassLoader());
        this.flag = in.readInt();
        this.firstInstallTime = in.readLong();
        this.lastUpdateTime = in.readLong();
    }


    public static final Parcelable.Creator<PackageSetting> CREATOR = new Parcelable.Creator<PackageSetting>() {
        @Override
        public PackageSetting createFromParcel(Parcel source) {
            return new PackageSetting(CURRENT_VERSION, source);
        }

        @Override
        public PackageSetting[] newArray(int size) {
            return new PackageSetting[size];
        }
    };
}
