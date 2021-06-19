package com.lody.virtual.client.core;

import android.app.IWallpaperManagerCallback;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.stub.ChooserActivity;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.client.stub.WindowPreviewActivity;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.compat.NotificationChannelCompat;

/**
 * @author Lody
 */
public abstract class SettingConfig {

    public abstract String getHostPackageName();

    public abstract String getPluginEnginePackageName();

    public String getBinderProviderAuthority() {
        return getHostPackageName() + ".virtual.service.BinderProvider";
    }

    public String get64bitHelperAuthority() {
        return getPluginEnginePackageName() + ".virtual.service.64bit_helper";
    }

    @Deprecated
    public String getProxyFileContentProviderAuthority() {
        return getHostPackageName() + ".virtual.fileprovider";
    }

    public boolean isEnableIORedirect() {
        return true;
    }

    public boolean isAllowCreateShortcut() {
        return true;
    }

    public boolean isUseRealDataDir(String packageName) {
        return false;
    }

    public boolean isUseRealLibDir(String packageName) {
        return false;
    }

    /**
     *
     * 当app请求回到桌面时调用此方法
     *
     * @return intent or null
     */
    public Intent onHandleLauncherIntent(Intent originIntent) {
        return null;
    }

    public enum AppLibConfig {
        UseRealLib,
        UseOwnLib,
    }

    public AppLibConfig getAppLibConfig(String packageName) {
        return AppLibConfig.UseOwnLib;
    }

    public boolean isAllowServiceStartForeground(String packageName) {
        return true;
    }

    public boolean isEnableAppFileSystemIsolation() {
        return false;
    }

    public boolean isHideForegroundNotification() {
        return false;
    }

    public Notification getForegroundNotification(){
        Notification.Builder builder = NotificationChannelCompat.createBuilder(VirtualCore.get().getContext(),
                NotificationChannelCompat.DAEMON_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_SECRET);
        }
        builder.setSound(null);
        return builder.build();
    }

    public FakeWifiStatus getFakeWifiStatus() {
        return null;
    }

    public boolean IsServiceCanRestart(ServiceInfo serviceInfo){
        return false;
    }

    /**
     * 是否禁止悬浮窗
     */
    public boolean isDisableDrawOverlays(String packageName){
        return false;
    }

    public boolean isFloatOnLockScreen(String className){
        if ((className!= null) && ("com.xdja.incallui.InCallActivity".equals(className)
                || className.endsWith("plugin.voip.ui.VideoActivity")
                || "com.xdja.securevoip.presenter.activity.InCallPresenter".equals(className)
                || "com.xdja.voip.sdk.incall.InCallActivity".equals(className)
                || "com.android.deskclock.alarms.AlarmActivity".equals(className))){
            return true;
        }
        return false;
    }

    /***
     * 深色模式处理
     */
    public void onDarkModeChange(boolean isDarkMode){

    }

    public boolean useOutsideResourcesBySameApk(String packageName){
        return false;
    }

    /**
     * 是否允许通过广播启动进程
     * 允许规则：
     * 1.userId对应的应用任意一个进程已经启动
     * 2.isAllowStartByReceiver返回true
     */
    public boolean isAllowStartByReceiver(String packageName, int userId, String action) {
        return false;
    }

    public void onFirstInstall(String packageName, boolean isClearData){

    }

    public boolean isNeedRealRequestInstall(String packageName){
        return false;
    }

    /**
     * 预留接口：定制白屏/黑屏，透明的默认显示界面
     * @param userId
     * @param info
     */
    public void startPreviewActivity(int userId, ActivityInfo info, VirtualCore.UiCallback callBack){
        WindowPreviewActivity.previewActivity(userId, info, callBack);
    }

    public boolean isForceVmSafeMode(String packageName){
        return false;
    }

    public void onPreLunchApp(){

    }

    /**
     *
     * @param intent 如果需要默认组件，就设置intent#setComponent
     * @param packageName
     * @param userId
     * @return true则提示找不到activity，false内部显示选择列表
     */
    public boolean onHandleView(Intent intent, String packageName, int userId){
        return false;
    }

    public Intent getChooserIntent(Intent orgIntent, IBinder resultTo, String resultWho, int requestCode, Bundle options, int userId){
        Bundle extras = new Bundle();
        extras.putInt(Constants.EXTRA_USER_HANDLE, userId);
        extras.putBundle(ChooserActivity.EXTRA_DATA, options);
        extras.putString(ChooserActivity.EXTRA_WHO, resultWho);
        extras.putInt(ChooserActivity.EXTRA_REQUEST_CODE, requestCode);
        if (Intent.ACTION_VIEW.equals(orgIntent.getAction()) || Intent.ACTION_GET_CONTENT.equals(orgIntent.getAction())) {
            extras.putParcelable(Intent.EXTRA_INTENT, new Intent(orgIntent).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (orgIntent.getAction() != null
                && (orgIntent.getAction().equals(ChooserActivity.ACTION) || orgIntent.getAction().equals(Intent.ACTION_CHOOSER))) {
            extras.putParcelable(Intent.EXTRA_INTENT, orgIntent.getParcelableExtra(Intent.EXTRA_INTENT));
        }
        BundleCompat.putBinder(extras, ChooserActivity.EXTRA_RESULTTO, resultTo);
        Intent intent =  new Intent();
        //如果上层需要重写ChooserActivity的界面，可以参考这个
        intent.setComponent(new ComponentName(StubManifest.PACKAGE_NAME, ChooserActivity.class.getName()));
        intent.putExtras(extras);
        intent.putExtra("_VA_CHOOSER",true);
        return intent;
    }

    public boolean isClearInvalidTask(){
        return true;
    }

    public boolean isClearInvalidProcess(){
        return true;
    }

    public boolean isCanShowNotification(String packageName, boolean currentSpace) {
        return false;
    }


    /**
     * @param cropHint
     * @param which
     * @return null,则由系统处理外部桌面响应；WallpaperResult#wallpaperFile为null，则无法设置桌面；wallpaperFile为自己创建的文件，由当前app取写入
     */
    public WallpaperResult onSetWallpaper(String packageName, int userId, String name, Rect cropHint, int which, IWallpaperManagerCallback lock){
        return null;
    }

    public int getWallpaperWidthHint(String packageName, int userId){
        return -1;
    }

    public int getWallpaperHeightHint(String packageName, int userId){
        return -1;
    }

    public static class WallpaperResult {
        public ParcelFileDescriptor wallpaperFile;
    }

    /**
     * 如果内部MediaProvider实现铃声的uri，则需要处理content://media/internal/audio/，返回false
     * 目前是使用外部铃声设置
     * @param uri
     */
    public boolean useOutsideNotificationSound(Uri uri){
        return true;
    }

    public static class FakeWifiStatus {

        public static String DEFAULT_BSSID = "66:55:44:33:22:11";
        public static String DEFAULT_MAC = "11:22:33:44:55:66";
        public static String DEFAULT_SSID = "VA_SSID";

        public String getSSID() {
            return DEFAULT_SSID;
        }

        public String getBSSID() {
            return DEFAULT_BSSID;
        }

        public String getMAC() {
            return DEFAULT_MAC;
        }

    }

}
