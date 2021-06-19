package com.lody.virtual.helper.compat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.RequestPermissionsActivity;
import com.lody.virtual.server.IRequestPermissionsResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionCompat {
    public static Set<String> DANGEROUS_PERMISSION = new HashSet<String>() {{
        // CALENDAR group
        add(Manifest.permission.READ_CALENDAR);
        add(Manifest.permission.WRITE_CALENDAR);

        // CAMERA
        add(Manifest.permission.CAMERA);

        // CONTACTS
        add(Manifest.permission.READ_CONTACTS);
        add(Manifest.permission.WRITE_CONTACTS);
        add(Manifest.permission.GET_ACCOUNTS);

        // LOCATION
        add(Manifest.permission.ACCESS_FINE_LOCATION);
        add(Manifest.permission.ACCESS_COARSE_LOCATION);

        // PHONE
        add(Manifest.permission.READ_PHONE_STATE);
        add(Manifest.permission.CALL_PHONE);
        if (Build.VERSION.SDK_INT >= 16) {
            add(Manifest.permission.READ_CALL_LOG);
            add(Manifest.permission.WRITE_CALL_LOG);
        }
        add(Manifest.permission.ADD_VOICEMAIL);
        add(Manifest.permission.USE_SIP);
        add(Manifest.permission.PROCESS_OUTGOING_CALLS);

        // SMS
        add(Manifest.permission.SEND_SMS);
        add(Manifest.permission.RECEIVE_SMS);
        add(Manifest.permission.READ_SMS);
        add(Manifest.permission.RECEIVE_WAP_PUSH);
        add(Manifest.permission.RECEIVE_MMS);

        add(Manifest.permission.RECORD_AUDIO);
        // STORAGE
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= 16) {
            add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (Build.VERSION.SDK_INT >= 20) {
            // SENSORS
            add(Manifest.permission.BODY_SENSORS);
        }
    }};

    public static String[] findDangerousPermissions(List<String> permissions) {
        if (permissions == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (String per : permissions) {
            if (DANGEROUS_PERMISSION.contains(per)) {
                list.add(per);
            }
        }
        return list.toArray(new String[0]);
    }

    public static String[] findDangrousPermissions(String[] permissions) {
        if (permissions == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (String permission : permissions) {
            if (DANGEROUS_PERMISSION.contains(permission)) {
                list.add(permission);
            }
        }
        return list.toArray(new String[0]);
    }

    public static boolean isCheckPermissionRequired(int targetSdkVersion) {
        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || VirtualCore.get().getTargetSdkVersion() < android.os.Build.VERSION_CODES.M) {
            return false;
        }
        return targetSdkVersion < android.os.Build.VERSION_CODES.M;
    }

    public static boolean checkPermissions(String[] permissions, boolean is64Bit) {
        if (permissions == null) {
            return true;
        }
        for (String per : permissions) {
            if (!VirtualCore.get().checkSelfPermission(per, is64Bit)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isRequestGranted(int[] grantResults) {
        boolean allGranted = true;
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                allGranted = false;
                break;
            }
        }
        return allGranted;
    }

    public interface CallBack {
        boolean onResult(int requestCode, String[] permissions, int[] grantResults);
    }

    public static void startRequestPermissions(Context context, boolean is64bit,
                                               String[] permissions, final CallBack callBack) {
        RequestPermissionsActivity.request(context, is64bit, permissions, new IRequestPermissionsResult.Stub() {
            @Override
            public boolean onResult(int requestCode, String[] permissions, int[] grantResults) {
                if (callBack != null) {
                    return callBack.onResult(requestCode, permissions, grantResults);
                }
                return false;
            }
        });

    }
}
