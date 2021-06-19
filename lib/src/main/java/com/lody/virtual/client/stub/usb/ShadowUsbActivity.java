package com.lody.virtual.client.stub.usb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.lody.virtual.R;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ShadowUsbActivity extends Activity {
    private static final String TAG = "ShadowUsbActivity";
    public static final String ADB_INTERFACE_NAME = "ADB Interface";
    public static final String AOAP_INTERFACE_NAME = "Android Accessory Interface";
    public static final String MTP_INTERFACE_NAME = "MTP";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        Intent input = getIntent();
        Intent target = new Intent(input);
        target.setComponent(null);
        target.setPackage(null);
        target.putExtras(input);


        UsbDevice device = target.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device == null) {
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            LinkedList<UsbDevice> devices = findAllPossibleAndroidDevices(usbManager);
            if (devices.size() > 0) {
                device = devices.getLast();
            }
        }
        if (device == null) {
            Toast.makeText(this, R.string.tip_invalid_usb_device, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "device:" + device);
        ArrayList<ResolveInfo> matches = new ArrayList<ResolveInfo>();
        findAllActivites(device, matches);
        if (matches.size() == 0) {
            Toast.makeText(this, getString(R.string.tip_not_found_usb_app), Toast.LENGTH_SHORT).show();
        } else if (matches.size() == 1) {
            // Single match, launch directly
            ResolveInfo info = matches.get(0);
            target.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            VActivityManager.get().startActivity(target, 0);
        } else {
            // Multiple matches, show a custom activity chooser dialog
            Intent chooser = new Intent(this, UsbListChooserActivity.class);
            chooser.putExtra(Intent.EXTRA_INTENT, target);
            chooser.putParcelableArrayListExtra(UsbListChooserActivity.EXTRA_RESOLVE_INFOS,
                    matches);
            startActivity(chooser);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static LinkedList<UsbDevice> findAllPossibleAndroidDevices(UsbManager usbManager) {
        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        LinkedList<UsbDevice> androidDevices = null;
        for (UsbDevice device : devices.values()) {
            if (possiblyAndroid(device)) {
                if (androidDevices == null) {
                    androidDevices = new LinkedList<>();
                }
                androidDevices.add(device);
            }
        }
        return androidDevices;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static boolean possiblyAndroid(UsbDevice device) {
        int numInterfaces = device.getInterfaceCount();
        for (int i = 0; i < numInterfaces; i++) {
            UsbInterface usbInterface = device.getInterface(i);
            String interfaceName = usbInterface.getName();
            // more thorough check can be added, later
            if (AOAP_INTERFACE_NAME.equals(interfaceName) ||
                    ADB_INTERFACE_NAME.equals(interfaceName) ||
                    MTP_INTERFACE_NAME.equals(interfaceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * packages/apps/Nfc/src/com/android/nfc/RegisteredComponentCache.java
     */
    private void findAllActivites(UsbDevice device, ArrayList<ResolveInfo> matches) {
        Intent intent = new Intent();
        List<ResolveInfo> resolveInfos = VPackageManager.get().queryIntentActivities(
                intent, null, PackageManager.GET_ACTIVITIES|PackageManager.GET_META_DATA, 0);
        Log.d(TAG, " find usb activities:" + resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            ActivityInfo ai = resolveInfo.activityInfo;
            if (ai.metaData == null || ai.metaData.getInt(UsbManager.ACTION_USB_DEVICE_ATTACHED) == 0) {
                continue;
            }
            try (XmlResourceParser parser = ai.loadXmlMetaData(getPackageManager(),
                    UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                if (parser == null) {
                    continue;
                }

                XmlUtils.nextElement(parser);
                while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                    if ("usb-device".equals(parser.getName())) {
                        DeviceFilter filter = DeviceFilter.read(parser);
                        if (filter.matches(device)) {
                            matches.add(resolveInfo);
                        } else {
                            Log.d(TAG, ai + " don't match");
                        }
                    }
                    XmlUtils.nextElement(parser);
                }
            } catch (Exception e) {
                Log.w(TAG, "Unable to load component info " + ai.toString(), e);
            }
        }
    }
}
