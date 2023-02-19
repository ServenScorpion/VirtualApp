package com.carlos.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.carlos.BuildConfig;
import com.carlos.R;
import com.carlos.common.VCommends;
import com.carlos.common.ui.activity.base.VFragment;
import com.carlos.common.utils.FileTools;
import com.carlos.common.utils.HVLog;
import com.carlos.common.utils.InstallTools;
import com.carlos.common.utils.ResponseProgram;
import com.carlos.common.widget.MainFunBtn;
import com.carlos.common.widget.MirrorDialog;
import com.carlos.common.widget.TextProgressBar;
import com.carlos.common.widget.toast.Toasty;
import com.carlos.home.models.AppData;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;
import com.lody.virtual.helper.utils.FileUtils;
import com.carlos.home.adapters.CloneAppListAdapter;
import com.carlos.home.adapters.DividerItemDecoration;
import com.carlos.home.models.AppInfo;
import com.carlos.home.models.AppInfoLite;
import com.carlos.widgets.DragSelectRecyclerView;
import com.carlos.widgets.FastIndexView;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * @author kook
 * APP列表
 */
public class ListAppFragment extends VFragment<ListAppContract.ListAppPresenter> implements ListAppContract.ListAppView {
    private static final String KEY_ACTION = "action";
    private DragSelectRecyclerView mRecyclerView;
    FastIndexView mFastIndexView;
    private ProgressBar mProgressBar;
    LinearLayoutManager mLinearLayoutManager;
    private MainFunBtn mInstallButton;
    private CloneAppListAdapter mAdapter;

    /**安装插件*/
    private int ACTION_INSTALL_PLUGIN = 1;
    /**安装插件的时候发现需要卸载过去老版本的插件*/
    private int ACTION_REINSTALL_PLUGIN = 2;

    Dialog  mDialog = null;
    ListAppActivity mListAppActivity;
    public static ListAppFragment newInstance() {
        Bundle args = new Bundle();
        ListAppFragment fragment = new ListAppFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListAppActivity = (ListAppActivity) getActivity();
        return inflater.inflate(R.layout.fragment_list_app, null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mRecyclerView = (DragSelectRecyclerView) view.findViewById(R.id.select_app_recycler_view);
        mProgressBar = (ProgressBar) view.findViewById(R.id.select_app_progress_bar);
        mFastIndexView = view.findViewById(R.id.fastIndexView);
        mInstallButton = (MainFunBtn) view.findViewById(R.id.main_fun_btn);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        //mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL));
        //mRecyclerView.addItemDecoration(new ItemOffsetDecoration(VUiKit.dpToPx(getContext(), 2)));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
        mAdapter = new CloneAppListAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        fastIndexView();//快速索引响应设置
        if (mListAppActivity.getCurrentStatus() == ListAppActivity.STATUS_CLONE_APP) {
            mInstallButton.setVisibility(View.VISIBLE);
            adapterItemClick();// adapter 子项点击相应
            adapterSelect();// adapter 选择子项后的响应
            installButton();//主菜单点击制作分身
        }else if (mListAppActivity.getCurrentStatus() == ListAppActivity.STATUS_APPS_MANAGER){
            mInstallButton.setVisibility(View.GONE);
            appManager();
        }

        //开始请求数据
        new ListAppPresenterImpl(getActivity(), this).start();
    }

    /**快速索引滑动响应*/
    public void fastIndexView(){
        mFastIndexView.setListener(new FastIndexView.OnLetterUpdateListener() {
            @Override
            public void onLetterUpdate(String letter) {
                mLinearLayoutManager.scrollToPositionWithOffset(mAdapter.getScrollPosition(letter), 0);
            }
        });
    }

    /*adapter 子项点击相应*/
    public void adapterItemClick(){
        mAdapter.setOnItemClickListener(new CloneAppListAdapter.ItemEventListener() {
            @Override
            public void onItemClick(AppInfo info, int position) {
                String hostSHA = getSHA1(getContext(), BuildConfig.PACKAGE_NAME);
                String pluginSHA = getSHA1(getContext(), BuildConfig.EXT_PACKAGE_NAME);

                HVLog.d("hostSHA:"+hostSHA);
                HVLog.d("pluginSHA:"+pluginSHA);

                if (!TextUtils.isEmpty(pluginSHA) && !hostSHA.equals(pluginSHA)) {
                    installApkWindow(ACTION_REINSTALL_PLUGIN);
                    return;
                }

                int count = mAdapter.getSelectedCount();

                boolean installAppByPackageName = InstallTools.isInstallAppByPackageName(getContext(), BuildConfig.EXT_PACKAGE_NAME);
                if (!installAppByPackageName){
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                ResponseProgram.defer().when(()->{
                    if (installAppByPackageName){
                        return false;
                    }
                    boolean isNeed64Proces = false;
                    try {
                        boolean support64bit = false, support32bit = false;
                        boolean shouldLinkInstalledSo = false;
                        NativeLibraryHelperCompat.SoLib installedSo = null;
                        Set<String> abiList = NativeLibraryHelperCompat.getSupportAbiList(info.path);
                        if (NativeLibraryHelperCompat.support64bitAbi(abiList)) {
                            support64bit = true;
                        }
                        if (NativeLibraryHelperCompat.contain32bitAbi(abiList)) {
                            support32bit = true;
                        }

                        if (support32bit) {
                            if (support64bit) {
                                isNeed64Proces = true;
                            } else {
                                isNeed64Proces = false;
                            }
                        } else {
                            isNeed64Proces = true;
                        }
                    } catch (Throwable throwable) {
                        HVLog.printThrowable(throwable);
                    }
                    return isNeed64Proces;
                }).done((isNeed64Proces)->{
                    mProgressBar.setVisibility(View.GONE);
                    HVLog.d("安装了拓展插件 "+installAppByPackageName+"    需要启动64 进程"+isNeed64Proces);
                    if (installAppByPackageName){
                        ((ListAppActivity)getActivity()).startBit64App(null,1107);
                    }else {
                        if (isNeed64Proces) {
                            installApkWindow(ACTION_INSTALL_PLUGIN);
                            return;
                        }
                    }

                    if (!mAdapter.isIndexSelected(position)) {
                        if (count >= 9) {
                            Toast.makeText(getContext(), R.string.install_too_much_once_time, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    mAdapter.toggleSelected(position);
                }).fail((throwable)->{
                    mProgressBar.setVisibility(View.GONE);
                    HVLog.printThrowable(throwable);
                });
            }

            @Override
            public boolean isSelectable(int position) {
                return mAdapter.isIndexSelected(position) || mAdapter.getSelectedCount() < 9;
            }
        });
    }

    public void adapterSelect(){// adapter 选择子项后的响应
        mAdapter.setSelectionListener(count -> {
            boolean selectedApp = count > 0;
            //mInstallButton.setTextColor(selectedApp ? Color.WHITE : Color.parseColor("#cfcfcf"));
            //mInstallButton.setEnabled(selectedApp);
            if (count > 0) {
                mInstallButton.setTopText(String.format(Locale.ENGLISH, getResources().getString(R.string.install_d), count));
            }else {
                mInstallButton.setTopText("+");
            }
        });
    }

    /**主菜单点击制作分身*/
    public void  installButton(){
        mInstallButton.setOnClickListener(v -> {
            Integer[] selectedIndices = mAdapter.getSelectedIndices();
            if (selectedIndices.length == 0){
                Toasty.info(getActivity(),"清选择需要克隆的APP").show();
                return;
            }
            ArrayList<AppInfoLite> dataList = new ArrayList<AppInfoLite>(selectedIndices.length);
            for (int index : selectedIndices) {
                View childAt = mLinearLayoutManager.getChildAt(index);
                AppInfo info = mAdapter.getItem(index);
                int itemMultiNumber = mAdapter.getItemMultiNumber(index);
                dataList.add(new AppInfoLite(info,itemMultiNumber));
            }
            Intent data = new Intent();
            data.putParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST, dataList);
            getActivity().setResult(Activity.RESULT_OK, data);
            getActivity().finish();
        });
    }

    /*管理应用 , 主要下载跟卸载*/
    private void appManager(){
        mAdapter.setItemEventManager((packageAppData,position)-> {
            /*显示备份的dialog*/
            AlertDialog.Builder mBuilder = new android.app.AlertDialog.Builder(mListAppActivity, R.style.VACustomTheme);

            @SuppressLint("InflateParams") View view1 = mListAppActivity.getLayoutInflater().inflate(R.layout.dialog_app_manager, null);
            mBuilder.setView(view1);
            if (!mListAppActivity.isFinishing()) {
                mDialog = mBuilder.show();
            }
            if (mDialog == null) {
                return;
            }

            File externalFilesDir = mListAppActivity.getExternalFilesDir(mListAppActivity.getPackageName() + "/");
            String targetpath = externalFilesDir+packageAppData.packageName+".apk";
            HVLog.d(targetpath);

            mDialog.setCanceledOnTouchOutside(true);
            TextView textView = view1.findViewById(R.id.tips_content);
            textView.setText("1 卸载当前应用 \n 2将该应用下载的外部存储"+"\n"+targetpath);
            mDialog.setCancelable(true);
            view1.findViewById(R.id.double_btn_layout).setVisibility(View.VISIBLE);

            view1.findViewById(R.id.btn_uninstall).setOnClickListener((view) -> {
                mDialog.dismiss();
                VirtualCore.get().uninstallPackageAsUser(packageAppData.packageName, packageAppData.getUserId());
                //开始请求数据
                new ListAppPresenterImpl(getActivity(), this).start();
            });
            view1.findViewById(R.id.btn_download).setOnClickListener((view) -> {
                mDialog.dismiss();
                InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(packageAppData.packageName, 0);
                ApplicationInfo applicationInfo = installedAppInfo.getApplicationInfo(installedAppInfo.getInstalledUsers()[0]);
                String apkPath = installedAppInfo.getApkPath();
                FileTools.copyFile(apkPath,targetpath);
                Toast.makeText(mListAppActivity,"备份成功",Toast.LENGTH_LONG).show();
            });
        });
    }

    //这里代码 activate_code 不能 修改,不然以后数据肯定异常
    private void installApkWindow(int type) {
        String activate_code = "activate_code";
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.VACustomTheme);
        @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.dialog_tips, null);
        builder.setView(view1);
        Dialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
        TextView textView = view1.findViewById(R.id.tips_content);
        if (type == ACTION_INSTALL_PLUGIN) {
            textView.setText("部分应用需要安装64插件才能使用,为了体验请安装插件");
        }else if (type == ACTION_REINSTALL_PLUGIN){
            textView.setText("请卸载老版插件应用,安装新版插件,请点击确认执行");
        }

        dialog.setCancelable(false);
        view1.findViewById(R.id.double_btn_layout).setVisibility(View.VISIBLE);
        view1.findViewById(R.id.btn_cancel).setOnClickListener((v2) -> dialog.dismiss());
        view1.findViewById(R.id.btn_ok).setOnClickListener((v2) -> {
            dialog.dismiss();
            if (type == ACTION_INSTALL_PLUGIN){
                try {
                    String assetFileName = "plugin_"+ BuildConfig.BUILD_TYPE+".apk";//这里的得来需要去看app-bit64 下面的build.gradle脚本
                    InputStream inputStream = null;
                    File dir = getActivity().getCacheDir();
                    inputStream = getActivity().getAssets().open(assetFileName);
                    File apkFile = new File(dir, "plugin_"+BuildConfig.BUILD_TYPE+".apk");
                    FileUtils.writeToFile(inputStream, apkFile);
                    install(apkFile);
                } catch (Exception e) {
                    Toasty.error(getActivity(),"插件程序未找到").show();
                    HVLog.printException(e);
                }
            }else if (type == ACTION_REINSTALL_PLUGIN){
                Intent uninstall_intent = new Intent();
                uninstall_intent.setAction(Intent.ACTION_DELETE);
                uninstall_intent.setData(Uri.parse("package:"+BuildConfig.EXT_PACKAGE_NAME));
                startActivity(uninstall_intent);
            }
        });
    }

    protected void install(File file) {
        try {//这里有文件流的读写，需要处理一下异常
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //如果SDK版本>=24，即：Build.VERSION.SDK_INT >= 24
                String authority = new StringBuilder(BuildConfig.APPLICATION_ID).append(".provider").toString();
                //uri = FileProvider.getUriForFile(getApplicationContext(), authority, file);
                uri = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID.concat(".provider"), file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            } else {
                uri = Uri.fromFile(file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            getActivity().startActivity(intent);
        } catch (Exception e) {
            HVLog.printException(e);
        }
    }

    @Override
    public void startLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void loadFinish(List infoList) {
        HVLog.d("ListAppFragment loadFinish "+infoList.size());
        if(!isAttach()){
            return;
        }
        mAdapter.setList(infoList);
        mRecyclerView.setDragSelectActive(false, 0);
        mAdapter.setSelected(0, false);
        mProgressBar.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }
/*    @Override
    public void loadFinish(List<AppData> list) {
        HVLog.d("加载完了 list:"+list.size());
        //list.add(new AddAppButton(this));
        mLaunchpadAdapter.setList(list);
        hideLoading();
        if (mLaunchpadAdapter.getItemCount() == 0){
            noApplicationList.setVisibility(View.VISIBLE);
        }else {
            noApplicationList.setVisibility(View.INVISIBLE);
        }
    }*/
    @Override
    public void setPresenter(ListAppContract.ListAppPresenter presenter) {
        this.mPresenter = presenter;
    }


    public static String getSHA1(Context context,String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo( /*context.getPackageName()*/packageName, PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i]).toUpperCase(Locale.US);
                if (appendString.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length() - 1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
