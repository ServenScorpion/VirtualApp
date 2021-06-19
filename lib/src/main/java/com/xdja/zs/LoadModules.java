package com.xdja.zs;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class LoadModules {

    private static String TAG = "wechat.LoadModules";

    /**
     * Module load result
     */
    enum ModuleLoadResult {
        DISABLED,
        NOT_EXIST,
        INVALID,
        SUCCESS,
        FAILED
    }

    public static ModuleLoadResult loadModule(final String moduleApkPath, String moduleOdexDir, String moduleLibPath,
                                              Application application) {
        if (!new File(moduleApkPath).exists()) {
            Log.e(TAG, moduleApkPath + " does not exist");
            return ModuleLoadResult.NOT_EXIST;
        }

        if(!moduleApkPath.contains("com.xdja.hookmodule")){
            return ModuleLoadResult.DISABLED;
        }

        Log.e(TAG, "Loading modules from " + moduleApkPath);

        ClassLoader hostClassLoader = LoadModules.class.getClassLoader();
        ClassLoader mcl = new DexClassLoader(moduleApkPath, moduleOdexDir, moduleLibPath, hostClassLoader);
        InputStream is = mcl.getResourceAsStream("assets/xposed_init");
        if (is == null) {
            Log.e(TAG,"assets/xposed_init not found in the APK");
            return ModuleLoadResult.INVALID;
        }

        BufferedReader moduleClassesReader = new BufferedReader(new InputStreamReader(is));
        try {
            String modulePolicy;
            while ((modulePolicy = moduleClassesReader.readLine()) != null) {
                modulePolicy = modulePolicy.trim();
                if (modulePolicy.isEmpty() || modulePolicy.startsWith("#"))
                    continue;

                try {
                    Log.e(TAG,"Hookmodule policy " + modulePolicy);
                    String[] str = modulePolicy.split("_");
                    if(!str[0].equals(application.getPackageName())){
                        return ModuleLoadResult.DISABLED;
                    }
                    Log.e(TAG,"Loading class " + str[1]);
                    Class<?> moduleClass = mcl.loadClass(str[1]);

                    final Object moduleInstance = moduleClass.newInstance();
                    Method m = moduleInstance.getClass().getMethod("startHook", Application.class);
                    Log.e(TAG,"Call method " + m.getName());
                    m.invoke(moduleInstance, application);

                    return ModuleLoadResult.SUCCESS;
                } catch (Throwable t) {
                    Log.e(TAG,t.toString());
                }
            }
        } catch (IOException e) {
            Log.e(TAG,e.toString());
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
        return ModuleLoadResult.FAILED;
    }
}

