package com.carlos.home;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import com.carlos.R;
import com.carlos.common.VCommends;
import com.carlos.common.ui.activity.base.VActivity;
import com.carlos.widgets.NoSwipeViewPager;
import com.lody.virtual.client.core.VirtualCore;

import com.carlos.home.adapters.AppPagerAdapter;

/**
 * @author LodyChen
 */
public class ListAppActivity extends VActivity {

    public static String ACTION_CLONE_APP = "com.carlos.CLONE_ACTION";
    public static String ACTION_APP_MANAGER = "com.carlos.APP_MANAGER";
    public static String ACTION_CLONE_APP_EXTERNAL_STORAGE = "com.carlos.CLONE_EXTERNAL_STORAGE";//external_storage外置存储

    public static int STATUS_CLONE_APP = 0;// 复制系统app, 这里对应ACTION_CLONE_APP
    public static int STATUS_APPS_MANAGER = 1;// 管理虚拟机 app, 这里对应 ACTION_APP_MANAGER
    public static int STATUS_CLONE_EXTERNAL_STORAGE = 2;

    public int CURRENT_STATUS = -1;

    private NoSwipeViewPager mViewPager;
    AppPagerAdapter mAppPagerAdapter;

    String currentAction;

    public static void gotoListApp(Activity activity,String action) {
        Intent intent = new Intent(activity, ListAppActivity.class);
        intent.setAction(action);
        activity.startActivityForResult(intent, VCommends.REQUEST_SELECT_APP);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clone_app);

        currentAction = getIntent().getAction();
        mViewPager = (NoSwipeViewPager) findViewById(R.id.clone_app_view_pager);
        mViewPager.setCanSwipe(false);
        ImageView titleLeftMenuIcon = getTitleLeftMenuIcon();
        titleLeftMenuIcon.setImageResource(R.drawable.icon_back);
        titleLeftMenuIcon.setOnClickListener((view)->{
            finish();
        });

        if (mAppPagerAdapter == null){
            mAppPagerAdapter = new AppPagerAdapter(this,getSupportFragmentManager());
        }
        setTitleName(mAppPagerAdapter.getPageTitle());
        CURRENT_STATUS = mAppPagerAdapter.getItemIndexByAction();
        mViewPager.setAdapter(mAppPagerAdapter);
        //mTabLayout.setupWithViewPager(mViewPager);
        // Request permission to access external storage
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && VirtualCore.get().getTargetSdkVersion() >= android.os.Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            } else {
                load();
            }
        } else {
            load();
        }

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setTitleName(mAppPagerAdapter.getPageTitle(position).toString());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public int getCurrentStatus(){
        return CURRENT_STATUS;
    }

    private void load() {
        mViewPager.setAdapter(mAppPagerAdapter);
        mViewPager.setCurrentItem(mAppPagerAdapter.getItemIndexByAction());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                load();
                break;
            }
        }
    }
}
