package io.virtualapp.home.repo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.remote.InstallOptions;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;

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

import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfo;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;

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

    @Override
    public Promise<List<AppData>, Throwable, Void> getVirtualApps() {

        return VUiKit.defer().when(() -> {
            List<AppData> models = new ArrayList<>();
            List<InstalledAppInfo> infos = VirtualCore.get().getInstalledApps(0);
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
    }

    @Override
    public Promise<List<AppData>, Throwable, Void> getVirtualXposedModules() {
        return VUiKit.defer().when(() -> {
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
    public Promise<List<AppInfo>, Throwable, Void> getInstalledApps(Context context) {
        return VUiKit.defer().when(() -> convertPackageInfoToAppData(context, context.getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS), true, true));
    }

    @Override
    public Promise<List<AppInfo>, Throwable, Void> getInstalledXposedModules(Context context) {
        return VUiKit.defer().when(() -> convertPackageInfoToAppData(context, context.getPackageManager().getInstalledPackages(0), true, true));
    }

    @Override
    public Promise<List<AppInfo>, Throwable, Void> getStorageApps(Context context, File rootDir) {
        return VUiKit.defer().when(() -> convertPackageInfoToAppData(context, findAndParseAPKs(context, rootDir, SCAN_PATH_LIST), false, false));
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

    private List<AppInfo> convertPackageInfoToAppData(Context context, List<PackageInfo> pkgList,
                                                      boolean cloneMode, boolean hideGApps) {
        PackageManager pm = context.getPackageManager();
        List<AppInfo> list = new ArrayList<>(pkgList.size());
        for (PackageInfo pkg : pkgList) {
            // ignore the host package
            if (StubManifest.isHostPackageName(pkg.packageName)) {
                continue;
            }
            if (hideGApps && GmsSupport.isGoogleAppOrService(pkg.packageName)) {
                continue;
            }
            if (cloneMode && isSystemApplication(pkg)) {
                continue;
            }
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
    public InstallResult addVirtualApp(AppInfoLite info) {
        InstallOptions options = InstallOptions.makeOptions(info.notCopyApk, false, InstallOptions.UpdateStrategy.COMPARE_VERSION);
        return VirtualCore.get().installPackageSync(info.path, options);
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
