package com.lody.virtual.client.hook.base;

import com.lody.virtual.helper.utils.Reflect;

import java.lang.reflect.Method;

/**
 * @author Lody
 */

public class AutoResultStaticMethodProxy extends StaticMethodProxy {

	public AutoResultStaticMethodProxy(String name) {
		super(name);
	}

	@Override
	public Object call(Object who, Method method, Object... args) throws Throwable {
		return getDefaultValue(who, method, args);
	}

    public Object getDefaultValue(Object who, Method method, Object... args){
        Class<?> type =  Reflect.wrapper(method.getReturnType());
        if (type == null) {
            return 0;
        } else if (type.isPrimitive()) {
            if (Boolean.class == type) {
                return false;
            } else if (Integer.class == type) {
                return 0;
            } else if (Long.class == type) {
                return 0L;
            } else if (Short.class == type) {
                return (short)0;
            } else if (Byte.class == type) {
                return (byte)0;
            } else if (Double.class == type) {
                return 0d;
            } else if (Float.class == type) {
                return 0f;
            } else if (Character.class == type) {
                return '\0';
            }
        }
        return null;
    }
}
