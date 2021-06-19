package com.lody.virtual.os;

import android.os.Binder;

import com.lody.virtual.client.ipc.VActivityManager;

/**
 * @author Lody
 */

public class VBinder {

    public static int getCallingUid() {
        return VActivityManager.get().getUidByPid(Binder.getCallingPid());
    }

    public static int getBaseCallingUid() {
        return VUserHandle.getAppId(getCallingUid());
    }

    public static int getCallingPid() {
        return Binder.getCallingPid();
    }

    /**
     * @see com.lody.virtual.os.VUserHandle#getCallingUserHandle
     * @deprecated
     */
    public static VUserHandle getCallingUserHandle() {
        return VUserHandle.getCallingUserHandle();
    }
}
