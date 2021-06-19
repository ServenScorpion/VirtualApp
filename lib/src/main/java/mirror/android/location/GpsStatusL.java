package mirror.android.location;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;

public class GpsStatusL {
    public static Class<?> TYPE = RefClass.load(GpsStatusL.class, android.location.GpsStatus.class);

    @MethodParams({int.class, int[].class, float[].class, float[].class, float[].class, int[].class, int[].class, int[].class})
    public static RefMethod<Void> setStatus;

}
