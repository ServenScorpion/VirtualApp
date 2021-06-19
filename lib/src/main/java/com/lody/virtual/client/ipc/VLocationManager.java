package com.lody.virtual.client.ipc;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.proxies.location.MockLocationHelper;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.vloc.VLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @see LocationManager
 * <p>
 * 实现代码多，资源回收不及时：拦截gps状态，定位请求，并且交给虚拟定位服务，虚拟服务根据一样的条件，再次向系统定位服务请求
 * LocationManager.addgpslistener
 * LocationManager.request
 * <p>
 * 实现代码少：GpsStatusListenerTransport、ListenerTransport这2个对象，hook里面的方法，修改参数，都是binder
 */
public class VLocationManager {
    public static final String TAG = "VLoc";
    private Handler mWorkHandler;
    private HandlerThread mHandlerThread;
    private final List<Object> mGpsListeners = new ArrayList<>();
    private static VLocationManager sVLocationManager = new VLocationManager();

    private VLocationManager() {
        LocationManager locationManager = (LocationManager) VirtualCore.get().getContext().getSystemService(Context.LOCATION_SERVICE);
        MockLocationHelper.fakeGpsStatus(locationManager);
    }

    public static VLocationManager get() {
        return sVLocationManager;
    }


    private void checkWork() {
        if (mHandlerThread == null) {
            synchronized (this) {
                if (mHandlerThread == null) {
                    mHandlerThread = new HandlerThread("loc_thread");
                    mHandlerThread.start();
                }
            }
        }
        if (mWorkHandler == null) {
            synchronized (this) {
                if (mWorkHandler == null) {
                    mWorkHandler = new Handler(mHandlerThread.getLooper());
                }
            }
        }
    }

    private void stopGpsTask() {
        if (mWorkHandler != null) {
            mWorkHandler.removeCallbacks(mUpdateGpsStatusTask);
        }
    }

    private void startGpsTask() {
        checkWork();
        stopGpsTask();
        mWorkHandler.postDelayed(mUpdateGpsStatusTask, 5000);
    }

    private Runnable mUpdateGpsStatusTask = new Runnable() {
        @Override
        public void run() {
            synchronized (mGpsListeners) {
                for (Object listener : mGpsListeners) {
                    notifyGpsStatus(listener);
                }
            }
            mWorkHandler.postDelayed(mUpdateGpsStatusTask, 8000);
        }
    };


    public boolean hasVirtualLocation(String packageName, int userId) {
        try {
            return VirtualLocationManager.get().getMode(userId, packageName) != VirtualLocationManager.MODE_CLOSE;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isProviderEnabled(String provider) {
        return LocationManager.GPS_PROVIDER.equals(provider);
    }

    public VLocation getLocation(String packageName, int userId) {
        return getVirtualLocation(packageName, null, userId);
    }

    public VLocation getCurAppLocation() {
        return getVirtualLocation(VClient.get().getCurrentPackage(), null, VUserHandle.myUserId());
    }

    public VLocation getVirtualLocation(String packageName, Location loc, int userId) {
        try {
            if (VirtualLocationManager.get().getMode(userId, packageName) == VirtualLocationManager.MODE_USE_GLOBAL) {
                return VirtualLocationManager.get().getGlobalLocation();
            } else {
                return VirtualLocationManager.get().getLocation(userId, packageName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getPackageName() {
        return VClient.get().getCurrentPackage();
    }

    public void removeGpsStatusListener(final Object[] args) {
        if (args[0] instanceof PendingIntent) {
            return;
        }
        boolean needStop;
        synchronized (mGpsListeners) {
            mGpsListeners.remove(args[0]);
            needStop = mGpsListeners.size() == 0;
        }
        if (needStop) {
            stopGpsTask();
        }
    }


    public void addGpsStatusListener(final Object[] args) {
        final Object GpsStatusListenerTransport = args[0];
        MockLocationHelper.invokeSvStatusChanged(GpsStatusListenerTransport);
        if (GpsStatusListenerTransport != null) {
            synchronized (mGpsListeners) {
                mGpsListeners.add(GpsStatusListenerTransport);
            }
        }
        checkWork();
        notifyGpsStatus(GpsStatusListenerTransport);
        startGpsTask();
    }

    private void notifyGpsStatus(final Object transport) {
        if (transport == null) {
            return;
        }
//        checkWork();
        mWorkHandler.post(new Runnable() {
            @Override
            public void run() {
//                GpsStatusGenerate.fakeGpsStatus(transport);
                MockLocationHelper.invokeSvStatusChanged(transport);
                MockLocationHelper.invokeNmeaReceived(transport);
            }
        });
    }

    public void removeUpdates(final Object[] args) {
        if (args[0] != null) {
            UpdateLocationTask task = getTask(args[0]);
            if (task != null) {
                task.stop();
            }
        }
    }

    public void requestLocationUpdates(Object[] args) {
        //15-16 last
        final int index;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            index = 1;
        } else {
            index = args.length - 1;
        }
        final Object listenerTransport = args[index];
        if (listenerTransport == null) {
            Log.e(TAG, "ListenerTransport:null");
        } else {
            //mInterval
            long mInterval;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    mInterval = Reflect.on(args[0]).get("mInterval");
                } catch (Throwable e) {
                    mInterval = 60 * 1000;
                }
            } else {
                mInterval = MethodParameterUtils.getFirstParam(args, Long.class);
            }
            VLocation location = getCurAppLocation();
            checkWork();
            notifyLocation(listenerTransport, location.toSysLocation(), true);
            UpdateLocationTask task = getTask(listenerTransport);
            if (task == null) {
                synchronized (mLocationTaskMap) {
                    task = new UpdateLocationTask(listenerTransport, mInterval);
                    mLocationTaskMap.put(listenerTransport, task);
                }
            }
            task.start();
        }
    }

    private boolean notifyLocation(final Object ListenerTransport, final Location location, boolean post) {
        if (ListenerTransport == null) {
            return false;
        }
        if (!post) {
            try {
                mirror.android.location.LocationManager.ListenerTransport.onLocationChanged.call(ListenerTransport, location);
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return false;
        }
        mWorkHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mirror.android.location.LocationManager.ListenerTransport.onLocationChanged.call(ListenerTransport, location);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }

    private final Map<Object, UpdateLocationTask> mLocationTaskMap = new HashMap<>();

    private UpdateLocationTask getTask(Object locationListener) {
        UpdateLocationTask task;
        synchronized (mLocationTaskMap) {
            task = mLocationTaskMap.get(locationListener);
        }
        return task;
    }

    private class UpdateLocationTask implements Runnable {
        private Object mListenerTransport;
        private long mTime;
        private volatile boolean mRunning;

        private UpdateLocationTask(Object ListenerTransport, long time) {
            mListenerTransport = ListenerTransport;
            mTime = time;
        }

        @Override
        public void run() {
            if (mRunning) {
                VLocation location = getCurAppLocation();
                if (location != null) {
                    if (notifyLocation(mListenerTransport, location.toSysLocation(), false)) {
                        start();
                    }
                }
            }
        }

        public void start() {
            mRunning = true;
            mWorkHandler.removeCallbacks(this);
            if (mTime > 0) {
                mWorkHandler.postDelayed(this, mTime);
            } else {
                mWorkHandler.post(this);
            }
        }

        public void stop() {
            mRunning = false;
            mWorkHandler.removeCallbacks(this);
        }
    }
}
