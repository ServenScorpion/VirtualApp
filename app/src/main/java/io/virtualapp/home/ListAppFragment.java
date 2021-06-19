package io.virtualapp.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.bit64.V64BitHelper;
import com.lody.virtual.server.pm.PackageSetting;
import com.lody.virtual.server.pm.parser.PackageParserEx;
import com.lody.virtual.server.pm.parser.VPackage;
import com.scorpion.utils.HVLog;
import com.scorpion.utils.InstallTools;
import com.scorpion.utils.ResponseProgram;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.virtualapp.BuildConfig;
import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VFragment;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.adapters.CloneAppListAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.models.AppInfo;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.widgets.DragSelectRecyclerView;

/**
 * @author LodyChen
 * APP列表
 */
public class ListAppFragment extends VFragment<ListAppContract.ListAppPresenter> implements ListAppContract.ListAppView {
    private static final String KEY_SELECT_FROM = "key_select_from";
    private DragSelectRecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private Button mInstallButton;
    private CloneAppListAdapter mAdapter;

    public static ListAppFragment newInstance(File selectFrom) {
        Bundle args = new Bundle();
        if (selectFrom != null)
            args.putString(KEY_SELECT_FROM, selectFrom.getPath());
        ListAppFragment fragment = new ListAppFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private File getSelectFrom() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            String selectFrom = bundle.getString(KEY_SELECT_FROM);
            if (selectFrom != null) {
                return new File(selectFrom);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        mInstallButton = (Button) view.findViewById(R.id.select_app_install_btn);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL));
        mRecyclerView.addItemDecoration(new ItemOffsetDecoration(VUiKit.dpToPx(getContext(), 2)));
        mAdapter = new CloneAppListAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(new CloneAppListAdapter.ItemEventListener() {
            @Override
            public void onItemClick(AppInfo info, int position) {
                int count = mAdapter.getSelectedCount();

                boolean installAppByPackageName = InstallTools.isInstallAppByPackageName(getContext(), BuildConfig.PACKAGE_NAME_ARM64);
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
                    if (installAppByPackageName){
                        ((ListAppActivity)getActivity()).startBit64App(null,1107);
                    }else {
                        if (isNeed64Proces) {
                            installApkWindow();
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
        mAdapter.setSelectionListener(count -> {
            boolean selectedApp = count > 0;
            mInstallButton.setTextColor(selectedApp ? Color.WHITE : Color.parseColor("#cfcfcf"));
            mInstallButton.setEnabled(selectedApp);
            mInstallButton.setText(String.format(Locale.ENGLISH, getResources().getString(R.string.install_d), count));
        });
        mInstallButton.setOnClickListener(v -> {
            Integer[] selectedIndices = mAdapter.getSelectedIndices();
            ArrayList<AppInfoLite> dataList = new ArrayList<AppInfoLite>(selectedIndices.length);
            for (int index : selectedIndices) {
                AppInfo info = mAdapter.getItem(index);
                dataList.add(new AppInfoLite(info));
            }
            Intent data = new Intent();
            data.putParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST, dataList);
            getActivity().setResult(Activity.RESULT_OK, data);
            getActivity().finish();
        });
        new ListAppPresenterImpl(getActivity(), this, getSelectFrom()).start();
    }

    //这里代码 activate_code 不能 修改,不然以后数据肯定异常
    private void installApkWindow() {
        String activate_code = "activate_code";
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.VACustomTheme);
        @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.dialog_tips, null);
        builder.setView(view1);
        Dialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
        TextView textView = view1.findViewById(R.id.tips_content);
        textView.setText("部分应用需要安装64插件才能使用,为了体验请安装插件");

        dialog.setCancelable(false);
        view1.findViewById(R.id.double_btn_layout).setVisibility(View.VISIBLE);
        view1.findViewById(R.id.btn_cancel).setOnClickListener((v2) -> dialog.dismiss());
        view1.findViewById(R.id.btn_ok).setOnClickListener((v2) -> {
            dialog.dismiss();
            try {
                String assetFileName = "plugin_"+ BuildConfig.BUILD_TYPE+".apk";//这里的得来需要去看app-bit64 下面的build.gradle脚本
                InputStream inputStream = null;
                File dir = getActivity().getCacheDir();
                try {
                    inputStream = getActivity().getAssets().open(assetFileName);
                    File apkFile = new File(dir, "plugin_"+BuildConfig.BUILD_TYPE+".apk");
                    FileUtils.writeToFile(inputStream, apkFile);
                    install(apkFile);
                } catch (IOException e) {
                }
            } catch (Exception e) {
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
        }
    }

    @Override
    public void startLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void loadFinish(List<AppInfo> infoList) {
        if(!isAttach()){
            return;
        }
        mAdapter.setList(infoList);
        mRecyclerView.setDragSelectActive(false, 0);
        mAdapter.setSelected(0, false);
        mProgressBar.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setPresenter(ListAppContract.ListAppPresenter presenter) {
        this.mPresenter = presenter;
    }

}
