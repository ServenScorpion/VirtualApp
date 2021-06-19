package com.lody.virtual.helper.compat;

import android.content.Intent;

import static android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

/**
 * @author Lody
 */
public class IntentCompat {

    public static final int IMMUTABLE_FLAGS = FLAG_GRANT_READ_URI_PERMISSION
            | FLAG_GRANT_WRITE_URI_PERMISSION | FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            | FLAG_GRANT_PREFIX_URI_PERMISSION;

    public static String getPackageName(Intent intent) {
        if (intent == null) {
            return null;
        }
        if (intent.getPackage() != null) {
            return intent.getPackage();
        }
        if (intent.getComponent() != null) {
            return intent.getComponent().getPackageName();
        }

        return null;
    }
}
