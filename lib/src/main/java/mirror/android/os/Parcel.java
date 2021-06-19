package mirror.android.os;

import mirror.RefClass;
import mirror.RefStaticInt;

public class Parcel {
    public static Class<?> TYPE = RefClass.load(Parcel.class, android.os.Parcel.class);

    public static RefStaticInt VAL_PARCELABLE;

    public static RefStaticInt VAL_PARCELABLEARRAY;
}
