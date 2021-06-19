package com.lody.virtual.remote;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.lody.virtual.os.VEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Lody
 */
public class VDeviceConfig implements Parcelable {

    private static final UsedDeviceInfoPool mPool = new UsedDeviceInfoPool();

    private static final class UsedDeviceInfoPool {
        final List<String> deviceIds = new ArrayList<>();
        final List<String> androidIds = new ArrayList<>();
        final List<String> wifiMacs = new ArrayList<>();
        final List<String> bluetoothMacs = new ArrayList<>();
        final List<String> iccIds = new ArrayList<>();
    }

    public static final int VERSION = 3;

    public boolean enable;

    public String deviceId;
    public String androidId;
    public String wifiMac;
    public String bluetoothMac;
    public String iccId;
    public String serial;
    public String gmsAdId;

    public final Map<String, String> buildProp = new HashMap<>();

    public VDeviceConfig() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.enable ? (byte) 1 : (byte) 0);
        dest.writeString(this.deviceId);
        dest.writeString(this.androidId);
        dest.writeString(this.wifiMac);
        dest.writeString(this.bluetoothMac);
        dest.writeString(this.iccId);
        dest.writeString(this.serial);
        dest.writeString(this.gmsAdId);
        dest.writeInt(this.buildProp.size());
        for (Map.Entry<String, String> entry : this.buildProp.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    public VDeviceConfig(Parcel in) {
        this.enable = in.readByte() != 0;
        this.deviceId = in.readString();
        this.androidId = in.readString();
        this.wifiMac = in.readString();
        this.bluetoothMac = in.readString();
        this.iccId = in.readString();
        this.serial = in.readString();
        this.gmsAdId = in.readString();
        int buildPropSize = in.readInt();
        for (int i = 0; i < buildPropSize; i++) {
            String key = in.readString();
            String value = in.readString();
            this.buildProp.put(key, value);
        }
    }

    public static final Creator<VDeviceConfig> CREATOR = new Creator<VDeviceConfig>() {
        @Override
        public VDeviceConfig createFromParcel(Parcel source) {
            return new VDeviceConfig(source);
        }

        @Override
        public VDeviceConfig[] newArray(int size) {
            return new VDeviceConfig[size];
        }
    };

    public String getProp(String key) {
        return buildProp.get(key);
    }

    public void setProp(String key, String value) {
        buildProp.put(key, value);
    }


    public void clear() {
        deviceId = null;
        androidId = null;
        wifiMac = null;
        bluetoothMac = null;
        iccId = null;
        serial = null;
        gmsAdId = null;
    }

    public static VDeviceConfig random() {
        VDeviceConfig info = new VDeviceConfig();
        String value;
        do {
            value = generateDeviceId();
            info.deviceId = value;
        } while (mPool.deviceIds.contains(value));
        do {
            value = generateHex(System.currentTimeMillis(), 16);
            info.androidId = value;
        } while (mPool.androidIds.contains(value));
        do {
            value = generateMac();
            info.wifiMac = value;
        } while (mPool.wifiMacs.contains(value));
        do {
            value = generateMac();
            info.bluetoothMac = value;
        } while (mPool.bluetoothMacs.contains(value));

        do {
            value = generate10(System.currentTimeMillis(), 20);
            info.iccId = value;
        } while (mPool.iccIds.contains(value));

        info.serial = generateSerial();

        addToPool(info);
        return info;
    }


    public static String generateDeviceId() {
        return generate10(System.currentTimeMillis(), 15);
    }


    public static String generate10(long seed, int length) {
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String generateHex(long seed, int length) {
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int nextInt = random.nextInt(16);
            if (nextInt < 10) {
                sb.append(nextInt);
            } else {
                sb.append((char) ((nextInt - 10) + 'a'));
            }
        }
        return sb.toString();
    }

    private static String generateMac() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        int next = 1;
        int cur = 0;
        while (cur < 12) {
            int val = random.nextInt(16);
            if (val < 10) {
                sb.append(val);
            } else {
                sb.append((char) (val + 87));
            }
            if (cur == next && cur != 11) {
                sb.append(":");
                next += 2;
            }
            cur++;
        }
        return sb.toString();
    }

    @SuppressLint("HardwareIds")
    private static String generateSerial() {
        String serial;
        if (Build.SERIAL == null || Build.SERIAL.length() <= 0) {
            serial = "0123456789ABCDEF";
        } else {
            serial = Build.SERIAL;
        }
        List<Character> list = new ArrayList<>();
        for (char c : serial.toCharArray()) {
            list.add(c);
        }
        Collections.shuffle(list);
        StringBuilder sb = new StringBuilder();
        for (Character c : list) {
            sb.append(c.charValue());
        }
        return sb.toString();
    }


    public File getWifiFile(int userId, boolean is64Bit) {
        if (TextUtils.isEmpty(wifiMac)) {
            return null;
        }
        File wifiMacFie = VEnvironment.getWifiMacFile(userId, is64Bit);
        if (!wifiMacFie.exists()) {
            try {
                RandomAccessFile file = new RandomAccessFile(wifiMacFie, "rws");
                file.write((wifiMac + "\n").getBytes());
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wifiMacFie;
    }

    public static void addToPool(VDeviceConfig info) {
        mPool.deviceIds.add(info.deviceId);
        mPool.androidIds.add(info.androidId);
        mPool.wifiMacs.add(info.wifiMac);
        mPool.bluetoothMacs.add(info.bluetoothMac);
        mPool.iccIds.add(info.iccId);
    }
}
