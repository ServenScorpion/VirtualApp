package com.lody.virtual.client.hook.proxies.clipboard;

import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.os.IInterface;
import android.text.TextUtils;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.VLog;

import java.lang.reflect.Method;

import mirror.android.content.ClipboardManager;
import mirror.android.content.ClipboardManagerOreo;

import com.xdja.zs.IOnPrimaryClipChangedListener;
import com.xdja.zs.VAppPermissionManager;

/**
 * @author Lody
 * @see ClipboardManager
 */
public class ClipBoardStub extends BinderInvocationProxy {
    private static final String TAG = ClipBoardStub.class.getSimpleName();

    public ClipBoardStub() {
        super(getInterface(), Context.CLIPBOARD_SERVICE);
    }

    private static IInterface getInterface() {
        // android < 26
        if (ClipboardManager.getService != null) {
            return ClipboardManager.getService.call();
        } else if (ClipboardManagerOreo.mService != null) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    VirtualCore.get().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            return ClipboardManagerOreo.mService.get(cm);
        } else if (ClipboardManagerOreo.sService != null) {
            //samsung
            return ClipboardManagerOreo.sService.get();
        } else {
            return null;
        }
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ClipBoardMethodProxy("getPrimaryClip"));
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            addMethodProxy(new ClipBoardMethodProxy("setPrimaryClip"));
            addMethodProxy(new ClipBoardMethodProxy("getPrimaryClipDescription"));
            addMethodProxy(new ClipBoardMethodProxy("hasPrimaryClip"));
            addMethodProxy(new ClipBoardMethodProxy("addPrimaryClipChangedListener"));
            addMethodProxy(new ClipBoardMethodProxy("removePrimaryClipChangedListener"));
            addMethodProxy(new ClipBoardMethodProxy("hasClipboardText"));
        }
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        if (ClipboardManagerOreo.mService != null) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    VirtualCore.get().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipboardManagerOreo.mService.set(cm, getInvocationStub().getProxyInterface());
        } else if (ClipboardManagerOreo.sService != null) {
            //samsung 8.0
            ClipboardManagerOreo.sService.set(getInvocationStub().getProxyInterface());
        }
    }

    private class ClipBoardMethodProxy extends ReplaceLastPkgMethodProxy {
        public ClipBoardMethodProxy(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String appPkg = getAppPkg();
            VLog.e(TAG, getMethodName() + " appPkg: " + appPkg);
            VAppPermissionManager vAppPermissionManager = VAppPermissionManager.get();
            switch (getMethodName()) {
                case "getPrimaryClip":
                    ClipData data = vAppPermissionManager.getClipData();
                    VLog.e(TAG, getMethodName() + " ClipData: " + (data == null ? "cache ClipData is null" : data.toString()));
                    return data;
                case "setPrimaryClip":
                    for (Object arg : args) {
                        if (arg == null) {
                            continue;
                        }
                        if (arg instanceof ClipData) {
                            ClipData clipData = (ClipData) arg;
                            CharSequence text = clipData.getItemAt(0).toString();
                            if (TextUtils.isEmpty(text)) {
                                VLog.e(TAG, getMethodName() + " clip data is null do not cache");
                                continue;
                            }
                            VLog.e(TAG, getMethodName() + " cache ClipData: " + clipData.toString());
                            vAppPermissionManager.cacheClipData(clipData);
                        }
                    }
                    VLog.e(TAG, getMethodName() + " call Primary Clip Changed Listener");
                    vAppPermissionManager.callPrimaryClipChangedListener();
                    return null;
                case "hasPrimaryClip":
                case "hasClipboardText":
                    ClipData clipData = vAppPermissionManager.getClipData();
                    VLog.e(TAG, getMethodName() + " clipData: " + (clipData != null));
                    return clipData != null;
                case "getPrimaryClipDescription":
                    ClipData cd = vAppPermissionManager.getClipData();
                    VLog.e(TAG, getMethodName() + " clipData: " + (cd != null));
                    return cd == null ? null : cd.getDescription();
                case "addPrimaryClipChangedListener":
                    for (Object arg : args) {
                        if (arg == null) {
                            continue;
                        }
                        if (arg instanceof IOnPrimaryClipChangedListener.Stub) {
                            IOnPrimaryClipChangedListener listener = (IOnPrimaryClipChangedListener) arg;
                            VLog.e(TAG, getMethodName() + " addPrimaryClipChangedListener ok");
                            vAppPermissionManager.cachePrimaryClipChangedListener(listener);
                        }
                    }
                    return super.call(who, method, args);
                case "removePrimaryClipChangedListener":
                    VLog.e(TAG, getMethodName() + " removePrimaryClipChangedListener ok");
                    vAppPermissionManager.removePrimaryClipChangedListener();
                    return super.call(who, method, args);
                default:
                    return super.call(who, method, args);
            }
        }
    }
}
