package io.virtualapp.delegate.hook.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClassUtil {
    private static final String TAG = "ClassUtil";

    public static void printMethodsInClass(String printTag, Class mClazz) {
        for (Method method : mClazz.getDeclaredMethods()) {
            String typeName = method.getReturnType().getSimpleName();
            String canonicalName = method.getReturnType().getCanonicalName();
            String methodName = method.getName();
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            String types = "";
            for (Class clazz : methodParameterTypes) {
//                LogUtil.d(TAG, "函数返回类型" + clazz.getName());
                types += clazz.getName() + ",";
            }
            LogUtil.d(TAG, printTag + " methodName=" + methodName + "，typeName=" + typeName + ",canonicalName=" + canonicalName + "，返回type=(" + types + ")");
            method.setAccessible(true);
        }
    }

    private static void printFieldsInClass(String printTag, Class mClazz) {
        if (true) {
//            return;
        }
        for (Field field : mClazz.getDeclaredFields()) {
            String fieldName = field.getName();
            field.setAccessible(true);
            try {
                LogUtil.d(TAG, printTag + " fieldName=" + fieldName);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void printFieldsInClassAndObject(String printTag, Class mClazz, Object object) {
        for (Field field : mClazz.getDeclaredFields()) {
            String fieldName = field.getName();
            field.setAccessible(true);
            try {
                LogUtil.d(TAG, printTag + " fieldName=" + fieldName + ",值是" + field.get(object));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}
