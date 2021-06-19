package com.lody.virtual.open;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;
import com.lody.virtual.remote.InstalledAppInfo;

/**
 * @author Lody
 */
public class MultiAppHelper {

    public static int installExistedPackage(String pkg) throws IllegalStateException {
        return installExistedPackage(VirtualCore.get().getInstalledAppInfo(pkg, 0));
    }

    public static int installExistedPackage(InstalledAppInfo info) throws IllegalStateException {
        if (info == null) {
            throw new IllegalStateException("pkg must be installed.");
        }
        int[] userIds = info.getInstalledUsers();
        int nextUserId = userIds.length;
                /*
                  Input : userIds = {0, 1, 3}
                  Output: nextUserId = 2
                 */
        for (int i = 0; i < userIds.length; i++) {
            if (userIds[i] != i) {
                nextUserId = i;
                break;
            }
        }
        if (VUserManager.get().getUserInfo(nextUserId) == null) {
            // user not exist, create it automatically.
            String nextUserName = "Space " + (nextUserId + 1);
            VUserInfo newUserInfo = VUserManager.get().createUser(nextUserName, VUserInfo.FLAG_ADMIN);
            if (newUserInfo == null) {
                throw new IllegalStateException();
            }
        }
        boolean success = VirtualCore.get().installPackageAsUser(nextUserId, info.packageName);
        if (!success) {
            throw new IllegalStateException("install fail");
        }
        return nextUserId;
    }
}
