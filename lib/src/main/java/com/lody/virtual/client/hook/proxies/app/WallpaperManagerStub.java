package com.lody.virtual.client.hook.proxies.app;

import android.app.IWallpaperManagerCallback;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.IInterface;

import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.annotations.LogInvocation;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceLastUserIdMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceSequencePkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.VLog;

import java.lang.reflect.Method;

import mirror.android.app.IWallpaperManager;

/**
 * @see WallpaperManager
 */
@LogInvocation(LogInvocation.Condition.ALWAYS)
public class WallpaperManagerStub extends BinderInvocationProxy {

    public WallpaperManagerStub() {
        super(IWallpaperManager.Stub.asInterface, Context.WALLPAPER_SERVICE);
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        WallpaperManager.getInstance(VirtualCore.get().getContext());
        final IInterface hookedService = getInvocationStub().getProxyInterface();
        if (mirror.android.app.WallpaperManager.sGlobals != null && hookedService != null){
            Object sGlobals = mirror.android.app.WallpaperManager.sGlobals.get();
            if(sGlobals != null){
                if(mirror.android.app.WallpaperManager.Globals.mService != null) {
                    Object old = mirror.android.app.WallpaperManager.Globals.mService.get(sGlobals);
                    if(old != hookedService) {
                        VLog.w("kk-test", "WallpaperManager.sGlobals.mService="+old);
                        mirror.android.app.WallpaperManager.Globals.mService.set(sGlobals, hookedService);
                    } else {
                        VLog.i("kk-test", "WallpaperManager.sGlobals.mService set ok");
                    }
                } else {
                    VLog.i("kk-test", "WallpaperManager.sGlobals.mService is not found ");
                }
            } else {
                VLog.w("kk-test", "WallpaperManager.sGlobals=null");
            }
        }
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new SetWallpaper());
        addMethodProxy(new ReplaceSequencePkgWithUserIdMethodProxy("setWallpaperComponentChecked", 1));
        addMethodProxy(new ReplaceLastUserIdMethodProxy("getWallpaper"){
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                if(BuildCompat.isPie()){
                    MethodParameterUtils.replaceFirstAppPkg(args);
                }
                return super.call(who, method, args);
            }
        });
        addMethodProxy(new ReplaceLastUserIdMethodProxy("getWallpaperInfo"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("setDimensionHints"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("isWallpaperSupported"));
        addMethodProxy(new ReplaceLastUserIdMethodProxy("isWallpaperBackupEligible"));
        if(BuildCompat.isQ()){
            addMethodProxy(new ReplaceSequenceUserIdMethodProxy("getWallpaperColors"));
            addMethodProxy(new ReplaceSequenceUserIdMethodProxy("registerWallpaperColorsCallback"));
            addMethodProxy(new ReplaceSequenceUserIdMethodProxy("unregisterWallpaperColorsCallback"));
        } else  if(BuildCompat.isPie()){
            addMethodProxy(new ReplaceLastUserIdMethodProxy("getWallpaperColors"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("registerWallpaperColorsCallback"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("unregisterWallpaperColorsCallback"));
        }
        addMethodProxy(new StaticMethodProxy("getWidthHint"){
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                int v = VirtualCore.getConfig().getWallpaperWidthHint(getAppPkg(), getAppUserId());
                if(v > 0){
                    return v;
                }
                return super.call(who, method, args);
            }
        });
        addMethodProxy(new StaticMethodProxy("getHeightHint"){
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                int v = VirtualCore.getConfig().getWallpaperHeightHint(getAppPkg(), getAppUserId());
                if(v > 0){
                    return v;
                }
                return super.call(who, method, args);
            }
        });
    }


    private class SetWallpaper extends ReplaceSequencePkgWithUserIdMethodProxy {
        public SetWallpaper() {
            super("setWallpaper", 1);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            //9.0
            //setWallpaper(String name, in String callingPackage,
            //            in Rect cropHint, boolean allowBackup, out Bundle extras, int which,
            //            IWallpaperManagerCallback completion, int userId);
            //7.0
            //    ParcelFileDescriptor setWallpaper(String name, in String callingPackage,
            //            in Rect cropHint, boolean allowBackup, out Bundle extras, int which,
            //            IWallpaperManagerCallback completion);
            String name = (String)args[0];
            Rect cropHint = (Rect)args[2];
            int which = (int)args[5];
            IWallpaperManagerCallback callback = (IWallpaperManagerCallback)args[6];
            SettingConfig.WallpaperResult result = VirtualCore.getConfig().onSetWallpaper(getAppPkg(), getAppUserId(), name, cropHint, which, callback);
            if(result != null){
                return result.wallpaperFile;
            }
            return super.call(who, method, args);
        }
    }

    private class ReplaceSequenceUserIdMethodProxy extends StaticMethodProxy{
        public ReplaceSequenceUserIdMethodProxy(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int index = args.length - 2;
            if (index >= 0) {
                int userId = (int) args[index];
                if (userId == getAppUserId() && userId != getRealUserId()) {
                    args[index] = getRealUserId();
                }
            }
            return super.call(who, method, args);
        }
    }
    private class ReplaceSequencePkgWithUserIdMethodProxy extends ReplaceSequencePkgMethodProxy{
        public ReplaceSequencePkgWithUserIdMethodProxy(String name, int sequence) {
            super(name, sequence);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (BuildCompat.isOreo()) {
                MethodParameterUtils.replaceLastUserId(args);
            }
            return super.call(who, method, args);
        }
    }
}
