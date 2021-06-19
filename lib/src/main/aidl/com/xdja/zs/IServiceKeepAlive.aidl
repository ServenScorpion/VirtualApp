// IServiceKeepAlive.aidl
package com.xdja.zs;

// Declare any non-default types here with import statements

interface IServiceKeepAlive {
    void scheduleRunKeepAliveService(String pkgName, int userId);
    void scheduleUpdateKeepAliveList(String pkgName, int action);
    boolean inKeepAliveServiceList(String pkgName);
}
