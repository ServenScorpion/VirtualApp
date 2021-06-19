package com.xdja.zs;


import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.server.am.VActivityManagerService;

import java.util.ArrayList;
import java.util.List;

public class VServiceKeepAliveService extends IServiceKeepAlive.Stub {

    private static final String TAG = "VServiceKeepAliveService";
    private static Singleton<VServiceKeepAliveService> sService = new Singleton<VServiceKeepAliveService>() {

        @Override
        protected VServiceKeepAliveService create() {
            return new VServiceKeepAliveService();
        }
    };
    private static final List<String> mKeepAliveServiceList = new ArrayList<>();
    private static List<String> mTemporaryCacheList = new ArrayList<>();
    private static HandlerThread mHandlerThread = new HandlerThread("keepAliveThread");
    private static Handler mHandler;
    private static final int UPDATE_APP_LIST = 1;
    private static final int RUN_KEEPALIVE_APP = 2;
    private static final int ACTION_ADD = 1;
    private static final int ACTION_DEL = 2;
    private static final int ACTION_TEMP_ADD = 3;
    private static final int ACTION_TEMP_DEL = 4;


//    static {
//        mKeepAliveServiceList.add("com.xdja.emm");
//    }

    public static void systemReady() {
        mHandlerThread.start();
        mHandler = new H(mHandlerThread.getLooper());
    }

    private void sendMessage(int what, int arg, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = arg;
        mHandler.sendMessage(msg);
    }

    public static VServiceKeepAliveService get() {
        return sService.get();
    }

    private static boolean hasKeepAliveService(String pkgName) {
        synchronized (mKeepAliveServiceList) {
            return mKeepAliveServiceList.contains(pkgName);
        }
    }

    private static void runKeepAliveService(String pkgName, int userId) {
        if (hasKeepAliveService(pkgName)) {
            Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
            VActivityManagerService.get().sendBroadcastAsUserWithPackage(intent, new VUserHandle(userId), pkgName);
        }
    }

    private static void clearAppFromList(String pkgName) {
        synchronized (mKeepAliveServiceList) {
            mKeepAliveServiceList.remove(pkgName);
        }
    }

    @Override
    public void scheduleRunKeepAliveService(String pkgName, int userId) {
        sendMessage(RUN_KEEPALIVE_APP, userId, pkgName);
    }

    @Override
    public void scheduleUpdateKeepAliveList(String pkgName, int action) {
        sendMessage(UPDATE_APP_LIST, action, pkgName);
    }

    @Override
    public boolean inKeepAliveServiceList(String pkgName) {
        return hasKeepAliveService(pkgName);
    }

    private static class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_APP_LIST:
                    String pkg = msg.obj.toString();
                    if (msg.arg1 == ACTION_ADD) {
                        synchronized (mKeepAliveServiceList) {
                            mKeepAliveServiceList.add(pkg);
                        }
                        VLog.d(TAG, "Update add List:" + mKeepAliveServiceList);
                    } else if (msg.arg1 == ACTION_DEL) {
                        clearAppFromList(pkg);
                        VLog.d(TAG, "Update del List:" + mKeepAliveServiceList);
                    } else if (msg.arg1 == ACTION_TEMP_DEL) {
                        if (hasKeepAliveService(pkg)) {
                            clearAppFromList(pkg);
                            mTemporaryCacheList.add(pkg);
                            VLog.d(TAG, "Update temporary add List:" + mKeepAliveServiceList);
                        }
                    } else if (msg.arg1 == ACTION_TEMP_ADD) {
                        if (mTemporaryCacheList.contains(pkg)) {
                            synchronized (mKeepAliveServiceList) {
                                mKeepAliveServiceList.add(pkg);
                            }
                            mTemporaryCacheList.remove(pkg);
                            VLog.d(TAG, "Update temporary del List:" + mKeepAliveServiceList);
                        }
                    }
                    break;
                case RUN_KEEPALIVE_APP:
                    runKeepAliveService((String) msg.obj, msg.arg1);
                    break;
                default:
                    break;
            }
        }
    }

}
