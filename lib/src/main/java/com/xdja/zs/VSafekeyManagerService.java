package com.xdja.zs;

import android.content.Context;
import android.os.RemoteException;
import android.util.Pair;

import com.lody.virtual.helper.utils.VLog;
import com.xdja.SafeKey.JNIAPI;
import com.xdja.multi.unitepin.jar.MultiChipUnitePinManager;
import com.xdja.multichip.jniapi.JarJniApiProxy;
import com.xdja.multichip.jniapi.JarMultiJniApiManager;
import com.xdja.multichip.jniapi.JarMultiJniApiVhsmManager;
import com.xdja.multichip.param.JniApiParam;

import com.xdja.zs.IVSCallback;


import java.util.List;

/**
 * Created by wxudong on 17-12-16.
 */

public class VSafekeyManagerService extends IVSafekey.Stub {

    private static final String TAG = VSafekeyManagerService.class.getSimpleName();
    private static VSafekeyManagerService sInstance;
    private static Context mContext = null;
    private static JarMultiJniApiManager jniApiManager = null;
    private static Pair<Integer, List<JniApiParam>> all = null;
    private static JarJniApiProxy jniProxy = null;
    private static JarMultiJniApiVhsmManager jniApiVhsmManager = null;
    private static MultiChipUnitePinManager multiChipUnitePinManager = null;
    private static String cardId = null;
    private static Pair<Integer, JarJniApiProxy> jniProxyPair = null;
    private static int pinRole = 0x11;
    private static boolean cardFlag = false;

    private IVSCallback mvsCallback = null;

    private static final int INIT_OK = 0;
    private static final int VERIFY_PIN_FAIL = -1;
    private static final int CHECK_CARD_EXCEPTION = -2;


    public static void systemReady(Context context) {
        mContext = context;
        sInstance = new VSafekeyManagerService();
        multiChipUnitePinManager = MultiChipUnitePinManager.getInstance();
    }

    public static VSafekeyManagerService get() {
        return sInstance;
    }

    public static boolean initSafekeyLib(){
        try {
            VLog.e(TAG, "VS initSafekeyLib");
            jniApiManager = JarMultiJniApiManager.getInstance();
            if(jniApiManager == null) {
                VLog.e(TAG, "jniApiManager is null ");
                return false;
            }

            //如果芯片管家为未启动，此处崩溃。
            all = jniApiManager.getAll(mContext);
            if(all == null) {
                VLog.e(TAG, "List<JniApiParam> is null ");
                return false;
            }
            cardId = getCardIdStatic();
            if(cardId == null) {
                VLog.e(TAG, "cardId is null ");
                return false;
            }
            jniProxy = getJniProxy(mContext, cardId);
            if(jniProxy == null) {
                VLog.e(TAG, "jniProxy is null ");
                return false;
            }
            if (jniProxy.getCardType() == JniApiParam.TYPE_VHSM ||
                    jniProxy.getCardType() == JniApiParam.TYPE_VHSM_NET) {
                String pin = "111111";
                int ret = jniProxy.VerifyPIN(pinRole, pin.getBytes(), pin.length());
                if (ret != 0) {
                    VLog.e(TAG, "VerifyPIN fail:" + ret);
                    return false;
                }
            }
            return true;

        }catch (Exception e){
            VLog.e(TAG, "checkCard exception ");
            e.printStackTrace();
            return false;
        }
    }



    private static String getCardIdStatic(){
        if(all != null){
            if (all.first != 0){
                VLog.e(TAG,"Get card id failed ");
                return null;
            }
            String cardIdStr = null;
            int size = all.second.size();
            if (size >= 1) {
                String[] cardIdCache  = new String[5];
                for (JniApiParam jap : all.second) {
                    VLog.d(TAG, "CardId : " + jap.cardId + "CardType : " + jap.chipType);
                    if (jap.chipType == JniApiParam.TYPE_TF) {
                       cardIdCache[0] = jap.cardId;
                    } else if (jap.chipType == JniApiParam.TYPE_COVERED) {
                        cardIdCache[1] = jap.cardId;
                    } else if (jap.chipType == JniApiParam.TYPE_VHSM || jap.chipType == JniApiParam.TYPE_VHSM_NET) {
                        cardIdCache[2] = jap.cardId;
                        jniApiVhsmManager = JarMultiJniApiVhsmManager.getInstance();
                    } else if (jap.chipType == JniApiParam.TYPE_ONBOARD) {
                        cardIdCache[3] = jap.cardId;
                    } else if(jap.chipType == JniApiParam.TYPE_BLUETOOTH) {
                        cardIdCache[4] = jap.cardId;
                    }
                }
                for (String cardId: cardIdCache) {
                    if (cardId != null) {
                        VLog.d(TAG, "Use CardId : " + cardId);
                        cardIdStr = cardId;
                        cardFlag = true;
                        return cardIdStr;
                    }
                }
            } else if (size == 1){
                cardIdStr = all.second.get(0).cardId;
                if(cardIdStr != null){
                    cardFlag = true;
                    return cardIdStr;
                }
            }
        } else{
            VLog.e(TAG,"List<JniApiParam> is null, Get card id failed ! ");
        }
        return null;
    }

    @Override
    public int initSafekeyCard() {
        int ret = INIT_OK;
        try {
            VLog.e(TAG, "VS initSafekeyCard");
            jniApiManager = JarMultiJniApiManager.getInstance();
            if(jniApiManager == null) {
                VLog.e(TAG, "jniApiManager is null ");
                return CHECK_CARD_EXCEPTION;
            }

            //如果芯片管家为未启动，此处崩溃。
            all = jniApiManager.getAll(mContext);
            if(all == null) {
                VLog.e(TAG, "List<JniApiParam> is null ");
                return CHECK_CARD_EXCEPTION;
            }
            cardId = getCardIdStatic();
            if(cardId == null) {
                VLog.e(TAG, "cardId is null ");
                return CHECK_CARD_EXCEPTION;
            }
            jniProxy = getJniProxy(mContext, cardId);
            if(jniProxy == null) {
                VLog.e(TAG, "jniProxy is null ");
                return CHECK_CARD_EXCEPTION;
            }
            if (jniProxy.getCardType() == JniApiParam.TYPE_VHSM ||
                    jniProxy.getCardType() == JniApiParam.TYPE_VHSM_NET) {
                Pair<Integer, String> pin = multiChipUnitePinManager.getPin(mContext, cardId, pinRole);
                if (pin.first == 0) {
                    ret = jniProxy.VerifyPIN(pinRole, pin.second.getBytes(), pin.second.length());
                    if (ret != 0) {
                        VLog.e(TAG, "VerifyPIN fail:" + ret);
                        return VERIFY_PIN_FAIL;
                    }
                } else {
                    return VERIFY_PIN_FAIL;
                }
            }
            return ret;

        }catch (Exception e){
            VLog.e(TAG, "checkCard exception ");
            e.printStackTrace();
            return CHECK_CARD_EXCEPTION;
        }
    }

    @Override
    public String getCardId() throws RemoteException {
        if(all != null){
            if (all.first != 0){
                VLog.e(TAG,"Get card id failed ");
                return null;
            }
            String cardIdStr = null;
            for (JniApiParam jap : all.second) {

                VLog.d(TAG, "CardId : " + jap.cardId + "CardType : " + jap.chipType);

                if (jap.chipType == JniApiParam.TYPE_ONBOARD) {
                    cardIdStr = jap.cardId;
                } else if (jap.chipType == JniApiParam.TYPE_TF) {
                    cardIdStr = jap.cardId;
                } else if (jap.chipType == JniApiParam.TYPE_BLUETOOTH) {
                    cardIdStr = jap.cardId;
                } else if (jap.chipType == JniApiParam.TYPE_COVERED) {
                    cardIdStr = jap.cardId;
                } else if (jap.chipType == JniApiParam.TYPE_VHSM) {
                    cardIdStr = jap.cardId;
                } else if (jap.chipType == JniApiParam.TYPE_VHSM_NET) {
                    cardIdStr = jap.cardId;
                }
                if(cardIdStr != null){
                    cardFlag = true;
                    return cardIdStr;
                }

            }
        } else{
            VLog.e(TAG,"List<JniApiParam> is null, Get card id failed ! ");
        }
        return null;
    }

    private static JarJniApiProxy getJniProxy(Context context, String cardId){
        try {
            VLog.e(TAG, "VS getJniProxy");
            jniProxyPair = jniApiManager.make(context, cardId);
            if (jniProxyPair.first == 0) {
                return jniProxyPair.second;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }



    @Override
    public boolean checkCardState(){
        try {
            VLog.e(TAG, "VS checkCardState");
            if (mContext == null || jniApiManager == null) {
                VLog.e(TAG, "mContext or jniApiManager is null ");
                return false;
            }
            all = jniApiManager.getAll(mContext);
            if (all == null) {
                VLog.e(TAG, "List<JniApiParam> is null ");
                return false;
            }
            cardId = getCardId();
            if (cardId == null) {
                VLog.e(TAG, "cardId is null ");
                return false;
            }
            jniProxy = getJniProxy(mContext, cardId);
            if(jniProxy == null){
                VLog.e(TAG, "jniProxy is null ");
                return false;
            }
            return true;
        }catch (Exception e){
            VLog.e(TAG, "checkCardState exception ");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getPinTryCount() throws RemoteException {
        try {
            VLog.e(TAG, "VS getPinTryCount");
            int ret = -1;
            int pinRole = 0x11;
            if (jniProxy != null) {
                int num = jniProxy.GetPinTryCount(pinRole);
                VLog.e(TAG, "getPinTryCount : " + num);
                return num;
            }
            return ret;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public byte[] encryptKey(byte[] key, int keylen) throws RemoteException {
        byte[] seckey = new byte[keylen];
        for(int i=0; i<keylen; i++){
            seckey[i] = 0;
        }
        try {
            VLog.e(TAG, "VS encryptKey");
            byte encrypt_kID = 0x08;
            int ret = -1;
            if(jniProxy != null) {
                if (jniProxy.getCardType() == JniApiParam.TYPE_VHSM ||
                        jniProxy.getCardType() == JniApiParam.TYPE_VHSM_NET) {
                    ret = jniApiVhsmManager.SM4(jniProxy, key, keylen, JNIAPI.ECB_ENCRYPT, seckey, encrypt_kID, null);
                } else {
                    ret = jniProxy.SM1(key, keylen, JNIAPI.ECB_ENCRYPT, seckey, encrypt_kID, null);
                }
                if(ret < 0){
                    visitSafeKeyErrorCallback(ret);
                }
            }
            if(ret < 0){
                visitSafeKeyErrorCallback(ret);
            }
            VLog.e(TAG, "VS encryptKey ret "+ ret);
            return seckey;
        }catch (Exception e){
            visitSafeKeyErrorCallback(-1);
            e.printStackTrace();
            return seckey;
        }
    }

    @Override
    public byte[] decryptKey(byte[] seckey, int seckeylen) throws RemoteException {
        byte[] key = new byte[seckeylen];
        for(int i=0; i<seckeylen; i++){
            key[i] = 0;
        }
        try {
            VLog.e(TAG, "VS decryptKey");
            byte decrypt_kID = 0x09;
            int ret = -1;
            if(jniProxy != null) {
                if (jniProxy.getCardType() == JniApiParam.TYPE_VHSM ||
                jniProxy.getCardType() == JniApiParam.TYPE_VHSM_NET) {
                    ret = jniApiVhsmManager.SM4(jniProxy, seckey, seckeylen, JNIAPI.ECB_DECRYPT, key, decrypt_kID, null);
                } else {
                    ret = jniProxy.SM1(seckey, seckeylen, JNIAPI.ECB_DECRYPT, key, decrypt_kID, null);
                }
                if(ret < 0){
                    visitSafeKeyErrorCallback(ret);
                }
            }
            if(ret < 0){
                visitSafeKeyErrorCallback(ret);
            }
            VLog.e(TAG, "VS decryptKey ret "+ ret);
            return key;
        }catch (Exception e){
            visitSafeKeyErrorCallback(-1);
            e.printStackTrace();
            return key;
        }
    }

    @Override
    public byte[] getRandom(int len) throws RemoteException {
        byte[] random = new byte[len];
        for(int i=0; i<len; i++){
            random[i] = 0;
        }
        try {
            VLog.e(TAG, "VS getRandom");
            int ret = -1;
            if(jniProxy != null) {
                ret = jniProxy.GenRandom(len, random);
            }
            if(ret < 0){
                visitSafeKeyErrorCallback(ret);
            }
            VLog.e(TAG, "VS getRandom ret "+ ret);
            return random;
        }catch (Exception e){
            visitSafeKeyErrorCallback(-1);
            e.printStackTrace();
            return random;
        }
    }

    @Override
    public void registerCallback(IVSCallback vsCallback) throws RemoteException {
        VLog.e(TAG, "VS registerCallback ");
        if(vsCallback != null){
            mvsCallback = vsCallback;
        }else {
            VLog.e(TAG, "VS vsCallback is null, registerCallback failed");
        }
    }

    @Override
    public void unregisterCallback() throws RemoteException {
        VLog.e(TAG, "VS unregisterCallback ");
        mvsCallback = null;
    }

    private void visitSafeKeyErrorCallback(int error){
        try {
            if (mvsCallback != null) {
                mvsCallback.visitSafeKeyError(error);
                VLog.e(TAG, "VS getRandom visitSafeKeyError ");
            } else {
                VLog.e(TAG, "VS mvsCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
