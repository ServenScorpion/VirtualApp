package com.lody.virtual.client.hook.base;

import android.os.IInterface;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Lody
 */

public abstract class ResultBinderMethodProxy extends AutoResultStaticMethodProxy{

    public ResultBinderMethodProxy(String name) {
        super(name);
    }

    @Override
    public Object call(Object who, Method method, Object... args) throws Throwable {
        final IInterface base = (IInterface) super.call(who, method, args);
        return newProxyInstance(base, createProxy(base));
    }

    public Object newProxyInstance(final IInterface iInterface, final InvocationHandler proxy) {
        return Proxy.newProxyInstance(iInterface.getClass().getClassLoader(), iInterface.getClass().getInterfaces(), proxy);
    }

    public abstract InvocationHandler createProxy(final IInterface base);
}