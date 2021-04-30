package io.virtualapp.home.models;

import android.graphics.drawable.Drawable;

/**
 * @author LodyChen
 */

public class AppInfo {
    public String packageName;
    public String path;
    public boolean cloneMode;
    public Drawable icon;
    public CharSequence name;
    public int cloneCount;
    public int targetSdkVersion;
    public String[] requestedPermissions;
}
