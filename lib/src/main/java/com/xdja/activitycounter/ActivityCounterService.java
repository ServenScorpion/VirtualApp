package com.xdja.activitycounter;

import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Date 18-11-28 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class ActivityCounterService extends IActivityCounterService.Stub {

    public static final int ADD = 0;
    public static final int RDU = 1;
    static final String TAG = ActivityCounterService.class.getName();

    private static final Singleton<ActivityCounterService> sService = new Singleton<ActivityCounterService>() {
        @Override
        protected ActivityCounterService create() {
            return new ActivityCounterService();
        }
    };

    public static ActivityCounterService get() {
        return sService.get();
    }
    /*
      when process is died set 0 to activityCounter & clear ProcessMap
     */
    // all of activity showing in process
    private Map<Integer,Integer> mActivityCounter = new ConcurrentHashMap<>();
    //all of process about of pkg
    private Map<Integer,String> mProcessesMap = new HashMap<>();

    @Override
    public void activityCountAdd(String pkg, String name, int pid) throws RemoteException {
        activityCounter(pkg,name,pid,ADD);
    }

    @Override
    public void activityCountReduce(String pkg, String name, int pid) throws RemoteException {
        activityCounter(pkg,name,pid,RDU);
    }

    @Override
    synchronized public boolean isForeGroundApp(String pkg) throws RemoteException {
        int count = 0;
        //get pid s
        for (int id: mProcessesMap.keySet()){
            if(mProcessesMap.get(id)!=null&&mProcessesMap.get(id).equalsIgnoreCase(pkg)){
                //get & add counter
                if(mActivityCounter.containsKey(id))
                    count += mActivityCounter.get(id);
            }
        }
        return count > 0;
    }

    @Override
    synchronized public boolean isForeGround() throws RemoteException {

        return getFroundCount() > 0;
    }

    public int getFroundCount(){
        int count = 0;
        //get all count of pid
        for(int id:mActivityCounter.keySet()){
            String packagename = mProcessesMap.get(id);
            Log.e(TAG,packagename);
            Log.e(TAG,"count "+mActivityCounter.get(id));
            count += mActivityCounter.get(id);
        }
        Log.d(TAG,"getFroundCount "+count);
        return count;
    }
    //clean count & pkg about of process
    synchronized public void cleanProcess(int pid){
        mActivityCounter.remove(pid);
        mProcessesMap.remove(pid);
    }
    //clean count about of packagename
    synchronized public void cleanPackage(String pkg){
        //get pid s
        List<Integer> pids  = new ArrayList<>();
        for (int id: mProcessesMap.keySet()){
            if(mProcessesMap.get(id)!=null&&mProcessesMap.get(id).equalsIgnoreCase(pkg)){
                pids.add(id);
            }
        }
        for (int id:pids){
            //put id 0
            cleanProcess(id);
        }
    }
    synchronized private void activityCounter(String pkg,String name,int pid, int mode){
        int num;
        if(mActivityCounter.containsKey(pid)){
            num = mActivityCounter.get(pid);
        }else{
            num = 0;
        }
        if(mode==ADD)
            num+=1;
        else if(mode==RDU){
            num-=1;
            num = num<0?0:num;
        }
        if(num > 0){
            mActivityCounter.put(pid,num);
            if(!mProcessesMap.containsKey(pid))
                mProcessesMap.put(pid,pkg);
        }
        else{
            mActivityCounter.remove(pid);
            mProcessesMap.remove(pid);
        }
        //回调上层
        CounterCallbackManager.get().nofityFroundChange(getFroundCount(), mode,name);
    }

    @Override
    public void registerCallback(IForegroundInterface fibCallback) throws RemoteException {
        if(fibCallback != null){
            CounterCallbackManager.get().registerCallback(fibCallback);
        }else {
            VLog.e(TAG, "ActivityCounterService csCallback is null, registerCallback failed");
        }
    }
    @Override
    public void unregisterCallback() throws RemoteException {
        CounterCallbackManager.get().unregisterCallback();
    }
}
