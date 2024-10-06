package com.carlos.splash;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;


import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.carlos.R;
import com.carlos.common.ui.activity.base.VActivity;
import com.carlos.common.utils.ResponseProgram;
import com.carlos.common.utils.SPTools;
import com.carlos.common.widget.AgreementsDialog;
import com.carlos.common.widget.effects.DialogDismissListener;
import com.carlos.common.widget.effects.DialogResultListener;
import com.kook.common.utils.HVLog;
import com.lody.virtual.client.core.VirtualCore;

import java.util.ArrayList;
import java.util.List;


import com.carlos.home.HomeActivity;

public class splashActivity extends VActivity {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private AgreementsDialog agreementsDialog;
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
    protected boolean isCheckLog() {
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        String checkAgreementsTips = SPTools.getString(this, "checkAgreementsTips", null);

        if (checkAgreementsTips == null){
            onUserAgreementSuccess();
        }else{
            checkPermissions(this);
        }
    }

    private void onUserAgreementSuccess() {
        //显示隐私协议框
        //int height = DensityUtils.getScreenHeight(this) - DensityUtils.dip2px(this, 240f);
        //int width = DensityUtils.getRealScreenWidth(this) - DensityUtils.dip2px(this, 64f);

        // 获取WindowManager
         WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        // 创建DisplayMetrics对象
        DisplayMetrics displayMetrics = new DisplayMetrics();
        // 获取屏幕尺寸到DisplayMetrics对象中
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        // 屏幕宽度和高度
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        if (agreementsDialog == null) {
            agreementsDialog = AgreementsDialog.newBuilder()
                    .setSize(width, height)
                    .setGravity(Gravity.CENTER)
                    .build();
        }
        agreementsDialog.setDialogDismissListener(new DialogDismissListener() {
            @Override
            public void dismiss(DialogFragment dialog) {
                agreementsDialog.dismissAllowingStateLoss();
                finish();
            }
        });
        agreementsDialog.setDialogResultListener(new DialogResultListener() {
            @Override
            public void result(Object result) {
                SPTools.putString(getContext(),"checkAgreementsTips","11111");
                checkPermissions(splashActivity.this);
            }
        });
        if (!agreementsDialog.isAdded()) {
            agreementsDialog.show(getSupportFragmentManager(), "AgreementsDialog");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RequestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    HVLog.e("p","拒绝的权限名称：" + permissions[i]);
                    HVLog.e("p","拒绝的权限结果：" + grantResults[i]);
                    HVLog.e("p","有权限未授权，可以弹框出来，让客户去手机设置界面授权。。。");
                }else {
                    HVLog.e("p","授权的权限名称：" + permissions[i]);
                    HVLog.e("p","授权的权限结果：" + grantResults[i]);
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

}
