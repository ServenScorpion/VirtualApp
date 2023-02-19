package com.carlos.home.repo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;

import com.carlos.common.utils.HVLog;
import com.carlos.common.utils.ResponseProgram;
import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.remote.InstalledAppInfo;
import com.carlos.home.models.AppData;
import com.carlos.home.models.AppInfo;
import com.carlos.home.models.AppInfoLite;
import com.carlos.utils.Utils;

import org.jdeferred.Promise;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.carlos.home.models.MultiplePackageAppData;
import com.carlos.home.models.PackageAppData;
import com.lody.virtual.remote.VAppInstallerParams;
import com.lody.virtual.remote.VAppInstallerResult;

/**
 * @author LodyChen
 */
public class AppRepository implements AppDataSource {

    private static final Collator COLLATOR = Collator.getInstance(Locale.CHINA);
    private final Map<String, String> mLabels = new HashMap<>();
    private static final List<String> SCAN_PATH_LIST = Arrays.asList(
            ".",
            "backups/apps",
            "wandoujia/app",
            "tencent/tassistant/apk",
            "BaiduAsa9103056",
            "360Download",
            "pp/downloader",
            "pp/downloader/apk",
            "pp/downloader/silent/apk");

    private Context mContext;

    public AppRepository(Context context) {
        mContext = context;
    }

    private static boolean isSystemApplication(PackageInfo packageInfo) {
        int uid = packageInfo.applicationInfo.uid;
        return uid < Process.FIRST_APPLICATION_UID || uid > Process.LAST_APPLICATION_UID
                || (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }


    /**
     * @param multi 表示是否显示多开的所有  true 表示所有,false 表示只是安装应用
     * @param islauncher   是否显示launcher的icon
     * @return All the Applications we Virtual. 获取多开应用列表
     */
    @Override
    public Promise<List<AppData>, Throwable, Void> getVirtualApps(boolean multi,boolean islauncher) {

        return ResponseProgram.defer().when(() -> {
            List<AppData> models = new ArrayList<>();
            List<InstalledAppInfo> infos = VirtualCore.get().getInstalledApps(InstalledAppInfo.FLAG_EXCLUDE_XPOSED_MODULE);
            for (InstalledAppInfo info : infos) {
                if (!VirtualCore.get().isPackageLaunchable(info.packageName) && !islauncher) {
                    continue;
                }
                PackageAppData data = new PackageAppData(mContext, info);
                if (VirtualCore.get().isAppInstalledAsUser(0, info.packageName)) {
                    models.add(data);
                }
                mLabels.put(info.packageName, data.name);
                if (multi) {// 表示要将多开的全部显示,否则表示只有安装的部分,多开的就不显示了
                    int[] userIds = info.getInstalledUsers();
                    for (int userId : userIds) {
                        if (userId != 0) {
                            models.add(new MultiplePackageAppData(data, userId));
                        }
                    }
                }
            }
            return models;
        });
    }
/*    @Override
    public Promise<List<AppData>, Throwable, Void> getVirtualApps() {
        HVLog.d("getVirtualApps ");
        return ResponseProgram.defer().when(() -> {
            HVLog.d("getVirtualApps  1 ");
            List<AppData> models = new ArrayList<>();
            try {
                VirtualCore.get().getInstalledApps(0);
            }catch (Exception e){
                HVLog.e(e.toString());
            }
            List<InstalledAppInfo> infos = VirtualCore.get().getInstalledApps(InstalledAppInfo.FLAG_EXCLUDE_XPOSED_MODULE);
            HVLog.d("getVirtualApps infos:"+infos.size());
            for (InstalledAppInfo info : infos) {
                if (!VirtualCore.get().isPackageLaunchable(info.packageName)) {
                    continue;
                }
                PackageAppData data = new PackageAppData(mContext, info);
                if (VirtualCore.get().isAppInstalledAsUser(0, info.packageName)) {
                    models.add(data);
                }
                mLabels.put(info.packageName, data.name);
                int[] userIds = info.getInstalledUsers();
                for (int userId : userIds) {
                    if (userId != 0) {
                        models.add(new MultiplePackageAppData(data, userId));
                    }
                }
            }
            return models;
        });
    }*/

    @Override
    public Promise<List<AppData>, Throwable, Void> getVirtualXposedModules() {
        return ResponseProgram.defer().when(() -> {
            List<InstalledAppInfo> infos = VirtualCore.get().getInstalledApps(InstalledAppInfo.FLAG_XPOSED_MODULE);
            List<AppData> models = new ArrayList<>();
            for (InstalledAppInfo info : infos) {
                PackageAppData data = new PackageAppData(mContext, info);
                if (VirtualCore.get().isAppInstalledAsUser(0, info.packageName)) {
                    models.add(data);
                }
                int[] userIds = info.getInstalledUsers();
                for (int userId : userIds) {
                    if (userId != 0) {
                        models.add(new MultiplePackageAppData(data, userId));
                    }
                }
            }
            return models;
        });
    }

    @Override
    public Promise<List<AppInfo>, Throwable, Void> getInstalledApps(Context context,boolean hideGApps) {
        HVLog.d("getInstalledApps 123");
        return ResponseProgram.defer().when(() -> convertPackageInfoToAppData(context, context.getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS), true, hideGApps/*true*/));
    }

    @Override
    public Promise<List<AppInfo>, Throwable, Void> getInstalledXposedModules(Context context) {
        return ResponseProgram.defer().when(() -> convertPackageInfoToAppData(context, context.getPackageManager().getInstalledPackages(0), true, true));
    }

    @Override
    public Promise<List<AppInfo>, Throwable, Void> getStorageApps(Context context, File rootDir) {
        HVLog.d("getStorageApps 134");
        return ResponseProgram.defer().when(() -> convertPackageInfoToAppData(context, findAndParseAPKs(context, rootDir, SCAN_PATH_LIST), false, false));
    }

    private List<PackageInfo> findAndParseAPKs(Context context, File rootDir, List<String> paths) {
        List<PackageInfo> packageList = new ArrayList<>();
        if (paths == null)
            return packageList;
        for (String path : paths) {
            File[] dirFiles = new File(rootDir, path).listFiles();
            if (dirFiles == null)
                continue;
            for (File f : dirFiles) {
                if (!f.getName().toLowerCase().endsWith(".apk"))
                    continue;
                PackageInfo pkgInfo = null;
                try {
                    pkgInfo = context.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), PackageManager.GET_PERMISSIONS);
                    pkgInfo.applicationInfo.sourceDir = f.getAbsolutePath();
                    pkgInfo.applicationInfo.publicSourceDir = f.getAbsolutePath();
                } catch (Exception e) {
                    // Ignore
                }
                if (pkgInfo != null)
                    packageList.add(pkgInfo);
            }
        }
        return packageList;
    }

    /**
     * @param context 上下文
     * @param pkgList 拿到的数据列表
     * @param cloneMode 是否是克隆app
     * @param hideGApps 是否因此Google 四件套
     * */
    private List<AppInfo> convertPackageInfoToAppData(Context context, List<PackageInfo> pkgList,
                                                      boolean cloneMode, boolean hideGApps) {
        PackageManager pm = context.getPackageManager();
        List<AppInfo> list = new ArrayList<>(pkgList.size());
        HVLog.d("查看安装数量 list："+list.size()+"    pkgList:"+pkgList.size());
        for (PackageInfo pkg : pkgList) {
            // ignore the host package
            if (StubManifest.isHostPackageName(pkg.packageName)) {
                continue;
            }
            if (!hideGApps && GmsSupport.isGoogleAppOrService(pkg.packageName)) {
                continue;
            }
            if (cloneMode && isSystemApplication(pkg)) {
                continue;
            }
            HVLog.d("PackageInfo "+pkg.packageName);
            if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_HAS_CODE) == 0) continue;
            ApplicationInfo ai = pkg.applicationInfo;
            String path = ai.publicSourceDir != null ? ai.publicSourceDir : ai.sourceDir;
            if (path == null) {
                continue;
            }
            InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(pkg.packageName, 0);
            AppInfo info = new AppInfo();
            info.packageName = pkg.packageName;
            info.cloneMode = cloneMode;
            info.path = path;
            info.icon = ai.loadIcon(pm);
            info.name = ai.loadLabel(pm);
            info.namePinyin = Utils.getPingYin(info.name.toString());
            info.targetSdkVersion = pkg.applicationInfo.targetSdkVersion;
            info.requestedPermissions = pkg.requestedPermissions;
            if (installedAppInfo != null) {
                info.path = installedAppInfo.getApkPath();
                info.cloneCount = installedAppInfo.getInstalledUsers().length;
            }
            list.add(info);
        }
        Collections.sort(list, (lhs, rhs) -> {
            int compareCloneCount = Integer.compare(lhs.cloneCount, rhs.cloneCount);
            if (compareCloneCount != 0) {
                return -compareCloneCount;
            }
            return COLLATOR.compare(lhs.name, rhs.name);
        });
        return list;
    }

    @Override
    public VAppInstallerResult addVirtualApp(AppInfoLite info) {
        //这里这个参数比较重要, 特别是第三个
/*        InstallOptions options = InstallOptions.makeOptions(info.dynamic, false, InstallOptions.UpdateStrategy.COMPARE_VERSION);
        return VirtualCore.get().installPackageSync(info.path, options);*/

        VAppInstallerParams params = new VAppInstallerParams();
        return VirtualCore.get().installPackage(info.getUri(), params);
    }

    @Override
    public boolean removeVirtualApp(String packageName, int userId) {
        return VirtualCore.get().uninstallPackageAsUser(packageName, userId);
    }

    @Override
    public String getLabel(String packageName) {
        String label = mLabels.get(packageName);
        if (label == null) {
            return packageName;
        }
        return label;
    }
}
