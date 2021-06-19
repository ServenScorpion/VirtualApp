package com.lody.virtual.client.core;

import com.lody.virtual.client.hook.base.BinderInvocationStub;

import java.util.HashMap;
import java.util.Map;

public class ServiceLocalManager {
    private static final Map<String, BinderInvocationStub> sCache = new HashMap<>();

    public static void addService(String name, BinderInvocationStub proxy) {
        synchronized (sCache){
            sCache.put(name, proxy);
        }
    }

    public static BinderInvocationStub getService(String name){
        BinderInvocationStub proxy;
        synchronized (sCache){
            proxy = sCache.get(name);
        }
        return proxy;
    }
}
