package mirror.android.app.usage;

import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefLong;

/**
 * @author Lody
 */
public class StorageStats {
    public static Class<?> TYPE = RefClass.load(StorageStats.class, "android.app.usage.StorageStats");

    public static RefLong codeBytes;
    public static RefLong dataBytes;
    public static RefLong cacheBytes;
    public static RefConstructor<android.app.usage.StorageStats> ctor;
}
