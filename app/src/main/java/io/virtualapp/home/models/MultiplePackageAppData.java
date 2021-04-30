package io.virtualapp.home.models;

import android.graphics.drawable.Drawable;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.pm.parser.VPackage;

/**
 * @author LodyChen
 */

public class MultiplePackageAppData extends AppData {

    public InstalledAppInfo appInfo;
    public int userId;
    public Drawable icon;
    public String name;
    public String packageName;

    public String versionName;

    public MultiplePackageAppData(PackageAppData target, int userId) {
        this.userId = userId;
        this.appInfo = VirtualCore.get().getInstalledAppInfo(target.packageName, 0);
        this.isFirstOpen = !appInfo.isLaunched(userId);
        if (target.icon != null) {
            Drawable.ConstantState state = target.icon.getConstantState();
            if (state != null) {
                icon = state.newDrawable();
            }
        }
        name = target.name;
        packageName = target.packageName;

        this.xposedModule = target.getXposedModule();
        this.versionName = target.getVersionName();
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public boolean isFirstOpen() {
        return isFirstOpen;
    }

    @Override
    public Drawable getIcon() {
        return icon;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public boolean canReorder() {
        return true;
    }

    @Override
    public boolean canLaunch() {
        return true;
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    @Override
    public boolean canCreateShortcut() {
        return true;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public VPackage.XposedModule getXposedModule() {
        return xposedModule;
    }
}
