package com.xdja.activitycounter;

import android.os.RemoteException;
import android.util.Log;

/**
 * @Date 19-03-20 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class FloatIconBallManager extends BaseCounterManager{

    private String TAG = "FloatIconBallManager";

    protected void changeState(int mode, boolean show,String name) {
        Log.d(TAG,"floaticonball "+show );
        if(mForegroundInterface==null){
            Log.e(TAG,"FloatIconBall Callback is null.");
            return;
        }

        try {
            mForegroundInterface.isForeground(show);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
