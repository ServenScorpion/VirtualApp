package com.lody.virtual.client.ipc;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AndroidRuntimeException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.ServiceCache;
import com.lody.virtual.server.interfaces.IServiceFetcher;

/**
 * @author Lody
 */
public class ServiceManagerNative {
    public static final String PACKAGE = "package";
    public static final String ACTIVITY = "activity";
    public static final String USER = "user";
    public static final String APP = "app";
    public static final String ACCOUNT = "account";
    public static final String CONTENT = "content";
    public static final String JOB = "job";
    public static final String NOTIFICATION = "notification";
    public static final String VS = "vs";
    public static final String DEVICE = "device";
    public static final String VIRTUAL_LOC = "virtual-loc";

    public static final String FLOATICONBALL = "floaticonball";
    public static final String APPPERMISSION = "app-permission";
    public static final String CONTROLLER = "controller";
    public static final String WATERMARK = "watermakr";
    public static final String WATERMARK_DIALOG = "watermark-dialog";
    public static final String SAFEKEY = "safekey";
    public static final String CKMSSAFEKEY = "ckms-safekey";
    public static final String KEEPALIVE = "keepalive";
    public static final String INSTALLERSETTING = "installersetting";

    public static final String SERVICE_DEF_AUTH = "virtual.service.BinderProvider";
    private static final String TAG = ServiceManagerNative.class.getSimpleName();
    public static String SERVICE_CP_AUTH = "virtual.service.BinderProvider";

    private static IServiceFetcher sFetcher;

    private static String getAuthority() {
        return VirtualCore.getConfig().getBinderProviderAuthority();
    }

    private static IServiceFetcher getServiceFetcher() {
        if (sFetcher == null || !sFetcher.asBinder().isBinderAlive()) {
            synchronized (ServiceManagerNative.class) {
                if (sFetcher == null || !sFetcher.asBinder().isBinderAlive()) {
                    Context context = VirtualCore.get().getContext();
                    Bundle response = new ProviderCall.Builder(context, getAuthority()).methodName("@").callSafely();
                    if (response != null) {
                        IBinder binder = BundleCompat.getBinder(response, "_VA_|_binder_");
                        linkBinderDied(binder);
                        sFetcher = IServiceFetcher.Stub.asInterface(binder);
                    }

                }
            }
        }
        return sFetcher;
    }

    public static void ensureServerStarted() {
        new ProviderCall.Builder(VirtualCore.get().getContext(), getAuthority()).methodName("ensure_created").callSafely();
    }

    public static void clearServerFetcher() {
        sFetcher = null;
    }

    private static void linkBinderDied(final IBinder binder) {

        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                try {
                    binder.unlinkToDeath(this, 0);
                }catch (Throwable e){
                    //ignore
                }
                
                onServerDied();
            }
        };

        try {
            binder.linkToDeath(deathRecipient, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static void onServerDied() {

        //不做任何判断，直接抛出’AndroidRuntimeException'
        throw new AndroidRuntimeException("X进程崩溃，所有进程退出！");

    }


    public static IBinder getService(String name) {
        if (VirtualCore.get().isServerProcess()) {
            return ServiceCache.getService(name);
        }
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                return fetcher.getService(name);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        VLog.e(TAG, "GetService(%s) return null.", name);
        return null;
    }

    public static void addService(String name, IBinder service) {
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                fetcher.addService(name, service);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    public static void removeService(String name) {
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                fetcher.removeService(name);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

}
