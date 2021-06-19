package mirror.android.app;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefObject;
import mirror.RefStaticMethod;
import mirror.RefStaticObject;

public class WallpaperManager {
    public static Class<?> TYPE = RefClass.load(WallpaperManager.class, "android.app.WallpaperManager");

    public static RefStaticObject<Object> sGlobals;

    public static class Globals {
        public static Class<?> TYPE = RefClass.load(Globals.class, "android.app.WallpaperManager$Globals");
        public static RefObject<Object> mService;
    }
}
