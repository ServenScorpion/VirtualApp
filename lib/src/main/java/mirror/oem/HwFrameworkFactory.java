package mirror.oem;

import mirror.RefClass;
import mirror.RefStaticMethod;

public class HwFrameworkFactory {
    public static Class<?> TYPE = RefClass.load(HwFrameworkFactory.class, "android.common.HwFrameworkFactory");
    public static RefStaticMethod<Object> getHwApiCacheManagerEx;
}
