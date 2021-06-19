package com.lody.virtual.server.content;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;

import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.server.accounts.RegisteredServicesParser;
import com.lody.virtual.server.pm.VPackageManagerService;

import org.xmlpull.v1.XmlPullParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mirror.android.content.SyncAdapterTypeN;
import mirror.com.android.internal.R_Hide;

/**
 * @author Lody
 */
public class SyncAdaptersCache {

    private Context mContext;
    private final Map<String, SyncAdapterInfo> mSyncAdapterInfos = new HashMap<>();

    public SyncAdaptersCache(Context context) {
        this.mContext = context;
    }

    public static class SyncAdapterInfo {
        public SyncAdapterType type;
        public ServiceInfo serviceInfo;
        public ComponentName componentName;

        SyncAdapterInfo(SyncAdapterType adapterType, ServiceInfo serviceInfo) {
            this.type = adapterType;
            this.serviceInfo = serviceInfo;
            this.componentName = ComponentUtils.toComponentName(serviceInfo);
        }

    }

    public void refreshServiceCache(String packageName) {
        Intent intent = new Intent("android.content.SyncAdapter");
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        generateServicesMap(
                VPackageManagerService.get().queryIntentServices(
                        intent, null, PackageManager.GET_META_DATA, 0
                ),
                mSyncAdapterInfos,
                new RegisteredServicesParser()
        );
    }

    public SyncAdapterInfo getServiceInfo(Account account, String providerName) {
        synchronized (mSyncAdapterInfos) {
            return mSyncAdapterInfos.get(account.type + "/" + providerName);
        }
    }

    public Collection<SyncAdapterInfo> getAllServices() {
        return mSyncAdapterInfos.values();
    }

    private void generateServicesMap(List<ResolveInfo> services, Map<String, SyncAdapterInfo> map,
                                     RegisteredServicesParser accountParser) {
        for (ResolveInfo info : services) {
            XmlResourceParser parser = accountParser.getParser(mContext, info.serviceInfo, "android.content.SyncAdapter");
            if (parser != null) {
                try {
                    AttributeSet attributeSet = Xml.asAttributeSet(parser);
                    int type;
                    while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
                        // Nothing to do
                    }
                    if ("sync-adapter".equals(parser.getName())) {
                        SyncAdapterType adapterType = parseSyncAdapterType(
                                accountParser.getResources(mContext, info.serviceInfo.applicationInfo), attributeSet);
                        if (adapterType != null) {
                            String key = adapterType.accountType + "/" + adapterType.authority;
                            map.put(key, new SyncAdapterInfo(adapterType, info.serviceInfo));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private SyncAdapterType parseSyncAdapterType(Resources res, AttributeSet set) {
        TypedArray obtainAttributes = res.obtainAttributes(set, R_Hide.styleable.SyncAdapter.get());
        try {
            String contentAuthority = obtainAttributes.getString(R_Hide.styleable.SyncAdapter_contentAuthority.get());
            String accountType = obtainAttributes.getString(R_Hide.styleable.SyncAdapter_accountType.get());
            if (contentAuthority == null || accountType == null) {
                obtainAttributes.recycle();
                return null;
            }
            boolean userVisible = obtainAttributes.getBoolean(R_Hide.styleable.SyncAdapter_userVisible.get(), true);
            boolean supportsUploading = obtainAttributes.getBoolean(R_Hide.styleable.SyncAdapter_supportsUploading.get(), true);
            boolean isAlwaysSyncable = obtainAttributes.getBoolean(R_Hide.styleable.SyncAdapter_isAlwaysSyncable.get(), true);
            boolean allowParallelSyncs = obtainAttributes.getBoolean(R_Hide.styleable.SyncAdapter_allowParallelSyncs.get(), true);
            String settingsActivity = obtainAttributes.getString(R_Hide.styleable.SyncAdapter_settingsActivity.get());
            SyncAdapterType type;
            if (SyncAdapterTypeN.ctor != null) {
                type = SyncAdapterTypeN.ctor.newInstance(contentAuthority, accountType, userVisible, supportsUploading, isAlwaysSyncable, allowParallelSyncs, settingsActivity, null);
                obtainAttributes.recycle();
                return type;
            }
            type = mirror.android.content.SyncAdapterType.ctor.newInstance(contentAuthority, accountType, userVisible, supportsUploading, isAlwaysSyncable, allowParallelSyncs, settingsActivity);
            obtainAttributes.recycle();
            return type;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
