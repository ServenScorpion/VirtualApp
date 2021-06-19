package com.lody.virtual.helper.compat;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mirror.com.android.internal.content.NativeLibraryHelper;

public class NativeLibraryHelperCompat {
    /**
     * @see PackageManager
     */
    public static final int INSTALL_SUCCEEDED = 1;
    public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;
    public static final int INSTALL_FAILED_INVALID_APK = -2;

    private static String TAG = NativeLibraryHelperCompat.class.getSimpleName();

    public static int copyNativeBinaries(File apkFile, File sharedLibraryDir) {
        return copyNativeBinaries(apkFile, sharedLibraryDir, null);
    }

    public static int copyNativeBinaries(File apkFile, File sharedLibraryDir, Map<String, List<String>> soMap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return copyNativeBinariesAfterL(apkFile, sharedLibraryDir, soMap);
        } else {
            return copyNativeBinariesBeforeL(apkFile, sharedLibraryDir);
        }
    }

    private static int copyNativeBinariesBeforeL(File apkFile, File sharedLibraryDir) {
        try {
            return Reflect.on(NativeLibraryHelper.TYPE).call("copyNativeBinariesIfNeededLI", apkFile, sharedLibraryDir)
                    .get();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static int copyNativeBinariesAfterL(File apkFile, File sharedLibraryDir, Map<String, List<String>> soMap) {
        try {
            Object handle = NativeLibraryHelper.Handle.create.call(apkFile);
            if (handle == null) {
                return -1;
            }
            String abi = null;
            Set<String> abiSet;
            if (soMap != null) {
                abiSet = soMap.keySet();
            } else {
                abiSet = getSupportAbiList(apkFile.getAbsolutePath());
            }

            boolean is64Bit = VirtualRuntime.is64bit();
            if (is64Bit && contain64bitAbi(abiSet)) {
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    int abiIndex = NativeLibraryHelper.findSupportedAbi.call(handle, Build.SUPPORTED_64_BIT_ABIS);
                    if (abiIndex >= 0) {
                        abi = Build.SUPPORTED_64_BIT_ABIS[abiIndex];
                    }
                }
            } else {
                final int len = Build.SUPPORTED_32_BIT_ABIS.length;
                if (len > 0) {
                    String[] optAbis = null;
                    if(soMap != null && containAbi(Build.SUPPORTED_32_BIT_ABIS, "armeabi-v7a")){
                        List<String> armList = soMap.get("armeabi");
                        List<String> armV7List = soMap.get("armeabi-v7a");
                        if (armList != null && armV7List != null) {
                            if (armV7List.size() < armList.size()) {
                                //remove armeabi-v7a
                                optAbis = new String[len - 1];
                                int i = 0, j = 0;
                                do {
                                    String var = Build.SUPPORTED_32_BIT_ABIS[j];
                                    if ("armeabi-v7a".equals(var)) {
                                        j++;
                                        continue;
                                    }
                                    optAbis[i++] = Build.SUPPORTED_32_BIT_ABIS[j++];
                                } while (i < optAbis.length);
                            }
                        }
                    }
                    if(optAbis == null){
                        optAbis = Build.SUPPORTED_32_BIT_ABIS;
                    }
                    int abiIndex = NativeLibraryHelper.findSupportedAbi.call(handle, optAbis);
                    if (abiIndex >= 0) {
                        abi = optAbis[abiIndex];
                    }
                }
            }
            if (abi == null) {
                VLog.e(TAG, "Not match any abi [%s].", apkFile.getAbsolutePath());
                return -1;
            }
            int targetCount = -1;
            List<String> soList = null;
            if(soMap != null){
                soList = soMap.get(abi);
                targetCount = soList.size();
            }
            if(NativeLibraryHelper.Handle.extractNativeLibs != null){
                if(!NativeLibraryHelper.Handle.extractNativeLibs.get(handle)){
                    NativeLibraryHelper.Handle.extractNativeLibs.set(handle, true);
                }
            }
            int ret = NativeLibraryHelper.copyNativeBinaries.call(handle, sharedLibraryDir, abi);
            int count = FileUtils.count(sharedLibraryDir);
            if (count == 0 && count < targetCount) {
                VLog.w(TAG, "copyNativeBinaries fail:%d < %d", count, targetCount);
                return copyNativeBinariesInner(apkFile, sharedLibraryDir, soList) ? INSTALL_SUCCEEDED : INSTALL_FAILED_INVALID_APK;
            }
            return ret;
        } catch (Throwable e) {
            VLog.d(TAG, "copyNativeBinaries with error : %s", e.getLocalizedMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private static boolean copyNativeBinariesInner(File apk, File sharedLibraryDir, String abi){
        try {
            ZipFile apkFile = new ZipFile(apk);
            Enumeration<? extends ZipEntry> entries = apkFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (name.startsWith("lib/") && !entry.isDirectory() && name.endsWith(".so")) {
                    String supportedAbi = name.substring(name.indexOf("/") + 1, name.lastIndexOf("/"));
                    if(TextUtils.equals(supportedAbi, abi)){
                        int index = name.lastIndexOf("/");
                        String filename = name.substring(index);
                        File file = new File(sharedLibraryDir, filename);
                        InputStream inputStream = apkFile.getInputStream(entry);
                        FileUtils.copyFile(inputStream, file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean copyNativeBinariesInner(File apk, File sharedLibraryDir, List<String> soList){
        try {
            ZipFile apkFile = new ZipFile(apk);
            Enumeration<? extends ZipEntry> entries = apkFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (soList.contains(name)) {
                    int index = name.lastIndexOf("/");
                    String filename = name.substring(index);
                    File file = new File(sharedLibraryDir, filename);
                    InputStream inputStream = apkFile.getInputStream(entry);
                    FileUtils.copyFile(inputStream, file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean containAbi(String[] supportedABIs, String abi) {
        for (String supportedAbi : supportedABIs) {
            if (TextUtils.equals(abi, supportedAbi)) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean is64bitAbi(String abi) {
        return "arm64-v8a".equals(abi)
                || "x86_64".equals(abi)
                || "mips64".equals(abi);
    }

    public static boolean is32bitAbi(String abi) {
        return "armeabi".equals(abi)
                || "armeabi-v7a".equals(abi)
                || "mips".equals(abi)
                || "x86".equals(abi);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean contain64bitAbi(Set<String> supportedABIs) {
        for (String supportedAbi : supportedABIs) {
            if (is64bitAbi(supportedAbi)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contain32bitAbi(Set<String> abiList) {
        for (String supportedAbi : abiList) {
            if (is32bitAbi(supportedAbi)) {
                return true;
            }
        }
        return false;
    }


    public static Map<String, List<String>> getSoMapForApk(String apk) {
        Map<String, List<String>> map = new HashMap<>();
        try {
            ZipFile apkFile = new ZipFile(apk);
            Enumeration<? extends ZipEntry> entries = apkFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (name.startsWith("lib/") && !entry.isDirectory() && name.endsWith(".so")) {
                    String supportedAbi = name.substring(name.indexOf("/") + 1, name.lastIndexOf("/"));
                    List<String> list = map.get(supportedAbi);
                    if(list == null){
                        list = new ArrayList<>();
                        map.put(supportedAbi, list);
                    }
                    list.add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static Set<String> getSupportAbiList(String apk) {
        try {
            ZipFile apkFile = new ZipFile(apk);
            Enumeration<? extends ZipEntry> entries = apkFile.entries();
            Set<String> supportedABIs = new HashSet<String>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (name.startsWith("lib/") && !entry.isDirectory() && name.endsWith(".so")) {
                    String supportedAbi = name.substring(name.indexOf("/") + 1, name.lastIndexOf("/"));
                    supportedABIs.add(supportedAbi);
                }
            }
            return supportedABIs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }
}
