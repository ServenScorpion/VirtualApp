package com.lody.virtual.client.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

public class ServiceRecord extends Binder {
    private static final String TAG = "ServiceRecord";
    private final Map<Intent.FilterComparison, BoundInfo> mBoundInfos = new HashMap<>();
    Service service;

    class BoundInfo {
        int connectCount;
        IBinder binder;
        boolean shouldRebind;
    }

    ServiceRecord() {
    }

    boolean decreaseConnectionCount(Intent intent) {
        Intent.FilterComparison comparison = new Intent.FilterComparison(intent);
        BoundInfo info = mBoundInfos.get(comparison);
        if (info == null) {
            return true;
        }
        info.connectCount--;
        if (info.connectCount <= 0) {
            mBoundInfos.remove(comparison);
            return true;
        }
        return false;
    }

    IBinder getBinder(Intent intent) {
        Intent.FilterComparison comparison = new Intent.FilterComparison(intent);
        BoundInfo info = mBoundInfos.get(comparison);
        if (info != null) {
            return info.binder;
        }
        return null;
    }

    boolean hasBinder(Intent intent) {
        Intent.FilterComparison comparison = new Intent.FilterComparison(intent);
        BoundInfo info = mBoundInfos.get(comparison);
        if (info != null) {
            return info.binder != null;
        }
        return false;
    }

    private BoundInfo getOrCreateBoundInfo(Intent intent) {
        return getOrCreateBoundInfo(new Intent.FilterComparison(intent));
    }

    private BoundInfo getOrCreateBoundInfo(Intent.FilterComparison comparison) {
        BoundInfo info = mBoundInfos.get(comparison);
        if (info == null) {
            info = new BoundInfo();
            mBoundInfos.put(comparison, info);
        }
        return info;
    }

    void increaseConnectionCount(Intent intent) {
        BoundInfo info = getOrCreateBoundInfo(intent);
        info.connectCount++;
    }

    void setBinder(Intent intent, IBinder binder) {
        BoundInfo info = getOrCreateBoundInfo(intent);
        info.binder = binder;
    }

    void setShouldRebind(Intent intent, boolean shouldRebind) {
        Intent.FilterComparison comparison = new Intent.FilterComparison(intent);
        BoundInfo info = getOrCreateBoundInfo(comparison);
        info.shouldRebind = shouldRebind;
        if (!shouldRebind) {
            mBoundInfos.remove(comparison);
        }
    }

    boolean shouldRebind(Intent intent) {
        BoundInfo info = mBoundInfos.get(new Intent.FilterComparison(intent));
        if (info == null) {
            return false;
        }
        return info.shouldRebind;
    }
}