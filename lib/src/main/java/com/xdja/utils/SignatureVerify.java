package com.xdja.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VPackageManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * @Date 19-5-29 11
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class SignatureVerify {
    private static String TAG = SignatureVerify.class.getName();
    private String[] keystores ={"43:BD:02:6B:9D:53:D7:F5:00:B2:BC:BD:BB:34:5B:F1:CD:EF:7F:C0"};

    public static boolean isEnable = true;

    public boolean checkSourceSignature(String source){
        String SHA1 =  getVSHA1Signature(source,0);
        Log.e(TAG,"getSHA1Signature "+SHA1);
        if(TextUtils.isEmpty(SHA1)){
            return false;
        }
        ArrayList<String> keystores = PackagePermissionManager.getInstallSourceSignature();
        Log.e(TAG,"getInstallSourceSignature "+keystores);
        if(keystores!=null){
            for (String key: keystores){
                if(SHA1.equals(key)){
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * 获取应用签名 SHA1
     * @param packagename
     * @return
     */
    public static String getSHA1Signature(@NonNull String packagename) {
        try {
            PackageInfo info = VirtualCore.getPM().getPackageInfo(packagename, PackageManager.GET_SIGNATURES);
            if (info == null)
                return null;
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                if(i < (publicKey.length-1))
                    hexString.append(":");
            }
            return hexString.toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取安全盒应用签名 SHA1
     * @return
     */
    public static String getHostSHA1Signature() {
        try {
            PackageInfo info = VirtualCore.get().getUnHookPackageManager().getPackageInfo(VirtualCore.get().getHostPkg(), PackageManager.GET_SIGNATURES);
            if (info == null)
                return null;
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                if(i < (publicKey.length-1))
                    hexString.append(":");
            }
            return hexString.toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取应用签名 SHA1
     * @param packagename
     * @return
     */
    public static String getVSHA1Signature(@NonNull String packagename, int userid) {
        PackageInfo info = VPackageManager.get().getPackageInfo(packagename, PackageManager.GET_SIGNATURES, userid);
        if (info == null)
            return null;
        return getPackageInfoSHA1(info);
    }

    private static String getPackageInfoSHA1(@NonNull PackageInfo info){
        try{
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                if(i < (publicKey.length-1))
                    hexString.append(":");
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
