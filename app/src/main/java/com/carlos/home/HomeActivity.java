package com.carlos.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.carlos.R;
import com.carlos.common.VCommends;
import com.carlos.common.clouddisk.ClouddiskLauncher;
import com.carlos.common.clouddisk.http.HttpWorker;
import com.carlos.common.clouddisk.listview.FileItem;
import com.carlos.common.ui.UIConstant;
import com.carlos.common.ui.activity.MirrorActivity;
import com.carlos.common.device.DeviceInfo;
import com.carlos.common.imagepicker.PhotoSelector;
import com.carlos.common.network.core.AuthRequest;
import com.carlos.common.network.core.Constant;
import com.carlos.common.network.core.MessageEntity;
import com.carlos.common.ui.activity.SettingActivity;
import com.carlos.common.ui.activity.base.VActivity;
import com.carlos.common.ui.adapter.decorations.ItemOffsetDecoration;
import com.carlos.common.utils.FileTools;
import com.carlos.common.utils.HVLog;
import com.carlos.common.utils.ResponseProgram;
import com.carlos.common.utils.SPTools;
import com.carlos.common.widget.toast.Toasty;
import com.carlos.home.XposedManager.XposedManagerActivity;
import com.carlos.utils.FileUtils1;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.compat.PackageParserCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.oem.OemPermissionHelper;
import com.carlos.common.ui.activity.abs.nestedadapter.SmartRecyclerAdapter;
import com.carlos.home.adapters.LaunchpadAdapter;
import com.carlos.home.models.AppData;
import com.carlos.home.models.AppInfoLite;
import com.carlos.widgets.TwoGearsView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import com.carlos.home.models.EmptyAppData;
import com.carlos.home.models.MultiplePackageAppData;
import com.carlos.home.models.PackageAppData;
import com.carlos.widgets.MarqueeTextView;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.VAppInstallerParams;
import com.lody.virtual.remote.VAppInstallerResult;

import mirror.android.content.pm.split.SplitDependencyLoader;

/**
 * @author LodyChen
 */
public class HomeActivity extends VActivity implements HomeContract.HomeView, LaunchpadAdapter.OnAppClickListener {


    public static final int CROP_CODE = 11;//剪切裁剪
    public static final int LOCATION_CODE = 12;//虚拟定位
    public static final int CHOOSE_FILE_CODE = 13;//文件选择 chooseFile

    private static final String TAG = "HV-"+HomeActivity.class.getSimpleName();

    private HomeContract.HomePresenter mPresenter;
    private TwoGearsView mLoadingView;
    private RecyclerView mLauncherView;

    private AppCompatImageView noApplicationList;
    private View mMenuView;
    private PopupMenu mPopupMenu;
    private MarqueeTextView tipsNotifiction;
    private LaunchpadAdapter mLaunchpadAdapter;
    private Handler mUiHandler;

    BottomSheetDialog bottomSheetDialog;
    public static void goHome(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @SuppressWarnings("SameParameterValue")
    private static void setIconEnable(Menu menu, boolean enable) {
        try {
            @SuppressLint("PrivateApi")
            Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);
            m.invoke(menu, enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mUiHandler = new Handler(Looper.getMainLooper());

        bindViews();
        initLaunchpad();
        initMenu();
        HomePresenterImpl homePresenter = new HomePresenterImpl(this);
        mPresenter.start();
        verifyStoragePermissions(this);
    }


    //先定义
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    //然后通过一个函数来申请
    public void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,"android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            } else {
                //读取文件信息
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initMenu() {
        mPopupMenu = new PopupMenu(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Light), mMenuView);
        Menu menu = mPopupMenu.getMenu();
        setIconEnable(menu, true);

        menu.add(R.string.kill_all_app).setIcon(R.drawable.ic_speed_up).setOnMenuItemClickListener(item -> {
            VActivityManager.get().killAllApps();
            Toast.makeText(this, "Memory release complete!", Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.add(R.string.menu_gms).setIcon(R.drawable.ic_google).setOnMenuItemClickListener(item -> {
            askInstallGms();
            return true;
        });
        menu.add("xposed管理器").setIcon(R.drawable.ic_xposed).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, XposedManagerActivity.class));
            return true;
        });
        menu.add("程序管理").setIcon(R.drawable.ic_application).setOnMenuItemClickListener(item -> {
            ListAppActivity.gotoListApp(this,ListAppActivity.ACTION_APP_MANAGER);
            return true;
        });
        menu.add("激活程序").setIcon(R.drawable.ic_activation).setOnMenuItemClickListener(item -> {
            activateInputWindow();
            return true;
        });
        menu.add("设置").setIcon(R.drawable.ic_settings).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        });

        mMenuView.setOnClickListener(v -> mPopupMenu.show());
    }

    public void saveApk(String pkg){
        InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(pkg, 0);
        ApplicationInfo applicationInfo = installedAppInfo.getApplicationInfo(installedAppInfo.getInstalledUsers()[0]);
        String apkPath = installedAppInfo.getApkPath();
        File externalFilesDir = getExternalFilesDir(getPackageName() + "/");
        HVLog.d(externalFilesDir+applicationInfo.packageName+".apk");
        FileTools.copyFile(apkPath,externalFilesDir+applicationInfo.packageName+".apk");

    }


    private void bindViews() {
        mLoadingView = findViewById(R.id.pb_loading_app);
        mLauncherView = findViewById(R.id.home_launcher);
        //mMenuView = findViewById(R.id.home_menu);
        mMenuView = getTitleLeftMenuIcon();
        setTitleName(R.string.application_title);
        setTitleLeftMenuIcon(R.drawable.ic_title_menu);

        noApplicationList = findViewById(R.id.no_application_list);
        findViewById(R.id.main_fun_btn).setOnClickListener((view)->{
            ListAppActivity.gotoListApp(this,ListAppActivity.ACTION_CLONE_APP);
        });
        tipsNotifiction = findViewById(R.id.tipsNotifiction);
        if (isUpgrade()) {
            isNovatioNecessaria();
            tipsNotifiction.setText(getString(R.string.txt_update)+mSoftVersions.getNotice());
            tipsNotifiction.setOnClickListener((view)->{
                //install();
            });
        }
    }

    //这里代码 activate_code 不能 修改,不然以后数据肯定异常
    private void activateInputWindow() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.VACustomTheme);
        @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.dialog_activate, null);
        builder.setView(view1);
        Dialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
        String pointCard = SPTools.getString(this, Constant.TextTag.POINT_CARD_KEY);

        HVLog.d("pointCard:"+pointCard);

        EditText editText1 = view1.findViewById(R.id.edt_activate);
        if (!TextUtils.isEmpty(pointCard)){
            editText1.setText(pointCard);
        }

        dialog.setCancelable(false);
        view1.findViewById(R.id.btn_cancel).setOnClickListener((v2) -> dialog.dismiss());
        view1.findViewById(R.id.btn_ok).setOnClickListener((v2) -> {

            try {
                String activate_num = editText1.getText().toString();
                if (TextUtils.isEmpty(activate_num)){
                    Toast.makeText(getContext(),"激活码不能为空",Toast.LENGTH_LONG).show();
                    return;
                }else {
                    dialog.dismiss();
                }

                HashMap<String,String> hashMap = AuthRequest.getInstance(this).getRequestMaps(Constant.Config.HTTP_URL+Constant.HttpUrl.Account.ACCOUNT_ACCOUNT_BINDING_CODE,true,true);
                DeviceInfo instance = DeviceInfo.getInstance(this);
                String softId = instance.getSoftId(this);
                HVLog.d("softId:"+softId);
                hashMap.put("devicesNo",instance.getDevicesNo());// 设备码
                hashMap.put("softId",instance.getSoftId(this));// 软件
                hashMap.put("cardNumber",activate_num);// 激活码
                AuthRequest.getInstance(this).postRequest(hashMap, new AuthRequest.EntityCallback<MessageEntity>() {
                    @Override
                    public void onError(Exception e) {
                    }

                    @Override
                    public void onResponse(MessageEntity response) {
                        String responseData = response.getData();// 拿到服务端的加密字段来解密,如果不是服务段的加密数据,说明被破解了
                        HVLog.d("responseData header 数据:"+responseData);
                        if (response.getCodeAction() == 0) {
                            Toasty.success(getContext(),response.getMsg()).show();
                            SPTools.putString(getContext(),Constant.TextTag.POINT_CARD_KEY,activate_num);
                        }else if (response.getCodeAction() == 1){
                            getHandler().post(()->{
                                Toasty.error(getContext(),response.getMsg()).show();
                            });
                        }else if (response.getCodeAction() == 2){
                            getHandler().post(()->{
                                Toasty.error(getContext(),response.getMsg()).show();
                            });
                        }
                        return;
                    }
                });
            } catch (Exception e) {
            }
        });
    }


    private void initLaunchpad() {
        mLauncherView.setHasFixedSize(true);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL);
        mLauncherView.setLayoutManager(layoutManager);
        mLaunchpadAdapter = new LaunchpadAdapter(this);
        SmartRecyclerAdapter wrap = new SmartRecyclerAdapter(mLaunchpadAdapter);
        View footer = new View(this);
        footer.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ResponseProgram.dpToPx(this, 60)));
        wrap.setFooterView(footer);
        mLauncherView.setAdapter(wrap);
        mLauncherView.addItemDecoration(new ItemOffsetDecoration(this, R.dimen.desktop_divider));
        mLaunchpadAdapter.setAppClickListener(this);

    }


    public void deleteApp(int position) {
        AppData data = mLaunchpadAdapter.getList().get(position);
        deleteApp(data);
    }

    public void deleteApp(AppData data) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.tip_delete)
                .setMessage(getString(R.string.text_delete_app, data.getName()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> mPresenter.deleteApp(data))
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public void enterAppSetting(AppData data) {
        if (data instanceof PackageAppData || data instanceof MultiplePackageAppData) {
            mPresenter.enterAppSetting(data);
        }
    }

    @Override
    public void setPresenter(HomeContract.HomePresenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showPermissionDialog() {
        Intent intent = OemPermissionHelper.getPermissionActivityIntent(this);
        new AlertDialog.Builder(this)
                .setTitle("Notice")
                .setMessage("You must to grant permission to allowed launch 64bit Engine.")
                .setCancelable(false)
                .setNegativeButton("GO", (dialog, which) -> {
                    try {
                        startActivity(intent);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }).show();
    }

    @Override
    public void showBottomAction() {
    }

    @Override
    public void hideBottomAction() {
    }

    @Override
    public void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
        mLoadingView.startAnim();
    }

    @Override
    public void showOverlayPermissionDialog() {
    }

    @Override
    public void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
        mLoadingView.stopAnim();
    }

    @Override
    public void loadFinish(List listData) {
        List<AppData> list = listData;
        HVLog.d("加载完了 list:"+list.size());
        //list.add(new AddAppButton(this));
        mLaunchpadAdapter.setList(list);
        hideLoading();
        if (mLaunchpadAdapter.getItemCount() == 0){
            noApplicationList.setVisibility(View.VISIBLE);
        }else {
            noApplicationList.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void loadError(Throwable err) {
        err.printStackTrace();
        hideLoading();
    }

    @Override
    public void showGuide() {

    }

    @Override
    public void addAppToLauncher(AppData model) {
        List<AppData> dataList = mLaunchpadAdapter.getList();
        boolean replaced = false;
        for (int i = 0; i < dataList.size(); i++) {
            AppData data = dataList.get(i);
            if (data instanceof EmptyAppData) {
                mLaunchpadAdapter.replace(i, model);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            mLaunchpadAdapter.add(model);
            mLauncherView.smoothScrollToPosition(mLaunchpadAdapter.getItemCount() - 1);
        }

        if (mLaunchpadAdapter.getItemCount() == 0){
            noApplicationList.setVisibility(View.VISIBLE);
        }else {
            noApplicationList.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void removeAppToLauncher(AppData model) {
        mLaunchpadAdapter.remove(model);
    }

    @Override
    public void refreshLauncherItem(AppData model) {
        mLaunchpadAdapter.refresh(model);
    }

    @Override
    public void askInstallGms() {
        if (GmsSupport.isInstalledGoogleService()) {
            Toast.makeText(this, "You have installed Gms.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.tip)
                .setMessage(R.string.text_install_gms)
                .setPositiveButton("导入系统的GMS应用", (dialog, which) -> {
                    if (GmsSupport.isOutsideGoogleFrameworkExist()) {
                        GmsSupport.installDynamicGms(0);
                        mPresenter.dataChanged();
                        Toast.makeText(this, "done!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No GMS installed outside.", Toast.LENGTH_SHORT).show();
                    }

                })
                .setNegativeButton("安装sdcard上APK", (dialog, which) -> {
                    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                    chooseFile.setType("*/*");//匹配所有的类型
                    //intent.setType(“image/*”);//选择图片
                    //intent.setType(“audio/*”); //选择音频
                    //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
                    //intent.setType(“video/*;image/*”);//同时选择视频和图片
                    chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);//设置可以多选文件
                    Intent intent = Intent.createChooser(chooseFile, "title");
                    startActivityForResult(intent, CHOOSE_FILE_CODE);
                })
                .setNeutralButton(android.R.string.cancel, null)
                .setCancelable(false)
                .show();
    }


    public void syncApplication(List<AppInfoLite> appInfoLites){
        ClouddiskLauncher.getInstance().launcherCloudByAppcation(getContext(),(fileItemList)->{
            //拿到当前需要上传日期目录的文件id
            List<FileItem> currentFolder = ClouddiskLauncher.getInstance().getCurrentFolder();
            String folderId = ClouddiskLauncher.getInstance().getCloudDiskFolderIdByDirectoryName(currentFolder, UIConstant.CLOUD_DISK_BACKUP_APPLICATION_DIRECTORY);
            HVLog.d( UIConstant.CLOUD_DISK_BACKUP_APPLICATION_DIRECTORY +" 的目录ID :" + folderId);
            if (!TextUtils.isEmpty(folderId)) {// 说明服务器上的目录id 不为空
                List<AppInfoLite> uploadList = new ArrayList<>();

                for (AppInfoLite appInfoLite : appInfoLites) {
                    String packageName = appInfoLite.packageName;
                    String path = appInfoLite.path;
                    String appName = appInfoLite.label;
                    boolean contains = contains(fileItemList, packageName,appName);
                    if (contains){
                        uploadList.add(appInfoLite);
                    }
                }

                for (AppInfoLite appInfoLite:uploadList) {
                    int maxlength = (100 - 1) * 1024 * 1024;//100M * 1
                    File file = new File(appInfoLite.path);
                    long sizeKb = file.length() / 1024;
                    long sizeMb = sizeKb / 1024;
                    HVLog.d("文件大小：" + sizeKb + "kb"+"   sizeMb:"+sizeMb+" MB");
                    if (file.length() <= maxlength){
                        String str[] = appInfoLite.path.split("/");
                        String apkName = str[str.length - 1];
                        String fileName = appInfoLite.packageName+"_"+appInfoLite.label+"_"+apkName;
                        String targetFile = getDataDir().getAbsolutePath()+File.separator+fileName;
                        FileTools.copyFile(appInfoLite.path,targetFile);
                        HVLog.d("查看apk 拷贝到 "+targetFile);
                        ClouddiskLauncher.getInstance().updaterCloud(targetFile, folderId, new HttpWorker.UpLoadCallbackListener() {
                            @Override
                            public void onError(int count) {
                                HVLog.d("上传APK "+ appInfoLite.path +"出错了 ");
                                FileTools.delete(targetFile);
                            }

                            long time = 0;
                            @Override
                            public void Progress(double progress) {
                                long timeMillis = System.currentTimeMillis();
                                if (timeMillis - time > 4000){
                                    HVLog.d("当前上传进度 progress："+progress);
                                    time = timeMillis;
                                }
                                if (progress >= 100) {
                                    HVLog.d("上传APK " + appInfoLite.path + " 成功了 ");
                                    FileTools.delete(targetFile);
                                }
                            }

                            @Override
                            public void onFinish(int count) {
                                HVLog.d("上传APK "+ appInfoLite.path +" 结束了 ");
                                FileTools.delete(targetFile);
                            }
                        });
                    }

                }
            }
        });
    }

    /**返回true 表示该报名的文件不再服务器的列表里面*/
    private boolean contains(List<FileItem> fileItems,String packageName,String appName){
        for (FileItem fileItem : fileItems) {//拿到服务器的所有文件信息
            String filename = fileItem.getFilename();
            if (filename.contains(packageName)) {
                HVLog.d(appName+":"+packageName+"  存在服务器上;"+fileItem);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VCommends.REQUEST_SELECT_APP) {
            if (resultCode == RESULT_OK && data != null) {
                List<AppInfoLite> appList = data.getParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST);
                if (appList != null) {
                    for (AppInfoLite info : appList) {
                        for (int i = 0; i< info.multiNumber;i++) {
                            //VLog.d"add "+info.packageName+"    "+info.);
                            mPresenter.addApp(info);
                        }
                    }
                }
                syncApplication(appList);
            }

        } else if (requestCode == VCommends.REQUEST_PERMISSION) {
            if (resultCode == RESULT_OK) {
                String packageName = data.getStringExtra("pkg");
                int userId = data.getIntExtra("user_id", -1);
                //LoadingActivity.launch(this,packageName,userId);
            }
        }else if (requestCode == CROP_CODE && resultCode == Activity.RESULT_OK) {
            //获取到裁剪后的图片的Uri进行处理
            Uri cropImageUri = PhotoSelector.getCropImageUri(data);
            String scheme = cropImageUri == null ? null : cropImageUri.getScheme();

        }else if (requestCode == LOCATION_CODE && resultCode == Activity.RESULT_OK){

        }else if (requestCode == CHOOSE_FILE_CODE && resultCode == Activity.RESULT_OK){
            ClipData clipData = data.getClipData();
            HVLog.e("CHOOSE_FILE_CODE :"+CHOOSE_FILE_CODE + "    clipData:"+clipData);
            if (clipData != null) {
                ResponseProgram.defer().when(()->{
                    try {
                        HVLog.e("clipData getItemCount :"+clipData.getItemCount());
                        for (int i=0;i<clipData.getItemCount();i++) {
                            ClipData.Item itemAt = clipData.getItemAt(i);
                            Uri uri = itemAt.getUri();

                            String path = FileUtils1.getPath(this, uri);
                            HVLog.e("uri:"+uri+"    path: "+path+"    ");

                            VirtualCore core = VirtualCore.get();
                            VAppInstallerParams params = new VAppInstallerParams(VAppInstallerParams.FLAG_INSTALL_OVERRIDE_NO_CHECK);
                            VAppInstallerResult result = core.installPackage(Uri.fromFile(new File(path)), params);

                            if (result.status == VAppInstallerResult.STATUS_SUCCESS) {
                                VLog.w(TAG, "install gms pkg success:" + uri);
                                getHandler().post(()->{
                                    Toast.makeText(this, result.packageName+" done!", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                VLog.w(TAG, "install gms pkg fail:" + uri + ",error : " + result.status);
                            }
                        }
                    }catch (Exception e){
                        HVLog.printException(e);
                    }
                }).done((VOID)->{
                    mPresenter.dataChanged();
                    getHandler().post(()->{
                        Toast.makeText(this, "done!", Toast.LENGTH_SHORT).show();
                    });
                });

            }
        }

    }
   // pos, data
    @Override
    public void onAppClick(int position, AppData data) {
        if (!data.isLoading()) {
            mLaunchpadAdapter.notifyItemChanged(position);
            //mPresenter.launchApp(data);

            MirrorActivity.launch(this,data.getPackageName(),data.getUserId());
            //VActivityManager.get().launchApp(data.getUserId(), data.getPackageName());
        }
    }

    @Override
    public void onAppLongClick(View view,AppData data,int position) {
    }
}
