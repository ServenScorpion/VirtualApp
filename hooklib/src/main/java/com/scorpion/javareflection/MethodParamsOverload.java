package com.scorpion.javareflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 判断SDK
 TYPE: 作用于类、接口或者枚举
 FIELD：作用于类中声明的字段或者枚举中的常量
 METHOD：作用于方法的声明语句中
 PARAMETER：作用于参数声明语句中
 CONSTRUCTOR：作用于构造函数的声明语句中
 LOCAL_VARIABLE：作用于局部变量的声明语句中
 ANNOTATION_TYPE：作用于注解的声明语句中
 PACKAGE：作用于包的声明语句中
 TYPE_PARAMETER：java 1.8之后，作用于类型声明的语句中
 TYPE_USE：java 1.8之后，作用于使用类型的任意语句中
 */

/**
 RetentionPolicy.SOURCE：注解只保留在源文件，当Java文件编译成class文件的时候，注解被遗弃；
 RetentionPolicy.CLASS：注解被保留到class文件，但jvm加载class文件时候被遗弃，这是默认的生命周期；
 RetentionPolicy.RUNTIME：注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在；
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodParamsOverload {
    public static String GT = "gt";//  - gt   GREATER THAN 是大于
    public static String LT = "lt";//  - lt   LESS THAN    是小于
    public static String EQ = "eq";//  - eq   EQUAL        是等于
    public static String NE = "ne";//  - ne   NOT EQUAL    是不等于
    public static String GE = "ge";//  - ge   GREATER THAN OR EQUAL 是大于等于
    public static String LE = "le";//  - le   LESS THAN OR EQUAL是小于等于

    /**
     *下面2个参数主要是做兼容使用，兼容目前主要是考虑两个方向
     *    1、一个是在初始化这个对象前的时候判断 当前sdk 是否有该 属性
     *    2、一个是在初始化过过程中判断参数是否 已经改变
     *
     *    使用中一定注意，value* 的值需要跟 intRange*的值对应，不然后续找不到
     *        @MethodParamsOverload(
     *             value1 = {IBinder.class, Boolean.class},
     *             value2 = {IBinder.class, List.class, boolean.class},
     *             intRange1 = {0,26},// 有 - 表示 至
     *             intRange2 = {-26,29})
     */
    Class<?>[] value1() default {};
    Class<?>[] value2() default {}; //这个其实是 当参数有改变的是时候 传参给这个变量就可有有多个参数类型的情况
    Class<?>[] value3() default {}; //同上
    Class<?>[] value4() default {};
    Class<?>[] value5() default {};

    String[] valueReflect1() default {};
    String[] valueReflect2() default {};
    String[] valueReflect3() default {};
    String[] valueReflect4() default {};
    String[] valueReflect5() default {};


    int[] intRange1() default {};
    int[] intRange2() default {};
    int[] intRange3() default {};
    int[] intRange4() default {};
    int[] intRange5() default {};

    String tips() default "";
}