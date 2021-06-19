package com.lody.virtual.client.stub;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lody.virtual.R;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.pm.VAppManagerService;

import java.util.List;

/**
 * @Date 18-4-16 15
 * @Author wxd@xdja.com
 * @Descrip:
 */

public class UnInstallerActivity extends Activity {

    private String TAG = "InstallerActivity";

    LinearLayout ll_install;
    LinearLayout ll_installing;
    LinearLayout ll_installed;
    LinearLayout ll_installed_1;
    TextView tv_warn;
    boolean tv_warn_isshow = false;
    private AppInfo apkinfo;
    private AppInfo sourceapkinfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_uninstaller);
        ll_install = (LinearLayout) findViewById(R.id.ll_install);
        ll_installing = (LinearLayout) findViewById(R.id.ll_installing);
        ll_installed = (LinearLayout) findViewById(R.id.ll_installed);
        ll_installed_1 = (LinearLayout) findViewById(R.id.ll_installed_1);
        tv_warn = (TextView) findViewById(R.id.tv_warn);
        tv_warn.setText("警告：该应用不是来自安全盒应用中心，请注意应用安全。建议在安全盒应用中心下载使用该应用");

        Button btn_install = (Button) findViewById(R.id.btn_install);
        Button btn_quit = (Button) findViewById(R.id.btn_quit);
        Button btn_open = (Button) findViewById(R.id.btn_open);
        ImageView img_appicon = (ImageView) findViewById(R.id.img_appicon);
        TextView tv_appname = (TextView) findViewById(R.id.tv_appname);
        TextView tv_source = (TextView) findViewById(R.id.tv_source);
        tv_source.setVisibility(View.INVISIBLE);

        String uninstall_app = getIntent().getStringExtra("uninstall_app");
        String source_apk_packagename = getIntent().getStringExtra("source_apk");
        Log.e(TAG, " uninstall app : " + uninstall_app + " source_apk : " + source_apk_packagename);

        if(TextUtils.isEmpty(uninstall_app)){
            Log.e(TAG,"Uninstall app name is NULL!");
            finish();
            return;
        }
        apkinfo = paseUninstallApp(uninstall_app);

        img_appicon.setImageDrawable(apkinfo.icon);
        tv_appname.setText(apkinfo.name);

        if(InstallerSetting.safeApps.contains(apkinfo.packageName)){
            tv_warn_isshow = false;
        }else{
            tv_warn_isshow = true;
        }

        stateChanged(STATE_INSTALL);
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
            }
        });
        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    private void stateChanged(int state){
        switch(state){
            case STATE_NONE:
                break;
            case STATE_INSTALL:
                ll_install.setVisibility(View.VISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.INVISIBLE);
                ll_installed_1.setVisibility(View.INVISIBLE);
                tv_warn.setVisibility(tv_warn_isshow?View.VISIBLE:View.INVISIBLE);
                break;
            case STATE_INSTALLING:
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.VISIBLE);
                ll_installed.setVisibility(View.INVISIBLE);
                ll_installed_1.setVisibility(View.INVISIBLE);
                tv_warn.setVisibility(tv_warn_isshow?View.VISIBLE:View.INVISIBLE);
                break;
            case STATE_INSTALLED:
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.VISIBLE);
                ll_installed_1.setVisibility(View.VISIBLE);
                tv_warn.setVisibility(View.INVISIBLE);
                break;
            case STATE_INSTALLFAILED:
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.VISIBLE);
                ll_installed_1.setVisibility(View.VISIBLE);
                tv_warn.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private AppInfo paseUninstallApp(String packageName) {
        AppInfo appinfo = new AppInfo();
        List<InstalledAppInfo> infos = VirtualCore.get().getInstalledApps(0);
        for (InstalledAppInfo info : infos) {
            if(info.packageName.equals(packageName)){
                ApplicationInfo applicationInfo = VPackageManager.get().getApplicationInfo(packageName, 0, 0);
                appinfo.icon = applicationInfo.loadIcon(VirtualCore.getPM());
                appinfo.name = applicationInfo.loadLabel(VirtualCore.getPM());
                appinfo.packageName = packageName;
            }
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
                            boolean res = VirtualCore.get().uninstallPackage(apkinfo.packageName);
                            Message msg1 = new Message();
                            msg1.what = STATE_INSTALLING;
                            msg1.obj = res;
                            mHandler.sendMessage(msg1);
                        }
                    }).start();

                    break;
                case STATE_INSTALLING:
                    if ((boolean)msg.obj) {
                        VAppManagerService.get().sendUninstalledBroadcast(apkinfo.packageName, VUserHandle.ALL);
                        stateChanged(STATE_INSTALLED);
                        try {
                            VirtualCore.AppRequestListener listener = VirtualCore.get().getAppRequestListener();
                            listener.onRequestUninstall(apkinfo.packageName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        stateChanged(STATE_INSTALLFAILED);
                    }

                    break;
                case STATE_INSTALLED:
                    break;
                case STATE_INSTALLFAILED:
                    break;

            }
        }
    }

}