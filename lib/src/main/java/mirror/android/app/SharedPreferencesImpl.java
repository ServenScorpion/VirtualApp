package mirror.android.app;

import java.io.File;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefConstructor;

public class SharedPreferencesImpl {
    public static Class<?> TYPE = RefClass.load(SharedPreferencesImpl.class, "android.app.SharedPreferencesImpl");

    @MethodParams({File.class, int.class})
    public static RefConstructor<Object> ctor;
}
