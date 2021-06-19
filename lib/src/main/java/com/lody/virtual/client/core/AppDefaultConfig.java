package com.lody.virtual.client.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import mirror.android.app.SharedPreferencesImpl;

public class AppDefaultConfig {
    private static final boolean DEBUG = false;
    private static final String TAG = "kk-test";

    @SuppressLint("ApplySharedPref")
    public static void setDefaultData(String packageName) {
        if (DEBUG) {
            VLog.i(TAG, "setDefaultData:%s", packageName);
        }
        try {
            initDefaultData(packageName);
        } catch (Throwable e) {
            Log.e(TAG, "setDefaultData", e);
        }
    }

    private static SharedPreferences getSharedPreferences(File file, int mode) {
        if (SharedPreferencesImpl.ctor != null) {
            try {
                return (SharedPreferences) SharedPreferencesImpl.ctor.newInstance(file, mode);
            } catch (Throwable e) {
                Log.w(TAG, "getSharedPreferences", e);
            }
        }
        return null;
    }

    @SuppressLint("ApplySharedPref")
    private static void initDefaultData(String packageName) {
        if ("com.mxtech.videoplayer.ad".equals(packageName)) {
            File dataDir = VEnvironment.getDataUserPackageDirectory(0, packageName);
            File xml = new File(dataDir, "shared_prefs/com.mxtech.videoplayer.ad_preferences.xml");
            SharedPreferences sharedPreferences = getSharedPreferences(xml, Context.MODE_PRIVATE);
            if (sharedPreferences != null) {
                if (DEBUG) {
                    VLog.i(TAG, "getExternalStorageDirectory=%s", Environment.getExternalStorageDirectory().getAbsolutePath());
                }
                //MX播放器默认扫描mnt的sdcard路径，导致重复显示
                String[] paths = new String[]{Uri.encode("/storage"), Uri.encode(Environment.getExternalStorageDirectory().getAbsolutePath())};
                sharedPreferences
                        .edit()
                        .putInt("video_scan_roots.version", Build.VERSION.SDK_INT)
                        .putString("video_scan_roots.2", TextUtils.join(File.pathSeparator, paths))
                        .commit();
            }
        }
    }

    private static void copyAssetFolder(String root, File dataDir) {
        AssetManager assetManager = VirtualCore.get().getContext().getAssets();
        try {
            String[] files = assetManager.list(root);
            if (files != null && files.length > 0) {
                for (String file : files) {
                    copyAssetFiles(assetManager, root + "/" + file, file, dataDir);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void copyAssetFiles(AssetManager assetManager, String path, String name, File dir) {
        try {
            if (DEBUG) {
                VLog.i(TAG, "copyAssetFiles:%s", path);
            }
            String[] files = assetManager.list(path);
            if (files != null && files.length > 0) {
                //dir
                File newDir = new File(dir, name);
                for (String file : files) {
                    copyAssetFiles(assetManager, path + "/" + file, file, newDir);
                }
            } else {
                copyAssetFile(assetManager, path, name, dir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyAssetFile(AssetManager assetManager, String path, String name, File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, name);
        if (DEBUG) {
            VLog.i(TAG, "copyAssetFile:%s->%s", path, file);
        }
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(path);
            FileUtils.writeToFile(inputStream, file);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtils.closeQuietly(inputStream);
        }
    }
}
