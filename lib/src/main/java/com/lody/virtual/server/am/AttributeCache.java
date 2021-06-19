/*
 **
 ** Copyright 2007, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.lody.virtual.server.am;

import java.util.HashMap;
import java.util.WeakHashMap;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.SparseArray;

import com.lody.virtual.client.core.VirtualCore;

/**
 * TODO: This should be better integrated into the system so it doesn't need
 * special calls from the activity manager to clear it.
 */
public final class AttributeCache {
    private static final AttributeCache sInstance = new AttributeCache();

    private final WeakHashMap<String, Package> mPackages = new WeakHashMap<String, Package>();
    private final Configuration mConfiguration = new Configuration();

    private AttributeCache() {
    }

    public static AttributeCache instance() {
        return sInstance;
    }

    public void removePackage(String packageName) {
        synchronized (this) {
            mPackages.remove(packageName);
        }
    }

    public void updateConfiguration(Configuration config) {
        synchronized (this) {
            int changes = mConfiguration.updateFrom(config);
            if ((changes & ~(ActivityInfo.CONFIG_FONT_SCALE | ActivityInfo.CONFIG_KEYBOARD_HIDDEN
                    | ActivityInfo.CONFIG_ORIENTATION)) != 0) {
                // The configurations being masked out are ones that commonly
                // change so we don't want flushing the cache... all others
                // will flush the cache.
                mPackages.clear();
            }
        }
    }

    public Entry get(String packageName, int resId, int[] styleable) {
        synchronized (this) {
            Package pkg = mPackages.get(packageName);
            HashMap<int[], Entry> map = null;
            Entry ent;
            if (pkg != null) {
                map = pkg.mMap.get(resId);
                if (map != null) {
                    ent = map.get(styleable);
                    if (ent != null) {
                        return ent;
                    }
                }
            } else {
                Resources res;
                try {
                    res = VirtualCore.get().getResources(packageName);
                } catch (Throwable e) {
                    return null;
                }
                pkg = new Package(res);
                mPackages.put(packageName, pkg);
            }

            if (map == null) {
                map = new HashMap<>();
                pkg.mMap.put(resId, map);
            }

            try {
                ent = new Entry(pkg.resources, pkg.resources.newTheme().obtainStyledAttributes(resId, styleable));
                map.put(styleable, ent);
            } catch (Resources.NotFoundException e) {
                return null;
            }

            return ent;
        }
    }

    public final static class Package {
        public final Resources resources;
        private final SparseArray<HashMap<int[], Entry>> mMap = new SparseArray<HashMap<int[], Entry>>();

        public Package(Resources res) {
            resources = res;
        }
    }

    public final static class Entry {
        public final Resources resource;
        public final TypedArray array;

        public Entry(Resources res, TypedArray ta) {
            resource = res;
            array = ta;
        }
    }
}
