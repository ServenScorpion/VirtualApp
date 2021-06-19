package com.lody.virtual.client.stub;

import java.util.Locale;

/**
 * @author Lody
 */

public class StubManifest {

    public static String PACKAGE_NAME = null;
    public static String PACKAGE_NAME_64BIT = null;

    public static String STUB_ACTIVITY = ShadowActivity.class.getName();
    public static String STUB_DIALOG = ShadowDialogActivity.class.getName();
    public static String STUB_CP = ShadowContentProvider.class.getName();
    public static String STUB_JOB = ShadowJobService.class.getName();
    public static String STUB_SERVICE = ShadowService.class.getName();

    public static String RESOLVER_ACTIVITY = ResolverActivity.class.getName();

    public static String STUB_CP_AUTHORITY = null;
    public static String STUB_CP_AUTHORITY_64BIT = null;

    public static String PROXY_CP_AUTHORITY_OUTSIDE = null;
    public static String PROXY_CP_AUTHORITY = null;
    public static String PROXY_CP_AUTHORITY_64BIT = null;


    public static int STUB_COUNT = 100;
    public static String[] PRIVILEGE_APPS = new String[]{
    };

    public static boolean isStubActivity(String clazz) {
        return clazz != null && clazz.startsWith(STUB_ACTIVITY + "$P");
    }

    public static String getStubActivityName(int index) {
        return String.format(Locale.ENGLISH, "%s$P%d", STUB_ACTIVITY, index);
    }

    public static String getStubDialogName(int index) {
        return String.format(Locale.ENGLISH, "%s$P%d", STUB_DIALOG, index);
    }

    public static String getStubContentProviderName(int index) {
        return String.format(Locale.ENGLISH, "%s$P%d", STUB_CP, index);
    }

    public static String getStubServiceName(int index) {
        return String.format(Locale.ENGLISH, "%s$P%d", STUB_SERVICE, index);
    }

    public static String getStubAuthority(int index, boolean is64bit) {
        return String.format(Locale.ENGLISH, "%s%d", is64bit ? STUB_CP_AUTHORITY_64BIT : STUB_CP_AUTHORITY, index);
    }

    public static String getProxyAuthority(boolean is64bit) {
        return is64bit ? PROXY_CP_AUTHORITY_64BIT : PROXY_CP_AUTHORITY;
    }

    public static String getStubPackageName(boolean is64bit) {
        return is64bit ? PACKAGE_NAME_64BIT : PACKAGE_NAME;
    }

    public static boolean isHostPackageName(String packageName) {
        return PACKAGE_NAME.equals(packageName)
                || PACKAGE_NAME_64BIT.equals(packageName);
    }

    public static boolean isHostPluginPackageName(String packageName) {
        if (PACKAGE_NAME_64BIT == null) {
            return false;
        }
        return PACKAGE_NAME_64BIT.equals(packageName);
    }

    public static final String[] REQUIRED_FRAMEWORK = {
            "com.android.location.provider"
    };

}
