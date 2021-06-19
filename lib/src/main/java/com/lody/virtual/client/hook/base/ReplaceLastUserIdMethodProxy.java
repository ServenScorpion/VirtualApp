package com.lody.virtual.client.hook.base;

import com.lody.virtual.helper.utils.ArrayUtils;

import java.lang.reflect.Method;

public class ReplaceLastUserIdMethodProxy extends StaticMethodProxy {

    public ReplaceLastUserIdMethodProxy(String name) {
        super(name);
    }

    @Override
    public boolean beforeCall(Object who, Method method, Object... args) {
        int index = ArrayUtils.indexOfLast(args, Integer.class);
        if (index != -1) {
            int userId = (int) args[index];
            if (userId == getAppUserId() && userId != getRealUserId()) {
                args[index] = getRealUserId();
            }
        }
        return super.beforeCall(who, method, args);
    }
}