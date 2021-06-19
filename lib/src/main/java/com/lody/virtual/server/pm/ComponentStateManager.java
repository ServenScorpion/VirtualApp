package com.lody.virtual.server.pm;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.lody.virtual.helper.PersistenceLayer;
import com.lody.virtual.os.VEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 */
public class ComponentStateManager extends PersistenceLayer {

    private static final ComponentStateManager sInstance = new ComponentStateManager();
    private SparseArray<UserComponentState> states = new SparseArray<>();

    public static ComponentStateManager get() {
        return sInstance;
    }

    public ComponentStateManager() {
        super(VEnvironment.getComponentStateFile());
    }


    public int getComponentState(ComponentName component, int userId) {
        synchronized (this) {
            return getOrCreate(userId).getOrCreate(component).state;
        }
    }

    public void setComponentState(ComponentName component, int state, int userId) {
        synchronized (this) {
            getOrCreate(userId).getOrCreate(component).state = state;
            save();
        }
    }

    public void clearAll(int userId){
        synchronized (this) {
            if(states.indexOfKey(userId) >= 0){
                states.remove(userId);
                save();
            }
        }
    }

    private UserComponentState getOrCreate(int userId) {
        UserComponentState state;
        state = states.get(userId);
        if (state == null) {
            state = new UserComponentState();
            states.put(userId, state);
        }
        return state;
    }

    @Override
    public int getCurrentVersion() {
        return 1;
    }

    @Override
    public void writePersistenceData(Parcel p) {
        p.writeSparseArray((SparseArray) states);
    }

    @Override
    public void readPersistenceData(Parcel p, int version) {
        states = p.readSparseArray(UserComponentState.class.getClassLoader());
    }

    static class UserComponentState implements Parcelable {
        Map<ComponentName, ComponentState> states = new HashMap<>();

        UserComponentState() {
        }

        public ComponentState getOrCreate(ComponentName component) {
            ComponentState state = states.get(component);
            if (state == null) {
                state = new ComponentState();
                states.put(component, state);
            }
            return state;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.states.size());
            dest.writeMap(states);
        }

        protected UserComponentState(Parcel in) {
            in.readMap(states, ComponentState.class.getClassLoader());
        }

        public static final Parcelable.Creator<UserComponentState> CREATOR = new Parcelable.Creator<UserComponentState>() {
            @Override
            public UserComponentState createFromParcel(Parcel source) {
                return new UserComponentState(source);
            }

            @Override
            public UserComponentState[] newArray(int size) {
                return new UserComponentState[size];
            }
        };
    }


}
