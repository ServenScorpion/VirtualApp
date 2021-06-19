// IForegroundInterface.aidl
package com.xdja.activitycounter;

// Declare any non-default types here with import statements

interface IForegroundInterface {

    void isForeground(boolean is);
    void screenChanged(int state);
}
