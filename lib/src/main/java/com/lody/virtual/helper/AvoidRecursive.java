package com.lody.virtual.helper;

/**
 * @author Lody
 */
public class AvoidRecursive {

    private boolean mCalling = false;

    public boolean beginCall() {
        if (mCalling) {
            return false;
        }
        mCalling = true;
        return true;
    }

    public void finishCall() {
        mCalling = false;
    }


}
