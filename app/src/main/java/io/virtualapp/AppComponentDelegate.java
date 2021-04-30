package io.virtualapp;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.UserHandle;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.AppCallback;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.sandxposed.SandXposed;
import com.scorpion.IHook.XC_MethodHook;
import com.scorpion.IHook.XposedBridge;
import com.scorpion.IHook.XposedHelpers;
import com.scorpion.IHook.callbacks.XC_LoadPackage;
import com.scorpion.reverse.dingding.DingTalk;
import com.swift.sandhook.xposedcompat.utils.ProcessUtils;


public class AppComponentDelegate implements AppCallback {
    private static final String TAG = "AppComponentDelegate";

    Context mContext;
    boolean isMainProcess = false;
    public AppComponentDelegate(Context context){
        mContext = context;
    }

    public boolean isMainProcess() {
        return isMainProcess;
    }

    public void setMainProcess(boolean mainProcess) {
        isMainProcess = mainProcess;
        if (isMainProcess){
            //ChannelConfig.syncChannel(mContext);
        }
    }

    @Override
    public void beforeStartApplication(String packageName, String processName, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                SandXposed.initForXposed(context, processName);
                SandXposed.injectXposedModule(context, packageName, processName);
            }catch (Exception e){
            }
        }

        //fix user id temp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && VUserHandle.realUserId() > 0) {
            try {
                XposedHelpers.findAndHookMethod(UserHandle.class, "getUserId", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                        param.setResult(VUserHandle.realUserId());
                    }
                });
            } catch (Throwable throwable) {

            }
        }
    }

    @Override
    public void beforeApplicationCreate(String packageName, String processName, Application application) {

    }

    @Override
    public void afterApplicationCreate(String packageName, String processName, Application application) {
        if (packageName.equals("com.alibaba.android.rimet")) {
            VLog.d("VA-","开始 hook 丁丁打卡");
            ClassLoader classLoader = VClient.get().getClassLoader();
            DingTalk.hook(classLoader);
        }else if (packageName.equals("com.taobao.taobao")){

        }

    }

    private XC_LoadPackage.LoadPackageParam getLoadPackageParam(Application application){
        //prepare LoadPackageParam
        XC_LoadPackage.LoadPackageParam packageParam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
        if (application != null) {
            if (packageParam.packageName == null) {
                packageParam.packageName = application.getPackageName();
            }

            if (packageParam.processName == null) {
                packageParam.processName = ProcessUtils.getProcessName(application);
            }
            if (packageParam.classLoader == null) {
                packageParam.classLoader = application.getClassLoader();
            }
            if (packageParam.appInfo == null) {
                packageParam.appInfo = application.getApplicationInfo();
            }
        }
        return packageParam;
    }
}
