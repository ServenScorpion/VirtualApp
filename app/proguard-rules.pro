-dontshrink
-keepattributes *Annotation*,InnerClasses
-keepattributes Signature,EnclosingMethod
-keepclassmembers class * implements java.io.Serializable {*;}

-dontwarn android.**
-dontwarn com.tencent.**
-dontwarn andhook.**
-dontwarn org.slf4j.**
-dontwarn org.eclipse.**

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.ContentProvider

# Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * extends android.os.Binder{
    public <methods>;
}

-keepclasseswithmembernames class * {
    native <methods>;
}
# android
-keep class android.**{
    *;
}

-repackageclass z2

-ignorewarning
-keepattributes *Annotation*
#保留部分泛型信息，必要!
-keepattributes Signature
#手动启用support keep注解
#http://tools.android.com/tech-docs/support-annotations
-dontskipnonpubliclibraryclassmembers
-printconfiguration
-keep,allowobfuscation @interface android.support.annotation.Keep

-keep @android.support.annotation.Keep class * {
*;
}

-keepclassmembers class * {
    @android.support.annotation.Keep *;
}
#手动启用Component注解
#http://tools.android.com/tech-docs/support-annotations
-keep,allowobfuscation @interface com.trend.lazyinject.annotation.Component

-keep,allowobfuscation @com.trend.lazyinject.annotation.Component class * {
*;
}

-keepclassmembers class * {
    @com.trend.lazyinject.annotation.Component *;
}



# Thirdparty Library
-keep class c.t.m.g.**{*;}
-keep class com.tencent.**{*;}
-keep class jonathanfinerty.once.**{public *;}

#-keepattributes SourceFile,LineNumberTable

-keep class andhook.lib.AndHook$Dalvik
-keepclassmembers class andhook.lib.AndHook$Dalvik {
   native <methods>;
}
-keep class andhook.lib.AndHook
-keepclassmembers class andhook.lib.AndHook {
   native <methods>;
}
-keep class andhook.lib.YunOSHelper
-keepclassmembers class andhook.lib.YunOSHelper {
   public *;
}

-keep class com.easy.android.easyxp.*
-keepclassmembers class com.easy.android.easyxp.* {
   *;
}
-keep class android.app.AndroidAppHelper
-keepclassmembers class android.app.AndroidAppHelper {
   public *;
}

-keepattributes Exceptions, InnerClasses, ...
-keep class andhook.lib.xposed.XC_MethodHook
-keepclassmembers class andhook.lib.xposed.XC_MethodHook {
   *;
}
-keep class andhook.lib.xposed.XC_MethodHook$*
-keepclassmembers class andhook.lib.xposed.XC_MethodHook$* {
   *;
}
-keep class * extends andhook.lib.xposed.XC_MethodHook
-keepclassmembers class * extends andhook.lib.xposed.XC_MethodHook {
   public *;
   protected *;
}
#-keep class * extends andhook.lib.xposed.XC_MethodReplacement
#-keepclassmembers class * extends andhook.lib.xposed.XC_MethodReplacement {
#   *;
#}

-keep class io.vposed.VPosed
-keepclassmembers class io.vposed.VPosed {
   public *;
}
