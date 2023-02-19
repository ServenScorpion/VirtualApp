package com.carlos.splash;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.carlos.R;
import com.carlos.common.ui.activity.base.VActivity;
import com.carlos.common.utils.FileTools;
import com.carlos.common.utils.HVLog;
import com.carlos.common.utils.ResponseProgram;
import com.lody.virtual.client.core.VirtualCore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import com.carlos.home.HomeActivity;

public class splashActivity extends VActivity {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    //需要申请权限的数组
    private static String[] PERMISSIONS_STORAGE = { Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };


    //保存真正需要去申请的权限
    private List<String> permissionList = new ArrayList<>();

    public static int RequestCode = 100;

    public void checkPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < PERMISSIONS_STORAGE.length; i++) {
                if (ContextCompat.checkSelfPermission(activity,PERMISSIONS_STORAGE[i]) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(PERMISSIONS_STORAGE[i]);
                }
            }
            //有需要去动态申请的权限
            if (permissionList.size() > 0) {
                requestPermission(activity);
            }else {
                launcherEngine();
            }
        }
    }
    //去申请的权限
    public void requestPermission(Activity activity) {
        HVLog.d("去动态申请权限");
        //ActivityCompat.requestPermissions(activity,permissionList.toArray(new String[permissionList.size()]),RequestCode);

        requestPermissions(permissionList.toArray(new String[permissionList.size()]), RequestCode);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //startActivity(new Intent(this, LoginActivity.class));
        //finish();
        //checkAndRequestPermission();

        checkPermissions(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RequestCode) {
            boolean isPermission = false;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    HVLog.e("p","拒绝的权限名称：" + permissions[i]);
                    HVLog.e("p","拒绝的权限结果：" + grantResults[i]);
                    HVLog.e("p","有权限未授权，可以弹框出来，让客户去手机设置界面授权。。。");

                    //requestPermissions(permissionList.toArray(new String[permissionList.size()]), RequestCode);
                    isPermission = false;
                }else {
                    HVLog.e("p","授权的权限名称：" + permissions[i]);
                    HVLog.e("p","授权的权限结果：" + grantResults[i]);
                    isPermission = true;
                }
            }

            launcherEngine();
        }
    }


    public void launcherEngine(){
        ResponseProgram.defer().when(() -> {
            long time = System.currentTimeMillis();
            doActionInThread();
            //readyAction();
            time = System.currentTimeMillis() - time;
            long delta = 500L - time;
            if (delta > 0) {
                ResponseProgram.sleep(delta);
            }
        }).done((res) -> {
            HomeActivity.goHome(this);
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void doActionInThread() {
        if (!VirtualCore.get().isEngineLaunched()) {
            VirtualCore.get().waitForEngine();
        }
    }

    @Override
    public void onBackPressed() {

    }

    public void readyAction(){
        //File externalFilesDir = getExternalFilesDir("");///storage/emulated/0/Android/data/com.carlos.multiapp/files
        String oldPath = "/storage/emulated/0/GMS3/gms.apk";
        String newPath = "/data/data/com.carlos.multiapp.ext/virtual/data/app/com.google.android.gms/base.apk";
        FileTools.copyFile(oldPath,newPath);
    }
}
