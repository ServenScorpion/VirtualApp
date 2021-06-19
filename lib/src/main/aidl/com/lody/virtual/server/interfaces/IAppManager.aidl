package com.lody.virtual.server.interfaces;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.InstallOptions;
import com.lody.virtual.server.interfaces.IPackageObserver;

import android.os.ResultReceiver;

import java.util.List;

/**
 * @author Lody
 */
interface IAppManager{

    int[] getPackageInstalledUsers(String packageName);

    void scanApps();

    void addVisibleOutsidePackage(String pkg);

    void removeVisibleOutsidePackage(String pkg);

    boolean isOutsidePackageVisible(String pkg);

    int getUidForSharedUser(in String sharedUserName);

    InstalledAppInfo getInstalledAppInfo(String pkg, int flags);

    oneway void installPackage(String path, in InstallOptions options, in ResultReceiver receiver);

    void requestCopyPackage64(String packageName);

    boolean isPackageLaunched(int userId, String packageName);

    void setPackageHidden(int userId, String packageName, boolean hidden);

    boolean installPackageAsUser(int userId, String packageName);

    boolean uninstallPackageAsUser(String packageName, int userId);

    boolean uninstallPackage(String packageName);

    List<InstalledAppInfo> getInstalledApps(int flags);

    List<InstalledAppInfo> getInstalledAppsAsUser(int userId, int flags);

    int getInstalledAppCount();

    boolean isAppInstalled(String packageName);

    boolean isAppInstalledAsUser(int userId, String packageName);

    void registerObserver(in IPackageObserver observer);

    void unregisterObserver(in IPackageObserver observer);

    boolean isRun64BitProcess(String packageName);

    boolean isIORelocateWork();

    boolean cleanPackageData(in String pkg, int userId);
}
