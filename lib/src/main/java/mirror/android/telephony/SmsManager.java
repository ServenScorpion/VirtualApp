package mirror.android.telephony;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;

public class SmsManager {
    public static Class<?> TYPE = RefClass.load(SmsManager.class, android.telephony.SmsManager.class);
    @MethodParams(boolean.class)
    public static RefMethod<Void> setAutoPersisting;
    public static RefMethod<Boolean> getAutoPersisting;
}
