package com.carlos.home.repo;

import android.content.Context;


import com.carlos.home.models.AppInfo;

import org.jdeferred.Promise;

import java.io.File;
import java.util.List;

import com.carlos.home.models.AppData;
import com.carlos.home.models.AppInfoLite;
import com.lody.virtual.remote.VAppInstallerResult;

/**
 * @author LodyChen
 * @version 1.0
 */
public interface AppDataSource {

    /**
     * @param multi 表示是否显示多开的所有  true 表示所有,false 表示只是安装应用
     * @param islauncher 表示是否显示launcher 的icon
     * @return All the Applications we Virtual. 获取多开应用列表
     */
    Promise<List<AppData>, Throwable, Void> getVirtualApps(boolean multi,boolean islauncher);

    Promise<List<AppData>, Throwable, Void> getVirtualXposedModules();

    /**
     * @param context Context
     * @param hideApp 是否返回隐藏app,  false 表示不返回,true 表示返回隐藏APP
     * @return All the Applications we Installed.
     */
    Promise<List<AppInfo>, Throwable, Void> getInstalledApps(Context context,boolean hideApp);

    Promise<List<AppInfo>, Throwable, Void> getInstalledXposedModules(Context context);

    Promise<List<AppInfo>, Throwable, Void> getStorageApps(Context context, File rootDir);

    VAppInstallerResult addVirtualApp(AppInfoLite info);

    boolean removeVirtualApp(String packageName, int userId);

    String getLabel(String packageName);
}
