package com.carlos.home;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.carlos.R;
import com.carlos.common.utils.HVLog;
import com.carlos.common.utils.ResponseProgram;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.RequestExternalStorageManagerActivity;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.PermissionCompat;
import com.lody.virtual.open.MultiAppHelper;
import com.lody.virtual.remote.InstalledAppInfo;
import com.carlos.home.models.AppData;
import com.carlos.home.models.AppInfoLite;
import com.carlos.home.models.MultiplePackageAppData;
import com.carlos.home.repo.AppRepository;
import com.carlos.home.repo.PackageAppDataStorage;

import java.util.ArrayList;
import java.util.List;

import com.carlos.home.models.PackageAppData;
import com.lody.virtual.remote.VAppInstallerResult;
import com.lody.virtual.server.extension.VExtPackageAccessor;

import static com.carlos.common.VCommends.REQUEST_PERMISSION;


/**
 * @author LodyChen
 */
class HomePresenterImpl implements HomeContract.HomePresenter {

    private String TAG = "VA-HomePresenterImpl";
    private HomeContract.HomeView mView;
    private Activity mActivity;
    private AppRepository mRepo;


    HomePresenterImpl(HomeContract.HomeView view) {
        mView = view;
        mActivity = view.getActivity();
        mRepo = new AppRepository(mActivity);
        mView.setPresenter(this);
    }

    @Override
    public void start() {
        HVLog.d("HPI start");
        dataChanged();
        /*if (!Once.beenDone(VCommends.TAG_SHOW_ADD_APP_GUIDE)) {
            mView.showGuide();
            Once.markDone(VCommends.TAG_SHOW_ADD_APP_GUIDE);
        }*/
    }

    @Override
    public String getLabel(String packageName) {
        return mRepo.getLabel(packageName);
    }

    public boolean check64bitEnginePermission() {
        //if (VirtualCore.get().is64BitEngineInstalled()) {
            //((HomeActivity)mActivity).startBit64App(null,1107);
            /*if (!V64BitHelper.has64BitEngineStartPermission()) {
                mView.showPermissionDialog();
                return true;
            }*/
        //}else {
            /*boolean isInstall = isInstallAppByPackageName(mActivity, BuildConfig.PACKAGE_NAME_ARM64);
            if (!isInstall){
                AlertDialog alertDialog;
                AlertDialog.Builder builder  = new AlertDialog.Builder(mActivity);
                builder.setTitle("权限申请" ) ;
                builder.setMessage("部分应用需要安装插件才能使用,为了体验请安装插件") ;
                builder.setPositiveButton("安装", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String assetFileName = "plugin_"+BuildConfig.BUILD_TYPE+".apk";//这里的得来需要去看app-bit64 下面的build.gradle脚本
                        InputStream inputStream = null;
                        File dir = mActivity.getCacheDir();
                        try {

                            inputStream = mActivity.getAssets().open(assetFileName);
                            File apkFile = new File(dir, "plugin_"+BuildConfig.BUILD_TYPE+".apk");
                            FileUtils.writeToFile(inputStream, apkFile);

                            String command     = "chmod " + 777 + " " + apkFile.getAbsolutePath();
                            Runtime runtime = Runtime.getRuntime();
                            runtime.exec(command);
                            Log.d("VA-","assets 中的文件名字是:"+assetFileName+"   dir:"+dir+"     apkFile is exist:"+apkFile.exists());
                            install(apkFile);
                        } catch (IOException e) {
                        }
                    }
                });
                builder.setCancelable(true);
                alertDialog = builder.show();
                return false;
            }*/
        //}
        return false;
    }

    /**
     * 检查手机上是否安装了指定的软件
     * @param context
     * @param packageName
     * @return
     */
    public boolean isInstallAppByPackageName(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        List<String> packageNames = new ArrayList<String>();
        if (packageInfos != null) {
            for (int i = 0; i < packageInfos.size(); i++) {
                String packName = packageInfos.get(i).packageName;
                packageNames.add(packName);
            }
        }
        // 判断packageNames中是否有目标程序的包名，有TRUE，没有FALSE
        return packageNames.contains(packageName);
    }

/*    protected void install(File file) {
        try {//这里有文件流的读写，需要处理一下异常
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //如果SDK版本>=24，即：Build.VERSION.SDK_INT >= 24
                String authority = new StringBuilder(BuildConfig.APPLICATION_ID).append(".provider").toString();
                //uri = FileProvider.getUriForFile(getApplicationContext(), authority, file);
                uri = FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID.concat(".provider"), file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            } else {
                uri = Uri.fromFile(file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            mActivity.startActivity(intent);
        } catch (Exception e) {
            VLog.printException(e);
        }
    }*/

    @Override
    public void launchApp(AppData data) {
/*        try {
            int userId = data.getUserId();
            String packageName = data.getPackageName();
            if (userId != -1 && packageName != null) {
                boolean runAppNow = true;
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(packageName, userId);
                    ApplicationInfo applicationInfo = info.getApplicationInfo(userId);
                    boolean isExt = VirtualCore.get().isRunInExtProcess(info.packageName);
                    int runHostTargetSdkVersion = VirtualCore.get().getHostApplicationInfo().targetSdkVersion;
                    if (isExt) {
                        try {
                            runHostTargetSdkVersion = mActivity.getPackageManager().
                                    getApplicationInfo(VirtualCore.getConfig().getExtPackageName(), 0).targetSdkVersion;
                        } catch (Exception e) {

                        }
                        if (checkExtPackageBootPermission()) {
                            return;
                        }
                    }

                    if (BuildCompat.isR()
                            && runHostTargetSdkVersion >= Build.VERSION_CODES.R
                            && info.getApplicationInfo(0).targetSdkVersion < Build.VERSION_CODES.R) {
                        if ((isExt && !VExtPackageAccessor.isExternalStorageManager())
                                || (!isExt && !Environment.isExternalStorageManager())) {
                            new AlertDialog.Builder(mActivity)
                                    .setTitle(R.string.permission_boot_notice)
                                    .setMessage(R.string.request_external_storage_manager_notice)
                                    .setCancelable(false)
                                    .setNegativeButton("GO", (dialog, which) -> {
                                        RequestExternalStorageManagerActivity.request(VirtualCore.get().getContext(), isExt);
                                    }).show();
                            return;
                        }
                    }

                    if (PermissionCompat.isCheckPermissionRequired(applicationInfo)) {
                        String[] permissions = VPackageManager.get().getDangerousPermissions(info.packageName);
                        if (!PermissionCompat.checkPermissions(permissions, isExt)) {
                            runAppNow = false;
                            PermissionRequestActivity.requestPermission(mActivity, permissions, data.getName(), userId, packageName, REQUEST_PERMISSION);
                        }
                    }
                }
                if (runAppNow) {
                    data.isFirstOpen = false;
                    launchApp(userId, packageName);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }*/
    }

/*    private void launchApp(int userId, String packageName) {
        if (VirtualCore.get().isRunInExtProcess(packageName)) {
            if (!VirtualCore.get().isExtPackageInstalled()) {
                Toast.makeText(mActivity, "Please install Extension Package.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!VExtPackageAccessor.hasExtPackageBootPermission()) {
                Toast.makeText(mActivity, R.string.permission_boot_content, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        VActivityManager.get().launchApp(userId, packageName);
    }*/


    @Override
    public void dataChanged() {
        HVLog.d("dataChanged");
        mView.showLoading();
        mRepo.getVirtualApps(true,false).done(mView::loadFinish).fail(mView::loadError);
    }

    @Override
    public void addApp(AppInfoLite info) {
        class AddResult {
            private PackageAppData appData;
            private int userId;
        }
        AddResult addResult = new AddResult();
        ProgressDialog dialog = ProgressDialog.show(mActivity, null, mActivity.getString(R.string.tip_add_apps));
        ResponseProgram.defer().when(() -> {
            //这里只是去容器里面查询一次看看该应用是否安装
            InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
            if (installedAppInfo != null) {
                //如果应用安装过那么这里就是多开
                addResult.userId = MultiAppHelper.installExistedPackage(installedAppInfo);
            } else {
                //如果没有安装过,那么这里是首次安装
                VAppInstallerResult res = mRepo.addVirtualApp(info);
                if (res.status != VAppInstallerResult.STATUS_SUCCESS) {
                    throw new IllegalStateException("error code: " + res.status);
                }
            }
        }).then((res) -> {
            addResult.appData = PackageAppDataStorage.get().acquire(info.packageName);
        }).fail((e) -> {
            HVLog.i("安装失败");
            HVLog.printThrowable(e);
            dialog.dismiss();
        }).done(res -> {

            if (addResult.userId == 0) {
                PackageAppData data = addResult.appData;
                data.isLoading = true;
                mView.addAppToLauncher(data);
                handleLoadingApp(data);
            } else {
                MultiplePackageAppData data = new MultiplePackageAppData(addResult.appData, addResult.userId);
                data.isLoading = true;
                mView.addAppToLauncher(data);
                handleLoadingApp(data);
            }
            dialog.dismiss();
        });
    }

    @Override
    public boolean checkExtPackageBootPermission() {
        return false;
    }


    private void handleLoadingApp(AppData data) {
        ResponseProgram.defer().when(() -> {
            long time = System.currentTimeMillis();
            time = System.currentTimeMillis() - time;
            if (time < 1500L) {
                try {
                    Thread.sleep(1500L - time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).done((res) -> {
            if (data instanceof PackageAppData) {
                ((PackageAppData) data).isLoading = false;
                ((PackageAppData) data).isFirstOpen = true;
            } else if (data instanceof MultiplePackageAppData) {
                ((MultiplePackageAppData) data).isLoading = false;
                ((MultiplePackageAppData) data).isFirstOpen = true;
            }
            mView.refreshLauncherItem(data);
        });
    }

    @Override
    public void deleteApp(AppData data) {
        mView.removeAppToLauncher(data);
        ProgressDialog dialog = ProgressDialog.show(mActivity, mActivity.getString(R.string.tip_delete), data.getName());
        ResponseProgram.defer().when(() -> {
            mRepo.removeVirtualApp(data.getPackageName(), data.getUserId());
        }).fail((e) -> dialog.dismiss()).done((rs) -> dialog.dismiss());
    }

    @Override
    public void enterAppSetting(AppData data) {
        AppSettingActivity.enterAppSetting(mActivity, data.getPackageName(), data.getUserId());
    }

    @Override
    public int getAppCount() {
        return VirtualCore.get().getInstalledApps(0).size();
    }
}
