package io.virtualapp.home.models;

import android.graphics.drawable.Drawable;

import com.lody.virtual.server.pm.parser.VPackage;

/**
 * @author LodyChen
 */

public abstract class AppData {

    public boolean isFirstOpen;
    public boolean isLoading;

    public VPackage.XposedModule xposedModule;

    public boolean isLoading() {
        return isLoading;
    }


    public boolean isFirstOpen() {
        return isFirstOpen;
    }


    public Drawable getIcon() {
        return null;
    }


    public String getName() {
        return null;
    }


    public String getPackageName() {
        return null;
    }

    public String getVersionName() {
        return null;
    }

    public boolean canReorder() {
        return false;
    }


    public boolean canLaunch() {
        return false;
    }


    public boolean canDelete() {
        return false;
    }


    public boolean canCreateShortcut() {
        return false;
    }


    public int getUserId() {
        return 0;
    }

    public VPackage.XposedModule getXposedModule() {
        return xposedModule;
    }
}
