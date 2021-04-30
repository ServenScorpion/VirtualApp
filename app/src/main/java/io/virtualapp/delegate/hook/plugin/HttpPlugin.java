package io.virtualapp.delegate.hook.plugin;

import android.app.Application;
import android.util.Log;
import android.view.View;


import com.scorpion.IHook.XC_MethodHook;
import com.scorpion.IHook.XposedBridge;
import com.scorpion.IHook.XposedHelpers;

import java.net.URL;

import io.virtualapp.delegate.hook.utils.ClassUtil;
import io.virtualapp.delegate.hook.utils.LogUtil;

import static com.scorpion.IHook.XposedHelpers.findClass;


/**
 * 搞Http
 */
public class HttpPlugin {
    private static final String TAG = "QQBrowserHookHttp";
    ClassLoader mClassLoader;
    String mVersionName;
    boolean isHooking = false;

    public void hook(String packageName, String processName, Application application) {
        mClassLoader = application.getClassLoader();
        if (isHooking) {
            return;
        }
        isHooking = true;
        hookHttp();
    }

    /**
     * hook QQBrowser
     */
    private void hookHttp() {
        LogUtil.d(TAG, "QQBrowser开始HookHttp");
        try {
//        XposedHelpers.findAndHookMethod("com.tencent.mtt.browser.window")
            Class UrlParamsClass = findClass("com.tencent.mtt.browser.window.UrlParams", mClassLoader);
            XposedBridge.hookAllConstructors(UrlParamsClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object object = param.thisObject;
                    ClassUtil.printFieldsInClassAndObject("UrlParams", object.getClass(), object);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            //xposed class java.net.HttpURLConnection
            final Class<?> httpUrlConnection = findClass("java.net.HttpURLConnection", mClassLoader);

            XposedBridge.hookAllConstructors(httpUrlConnection, new XC_MethodHook() {
                @Override //trước khi getoutputstream
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length != 1 || param.args[0].getClass() != URL.class)
                        return;
                    URL url = (URL) param.args[0];
//                    http://113.96.232.45:8080/?tk=5c69cd51f920b5bf39b5abea53682de20f0e2f956c5de7871cb02aaf34fde67d323f6e7f03b881db21133b1bf2ae5bc5&iv=6f0008ec31316473&encrypt=17
                    LogUtil.d(TAG, "HttpURLConnection: " + param.args[0] + "");
                    if (url.toString().contains("113.96")) {
                        //打印堆栈
                        StringBuilder TraceString = new StringBuilder("");
                        if (false) {
                            try {
                                int b = 1 / 0;
                            } catch (Throwable e) {
                                StackTraceElement[] stackTrace = e.getStackTrace();
                                TraceString.append(" --------------------------  >>>> " + "\n");
                                for (StackTraceElement stackTraceElement : stackTrace) {
                                    TraceString.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("行数  ").append(stackTraceElement.getLineNumber()).append("\n");
                                }
                                TraceString.append("<<<< --------------------------  " + "\n");
                            }
                        }
                        TraceString.append("<<<<------------------------------>>>>>  \n").append("\n <<<<------------------------------>>>>>").append("\n");
                        LogUtil.e(TAG, "堆栈信息：" + TraceString.toString());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*try {
            Class MttRequestBaseClass = XposedHelpers.findClass("com.tencent.common.http.MttRequestBase", mClassLoader);
            XposedHelpers.findAndHookMethod("com.tencent.common.http.HttpRequesterBase", mClassLoader, "createConnection", MttRequestBaseClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object MttRequestBaseObject = param.args[0];
                    ClassUtil.printFieldsInClassAndObject("HttpRequesterBase.createConnection-param0", MttRequestBaseObject.getClass(), MttRequestBaseObject);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        try {
            Class MttRequestBaseClass = findClass("com.tencent.common.http.MttRequestBase", mClassLoader);
            XposedBridge.hookAllMethods(MttRequestBaseClass, "addHeaders", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object thisObject = param.thisObject;
//                    ClassUtil.printFieldsInClassAndObject("MttRequestBase.addHeaders-thisObject", thisObject.getClass(), thisObject);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Class MttRequestBaseClass = findClass("com.tencent.common.http.MttRequestBase", mClassLoader);
            XposedBridge.hookAllMethods(MttRequestBaseClass, "addHeader", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object thisObject = param.thisObject;
//                    ClassUtil.printFieldsInClassAndObject("MttRequestBase.addHeader-thisObject", thisObject.getClass(), thisObject);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Class RequestClass = findClass("com.squareup.okhttp.Request", mClassLoader);
            XposedHelpers.findAndHookMethod("com.squareup.okhttp.OkHttpClient", mClassLoader, "newCall", RequestClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object param0 = param.args[0];
                    ClassUtil.printFieldsInClassAndObject("OkHttpClient.newCall-param0", param0.getClass(), param0);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            XposedHelpers.findAndHookMethod("com.tencent.mtt.WindowComponentExtensionImp", mClassLoader, "j", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    LogUtil.d(TAG, "WindowComponentExtensionImp.j() 执行了");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            XposedHelpers.findAndHookMethod("com.tencent.mtt.browser.bra.toolbar.h", mClassLoader, "onClick", View.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    LogUtil.d(TAG, "toolbar.h.onClick() 执行了");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
