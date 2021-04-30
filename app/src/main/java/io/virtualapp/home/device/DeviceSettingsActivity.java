package io.virtualapp.home.device;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.lody.virtual.client.core.VirtualCore;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.home.adapters.DevicePagerAdapter;

public class DeviceSettingsActivity  extends VActivity {
    private Toolbar mToolBar;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_devices);
        mToolBar = (Toolbar) findViewById(R.id.clone_app_tool_bar);
        mTabLayout = (TabLayout) mToolBar.findViewById(R.id.clone_app_tab_layout);
        mViewPager = (ViewPager) findViewById(R.id.clone_app_view_pager);
        setupToolBar();
        mViewPager.setAdapter(new DevicePagerAdapter(getSupportFragmentManager()));
        // Request permission to access external storage
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && VirtualCore.get().getTargetSdkVersion() >= android.os.Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 0);
        } else {
            mTabLayout.setupWithViewPager(mViewPager);
        }
    }

    private void setupToolBar() {
        setSupportActionBar(mToolBar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                mTabLayout.setupWithViewPager(mViewPager);
                break;
            }
        }
    }
}
