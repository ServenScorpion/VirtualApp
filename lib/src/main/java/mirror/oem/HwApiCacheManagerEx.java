package mirror.oem;

import android.content.pm.PackageManager;

import mirror.RefClass;
import mirror.RefObject;
import mirror.RefStaticMethod;

public class HwApiCacheManagerEx {
    public static Class<?> TYPE = RefClass.load(HwApiCacheManagerEx.class, "huawei.android.app.HwApiCacheMangerEx");
    public static RefStaticMethod<Object> getDefault;
    public static RefObject<PackageManager> mPkg;
}