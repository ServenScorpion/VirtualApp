package com.lody.virtual.client.stub;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.R;

import java.util.HashSet;
import java.util.Set;

/**
 * @Date 18-4-19 15
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class InstallerSetting {

    /**
     *
     */
    public static final String CLOCK_PKG = "com.xdja.deskclock";

    /**
     * MDMService包名
     */
    public static final String MDM_SERVICE_PKG = "com.xdja.mdmservice";
    /**
     * MDMClient包名
     */
    public static final String MDM_CLIENT_PKG = "com.xdja.emm";
    /**
     * 电话包名
     */
    public static final String DIALER_PKG = "com.xdja.dialer";
    /**
     * 文件管理器包名
     */
    public static final String FILE_EXPLORER_PKG = "com.secspace.app.explorer";
    /**
     * 图库包名
     */
    public static final String GALLERY_PKG = "com.android.gallery3d";
    /**
     * 浏览器包名
     */
    public static final String BROWSER_PKG = "mark.via";
    /**
     * 计算器包名
     */
    public static final String CALCULATOR_PKG = "com.android.calculator2";
    /**
     * 视频播放器
     */
    public static final String VIDEO_PLAYER_PKG = "com.mxtech.videoplayer.ad";
    /**
     * 记事本包名
     */
    public static final String NOTE_PKG = "com.secspace.app.note";
    /**
     * 照相机包名
     */
    public static final String CAMERA_PKG = "com.baby518.camera2";
    /**
     * wps包名
     */
    public static final String WPS_PKG = "cn.wps.moffice_eng";
    /**
     * 安全接入包名
     */
    public static final String SAFE_CLIENT_PKG = "com.xdja.safeclient";

    public static final String MESSAGING_PKG = "com.xdja.mms";

    public static final String PROVIDER_TELEPHONY_PKG = "com.android.providers.telephony";

    public static final String PROVIDER_CONTACTS_PKG = "com.android.providers.contacts";

    public static final String PROVIDER_MEDIA_PKG = "com.android.providers.media";

    static public Set<String> safeApps = new HashSet<>();   //认证应用
    static public Set<String> systemApps = new HashSet<>(); //系统应用
    static public Set<String> protectApps = new HashSet<>();//保护应用

    static public Set<String> privApps = new HashSet<>();

    static{

        safeApps.add("com.tencent.mm");         //微信
        safeApps.add("cn.wps.moffice_eng");     //WPS

        safeApps.add("com.xdja.jxclient");      //警信
        safeApps.add("com.xdja.safeclient");    //安全客户端
        safeApps.add("com.xdja.jwt.law");       //法律法规
        safeApps.add("com.xdja.jwt.bj");        //一键报警
        safeApps.add("com.xdja.jwtlxhc");       //拍照识别 & 识别库
        safeApps.add("com.xdja.hdfjwt");        //综合查询
        safeApps.add("com.xdja.jwt.jtgl");      //交通管理
        safeApps.add("com.xdja.jwtlxhc");       //离线核查
        safeApps.add("com.xdja.eoa");           //移动办公
        safeApps.add("com.xdja.uaac");          //统一认证
        safeApps.add("com.xdja.jwt.portal");    //陕西警务
        safeApps.add("com.xdja.swbg");          //税务办公
        safeApps.add("com.xdja.jxpush");        //指令推送 警信依赖
        //预置应用
        systemApps.add(MDM_SERVICE_PKG);
        systemApps.add(MDM_CLIENT_PKG);
        systemApps.add(DIALER_PKG);
        systemApps.add(FILE_EXPLORER_PKG);
        systemApps.add(GALLERY_PKG);
        systemApps.add(BROWSER_PKG);
        systemApps.add(CALCULATOR_PKG);
        systemApps.add(VIDEO_PLAYER_PKG);
        systemApps.add(NOTE_PKG);
        systemApps.add(CAMERA_PKG);
        systemApps.add(WPS_PKG);
        systemApps.add(SAFE_CLIENT_PKG);

        privApps.add(MESSAGING_PKG);
        privApps.add(MDM_CLIENT_PKG);
        privApps.add(MDM_SERVICE_PKG);
        privApps.add(CLOCK_PKG);
        privApps.add(DIALER_PKG);
        privApps.add(PROVIDER_MEDIA_PKG);
        privApps.add(PROVIDER_CONTACTS_PKG);
		privApps.add(PROVIDER_TELEPHONY_PKG);
    }
    public static void addProtectApps(String packageName){
        if(!protectApps.contains(packageName))
            protectApps.add(packageName);
    }
    public static Set<String> getProtectApps(){
        return protectApps;
    }
    public static void deleteProtectApps(String packageName){
        if(protectApps.contains(packageName)){
            protectApps.remove(packageName);
        }
    }

    public static void showToast(Context context, String message, int duration) {
        Toast toast = new Toast(context);
        View toastView = LayoutInflater.from(context).inflate(R.layout.toast_install_del, null);
        TextView contentView = toastView.findViewById(R.id.TextViewInfo);
        contentView.setText(message);
        toast.setView(toastView);
        toast.setDuration(duration);
        toast.setGravity(Gravity.BOTTOM, 0,
                context.getResources().getDimensionPixelOffset(R.dimen.dp_110));
        toast.show();
    }
}
