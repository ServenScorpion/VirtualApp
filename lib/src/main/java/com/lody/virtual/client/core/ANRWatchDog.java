package com.lody.virtual.client.core;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.lody.virtual.client.env.VirtualRuntime;

public class ANRWatchDog extends Thread {
    private static final int MESSAGE_WATCHDOG_TIME_TICK = 0;
    private static final int ANR_TIMEOUT = 5000;


    private static int lastTimeTick = -1;
    private static int timeTick = 0;
    private boolean makeCrash;

    public ANRWatchDog(boolean makeCrash) {
        this.makeCrash = makeCrash;
    }

    public ANRWatchDog() {
        this(false);
    }

    @SuppressLint("HandlerLeak")
    private final Handler watchDogHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            timeTick++;
            timeTick = timeTick % Integer.MAX_VALUE;
        }
    };

    @Override
    public void run() {
        while (true) {
            watchDogHandler.sendEmptyMessage(MESSAGE_WATCHDOG_TIME_TICK);
            try {
                Thread.sleep(ANR_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (timeTick == lastTimeTick) {
                triggerANR();
            } else {
                lastTimeTick = timeTick;
            }
        }
    }

    private void triggerANR() {
        if (makeCrash) {
            throw new ANRException();
        } else {
            try {
                throw new ANRException();
            } catch (ANRException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ANRException extends RuntimeException {
        public ANRException() {
            super("========= ANR =========" + getAnrDesc());
            Thread mainThread = Looper.getMainLooper().getThread();
            setStackTrace(mainThread.getStackTrace());
        }
        private static String getAnrDesc() {
            return VirtualCore.get().isVAppProcess() ? VirtualRuntime.getProcessName() : VirtualCore.get().getProcessName();
        }
    }
}  