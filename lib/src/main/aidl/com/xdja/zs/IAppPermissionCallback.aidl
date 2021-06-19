// IAppPermissionCallback.aidl
package com.xdja.zs;

// Declare any non-default types here with import statements

interface IAppPermissionCallback {
    void onPermissionTrigger(in String appPackageName,in String permissionName);
}
