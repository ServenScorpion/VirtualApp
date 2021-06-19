package com.lody.virtual.client.stub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IInterface;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.R;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.utils.MediaFileUtil;
import com.lody.virtual.remote.InstallOptions;
import com.xdja.utils.PackagePermissionManager;
import com.xdja.utils.SignatureVerify;
import com.xdja.utils.Stirrer;
import com.xdja.zs.VAppPermissionManager;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.pm.VAppManagerService;

import org.w3c.dom.Text;

import java.io.File;

import mirror.android.content.ContentProviderClientICS;
import mirror.android.content.ContentProviderClientJB;

/**
 * @Date 18-4-16 15
 * @Author wxd@xdja.com
 * @Descrip:
 */

public class InstallerActivity extends Activity {

    private String TAG = "InstallerActivity";

    RelativeLayout rl_check;
    RelativeLayout rl_install;

    LinearLayout ll_install;
    LinearLayout ll_installing;
    LinearLayout ll_installed;
    LinearLayout ll_installed_1;
    LinearLayout ll_openning;
    TextView tv_warn;
    TextView tv_ckwarn;
    TextView tv_check;
    Button btn_open;
    ImageView img_appicon;
    TextView tv_appname;
    TextView tv_source;

    boolean tv_warn_isshow = false;
    private AppInfo apkinfo;
    private AppInfo sourceapkinfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_installer);

        rl_check = (RelativeLayout) findViewById(R.id.rl_check);
        rl_install = (RelativeLayout) findViewById(R.id.rl_install);
        ll_install = (LinearLayout) findViewById(R.id.ll_install);
        ll_installing = (LinearLayout) findViewById(R.id.ll_installing);
        ll_installed = (LinearLayout) findViewById(R.id.ll_installed);
        ll_installed_1 = (LinearLayout) findViewById(R.id.ll_installed_1);
        ll_openning =(LinearLayout) findViewById(R.id.ll_openning);
        tv_warn = (TextView) findViewById(R.id.tv_warn);
        tv_warn.setText("警告：该应用不是来自安全盒应用中心，请注意应用安全。建议在安全盒应用中心下载使用该应用");


        final TextView tv_ckc = (TextView) findViewById(R.id.tv_ckc);
        tv_ckwarn = (TextView) findViewById(R.id.tv_ckwarn);
        tv_check = (TextView) findViewById(R.id.tv_check);
        Button btn_install = (Button) findViewById(R.id.btn_install);
        Button btn_quit = (Button) findViewById(R.id.btn_quit);
        btn_open = (Button) findViewById(R.id.btn_open);
        Button btn_cancle = (Button) findViewById(R.id.btn_cancle);
        img_appicon = (ImageView) findViewById(R.id.img_appicon);
        tv_appname = (TextView) findViewById(R.id.tv_appname);
        tv_source = (TextView) findViewById(R.id.tv_source);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        Animation operatingAnim = AnimationUtils.loadAnimation(this, R.anim.imageroate);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        imageView.startAnimation(operatingAnim);
        ImageView ivOpenning = (ImageView) findViewById(R.id.iv_openning);
        ivOpenning.startAnimation(operatingAnim);

        btn_install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.sendEmptyMessage(STATE_INSTALL);
                stateChanged(STATE_INSTALLING);
            }
        });

        btn_quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                if(apkinfo!=null) {
                    deleteCachePackage(apkinfo.path);
                }
            }
        });
        tv_ckc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_ckc.setTextColor(Color.parseColor("#4b4b4b"));
                finish();
            }
        });
        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDelDialog(true);
            }
        });
        btn_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDelDialog(false);
            }
        });

        String path = getIntent().getStringExtra("installer_path");
        String source_apk_packagename = getIntent().getStringExtra("source_apk");
        //如果允许第三方应用安装不做签名验证
        //如果不允许第三方应用安装做签名验证 默认不允许
        if(!VAppPermissionManager.get().getThirdAppInstallationEnable()){
            Log.e("lxf-Installer","NOT Enable thrid app install !");
            if(!PackagePermissionManager.getEnableInstallationSource().contains("*")
                    && !PackagePermissionManager.getEnableInstallationSource().contains(source_apk_packagename)){
                InstallerSetting.showToast(this,"安全策略已阻止第三方应用安装", Toast.LENGTH_LONG);
                finish();
                return;
            }
            //xdja　安装源签名验证
            if(SignatureVerify.isEnable) {
                if (!TextUtils.isEmpty(source_apk_packagename)) {
                    boolean pass = new SignatureVerify().checkSourceSignature(source_apk_packagename);
                    if (!pass){
                        InstallerSetting.showToast(this, "安装源签名验证失败", Toast.LENGTH_LONG);
                        finish();
                        return;
                    }
                } else {
                    InstallerSetting.showToast(this, "安装源签名获取失败", Toast.LENGTH_LONG);
                    finish();
                    return;
                }
            }
        }
        Log.e("lxf-Installer","EEEEnable thrid app install !");

        initView(getIntent());
        stateChanged(STATE_INSTALL);

        //关闭了透明加解密
//        if("com.tencent.mm".equals(source_apk_packagename)
//                ||"cn.wps.moffice".equals(source_apk_packagename)
//                ||"com.android.gallery3d".equals(source_apk_packagename)
//                || "com.xdja.jxclient".equals(source_apk_packagename)){
//
//            IntentFilter filter=new IntentFilter();
//            filter.addAction(SpecialComponentList.protectAction("com.xdja.decrypt.DecryptService.DECRYPT_RESULT"));
//            registerReceiver(myReceiver,filter);
//            isRegisterReceiver = true;
//
//            Intent intent = new Intent();
//            intent.setAction("com.xdja.decrypt.COPYFILE");
//            intent.putExtra("workspace",VirtualCore.get().getHostPkg());
//            intent.putExtra("source_apk", source_apk_packagename);
//            intent.putExtra("installer_path", path);
//            intent.putExtra("_VA_|_user_id_",0);
//            intent.setComponent(new ComponentName("com.xdja.decrypt", "com.xdja.decrypt.DecryptService"));
//            VirtualCore.get().getContext().startService(intent);
//            stateChanged(STATE_NONE);
//        }
//        else{
//            initView(getIntent());
//            stateChanged(STATE_INSTALL);
//        }
    }

    private void initView(Intent intent){
        String path = intent.getStringExtra("installer_path");
        String source_apk_packagename = intent.getStringExtra("source_apk");
        String source_lable = intent.getStringExtra("source_label");

        String sourceText;
        if(!TextUtils.isEmpty(source_lable)){
            sourceText = "应用来源："+source_lable;
        }else {
            if(!TextUtils.isEmpty(source_apk_packagename)){
                InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(source_apk_packagename, 0);
                if(info==null|| TextUtils.isEmpty(info.getApkPath())){
                    sourceText = "应用来源：未知";
                }else{
                    sourceapkinfo = parseInstallApk(info.getApkPath());
                    if(sourceapkinfo==null){
                        InstallerSetting.showToast(this,"安装源解析失败", Toast.LENGTH_LONG);
                        sourceText = "应用来源：未知";
                    }else{
                        sourceText = "应用来源："+sourceapkinfo.name;
                    }
                }
            }else{
                sourceText = "应用来源：未知";
            }
        }
        tv_source.setText(sourceText);
        if(!TextUtils.isEmpty(path)){
            apkinfo = parseInstallApk(path);
            if(apkinfo==null){
                InstallerSetting.showToast(this,"安装包解析错误", Toast.LENGTH_LONG);
                finish();
                return;
            }
            img_appicon.setImageDrawable(apkinfo.icon);
            tv_appname.setText(apkinfo.name);
            if(InstallerSetting.safeApps.contains(apkinfo.packageName)){
                tv_warn_isshow = false;
            }else{
                tv_warn_isshow = true;
            }
        }else{
            Log.e(TAG,"InstallApk path is NULL!");
            finish();
        }
    }

    public boolean isRegisterReceiver = false;
    @Override
    protected void onDestroy() {
        if(myReceiver!=null&&isRegisterReceiver){
            unregisterReceiver(myReceiver);
            isRegisterReceiver = false;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.xdja.decrypt", "com.xdja.decrypt.DecryptService"));
        stopService(intent);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==100){
                initView(data);
                stateChanged(STATE_INSTALL);
            }
        }
    }

    private void stateChanged(int state){
        rl_check.setVisibility(View.GONE);
        rl_install.setVisibility(View.VISIBLE);
        tv_source.setTextColor(Color.parseColor("#535353"));
        ll_openning.setVisibility(View.INVISIBLE);
        ll_install.setVisibility(View.INVISIBLE);
        ll_installing.setVisibility(View.INVISIBLE);
        ll_installed.setVisibility(View.INVISIBLE);
        ll_installed_1.setVisibility(View.INVISIBLE);
        tv_warn.setVisibility(tv_warn_isshow?View.VISIBLE:View.INVISIBLE);
        switch(state){
            case STATE_NONE:
                rl_check.setVisibility(View.VISIBLE);
                rl_install.setVisibility(View.GONE);

                tv_check.setTextColor(Color.parseColor("#01dd8d"));
                break;
            case STATE_INSTALL:
                ll_install.setVisibility(View.VISIBLE);
                break;
            case STATE_INSTALLING:
                ll_installing.setVisibility(View.VISIBLE);
                break;
            case STATE_INSTALLED:
            case STATE_INSTALLFAILED:
                ll_installed.setVisibility(View.VISIBLE);
                ll_installed_1.setVisibility(View.VISIBLE);
                tv_warn.setVisibility(View.INVISIBLE);
                Intent intent = VirtualCore.get().getLaunchIntent(apkinfo.packageName, VirtualCore.get().myUserId());
                if(intent==null){
                    btn_open.setText("完成");
                }else {
                    btn_open.setText("打开");
                }
                deleteCachePackage(apkinfo.path);
                break;
            case STATE_OPENNING:
                ll_openning.setVisibility(View.VISIBLE);
                break;
            case STATE_CHECKERROR:
                rl_check.setVisibility(View.VISIBLE);
                rl_install.setVisibility(View.GONE);
                tv_check.setTextColor(Color.RED);
                tv_check.setText("检测失败");
                tv_ckwarn.setText("提示：没有找到需要的安装包！");
                tv_ckwarn.setVisibility(View.VISIBLE);
                break;
        }

    }

    private AppInfo parseInstallApk(@NonNull String path) {
        AppInfo appinfo = null;
        File f = new File(path);
        PackageManager pm = VirtualCore.get().getContext().getPackageManager();
        try {
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(f.getAbsolutePath(), 0);
            if(pkgInfo==null){
                return null;
            }
            appinfo = new AppInfo();
            ApplicationInfo ai = pkgInfo.applicationInfo;
            ai.sourceDir = f.getAbsolutePath();
            ai.publicSourceDir = f.getAbsolutePath();
            appinfo.packageName = pkgInfo.packageName;
            appinfo.icon = ai.loadIcon(pm);
            appinfo.name = ai.loadLabel(pm);
            appinfo.path = path;
            Log.e(TAG, " packageName : " + appinfo.packageName + " name : " + appinfo.name);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return appinfo;
    }

    public class AppInfo {
        public String packageName;
        public Drawable icon;
        public CharSequence name;
        public String path;
    }
    private final int STATE_NONE= -1;
    private final int STATE_INSTALL = 0;
    private final int STATE_INSTALLING = 1;
    private final int STATE_INSTALLED = 2;
    private final int STATE_INSTALLFAILED = 3;
    private final int STATE_OPENNING = 4;
    private final int STATE_CHECKERROR = 5;
    private InstallerHandler mHandler = new InstallerHandler();
    class InstallerHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case STATE_INSTALL:

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InstallOptions options = InstallOptions.makeOptions(false, InstallOptions.UpdateStrategy.COMPARE_VERSION);
                            InstallResult res = VirtualCore.get().installPackage(apkinfo.path, options);
                            Message msg1 = new Message();
                            msg1.what = STATE_INSTALLING;
                            msg1.obj = res;
                            mHandler.sendMessage(msg1);
                        }
                    }).start();
                    break;
                case STATE_INSTALLING:
                    InstallResult res = (InstallResult)msg.obj;
                    if (res.isSuccess) {
                        try {
                            VirtualCore.AppRequestListener listener = VirtualCore.get().getAppRequestListener();
                            if(listener!=null)
                            listener.onRequestInstall(apkinfo.packageName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //mHandler.sendEmptyMessage(STATE_INSTALLED);
                        stateChanged(STATE_INSTALLED);
                    }else{
                        //mHandler.sendEmptyMessage(STATE_INSTALLFAILED);
                        stateChanged(STATE_INSTALLFAILED);
                    }
                    break;
                case STATE_INSTALLED:
                case STATE_INSTALLFAILED:
                    break;

            }
        }
    }

    /**
     * 删除临时目录安装文件
     * @param path
     */
    private void deleteCachePackage(String path){
        Log.e(TAG,"deleteCachePackage "+ path);
        if(!TextUtils.isEmpty(path)&&path.startsWith("/data/user/0/"+this.getPackageName()+"/cache")){
            File file = new File(path);
            boolean apkexit = file.exists();
            if(apkexit){
                FileUtils.deleteDir(path);
            }
        }

    }
    private void showDelDialog(final boolean open){

        final AlertDialog delDlg = new AlertDialog.Builder(InstallerActivity.this).create();
        delDlg.getWindow().setGravity(Gravity.BOTTOM);
        delDlg.show();
        delDlg.setContentView(R.layout.custom_installer_del);

        Button btn_del_cancle = delDlg.getWindow().findViewById(R.id.btn_del_cancel);
        btn_del_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stateChanged(STATE_OPENNING);
                delDlg.dismiss();
                if(open){
                    Intent intent = VirtualCore.get().getLaunchIntent(apkinfo.packageName, VirtualCore.get().myUserId());
                    if(intent!=null)
                        VActivityManager.get().startActivity(intent, VirtualCore.get().myUserId());
                }
                finish();
            }
        });
        Button btn_del_del = delDlg.getWindow().findViewById(R.id.btn_del_del);
        btn_del_del.setOnClickListener(new View.OnClickListener() {
            private Uri getContentUriByCategory(String path) {
                Uri uri;
                String volumeName = "external";
                if(MediaFileUtil.isImageFileTypeForPath(path)){
                    uri = MediaStore.Images.Media.getContentUri(volumeName);
                }else if(MediaFileUtil.isVideoFileTypeForPath(path)){
                    uri = MediaStore.Video.Media.getContentUri(volumeName);
                }else {
                    uri = MediaStore.Files.getContentUri(volumeName);
                }
                return uri;
            }


            @Override
            public void onClick(View view) {

                File file = new File(apkinfo.path);
                boolean apkexit = file.exists();
                if(apkexit){
                    boolean delsuc = FileUtils.deleteDir(apkinfo.path) > 0;
                    if(delsuc){
                        InstallerSetting.showToast(InstallerActivity.this,"安装包删除成功",Toast.LENGTH_SHORT);

                        // notify MediaProvider to remove the item
                        {
                            int root_idx = apkinfo.path.indexOf("/storage/emulated/");
                            if (root_idx != -1) {
                                String inner_path = apkinfo.path.substring(root_idx);
                                {
                                    Uri uri = getContentUriByCategory(inner_path);
                                    ContentProviderClient contentProviderClient = Stirrer.getConentProvider("media");
                                    if (contentProviderClient != null) {
                                        try {
                                            contentProviderClient.delete(uri,MediaStore.MediaColumns.DATA+"=?",
                                                    new String[]{inner_path});
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }else{
                        InstallerSetting.showToast(InstallerActivity.this,"安装包删除失败",Toast.LENGTH_SHORT);
                    }
                }else{
                    InstallerSetting.showToast(InstallerActivity.this,"安装包已被删除",Toast.LENGTH_SHORT);
                }

                stateChanged(STATE_OPENNING);
                delDlg.dismiss();
                if(open){
                    Intent intent = VirtualCore.get().getLaunchIntent(apkinfo.packageName, VirtualCore.get().myUserId());
                    if(intent!=null)
                        VActivityManager.get().startActivity(intent, VirtualCore.get().myUserId());
                }
                finish();
            }
        });
    }

    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stateChanged(STATE_CHECKERROR);
        }
    };

}