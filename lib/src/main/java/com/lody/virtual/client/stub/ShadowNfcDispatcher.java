package com.lody.virtual.client.stub;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.lody.virtual.R;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShadowNfcDispatcher extends Activity {
    private static final String TAG = "ShadowNfcDispatcher";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        Intent input = getIntent();
        Intent target = new Intent(input);
        target.setComponent(null);
        target.setPackage(null);
        target.putExtras(input);

        ArrayList<ResolveInfo> matches = new ArrayList<ResolveInfo>();
        ArrayList<ComponentInfo> registered = generateComponentsList();
        Tag tag = input.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            Toast.makeText(this, R.string.tip_invalid_nfc_tag, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] tagTechs = tag.getTechList();
        Arrays.sort(tagTechs);
        // Check each registered activity to see if it matches
        for (ComponentInfo info : registered) {
            // Don't allow wild card matching
            if (filterMatch(tagTechs, info.techs) &&
                    isComponentEnabled(info.resolveInfo)) {
                // Add the activity as a match if it's not already in the list
                // Check if exported flag is not explicitly set to false to prevent
                // SecurityExceptions.
                if (!matches.contains(info.resolveInfo) && info.resolveInfo.activityInfo.exported) {
                    matches.add(info.resolveInfo);
                }
            }
        }
        if (matches.size() == 0) {
            Toast.makeText(this, getString(R.string.tip_not_found_nfc_app), Toast.LENGTH_SHORT).show();
        } else if (matches.size() == 1) {
            // Single match, launch directly
            ResolveInfo info = matches.get(0);
            target.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            VActivityManager.get().startActivity(target, 0);
        } else {
            // Multiple matches, show a custom activity chooser dialog
            Intent chooser = new Intent(this, TechListChooserActivity.class);
            chooser.putExtra(Intent.EXTRA_INTENT, target);
            chooser.putParcelableArrayListExtra(TechListChooserActivity.EXTRA_RESOLVE_INFOS,
                    matches);
            startActivity(chooser);
        }
//
//        Bundle extras = new Bundle();
//        extras.putInt(Constants.EXTRA_USER_HANDLE, 0);
//        extras.putBundle(ChooserActivity.EXTRA_DATA, null);
//        extras.putString(ChooserActivity.EXTRA_WHO, null);
//        extras.putInt(ChooserActivity.EXTRA_REQUEST_CODE, 0);
//        extras.putBoolean(ChooserActivity.EXTRA_IGNORE_DEFAULT, true);
//        extras.putParcelable(Intent.EXTRA_INTENT, target);
//
//
//        Intent intent = new Intent();
//        intent.setComponent(new ComponentName(StubManifest.PACKAGE_NAME, ChooserActivity.class.getName()));
//        intent.putExtras(extras);
//        intent.putExtra("_VA_CHOOSER", true);
//        startActivity(intent);
    }


    boolean filterMatch(String[] tagTechs, String[] filterTechs) {
        if (filterTechs == null || filterTechs.length == 0) return false;
        for (String tech : filterTechs) {
            if (Arrays.binarySearch(tagTechs, tech) < 0) {
                return false;
            }
        }
        return true;
    }

    static boolean isComponentEnabled(ResolveInfo info) {
        boolean enabled = false;
        ComponentName compname = new ComponentName(
                info.activityInfo.packageName, info.activityInfo.name);
        try {
            // Note that getActivityInfo() will internally call
            // isEnabledLP() to determine whether the component
            // enabled. If it's not, null is returned.
            if (VirtualCore.get().resolveActivityInfo(compname, 0) != null) {
                enabled = true;
            }
        } catch (Throwable e) {
            enabled = false;
        }
        if (!enabled) {
            Log.d(TAG, "Component not enabled: " + compname);
        }
        return enabled;
    }

    /**
     * packages/apps/Nfc/src/com/android/nfc/RegisteredComponentCache.java
     */
    private ArrayList<ComponentInfo> generateComponentsList() {
        ArrayList<ComponentInfo> components = new ArrayList<>();
        Intent intent = new Intent(NfcAdapter.ACTION_TECH_DISCOVERED);
        List<ResolveInfo> resolveInfos = VPackageManager.get().queryIntentActivities(
                intent, null, PackageManager.GET_META_DATA, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            try {
                parseComponentInfo(VirtualCore.getPM(), resolveInfo, components);
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Unable to load component info " + resolveInfo.toString(), e);
            } catch (IOException e) {
                Log.w(TAG, "Unable to load component info " + resolveInfo.toString(), e);
            }
        }
        return components;
    }

    void parseComponentInfo(PackageManager pm, ResolveInfo info,
                            ArrayList<ComponentInfo> components) throws XmlPullParserException, IOException {
        String mMetaDataName = NfcAdapter.ACTION_TECH_DISCOVERED;
        ActivityInfo ai = info.activityInfo;

        XmlResourceParser parser = null;
        try {
            parser = ai.loadXmlMetaData(pm, mMetaDataName);
            if (parser == null) {
                throw new XmlPullParserException("No " + mMetaDataName + " meta-data");
            }
            parseTechLists(pm.getResourcesForApplication(ai.applicationInfo), ai.packageName,
                    parser, info, components);
        } catch (PackageManager.NameNotFoundException e) {
            throw new XmlPullParserException("Unable to load resources for " + ai.packageName);
        } finally {
            if (parser != null) parser.close();
        }
    }

    void parseTechLists(Resources res, String packageName, XmlPullParser parser,
                        ResolveInfo resolveInfo, ArrayList<ComponentInfo> components)
            throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.START_TAG) {
            eventType = parser.next();
        }

        ArrayList<String> items = new ArrayList<String>();
        String tagName;
        eventType = parser.next();
        do {
            tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG && "tech".equals(tagName)) {
                items.add(parser.nextText());
            } else if (eventType == XmlPullParser.END_TAG && "tech-list".equals(tagName)) {
                int size = items.size();
                if (size > 0) {
                    String[] techs = new String[size];
                    techs = items.toArray(techs);
                    items.clear();
                    components.add(new ComponentInfo(resolveInfo, techs));
                }
            }
            eventType = parser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);
    }

    public static class ComponentInfo {
        public final ResolveInfo resolveInfo;
        public final String[] techs;

        ComponentInfo(ResolveInfo resolveInfo, String[] techs) {
            this.resolveInfo = resolveInfo;
            this.techs = techs;
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder("ComponentInfo: ");
            out.append(resolveInfo);
            out.append(", techs: ");
            for (String tech : techs) {
                out.append(tech);
                out.append(", ");
            }
            return out.toString();
        }
    }
}
