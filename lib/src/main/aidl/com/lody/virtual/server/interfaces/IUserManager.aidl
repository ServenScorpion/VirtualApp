package com.lody.virtual.server.interfaces;

import android.graphics.Bitmap;
import android.os.RemoteException;

import com.lody.virtual.os.VUserInfo;

import java.util.List;

/**
 * @author Lody
 */
interface IUserManager{
    VUserInfo createUser(String name, int flags);

    boolean removeUser(int userHandle);

    void setUserName(int userHandle, String name);

    void setUserIcon(int userHandle,in  Bitmap icon);

    Bitmap getUserIcon(int userHandle);

    List<VUserInfo> getUsers(boolean excludeDying);

    VUserInfo getUserInfo(int userHandle);

    void setGuestEnabled(boolean enable);

    boolean isGuestEnabled();

    void wipeUser(int userHandle);

    int getUserSerialNumber(int userHandle);

    int getUserHandle(int userSerialNumber);
}
