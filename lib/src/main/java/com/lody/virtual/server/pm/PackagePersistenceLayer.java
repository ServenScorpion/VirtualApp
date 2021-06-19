package com.lody.virtual.server.pm;

import android.os.Parcel;

import com.lody.virtual.helper.PersistenceLayer;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.server.pm.legacy.PackageSettingV1;
import com.lody.virtual.server.pm.parser.VPackage;

import java.util.Arrays;

import static com.lody.virtual.remote.InstalledAppInfo.MODE_APP_COPY_APK;
import static com.lody.virtual.remote.InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK;

/**
 * @author Lody
 */

class PackagePersistenceLayer extends PersistenceLayer {

    private static final char[] MAGIC = {'v', 'p', 'k', 'g'};
    private static final int CURRENT_VERSION = PackageSetting.CURRENT_VERSION;
    public boolean changed = false;

    private VAppManagerService mService;

    PackagePersistenceLayer(VAppManagerService service) {
        super(VEnvironment.getPackageListFile());
        mService = service;
    }

    @Override
    public int getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public void writeMagic(Parcel p) {
        p.writeCharArray(MAGIC);
    }

    @Override
    public boolean verifyMagic(Parcel p) {
        char[] magic = p.createCharArray();
        return Arrays.equals(magic, MAGIC);
    }


    @Override
    public void writePersistenceData(Parcel p) {
        synchronized (PackageCacheManager.PACKAGE_CACHE) {
            p.writeInt(PackageCacheManager.PACKAGE_CACHE.size());
            for (VPackage pkg : PackageCacheManager.PACKAGE_CACHE.values()) {
                PackageSetting ps = (PackageSetting) pkg.mExtras;
                ps.writeToParcel(p, 0);
            }
        }
    }

    @Override
    public void readPersistenceData(Parcel p, int version) {
        int count = p.readInt();
        while (count-- > 0) {
            PackageSetting setting;
            if (version < PackageSetting.FIRST_V2_VERSION) {
                changed = true;
                PackageSettingV1 v1 = new PackageSettingV1();
                v1.readFromParcel(p, version);
                PackageSetting v2 = new PackageSetting();
                v2.packageName = v1.packageName;
                v2.appMode = v1.notCopyApk ? MODE_APP_USE_OUTSIDE_APK : MODE_APP_COPY_APK;
                v2.appId = v1.appId;
                v2.flag = v1.flag;
                v2.userState = v1.userState;
                v2.firstInstallTime = System.currentTimeMillis();
                v2.lastUpdateTime = v2.firstInstallTime;
                setting = v2;
            } else {
                setting = new PackageSetting(version, p);
            }
            if (!mService.loadPackage(setting)) {
                changed = true;
            }
        }
    }

    @Override
    public void onPersistenceFileDamage() {
        getPersistenceFile().delete();
        VAppManagerService.get().restoreFactoryState();
    }
}
