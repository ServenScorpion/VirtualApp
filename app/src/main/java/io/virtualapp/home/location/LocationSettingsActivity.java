package io.virtualapp.home.location;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VLocationManager;
import com.lody.virtual.client.ipc.VirtualLocationManager;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.vloc.VLocation;

import java.util.ArrayList;
import java.util.List;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.adapters.AppLocationAdapter;
import io.virtualapp.home.models.LocationData;

import static io.virtualapp.VCommends.EXTRA_LOCATION;

public class LocationSettingsActivity extends VActivity implements AdapterView.OnItemClickListener {
    private static final int REQUEST_CODE = 1001;
    private AppLocationAdapter mAppLocationAdapter;
    private LocationData mSelectData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_settings);
        Toolbar toolbar = bind(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        enableBackHome();

        ListView listView = findViewById(R.id.appdata_list);
        mAppLocationAdapter = new AppLocationAdapter(this);
        listView.setAdapter(mAppLocationAdapter);
        listView.setOnItemClickListener(this);
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && VirtualCore.get().getTargetSdkVersion() >= android.os.Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        } else {
            loadData();
        }
    }

    private void readLocation(LocationData locationData) {
        locationData.mode = VirtualLocationManager.get().getMode(locationData.userId, locationData.packageName);
        locationData.location = VLocationManager.get().getLocation(locationData.packageName, locationData.userId);
    }

    private void saveLocation(LocationData locationData) {
        VirtualCore.get().killApp(locationData.packageName, locationData.userId);
        if (locationData.location == null || locationData.location.isEmpty()) {
            VirtualLocationManager.get().setMode(locationData.userId, locationData.packageName, VirtualLocationManager.MODE_CLOSE);
        } else if (locationData.mode != 2) {
            VirtualLocationManager.get().setMode(locationData.userId, locationData.packageName, VirtualLocationManager.MODE_USE_SELF);
        }
        VirtualLocationManager.get().setLocation(locationData.userId, locationData.packageName, locationData.location);
    }

    private void loadData() {
        ProgressDialog dialog = ProgressDialog.show(this, null, "loading");
        VUiKit.defer().when(() -> {
            List<InstalledAppInfo> infos = VirtualCore.get().getInstalledApps(0);
            List<LocationData> models = new ArrayList<>();
            for (InstalledAppInfo info : infos) {
                if (!VirtualCore.get().isPackageLaunchable(info.packageName)) {
                    continue;
                }
                /*if ("com.alibaba.android.rimet".equals(info.packageName)) {
                    //排除钉钉
                    continue;
                }*/
                int[] userIds = info.getInstalledUsers();
                for (int userId : userIds) {
                    LocationData data = new LocationData(this, info, userId);
                    readLocation(data);
                    models.add(data);
                }
            }
            return models;
        }).done((list) -> {
            dialog.dismiss();
            mAppLocationAdapter.set(list);
            mAppLocationAdapter.notifyDataSetChanged();
        }).fail((e) -> dialog.dismiss());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelectData = mAppLocationAdapter.getItem(position);
        Intent intent = new Intent(this, ChooseLocationActivity.class);
        if (mSelectData.location != null) {
            intent.putExtra(EXTRA_LOCATION, mSelectData.location);
        }
        intent.putExtra(VCommends.EXTRA_PACKAGE, mSelectData.packageName);
        intent.putExtra(VCommends.EXTRA_USERID, mSelectData.userId);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                VLocation location = data.getParcelableExtra(EXTRA_LOCATION);
                if (mSelectData != null) {
                    mSelectData.location = location;
                    saveLocation(mSelectData);
                    mSelectData = null;
                    loadData();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        loadData();
    }
}
