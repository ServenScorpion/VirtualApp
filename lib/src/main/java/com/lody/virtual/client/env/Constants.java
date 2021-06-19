package com.lody.virtual.client.env;

import android.content.Intent;

import com.lody.virtual.client.stub.ShortcutHandleActivity;
import com.lody.virtual.os.VUserManager;

/**
 * @author Lody
 *
 */
public class Constants {

	public static final int OUTSIDE_APP_UID = 9999;

	public static final String EXTRA_USER_HANDLE = "android.intent.extra.user_handle";
    public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.package_name";

	/**
	 * If an apk declared the "fake-signature" attribute on its Application TAG,
	 * we will use its signature instead of the real signature.
	 *
	 * For more detail, please see :
	 * https://github.com/microg/android_packages_apps_GmsCore/blob/master/
	 * patches/android_frameworks_base-M.patch.
	 */
	public static final String FEATURE_FAKE_SIGNATURE = "fake-signature";


    public static final String ACTION_NEW_TASK_CREATED = "virtual.intent.action.APP_LAUNCHED";
    public static final String ACTION_PACKAGE_WILL_ADDED = "virtual.intent.action.PACKAGE_WILL_ADDED";
	public static final String ACTION_PACKAGE_ADDED = "virtual." + Intent.ACTION_PACKAGE_ADDED;
	public static final String ACTION_PACKAGE_REMOVED = "virtual." + Intent.ACTION_PACKAGE_REMOVED;
	public static final String ACTION_PACKAGE_REPLACED = "virtual." + Intent.ACTION_PACKAGE_REPLACED;
	public static final String ACTION_PACKAGE_CHANGED = "virtual." + Intent.ACTION_PACKAGE_CHANGED;
	public static final String ACTION_USER_ADDED = "virtual." + VUserManager.ACTION_USER_ADDED;
	public static final String ACTION_USER_REMOVED = "virtual." + VUserManager.ACTION_USER_REMOVED;
	public static final String ACTION_USER_INFO_CHANGED = "virtual." + VUserManager.ACTION_USER_INFO_CHANGED;
	public static final String ACTION_USER_STARTED = "virtual." + VUserManager.ACTION_USER_STARTED;
	public static final String ACTION_BOOT_COMPLETED = "virtual." + Intent.ACTION_BOOT_COMPLETED;
	public static final String ACTION_MEDIA_SCANNER_SCAN_FILE = "virtual." + Intent.ACTION_MEDIA_SCANNER_SCAN_FILE;
	public static final String ACTION_WALLPAPER_CHANGED = "virtual." + Intent.ACTION_WALLPAPER_CHANGED;
    /**
	 * Server process name of VA
	 */
	public static String SERVER_PROCESS_NAME = ":x";

	public static String HELPER_PROCESS_NAME = ":helper";
	/**
	 * The activity who handle the shortcut.
	 */
	public static String SHORTCUT_PROXY_ACTIVITY_NAME = ShortcutHandleActivity.class.getName();

	public static String ACTION_SHORTCUT = ".virtual.action.shortcut";

	public static String ACTION_BADGER_CHANGE = ".virtual.action.BADGER_CHANGE";

	public static String NOTIFICATION_CHANNEL = "virtual_default";

    public static String NOTIFICATION_DAEMON_CHANNEL = "virtual_daemon";

	public static String NOTIFICATION_LIGHT_CHANNEL = "virtual_light";

	public static String NOTIFICATION_SYSTEM_CHANNEL = "virtual_system";

	public static String NOTIFICATION_GROUP_DAEMON = "virtual_group_daemon";

	public static String NOTIFICATION_GROUP_APP = "virtual_group_app";

	public static String NOTIFICATION_GROUP_SYSTEM = "virtual_group_system";

	public static String NOTIFICATION_GROUP_PHONE = "virtual_group_phone";

}
