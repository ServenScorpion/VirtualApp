package com.xdja.activitycounter;

import android.os.RemoteException;

import com.lody.virtual.helper.utils.VLog;

/**
 * @Date 19-7-23 11
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class CounterCallbackManager {

    private final String TAG = CounterCallbackManager.class.getName();
    private static CounterCallbackManager sInstance = new CounterCallbackManager();
    public static CounterCallbackManager get() {
        if(sInstance==null){
            sInstance = new CounterCallbackManager();
        }
        return sInstance;
    }

    private FloatIconBallManager floatIconBallManager = new FloatIconBallManager();
    private ScreenLockManager screenLockManager = new ScreenLockManager();

    public void registerCallback(IForegroundInterface fibCallback) throws RemoteException {
        if(fibCallback != null){
            floatIconBallManager.mForegroundInterface = fibCallback;
            screenLockManager.mForegroundInterface = fibCallback;
        }else {
            VLog.e(TAG, "ActivityCounterService csCallback is null, registerCallback failed");
        }
    }
    public void unregisterCallback() throws RemoteException {
        floatIconBallManager.mForegroundInterface = null;
        screenLockManager.mForegroundInterface = null;
    }
    public void nofityFroundChange(int count, int mode, String name){
        floatIconBallManager.nofityFroundChange(count, mode,name);
        screenLockManager.nofityFroundChange(count,mode,name);
    }

}
