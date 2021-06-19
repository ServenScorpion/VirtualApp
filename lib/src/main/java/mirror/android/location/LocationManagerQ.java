package mirror.android.location;

import android.util.ArrayMap;

import mirror.RefClass;
import mirror.RefObject;

public class LocationManagerQ {
    public static Class<?> TYPE = RefClass.load(LocationManagerQ.class, "android.location.LocationManager");
    public static RefObject<ArrayMap> mGnssNmeaListeners;
    public static RefObject<ArrayMap> mGnssStatusListeners;
    public static RefObject<ArrayMap> mGpsNmeaListeners;
    public static RefObject<ArrayMap> mGpsStatusListeners;
    public static RefObject<ArrayMap> mListeners;
}
