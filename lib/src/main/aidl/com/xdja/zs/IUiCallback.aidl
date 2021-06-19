// IUiCallback.aid
package com.xdja.zs;

interface IUiCallback {
    void onAppOpened(in String packageName, in int userId);
    boolean isLaunched(in String packageName, in int userId);
}
