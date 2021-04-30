package com.swift.sandhook;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Process;

import dalvik.system.VMRuntime;

public class SandHookConfig {

    public volatile static int SDK_INT = Build.VERSION.SDK_INT;
    //Debug status of hook target process
    public volatile static boolean DEBUG = true;
    //Enable compile with jit
    public volatile static boolean compiler = SDK_INT < 29;
    public volatile static ClassLoader initClassLoader;
    public volatile static int curUser = 0;
    public volatile static boolean delayHook = true;

    public volatile static String libSandHookPath;

    private static final String LIB_NAME = "sandhook";
    private static final String LIB_NAME_64 = "sandhook_64";

    public volatile static LibLoader libLoader = new LibLoader() {
        @SuppressLint("UnsafeDynamicallyLoadedCode")
        @Override
        public void loadLib() {
            if (SandHookConfig.libSandHookPath == null) {
                //System.loadLibrary("sandhook");
                if (is64bit()) {
                    System.loadLibrary(LIB_NAME_64);
                } else {
                    System.loadLibrary(LIB_NAME);
                }
            } else {
                System.load(SandHookConfig.libSandHookPath);
            }
        }
    };

    public static boolean is64bit() {
        try {
            Class.forName("dalvik.system.VMRuntime");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Process.is64Bit();
        }
        return VMRuntime.is64Bit.call(VMRuntime.getRuntime.call());
    }

    public interface LibLoader {
        void loadLib();
    }
}
