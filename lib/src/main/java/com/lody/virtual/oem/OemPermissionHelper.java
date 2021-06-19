package com.lody.virtual.oem;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import com.lody.virtual.helper.compat.BuildCompat;

import java.util.Arrays;
import java.util.List;

/**
 * @author Lody
 */
public class OemPermissionHelper {
    private static List<ComponentName> EMUI_AUTO_START_COMPONENTS = Arrays.asList(
            new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"),
            new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupAwakedAppListActivity")
    );

    private static List<ComponentName> FLYME_AUTO_START_COMPONENTS = Arrays.asList(
            new ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"),
            new ComponentName("com.meizu.safe", "com.meizu.safe.security.HomeActivity")
    );

    private static List<ComponentName> VIVO_AUTO_START_COMPONENTS = Arrays.asList(
            new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity"),
            new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"),
            new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewActivity")
    );


    public static Intent getPermissionActivityIntent(Context context) {
        BuildCompat.ROMType romType = BuildCompat.getROMType();
        switch (romType) {
            case EMUI: {
                for (ComponentName component : EMUI_AUTO_START_COMPONENTS) {
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setComponent(component);
                    if (verifyIntent(context, intent)) {
                        return intent;
                    }
                }
                break;
            }
            case MIUI: {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                if (verifyIntent(context, intent)) {
                    return intent;
                }
                break;
            }
            case FLYME: {
                for (ComponentName component : FLYME_AUTO_START_COMPONENTS) {
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setComponent(component);
                    if (verifyIntent(context, intent)) {
                        return intent;
                    }
                }
                break;
            }
            case COLOR_OS: {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClassName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity");
                if (verifyIntent(context, intent)) {
                    return intent;
                }
                break;
            }
            case LETV: {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClassName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity");
                if (verifyIntent(context, intent)) {
                    return intent;
                }
                break;
            }
            case VIVO: {
                for (ComponentName component : VIVO_AUTO_START_COMPONENTS) {
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setComponent(component);
                    if (verifyIntent(context, intent)) {
                        return intent;
                    }
                }
                break;
            }
            case _360: {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClassName("com.qihoo360.mobilesafe", "com.qihoo360.mobilesafe.ui.index.AppEnterActivity");
                if (verifyIntent(context, intent)) {
                    return intent;
                }
                break;
            }

        }
        return null;
    }

    private static boolean verifyIntent(Context context, Intent intent) {
        ResolveInfo info = context.getPackageManager().resolveActivity(intent, 0);
        return info != null && info.activityInfo != null && info.activityInfo.exported;
    }
}
