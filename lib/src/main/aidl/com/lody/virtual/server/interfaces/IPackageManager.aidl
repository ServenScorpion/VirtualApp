package com.lody.virtual.server.interfaces;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;

import com.lody.virtual.remote.VParceledListSlice;
import com.lody.virtual.remote.ReceiverInfo;

import java.util.List;

/**
 * @author Lody
 */
interface IPackageManager{

    int getPackageUid(String packageName, int userId);

    String[] getPackagesForUid(int vuid);

    List<String> getSharedLibraries(String pkgName);

    int checkPermission(boolean is64bit, String permName, String pkgName, int userId);

    PackageInfo getPackageInfo(String packageName, int flags, int userId);

    ActivityInfo getActivityInfo(in ComponentName componentName, int flags, int userId);

    boolean activitySupportsIntent(in ComponentName component,in  Intent intent, String resolvedType);

    ActivityInfo getReceiverInfo(in ComponentName componentName, int flags, int userId);

    ServiceInfo getServiceInfo(in ComponentName componentName, int flags, int userId);

    ProviderInfo getProviderInfo(in ComponentName componentName, int flags, int userId);

    ResolveInfo resolveIntent(in Intent intent, String resolvedType, int flags, int userId);

    List<ResolveInfo> queryIntentActivities(in Intent intent, String resolvedType, int flags, int userId);

    List<ResolveInfo> queryIntentReceivers(in Intent intent, String resolvedType, int flags, int userId);

    ResolveInfo resolveService(in Intent intent, String resolvedType, int flags, int userId);

    List<ResolveInfo> queryIntentServices(in Intent intent, String resolvedType, int flags, int userId);

    List<ResolveInfo> queryIntentContentProviders(in Intent intent, String resolvedType, int flags, int userId);

    VParceledListSlice getInstalledPackages(int flags, int userId);

    VParceledListSlice getInstalledApplications(int flags, int userId);

    List<ReceiverInfo> getReceiverInfos(String packageName, String processName, int userId);

    PermissionInfo getPermissionInfo(String name, int flags);

    List<PermissionInfo> queryPermissionsByGroup(String group, int flags);

    PermissionGroupInfo getPermissionGroupInfo(String name, int flags);

    List<PermissionGroupInfo> getAllPermissionGroups(int flags);

    ProviderInfo resolveContentProvider(String name, int flags, int userId);

    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId);

    VParceledListSlice queryContentProviders(String processName, int vuid, int flags);

    List<String> querySharedPackages(String packageName);

    String getNameForUid(int uid);

    IBinder getPackageInstaller();

    int checkSignatures(String pkg1, String pkg2);

    String[] getDangrousPermissions(String packageName);

    boolean isVirtualAuthority(String authority);

    void setComponentEnabledSetting(in ComponentName componentName, int newState, int flags, int userId);

    int getComponentEnabledSetting(in ComponentName component, int userId);
}
