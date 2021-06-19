package com.lody.virtual.helper.compat;

/**
 * @author Lody
 */

public class ContentResolverCompat {

    /* Extensions to API. TODO: Not clear if we will keep these as public flags. */
    /**
     * User-specified flag for expected upload size.
     */
    public static final String SYNC_EXTRAS_EXPECTED_UPLOAD = "expected_upload";

    /**
     * User-specified flag for expected download size.
     */
    public static final String SYNC_EXTRAS_EXPECTED_DOWNLOAD = "expected_download";

    /**
     * Priority of this sync with respect to other syncs scheduled for this application.
     */
    public static final String SYNC_EXTRAS_PRIORITY = "sync_priority";

    /**
     * Flag to allow sync to occur on metered network.
     */
    public static final String SYNC_EXTRAS_DISALLOW_METERED = "allow_metered";

    public static final int SYNC_OBSERVER_TYPE_STATUS = 1 << 3;


    public static final int SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS = 1;

    public static final int SYNC_ERROR_AUTHENTICATION = 2;

    public static final int SYNC_ERROR_IO = 3;

    public static final int SYNC_ERROR_PARSE = 4;

    public static final int SYNC_ERROR_CONFLICT = 5;

    public static final int SYNC_ERROR_TOO_MANY_DELETIONS = 6;

    public static final int SYNC_ERROR_TOO_MANY_RETRIES = 7;

    public static final int SYNC_ERROR_INTERNAL = 8;

    private static final String[] SYNC_ERROR_NAMES = new String[] {
            "already-in-progress",
            "authentication-error",
            "io-error",
            "parse-error",
            "conflict",
            "too-many-deletions",
            "too-many-retries",
            "internal-error",
    };

    public static String syncErrorToString(int error) {
        if (error < 1 || error > SYNC_ERROR_NAMES.length) {
            return String.valueOf(error);
        }
        return SYNC_ERROR_NAMES[error - 1];
    }
}
