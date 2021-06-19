package com.lody.virtual.helper;

import android.util.SparseBooleanArray;

/**
 * @author Lody
 */
public class MultiAvoidRecursive {

    private SparseBooleanArray mCallings;

    public MultiAvoidRecursive(int initialCapacity) {
        mCallings = new SparseBooleanArray(initialCapacity);
    }

    public MultiAvoidRecursive() {
        this(7);
    }

    public boolean beginCall(int id) {
        if (mCallings.get(id)) {
            return false;
        }
        mCallings.put(id, true);
        return true;
    }

    public void finishCall(int id) {
        mCallings.put(id, false);
    }


}
