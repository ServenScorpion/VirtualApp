package com.lody.virtual.client.core;

import java.io.File;

public interface SettingHandler {
    boolean isDisableDlOpen(String packageName, String apkPath);

    boolean isUseRealDataDir(String packageName);

    boolean isDisableNotCopyApk(String packageName, File apkPath);

    boolean isUseOwnLibraryFiles(String packageName, String apkPath);
}
