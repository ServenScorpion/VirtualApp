package io.virtualapp;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatDelegate;


import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;
import com.scorpion.splash.AppComponentDelegate;
import com.scorpion.utils.SPTools;

import io.virtualapp.delegate.MyAppRequestListener;
import io.virtualapp.delegate.MyTaskDescDelegate;
import io.virtualapp.home.BackHomeActivity;
//import jonathanfinerty.once.Once;
import android.content.BroadcastReceiver;

/**
 * @author LodyChen
 */
public class App extends Application {

    private static App gApp;
    AppComponentDelegate mAppComponentDelegate;

    public SettingConfig mConfig = new SettingConfig() {
        public String SSID_KEY = "ssid_key";
        public String MAC_KEY = "mac_key";
        public FakeWifiStatus fakeWifiStatus = new FakeWifiStatus();
        @Override
        public String getHostPackageName() {
            return BuildConfig.APPLICATION_ID;
        }

        @Override
        public String get64bitEnginePackageName() {
            return BuildConfig.PACKAGE_NAME_ARM64;
        }

        @Override
        public boolean isEnableIORedirect() {
            return true;
        }

        @Override
        public Intent onHandleLauncherIntent(Intent originIntent) {
            Intent intent = new Intent();
            ComponentName component = new ComponentName(getHostPackageName(), BackHomeActivity.class.getName());
            intent.setComponent(component);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
            //return null;
        }

        @Override
        public boolean isUseRealDataDir(String packageName) {
            return false;
        }

        @Override
        public AppLibConfig getAppLibConfig(String packageName) {
            return AppLibConfig.UseRealLib;
        }

        @Override
        public boolean isAllowCreateShortcut() {
            return false;
        }

        /**
         * 如果返回[true], 则认为将要启动的Activity在宿主之中。
         * @param intent intent
         * @return bool
         */
        @Override
        public boolean isHostIntent(Intent intent) {
            return intent.getData() != null && "market".equals(intent.getData().getScheme());
        }

        @Override
        public FakeWifiStatus getFakeWifiStatus() {
            fakeWifiStatus.setSSID(SPTools.getString(VirtualCore.get().getContext(),SSID_KEY,FakeWifiStatus.DEFAULT_SSID));
            fakeWifiStatus.setDefaultMac(SPTools.getString(VirtualCore.get().getContext(),MAC_KEY,FakeWifiStatus.DEFAULT_MAC));
            return fakeWifiStatus;
        }
    };

    public static App getApp() {
        return gApp;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //SandXposed.init();
        VLog.OPEN_LOG = true;
        try {
            VirtualCore.get().startup(base, mConfig);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        gApp = this;
        super.onCreate();
        lazyInjectInit();
        if (mAppComponentDelegate == null){
            mAppComponentDelegate = new AppComponentDelegate(gApp);
        }

        VirtualCore virtualCore = VirtualCore.get();
        virtualCore.setAppCallback(mAppComponentDelegate);
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
                mAppComponentDelegate.setMainProcess(true);
                //Once.initialise(App.this);
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onVirtualProcess() {
                //listener components
                mAppComponentDelegate.setMainProcess(false);
                //fake task description's icon and title
                virtualCore.setTaskDescriptionDelegate(new MyTaskDescDelegate());
                //内部安装，不调用系统的安装，而是自己处理（参考MyAppRequestListener），默认是静默安装在va里面。
                virtualCore.setAppRequestListener(new MyAppRequestListener(App.this));
            }

            @Override
            public void onServerProcess() {
//                 外部安装了下面应用，但是内部没有安装（双开），内部应用在调用下面应用的时候，会调用外面的应用，如果没用addVisibleOutsidePackage，则会相当于没有安装
//                 比如：内部微信调用QQ分享，但是内部没有QQ，如果没用addVisibleOutsidePackage，那么提示没有安装QQ，如果用了addVisibleOutsidePackage，则启动外部的QQ
//                 注：应用调用的校验越来越严，与外部的调用可能会失败，这时候就需要都安装在va内部。
//                 2018: 调用外部QQ登录将提示非正版应用

//                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqq");
//                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqqi");
//                virtualCore.addVisibleOutsidePackage("com.tencent.minihd.qq");
//                virtualCore.addVisibleOutsidePackage("com.tencent.qqlite");
//                virtualCore.addVisibleOutsidePackage("com.facebook.katana");
//                virtualCore.addVisibleOutsidePackage("com.whatsapp");
//                virtualCore.addVisibleOutsidePackage("com.tencent.mm");
//                virtualCore.addVisibleOutsidePackage("com.immomo.momo");
            }
        });
    }

    private void lazyInjectInit() {
        //LazyInject.init(this);
//        LazyInject.removeComponent();
//        LazyInject.addBuildMap(Auto_ComponentBuildMap.class);
    }
    private class VAppReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*String season = intent.getStringExtra(Constants.EXTRA_SEASON);
            //
            String error = intent.getStringExtra(Constants.EXTRA_ERROR);
            if (Constants.ACTION_NEED_PERMISSION.equals(intent.getAction())) {
                if ("startActivityForBg".equals(season)) {
                    //TODO vivo start activity by service
                    //跳到vivo的后台弹activity权限
                }
            }else if(Constants.ACTION_PROCESS_ERROR.equals(intent.getAction())){
                if("requestPermissions".equals(season)){
                    //user cancel
                }
            }*/
        }
    }
}
