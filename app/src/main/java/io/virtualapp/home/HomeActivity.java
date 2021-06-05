package io.virtualapp.home;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.oem.OemPermissionHelper;
import com.scorpion.splash.LoadingActivity;
import com.scorpion.utils.SPTools;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.List;

import io.virtualapp.App;
import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.nestedadapter.SmartRecyclerAdapter;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.XposedManager.XposedManagerActivity;
import io.virtualapp.home.adapters.LaunchpadAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.device.DeviceSettingsActivity;
import io.virtualapp.home.location.LocationSettingsActivity;
import io.virtualapp.home.models.AddAppButton;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.EmptyAppData;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.widgets.MarqueeTextView;
import io.virtualapp.widgets.TwoGearsView;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_DRAG;
import static android.support.v7.widget.helper.ItemTouchHelper.DOWN;
import static android.support.v7.widget.helper.ItemTouchHelper.END;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static android.support.v7.widget.helper.ItemTouchHelper.START;
import static android.support.v7.widget.helper.ItemTouchHelper.UP;

/**
 * @author LodyChen
 */
public class HomeActivity extends VActivity implements HomeContract.HomeView {

    @SuppressWarnings("unused")
    private static final String TAG = HomeActivity.class.getSimpleName();

    private HomeContract.HomePresenter mPresenter;
    private TwoGearsView mLoadingView;
    private RecyclerView mLauncherView;
    private View mMenuView;
    private PopupMenu mPopupMenu;
    private View mBottomArea;
    private TextView mEnterSettingTextView;
    private View mDeleteAppBox;
    private TextView mDeleteAppTextView;
    private MarqueeTextView tipsNotifiction;
    private LaunchpadAdapter mLaunchpadAdapter;
    private Handler mUiHandler;

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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mUiHandler = new Handler(Looper.getMainLooper());
        bindViews();
        initLaunchpad();
        initMenu();
        new HomePresenterImpl(this);
        mPresenter.check64bitEnginePermission();
        mPresenter.start();
        verifyStoragePermissions(this);
    }

    //复制assets文件到内部存储中
    public void copyWX() {
        Log.i("HomeActivity","开始复制微信");
        Context context = this;
        AssetManager am = context.getAssets();
        AssetFileDescriptor afd = null;
        try {
            afd = am.openFd("apps/wx7010.apk");
            // Create new file to copy into.
            File file = new File(Environment.getExternalStorageDirectory() + java.io.File.separator + "apps/wx7010.apk");
            file.createNewFile();
            copyFdToFile(afd.getFileDescriptor(), file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFdToFile(FileDescriptor src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
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
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            } else {
                //读取文件信息
//                copyWX();
                /*FileUtils1.getInstance(getApplicationContext()).copyAssetsToSD("apps", "apps").setFileOperateCallback(new FileUtils1.FileOperateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.e(TAG, "onSuccess: ------------------");
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e(TAG, "onFailed: ------------------");
                    }
                });*/

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

/*        menu.add(R.string.menu_accounts).setIcon(R.drawable.ic_account).setOnMenuItemClickListener(item -> {
            List<VUserInfo> users = VUserManager.get().getUsers();
            List<String> names = new ArrayList<>(users.size());
            for (VUserInfo info : users) {
                names.add(info.name);
            }
            CharSequence[] items = new CharSequence[names.size()];
            for (int i = 0; i < names.size(); i++) {
                items[i] = names.get(i);
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.choose_user_title)
                    .setItems(items, (dialog, which) -> {
                        VUserInfo info = users.get(which);
                        Intent intent = new Intent(this, ChooseTypeAndAccountActivity.class);
                        intent.putExtra(ChooseTypeAndAccountActivity.KEY_USER_ID, info.id);
                        startActivity(intent);
                    }).show();
            return false;
        });*/
        menu.add(R.string.kill_all_app).setIcon(R.drawable.ic_speed_up).setOnMenuItemClickListener(item -> {
            VActivityManager.get().killAllApps();
            Toast.makeText(this, "Memory release complete!", Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.add(R.string.menu_gms).setIcon(R.drawable.ic_google).setOnMenuItemClickListener(item -> {
            askInstallGms();
            return true;
        });

        menu.add(R.string.menu_mock_phone).setIcon(R.drawable.ic_device).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, DeviceSettingsActivity.class));
            return true;
        });

        menu.add(R.string.menu_mock_wifi).setIcon(R.drawable.ic_wifi).setOnMenuItemClickListener(item -> {
            showInputWindow();
            return true;
        });

        menu.add(R.string.virtual_location).setIcon(R.drawable.ic_location).setOnMenuItemClickListener(item -> {
            if (mPresenter.getAppCount() == 0) {
                Toast.makeText(this, R.string.tip_no_app, Toast.LENGTH_SHORT).show();
                return false;
            }
            startActivity(new Intent(this, LocationSettingsActivity.class));
            return true;
        });

        menu.add("xposed管理器").setIcon(R.drawable.ic_xposed).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, XposedManagerActivity.class));
            return true;
        });

        menu.add("激活程序").setIcon(R.drawable.ic_about).setOnMenuItemClickListener(item -> {
            activateInputWindow();
            return true;
        });
        mMenuView.setOnClickListener(v -> mPopupMenu.show());
    }

    private void bindViews() {
        mLoadingView = findViewById(R.id.pb_loading_app);
        mLauncherView = findViewById(R.id.home_launcher);
        mMenuView = findViewById(R.id.home_menu);
        mBottomArea = findViewById(R.id.bottom_area);
        mEnterSettingTextView = findViewById(R.id.enter_app_setting_text);
        mDeleteAppBox = findViewById(R.id.delete_app_area);
        mDeleteAppTextView = findViewById(R.id.delete_app_text);
        tipsNotifiction = findViewById(R.id.tipsNotifiction);
    }

    //region 更多功能
    private void showInputWindow() {
        String SSID_KEY = "ssid_key";
        String MAC_KEY = "mac_key";
        SettingConfig.FakeWifiStatus fakeWifiStatus = App.getApp().mConfig.getFakeWifiStatus();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.VACustomTheme);
        @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.dialog_change_wifi, null);
        builder.setView(view1);
        Dialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
        EditText editText1 = view1.findViewById(R.id.edt_ssid);
        editText1.setText((fakeWifiStatus.getSSID()));

        EditText editText2 = view1.findViewById(R.id.edt_mac);
        editText2.setText(fakeWifiStatus.getMAC());

        dialog.setCancelable(false);
        view1.findViewById(R.id.btn_cancel).setOnClickListener((v2) -> dialog.dismiss());
        view1.findViewById(R.id.btn_ok).setOnClickListener((v2) -> {
            dialog.dismiss();
            try {
                String ssid = editText1.getText().toString();
                String mac = editText2.getText().toString();

                SPTools.putString(this,SSID_KEY,ssid);
                SPTools.putString(this,MAC_KEY, mac);
            } catch (Exception e) {
                Toast.makeText(this, R.string.input_loc_error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    //这里代码 activate_code 不能 修改,不然以后数据肯定异常
    private void activateInputWindow() {
        String activate_code = "activate_code";
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.VACustomTheme);
        @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.dialog_activate, null);
        builder.setView(view1);
        Dialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
        EditText editText1 = view1.findViewById(R.id.edt_activate);


        dialog.setCancelable(false);
        view1.findViewById(R.id.btn_cancel).setOnClickListener((v2) -> dialog.dismiss());
        view1.findViewById(R.id.btn_ok).setOnClickListener((v2) -> {
            dialog.dismiss();
            try {
                String activate_num = editText1.getText().toString();
                SPTools.putString(this,activate_code,activate_num);

                String laveTime = VirtualCore.get().getLaveTime();
                if (TextUtils.isEmpty(laveTime)) {
                    tipsNotifiction.setVisibility(View.GONE);
                }else {
                    tipsNotifiction.setText(laveTime);
                }

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
        footer.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VUiKit.dpToPx(this, 60)));
        wrap.setFooterView(footer);
        mLauncherView.setAdapter(wrap);
        mLauncherView.addItemDecoration(new ItemOffsetDecoration(this, R.dimen.desktop_divider));
        ItemTouchHelper touchHelper = new ItemTouchHelper(new LauncherTouchCallback());
        touchHelper.attachToRecyclerView(mLauncherView);
        mLaunchpadAdapter.setAppClickListener((pos, data) -> {
            if (!data.isLoading()) {
                if (data instanceof AddAppButton) {
                    onAddAppButtonClick();
                }
                mLaunchpadAdapter.notifyItemChanged(pos);
                mPresenter.launchApp(data);
            }
        });

        String laveTime = VirtualCore.get().getLaveTime();
        if (TextUtils.isEmpty(laveTime)) {
            tipsNotifiction.setVisibility(View.GONE);
        }else {
            tipsNotifiction.setText(laveTime);
        }
    }

    private void onAddAppButtonClick() {
        ListAppActivity.gotoListApp(this);
    }

    private void deleteApp(int position) {
        AppData data = mLaunchpadAdapter.getList().get(position);
        new AlertDialog.Builder(this)
                .setTitle(R.string.tip_delete)
                .setMessage(getString(R.string.text_delete_app, data.getName()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> mPresenter.deleteApp(data))
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void enterAppSetting(int position) {
        AppData model = mLaunchpadAdapter.getList().get(position);
        if (model instanceof PackageAppData || model instanceof MultiplePackageAppData) {
            mPresenter.enterAppSetting(model);
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
        mBottomArea.setTranslationY(mBottomArea.getHeight());
        mBottomArea.setVisibility(View.VISIBLE);
        mBottomArea.animate().translationY(0).setDuration(500L).start();
    }

    @Override
    public void hideBottomAction() {
        mBottomArea.setTranslationY(0);
        ObjectAnimator transAnim = ObjectAnimator.ofFloat(mBottomArea, "translationY", 0, mBottomArea.getHeight());
        transAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mBottomArea.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mBottomArea.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        transAnim.setDuration(500L);
        transAnim.start();
    }

    @Override
    public void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
        mLoadingView.startAnim();
    }

    @Override
    public void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
        mLoadingView.stopAnim();
    }

    @Override
    public void loadFinish(List<AppData> list) {
        list.add(new AddAppButton(this));
        mLaunchpadAdapter.setList(list);
        hideLoading();
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
        if (!GmsSupport.isOutsideGoogleFrameworkExist()) {
            return;
        }
        if (GmsSupport.isInstalledGoogleService()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.tip)
                .setMessage(R.string.text_install_gms)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        defer().when(() -> GmsSupport.installGApps(0))
                                .done((res) -> mPresenter.dataChanged()))
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VCommends.REQUEST_SELECT_APP) {
            if (resultCode == RESULT_OK && data != null) {
                List<AppInfoLite> appList = data.getParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST);
                if (appList != null) {
                    for (AppInfoLite info : appList) {
                        mPresenter.addApp(info);
                    }
                }
            }
        } else if (requestCode == VCommends.REQUEST_PERMISSION) {
            if (resultCode == RESULT_OK) {
                String packageName = data.getStringExtra("pkg");
                int userId = data.getIntExtra("user_id", -1);
                LoadingActivity.launch(this,packageName,userId);
            }
        }
    }

    private class LauncherTouchCallback extends ItemTouchHelper.SimpleCallback {

        int[] location = new int[2];
        boolean upAtDeleteAppArea;
        boolean upAtEnterSettingArea;
        RecyclerView.ViewHolder dragHolder;

        LauncherTouchCallback() {
            super(UP | DOWN | LEFT | RIGHT | START | END, 0);
        }

        @Override
        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
            return 0;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            try {
                AppData data = mLaunchpadAdapter.getList().get(viewHolder.getAdapterPosition());
                if (!data.canReorder()) {
                    return makeMovementFlags(0, 0);
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return super.getMovementFlags(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int pos = viewHolder.getAdapterPosition();
            int targetPos = target.getAdapterPosition();
            mLaunchpadAdapter.moveItem(pos, targetPos);
            return true;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder instanceof LaunchpadAdapter.ViewHolder) {
                if (actionState == ACTION_STATE_DRAG) {
                    if (dragHolder != viewHolder) {
                        dragHolder = viewHolder;
                        viewHolder.itemView.setScaleX(1.2f);
                        viewHolder.itemView.setScaleY(1.2f);
                        if (mBottomArea.getVisibility() == View.GONE) {
                            showBottomAction();
                        }
                    }
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current, RecyclerView.ViewHolder target) {
            if (upAtEnterSettingArea || upAtDeleteAppArea) {
                return false;
            }
            try {
                AppData data = mLaunchpadAdapter.getList().get(target.getAdapterPosition());
                return data.canReorder();
            } catch (IndexOutOfBoundsException e) {
                //ignore
            }
            return false;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof LaunchpadAdapter.ViewHolder) {
                viewHolder.itemView.setScaleX(1f);
                viewHolder.itemView.setScaleY(1f);
            }
            super.clearView(recyclerView, viewHolder);
            if (dragHolder == viewHolder) {
                if (mBottomArea.getVisibility() == View.VISIBLE) {
                    mUiHandler.postDelayed(HomeActivity.this::hideBottomAction, 200L);
                    if (upAtEnterSettingArea) {
                        enterAppSetting(viewHolder.getAdapterPosition());
                    } else if (upAtDeleteAppArea) {
                        deleteApp(viewHolder.getAdapterPosition());
                    }
                }
                dragHolder = null;
            }
        }


        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            if (actionState != ACTION_STATE_DRAG || !isCurrentlyActive) {
                return;
            }
            View itemView = viewHolder.itemView;
            itemView.getLocationInWindow(location);
            int x = (int) (location[0] + dX);
            int y = (int) (location[1] + dY);

            mBottomArea.getLocationInWindow(location);
            int baseLine = location[1] - mBottomArea.getHeight();
            if (y >= baseLine) {
                mDeleteAppBox.getLocationInWindow(location);
                int deleteAppAreaStartX = location[0];
                if (x < deleteAppAreaStartX) {
                    upAtEnterSettingArea = true;
                    upAtDeleteAppArea = false;
                    mEnterSettingTextView.setTextColor(Color.parseColor("#0099cc"));
                    mDeleteAppTextView.setTextColor(Color.BLACK);
                } else {
                    upAtDeleteAppArea = true;
                    upAtEnterSettingArea = false;
                    mDeleteAppTextView.setTextColor(Color.parseColor("#0099cc"));
                    mEnterSettingTextView.setTextColor(Color.BLACK);
                }
            } else {
                upAtEnterSettingArea = false;
                upAtDeleteAppArea = false;
                mDeleteAppTextView.setTextColor(Color.BLACK);
                mEnterSettingTextView.setTextColor(Color.BLACK);
            }
        }
    }
}
