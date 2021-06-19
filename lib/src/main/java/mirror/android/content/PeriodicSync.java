package mirror.android.content;


import android.os.Bundle;

import com.lody.virtual.helper.compat.ObjectsCompat;

import mirror.RefClass;
import mirror.RefLong;

public class PeriodicSync {
    public static Class<?> TYPE = RefClass.load(PeriodicSync.class, android.content.PeriodicSync.class);
    public static RefLong flexTime;

    public static android.content.PeriodicSync clone(android.content.PeriodicSync sync) {
        android.content.PeriodicSync clone = new android.content.PeriodicSync(sync.account, sync.authority, sync.extras, sync.period);
        flexTime.set(clone, flexTime.get(sync));
        return clone;
    }
    public static boolean syncExtrasEquals(Bundle b1, Bundle b2) {
        if (b1.size() != b2.size()) {
            return false;
        }
        if (b1.isEmpty()) {
            return true;
        }
        for (String key : b1.keySet()) {
            if (!b2.containsKey(key)) {
                return false;
            }
            // Null check. According to ContentResolver#validateSyncExtrasBundle null-valued keys
            // are allowed in the bundle.
            if (!ObjectsCompat.equals(b1.get(key), b2.get(key))) {
                return false;
            }
        }
        return true;
    }
}