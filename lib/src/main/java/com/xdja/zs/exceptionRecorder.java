package com.xdja.zs;

import android.content.Context;
import android.os.Environment;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

public class exceptionRecorder {

    public static String SSBEL = "saftySandBoxExceptionLog";

    private static File ensureCreated(File folder) {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public static String stampToDate(Long lt){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }

    public static String getExceptionRecordPath()
    {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + SSBEL;
    }

    public static void recordException(Throwable exception)
    {
        String logPath = getExceptionRecordPath();
        ensureCreated(new File(logPath));
        String processName = VirtualCore.get().getProcessName();
        String replaceName = null;
        if(processName.contains(":")) {
            replaceName = processName.replace(":","_");
        }
        String logFile = logPath + "/" + replaceName + "_" + stampToDate(System.currentTimeMillis()) + ".log";

        File file = new File(logFile);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter fileWriter = new FileWriter(file);
            exception.printStackTrace(new PrintWriter(fileWriter));
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class defaulUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            recordException(e);
        }
    }
}
