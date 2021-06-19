package com.lody.virtual.server.interfaces;

import android.os.RemoteException;

/**
 * @author Lody
 */
interface IVirtualStorageService{

    void setVirtualStorage(String packageName, int userId, String vsPath);

    String getVirtualStorage(String packageName, int userId);

    void setVirtualStorageState(String packageName, int userId, boolean enable);

    boolean isVirtualStorageEnable(String packageName, int userId);
}
