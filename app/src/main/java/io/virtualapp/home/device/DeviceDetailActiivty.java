package io.virtualapp.home.device;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.remote.VDeviceConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.home.models.DeviceData;

public class DeviceDetailActiivty extends VActivity {

    private static final String TAG = "DeviceData";

    public static void open(Fragment fragment, DeviceData data, int position) {
        Intent intent = new Intent(fragment.getContext(), DeviceDetailActiivty.class);
        intent.putExtra("title", data.name);
        intent.putExtra("pkg", data.packageName);
        intent.putExtra("user", data.userId);
        intent.putExtra("pos", position);
        fragment.startActivityForResult(intent, 1001);
    }

    private String mPackageName;
    private String mTitle;
    private int mUserId;
    private int mPosition;
    private VDeviceConfig mDeviceConfig;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private EditText edt_androidId, edt_imei, edt_imsi, edt_mac;
    private EditText edt_brand, edt_model, edt_name, edt_device, edt_board, edt_display, edt_id, edt_serial, edt_manufacturer, edt_fingerprint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_mock_device);
        Toolbar toolbar = bind(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        enableBackHome();
        edt_androidId = (EditText) findViewById(R.id.edt_androidId);
        edt_imei = (EditText) findViewById(R.id.edt_imei);
        edt_imsi = (EditText) findViewById(R.id.edt_imsi);
        edt_mac = (EditText) findViewById(R.id.edt_mac);

        edt_brand = (EditText) findViewById(R.id.edt_brand);
        edt_model = (EditText) findViewById(R.id.edt_model);
        edt_name = (EditText) findViewById(R.id.edt_name);
        edt_device = (EditText) findViewById(R.id.edt_device);
        edt_board = (EditText) findViewById(R.id.edt_board);
        edt_display = (EditText) findViewById(R.id.edt_display);
        edt_id = (EditText) findViewById(R.id.edt_id);
        edt_serial = (EditText) findViewById(R.id.edt_serial);
        edt_manufacturer = (EditText) findViewById(R.id.edt_manufacturer);
        edt_fingerprint = (EditText) findViewById(R.id.edt_fingerprint);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (TextUtils.isEmpty(mTitle)) {
            mPackageName = getIntent().getStringExtra("pkg");
            mUserId = getIntent().getIntExtra("user", 0);
            mTitle = getIntent().getStringExtra("title");
        }
        setTitle(mTitle);
        mDeviceConfig = VDeviceManager.get().getDeviceConfig(mUserId);
        updateConfig();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mPackageName = intent.getStringExtra("pkg");
        mUserId = intent.getIntExtra("user", 0);
        mTitle = intent.getStringExtra("title");
        mPosition = intent.getIntExtra("pos", -1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device, menu);
        return true;
    }

    private void killApp() {
        if (TextUtils.isEmpty(mPackageName)) {
            VirtualCore.get().killAllApps();
        } else {
            VirtualCore.get().killApp(mPackageName, mUserId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save: {
                mDeviceConfig.enable = true;
                fillConfig();
                updateConfig();
                VDeviceManager.get().updateDeviceConfig(mUserId, mDeviceConfig);
                Intent intent = new Intent();
                intent.putExtra("pkg", mPackageName);
                intent.putExtra("user", mUserId);
                intent.putExtra("pos", mPosition);
                intent.putExtra("result", "save");
                setResult(RESULT_OK, intent);
                if (TextUtils.isEmpty(mPackageName)) {
                    VirtualCore.get().killAllApps();
                } else {
                    VirtualCore.get().killApp(mPackageName, mUserId);
                }
                killApp();
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            }
            break;
            case R.id.action_reset:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.dlg_reset_device)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            mDeviceConfig.enable = false;
                            mDeviceConfig.clear();
                            VDeviceManager.get().updateDeviceConfig(mUserId, mDeviceConfig);

                            Intent intent = new Intent();
                            intent.putExtra("pkg", mPackageName);
                            intent.putExtra("user", mUserId);
                            intent.putExtra("pos", mPosition);
                            intent.putExtra("result", "reset");
                            setResult(RESULT_OK, intent);
                            killApp();
                            updateConfig();
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private String getValue(EditText text) {
        return text.getText().toString().trim();
    }

    private void setValue(EditText text, String value, String defValue) {
        if (TextUtils.isEmpty(value)) {
            text.setText(defValue);
            return;
        }
        text.setText(value);
    }

    private void fillConfig() {
        mDeviceConfig.setProp("BRAND", getValue(edt_brand));
        mDeviceConfig.setProp("MODEL", getValue(edt_model));
        mDeviceConfig.setProp("PRODUCT", getValue(edt_name));
        mDeviceConfig.setProp("DEVICE", getValue(edt_device));
        mDeviceConfig.setProp("BOARD", getValue(edt_board));
        mDeviceConfig.setProp("DISPLAY", getValue(edt_display));
        mDeviceConfig.setProp("ID", getValue(edt_id));
        mDeviceConfig.setProp("MANUFACTURER", getValue(edt_manufacturer));
        mDeviceConfig.setProp("FINGERPRINT", getValue(edt_fingerprint));

        mDeviceConfig.serial = getValue(edt_serial);
        mDeviceConfig.deviceId = getValue(edt_imei);
        mDeviceConfig.iccId = getValue(edt_imsi);
        mDeviceConfig.wifiMac = getValue(edt_mac);
        mDeviceConfig.androidId = getValue(edt_androidId);
    }

    @SuppressLint("HardwareIds")
    private void updateConfig() {
        setValue(edt_brand, mDeviceConfig.getProp("BRAND"), Build.BRAND);
        setValue(edt_model, mDeviceConfig.getProp("MODEL"), Build.MODEL);
        setValue(edt_name, mDeviceConfig.getProp("PRODUCT"), Build.PRODUCT);
        setValue(edt_device, mDeviceConfig.getProp("DEVICE"), Build.DEVICE);
        setValue(edt_board, mDeviceConfig.getProp("BOARD"), Build.BOARD);
        setValue(edt_display, mDeviceConfig.getProp("DISPLAY"), Build.DISPLAY);
        setValue(edt_id, mDeviceConfig.getProp("ID"), Build.ID);
        setValue(edt_manufacturer, mDeviceConfig.getProp("MANUFACTURER"), Build.MANUFACTURER);
        setValue(edt_fingerprint, mDeviceConfig.getProp("FINGERPRINT"), Build.FINGERPRINT);

        setValue(edt_serial, mDeviceConfig.serial, Build.SERIAL);
        setValue(edt_imei, mDeviceConfig.deviceId, mTelephonyManager.getDeviceId());
        setValue(edt_imsi, mDeviceConfig.iccId, mTelephonyManager.getSimSerialNumber());
        setValue(edt_mac, mDeviceConfig.wifiMac, getDefaultWifiMac());
        setValue(edt_androidId, mDeviceConfig.androidId, Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
    }

    @SuppressLint("HardwareIds")
    private String getDefaultWifiMac() {
        String[] files = {"/sys/class/net/wlan0/address", "/sys/class/net/eth0/address", "/sys/class/net/wifi/address"};
        String mac = mWifiManager.getConnectionInfo().getMacAddress();
        if (TextUtils.isEmpty(mac)) {
            for (String file : files) {
                try {
                    mac = readFileAsString(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!TextUtils.isEmpty(mac)) {
                    break;
                }
            }
        }
        return mac;
    }

    private String readFileAsString(String filePath)
            throws java.io.IOException {
        StringBuilder sb = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            sb.append(readData);
        }
        reader.close();
        return sb.toString().trim();
    }
}
