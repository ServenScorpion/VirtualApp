package io.virtualapp.utils;


import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.helper.utils.Reflect;

import java.lang.reflect.Method;

import mirror.android.os.ServiceManager;

public class IBinderTool {

    public static String tag = "IBinderTool";

    public static void setTag(String tag) {
        IBinderTool.tag = tag;
    }

    public static void printIBinder(String clazz) {
        try {
            Class<?> cls = Class.forName(clazz);
            Method[] methods = cls.getDeclaredMethods();
            for (Method method : methods) {
                Log.i(tag, "method=" + method);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void printAllService() {
        String[] srvs = Reflect.on(ServiceManager.TYPE).call("listServices").get();
        for (String srv : srvs) {
            IBinder service = Reflect.on(ServiceManager.TYPE).call("getService", srv).get();
            if (service == null) {
                Log.w(tag, "srv=" + srv + " no find ");
            } else {
                try {
                    Log.i(tag, "srv=" + srv + "@" + service.getInterfaceDescriptor());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
