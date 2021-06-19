package com.lody.virtual.client;

import android.os.Build;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.os.VEnvironment;

import java.io.File;
import java.io.IOException;

/**
 * After android O and targetSdkVersion >= 26, we cannot access '/proc/stat' :(
 *
 * @author Lody
 */
public class LinuxCompat {

    public static void forgeProcDriver(boolean is64bit) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        if (VirtualCore.get().isPluginEngine()) {
            return;
        }
        if (FileUtils.canRead("/proc/stat")) {
            return;
        }
        File procDir = is64bit ? VEnvironment.getProcDirectory64() : VEnvironment.getProcDirectory();
        File forgeStatFile = new File(procDir, "stat");
        if (!forgeStatFile.exists()) {
            try {
                forgeProcStat(forgeStatFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        NativeEngine.redirectFile("/proc/stat", forgeStatFile.getPath());

    }

    private static void forgeProcStat(File forgeFile) throws IOException {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        StringBuilder sb = new StringBuilder();
        sb.append("cpu  8071384 2507746 5791644 22617170 15498 0 160238 0 0 0\n");
        for (int i = 0; i < cpuCount; i++) {
            sb.append("cpu").append(i).append(" 1424348 285752 1232854 19501475 14634 0 86934 0 0 0\n");
        }
        sb.append("intr 472407077 0 0 0 117600205 0 31537966 99671 0 333482 0 100995 6 58021 2660550 4476 4756954 1952170 5377186 0 0 0 40869 0\n");
        sb.append("ctxt 856241281\n");
        sb.append("btime 1533516565\n");
        sb.append("processes 932184\n");
        sb.append("procs_running 1\n");
        sb.append("procs_blocked 0\n");
        sb.append("softirq 229332891 271435 62396148 1767168 5025031 2687531 60368 28179007 57936444 0 71009759");
        FileUtils.writeToFile(sb.toString().getBytes(), forgeFile);
    }

}
