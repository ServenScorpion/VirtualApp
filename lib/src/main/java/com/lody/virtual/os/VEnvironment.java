package com.lody.virtual.os;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.utils.EncodeUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.VLog;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * @author Lody
 */

public class VEnvironment {

    private static final String TAG = VEnvironment.class.getSimpleName();

    private static final File ROOT;
    private static final File DATA_DIRECTORY;
    private static final File USER_DIRECTORY;
    private static final File USER_DE_DIRECTORY;
    private static final File DALVIK_CACHE_DIRECTORY;
    private static final File EXTERNAL_STORAGE_DIRECTORY;
    private static final File EMULATED_DIRECTORY;

    private static final File ROOT64;
    private static final File DATA_DIRECTORY64;
    private static final File USER_DIRECTORY64;
    private static final File USER_DE_DIRECTORY64;
    private static final File DALVIK_CACHE_DIRECTORY64;

    private static final String DIRECTORY_MUSIC = "Music";
    private static final String DIRECTORY_PODCASTS = "Podcasts";
    private static final String DIRECTORY_RINGTONES = "Ringtones";
    private static final String DIRECTORY_ALARMS = "Alarms";
    private static final String DIRECTORY_NOTIFICATIONS = "Notifications";
    private static final String DIRECTORY_PICTURES = "Pictures";
    private static final String DIRECTORY_MOVIES = "Movies";
    private static final String DIRECTORY_DOWNLOADS = "Download";
    private static final String DIRECTORY_DCIM = "DCIM";
    private static final String DIRECTORY_DOCUMENTS = "Documents";

    public static final String[] STANDARD_DIRECTORIES = {
            DIRECTORY_MUSIC,
            DIRECTORY_PODCASTS,
            DIRECTORY_RINGTONES,
            DIRECTORY_ALARMS,
            DIRECTORY_NOTIFICATIONS,
            DIRECTORY_PICTURES,
            DIRECTORY_MOVIES,
            DIRECTORY_DOWNLOADS,
            DIRECTORY_DCIM,
            DIRECTORY_DOCUMENTS
    };

    static {
        File host = new File(getContext().getApplicationInfo().dataDir);
        // Point to: /
        ROOT = ensureCreated(new File(host, "virtual"));
        // Point to: /data/
        DATA_DIRECTORY = ensureCreated(new File(ROOT, "data"));
        // Point to: /data/user/
        USER_DIRECTORY = ensureCreated(new File(DATA_DIRECTORY, "user"));

        USER_DE_DIRECTORY = ensureCreated(new File(DATA_DIRECTORY, "user_de"));
        // Point to: /opt/
        DALVIK_CACHE_DIRECTORY = ensureCreated(new File(ROOT, "opt"));

        File host64 = new File(host.getParent(), StubManifest.PACKAGE_NAME_64BIT);
        // Point to: /
        ROOT64 = ensureCreated(new File(host64, "virtual"));
        // Point to: /data/
        DATA_DIRECTORY64 = ensureCreated(new File(ROOT64, "data"));
        // Point to: /data/user/
        USER_DIRECTORY64 = ensureCreated(new File(DATA_DIRECTORY64, "user"));
        USER_DE_DIRECTORY64 = ensureCreated(new File(DATA_DIRECTORY64, "user_de"));
        // Point to: /opt/
        DALVIK_CACHE_DIRECTORY64 = ensureCreated(new File(ROOT64, "opt"));
    }

    // add by lml@xdja.com
    static {
        // Point to: /storage/
        EXTERNAL_STORAGE_DIRECTORY = ensureCreated(new File(ROOT, "storage"));

        // Point to: /storage/emulated
        EMULATED_DIRECTORY = ensureCreated(new File(EXTERNAL_STORAGE_DIRECTORY, "emulated"));
    }

    @SuppressLint("SdCardPath")
    public static void systemReady() {
        //create private dir at SdCard & all of TFCard
        getContext().getExternalFilesDir(null).getAbsolutePath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                FileUtils.chmod(ROOT.getAbsolutePath(), FileUtils.FileMode.MODE_755);
                FileUtils.chmod(DATA_DIRECTORY.getAbsolutePath(), FileUtils.FileMode.MODE_755);
                FileUtils.chmod(getDataAppDirectory().getAbsolutePath(), FileUtils.FileMode.MODE_755);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //创建 /data/user/0/ & /data/data/
        //创建链接
        try {
            FileUtils.createSymlink(getUserDataDirectory(0).getPath(), new File(DATA_DIRECTORY, "data").getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Context getContext() {
        return VirtualCore.get().getContext();
    }

    private static File ensureCreated(File folder){
        return ensureCreated(folder, false);
    }

    private static File ensureCreated(File folder, boolean chmod) {
        if (!folder.exists()) {
            folder.mkdirs();
            if(chmod){
                try {
                    FileUtils.chmod(folder.getPath(), FileUtils.FileMode.MODE_755);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return folder;
    }

    public static void chmodPackageDictionary(File packageFile) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (FileUtils.isSymlink(packageFile)) {
                    return;
                }
                FileUtils.chmod(packageFile.getParentFile().getAbsolutePath(), FileUtils.FileMode.MODE_755);
                FileUtils.chmod(packageFile.getAbsolutePath(), FileUtils.FileMode.MODE_755);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getProcDirectory() {
        return ensureCreated(new File(ROOT, "proc"));
    }

    public static File getProcDirectory64() {
        return ensureCreated(new File(ROOT64, "proc"));
    }

    public static File getDataUserPackageDirectory(int userId,
                                                   String packageName) {
        return ensureCreated(new File(getUserDataDirectory(userId), packageName));
    }

    public static File getDataUserPackageDirectory64(int userId,
                                                     String packageName) {
        return ensureCreated(new File(getUserDataDirectory64(userId), packageName));
    }

    public static File getDeDataUserPackageDirectory(int userId,
                                                     String packageName) {
        return ensureCreated(new File(getUserDeDataDirectory(userId), packageName));
    }

    public static File getDeDataUserPackageDirectory64(int userId,
                                                       String packageName) {
        return ensureCreated(new File(getUserDeDataDirectory64(userId), packageName));
    }

    public static File getPackageResourcePath64(String packageName) {
        return new File(getDataAppPackageDirectory64(packageName), /*base.apk*/EncodeUtils.decodeBase64("YmFzZS5hcGs="));
    }

    public static File getDataAppDirectory() {
        return ensureCreated(new File(getDataDirectory(), "app"));
    }

    public static File getEmptySdcardDirectory() {
        return ensureCreated(new File(getDataDirectory(), "empty_sdcard"));
    }

    public static File getDataAppDirectory64() {
        return ensureCreated(new File(getDataDirectory64(), "app"));
    }

    public static File getUidListFile() {
        return new File(getSystemSecureDirectory(), "uid-list.ini");
    }

    public static File getBakUidListFile() {
        return new File(getSystemSecureDirectory(), "uid-list.ini.bak");
    }

    public static File getAccountConfigFile() {
        return new File(getSystemSecureDirectory(), "account-list.ini");
    }

    public static File getComponentStateFile() {
        return new File(getSystemSecureDirectory(), "component-state.ini");
    }

    public static File getSyncDirectory() {
        return ensureCreated(new File(getSystemSecureDirectory(), "sync"));
    }

    public static File getAccountVisibilityConfigFile() {
        return new File(getSystemSecureDirectory(), "account-visibility-list.ini");
    }

    public static File getVirtualLocationFile() {
        return new File(getSystemSecureDirectory(), "virtual-loc.ini");
    }

    public static File getDeviceInfoFile() {
        return new File(getSystemSecureDirectory(), "device-config.ini");
    }

    public static File getSettingRuleFile() {
        return new File(getSystemSecureDirectory(), "app-setting.ini");
    }

    public static File getBuildInfoFile() {
        return new File(getSystemSecureDirectory(), "device-build.ini");
    }

    public static File getPackageListFile() {
        return new File(getSystemSecureDirectory(), "packages.ini");
    }

    /**
     * @return Virtual storage config file
     */
    public static File getVSConfigFile() {
        return new File(getSystemSecureDirectory(), "vss.ini");
    }

    public static File getBakPackageListFile() {
        return new File(getSystemSecureDirectory(), "packages.ini.bak");
    }

    public static File getJobConfigFile() {
        return new File(getSystemSecureDirectory(), "job-list.ini");
    }

    public static File getDalvikCacheDirectory() {
        return DALVIK_CACHE_DIRECTORY;
    }

    public static File getDalvikCacheDirectory64() {
        return DALVIK_CACHE_DIRECTORY64;
    }

    public static File getOdexFile64(String packageName) {
        return new File(DALVIK_CACHE_DIRECTORY64, "data@app@" + packageName + "-1@base.apk@classes.dex");
    }

    public static File getDataAppPackageDirectory(String packageName) {
        return ensureCreated(new File(getDataAppDirectory(), packageName));
    }

    public static File getDataAppPackageDirectory64(String packageName) {
        return ensureCreated(new File(getDataAppDirectory64(), packageName));
    }

    public static File getAppLibDirectory(String packageName) {
        return ensureCreated(new File(getDataAppPackageDirectory(packageName), "lib"), true);
    }

    public static File getAppLibDirectory64(String packageName) {
        return ensureCreated(new File(getDataAppPackageDirectory64(packageName), "lib"), true);
    }

    public static File getUserAppLibDirectory(int userId, String packageName) {
        return new File(getDataUserPackageDirectory(userId, packageName), "lib");
    }

    public static File getUserAppLibDirectory64(int userId, String packageName) {
        return new File(getDataUserPackageDirectory64(userId, packageName), "lib");
    }

    public static File getPackageCacheFile(String packageName) {
        return new File(getDataAppPackageDirectory(packageName), "package.ini");
    }

    public static File getSignatureFile(String packageName) {
        return new File(getDataAppPackageDirectory(packageName), "signature.ini");
    }

    public static File getFrameworkDirectory32() {
        return ensureCreated(new File(ROOT, "framework"));
    }

    public static File getFrameworkDirectory32(String name) {
        return ensureCreated(new File(getFrameworkDirectory32(), name));
    }

    public static File getOptimizedFrameworkFile32(String name) {
        return new File(getFrameworkDirectory32(name), /*TODO: base64 encode it*/"classes.dex");
    }


    public static File getFrameworkFile32(String name) {
        return new File(getFrameworkDirectory32(name), "extracted.jar");
    }

    public static File getUserSystemDirectory() {
        return USER_DIRECTORY;
    }

    public static File getUserDeSystemDirectory() {
        return USER_DE_DIRECTORY;
    }

    /**
     * @param userId
     * @return
     * @see #getUserDataDirectory
     * @deprecated
     */
    public static File getUserSystemDirectory(int userId) {
        return getUserDataDirectory(userId);
    }

    public static File getUserDataDirectory(int userId) {
        return ensureCreated(new File(USER_DIRECTORY, String.valueOf(userId)));
    }

    public static File getUserDeDataDirectory(int userId) {
        return ensureCreated(new File(USER_DE_DIRECTORY, String.valueOf(userId)));
    }

    public static File getUserDataDirectory64(int userId) {
        return ensureCreated(new File(USER_DIRECTORY64, String.valueOf(userId)));
    }

    public static File getUserDeDataDirectory64(int userId) {
        return ensureCreated(new File(USER_DE_DIRECTORY64, String.valueOf(userId)));
    }

    public static File getSystemDirectory(int userId) {
        return ensureCreated(new File(getUserDataDirectory(userId), "system"));
    }

    public static File getSystemDirectory64(int userId) {
        return ensureCreated(new File(getUserDataDirectory64(userId), "system"));
    }

    /**
     * @deprecated
     */
    public static File getSystemBuildFile(int userId) {
        return new File(getSystemDirectory(userId), "build.prop");
    }

    /**
     * @deprecated
     */
    public static File getAppBuildFile(String packageName, int userId) {
        return new File(getSystemDirectory(userId), packageName + "_build.prop");
    }

    public static File getWifiMacFile(int userId, boolean is64Bit) {
        if (is64Bit) {
            return new File(getSystemDirectory64(userId), "wifiMacAddress");
        }
        return new File(getSystemDirectory(userId), "wifiMacAddress");
    }

    public static File getDataDirectory() {
        return DATA_DIRECTORY;
    }

    public static File getDataDirectory64() {
        return DATA_DIRECTORY64;
    }

    public static File getSystemSecureDirectory() {
        return ensureCreated(new File(getDataAppDirectory(), "system"));
    }

    public static File getPackageInstallerStageDir() {
        return ensureCreated(new File(getSystemSecureDirectory(), ".session_dir"));
    }

    public static File getNativeCacheDir(boolean is64bit) {
        return ensureCreated(new File(is64bit ? ROOT64 : ROOT, ".native"));
    }

    /**
     * get all of data dir of current context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static File[] getTFRoots(){
        return getContext().getExternalFilesDirs(null);
    }
    /**
     * get TFCard root dir
     * @param Dir  /storage/XXXXX/Android/data/@packagename
     * @return /storage/XXXXX
     */
    public static File getTFRoot(String Dir){
        int lastIndex = Dir.lastIndexOf("/Android/data/");
        return ensureCreated(new File(Dir.substring(0,lastIndex)));
    }
    /**
     * create and return virtual dir of safetybox at TFCard
     * @param tfroot /storage/XXXXXX
     * @return  /storage/XXXXXX/Android/daa/com.xdja/safetybox/virtual/
     */
    public static File getTFVirtualRoot(String tfroot){
        String appFileDir = tfroot + "/Android/data/"+getContext().getPackageName()+"";
        return ensureCreated(new File(appFileDir, "virtual"));
    }
    public static File getTFVirtualRoot(String tfroot,String Dir) {
        return ensureCreated(new File(getTFVirtualRoot(tfroot).getAbsolutePath(), Dir));
    }

    public static File getExternalStorageDirectory(int userId) {
        File storage_dir = ensureCreated(new File(EMULATED_DIRECTORY, String.valueOf(userId)));
        for (String sdir : STANDARD_DIRECTORIES) {
            ensureCreated(new File(storage_dir, sdir));
        }
        //return ensureCreated(new File(EMULATED_DIRECTORY, String.valueOf(userId)));
        return storage_dir;
    }

    public static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (cur == null) {
                cur = new File(segment);
            } else {
                cur = new File(cur, segment);
            }
        }

        return cur;
    }

    public static File getExternalStorageAppDataDir(int userId, String packageName) {
        return buildPath(getExternalStorageDirectory(userId), "Android", "data", packageName);
    }


    public static File getPackageResourcePath(String packageName) {
        File file;
        for (int i = 1; i <= 10; i++) {
            //1-10
            file = new File(getDataAppPackageDirectory(packageName), "base-" + i + ".apk");
            if (file.exists()) {
                return file;
            }
        }
        //适配老版本
        return new File(getDataAppPackageDirectory(packageName), /*base.apk*/EncodeUtils.decodeBase64("YmFzZS5hcGs="));
    }

    public static File getOdexFile(String packageName) {
        File file;
        //整个遍历过程就0-2ms
        for (int i = 1; i <= 10; i++) {
            //1-10
            file = new File(getDataAppPackageDirectory(packageName), "base-" + i + ".apk");
            if (file.exists()) {
                return new File(DALVIK_CACHE_DIRECTORY, "data@app@" + packageName + "@base-" + i + ".apk@classes.dex");
            }
        }
        //适配老版本
        return new File(DALVIK_CACHE_DIRECTORY, "data@app@" + packageName + "-1@base.apk@classes.dex");
    }

    public static File getPackageResourcePathNext(String packageName) {
        File file;
        for (int i = 1; i <= 10; i++) {
            //1-9
            file = new File(getDataAppPackageDirectory(packageName), "base-" + i + ".apk");
            if (!file.exists()) {
                return file;
            }
        }
        //如果走了这里，说明旧文件没删除（base-1.apk存在）
        return new File(getDataAppPackageDirectory(packageName), "base-2.apk");
    }

    public static File getPublicResourcePath(String packageName){
        return new File(getDataAppPackageDirectory(packageName), /*base.apk*/EncodeUtils.decodeBase64("YmFzZS5hcGs="));
    }

    public static File getPublicResourcePath64(String packageName){
        return new File(getDataAppPackageDirectory64(packageName), /*base.apk*/EncodeUtils.decodeBase64("YmFzZS5hcGs="));
    }

    public static File getNetStrategyInfoFile() {
        return new File(getSystemSecureDirectory(), "netstrategy-list.ini");
    }

    public static File getNetEnableInfoFile() {
        return new File(getSystemSecureDirectory(), "network-enable.ini");
    }

}
