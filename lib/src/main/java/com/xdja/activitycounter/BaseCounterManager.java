package com.xdja.activitycounter;
/**
 * @Date 19-03-20 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
abstract public class BaseCounterManager {

    public IForegroundInterface mForegroundInterface = null;
    protected boolean mShow = false;
    public void nofityFroundChange(int count, int mode, String name){

        if(mode==ActivityCounterService.ADD && count > 0 && !mShow){
            mShow = true;
            changeState(mode,true, name);
        }
        else if(mode==ActivityCounterService.RDU && count<=0 && mShow){
            mShow = false;
            changeState(mode,false, name);
        }
    }
    abstract void changeState(int mode, boolean on,String name);
}
