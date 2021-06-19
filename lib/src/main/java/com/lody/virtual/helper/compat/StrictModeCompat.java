package com.lody.virtual.helper.compat;

import mirror.android.os.StrictMode;

public class StrictModeCompat {
    public static int DETECT_VM_FILE_URI_EXPOSURE = StrictMode.DETECT_VM_FILE_URI_EXPOSURE == null ?
            (0x20 << 8) : StrictMode.DETECT_VM_FILE_URI_EXPOSURE.get();

    public static int PENALTY_DEATH_ON_FILE_URI_EXPOSURE = StrictMode.PENALTY_DEATH_ON_FILE_URI_EXPOSURE == null ?
            (0x04 << 24) : StrictMode.PENALTY_DEATH_ON_FILE_URI_EXPOSURE.get();

    public static boolean disableDeathOnFileUriExposure(){
        try {
            StrictMode.disableDeathOnFileUriExposure.call();
            return true;
        } catch (Throwable e) {
            try {
                int sVmPolicyMask = StrictMode.sVmPolicyMask.get();
                sVmPolicyMask &= ~(DETECT_VM_FILE_URI_EXPOSURE | PENALTY_DEATH_ON_FILE_URI_EXPOSURE);
                StrictMode.sVmPolicyMask.set(sVmPolicyMask);
                return true;
            } catch (Throwable e2) {
                e2.printStackTrace();
            }
        }
        return false;
    }
}
