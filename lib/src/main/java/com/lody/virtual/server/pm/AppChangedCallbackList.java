package com.lody.virtual.server.pm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lody
 */
public class AppChangedCallbackList {
    private static final AppChangedCallbackList sInstance = new AppChangedCallbackList();
    private List<IAppChangedCallback> mList = new ArrayList<>(2);

    public static AppChangedCallbackList get() {
        return sInstance;
    }

    public void register(IAppChangedCallback callback) {
        mList.add(callback);
    }

    public void unregister(IAppChangedCallback callback) {
        mList.remove(callback);
    }

    /*package*/ void notifyCallbacks(boolean removed) {
        for (IAppChangedCallback callback : mList) {
            callback.onCallback(removed);
        }
    }
}
