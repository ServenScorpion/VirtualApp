package com.xdja.zs;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.helper.utils.VLog;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VSafekeyCkmsManagerService extends IVSafekeyCkmsManager.Stub {
    private static final String TAG = "CkmsManagerService";
    private IVSKeyCallback mIvsKeyCallback = null;
    private static VSafekeyCkmsManagerService sInstance;
    private static Context mContext;

    private static final int KEYCACHE_SIZE = 50;
    private static Map<ByteBuffer,byte[]> mkeyCache = new ConcurrentHashMap<>();
    private static final int COUNT_NUM = 40;

    public static void systemReady(Context context) {
        mContext = context;
        sInstance = new VSafekeyCkmsManagerService();
    }

    public static VSafekeyCkmsManagerService get() {
        return sInstance;
    }

    @Override
    public byte[] ckmsencryptKey(byte[] key, int keylen) {
        if (mIvsKeyCallback != null) {
            try {
                Log.d(TAG, "ckmsencryptKey");
                byte[] seckey = mIvsKeyCallback.encryptKey(key, keylen);
                if(mkeyCache.size() > KEYCACHE_SIZE) {
                    Iterator<Map.Entry<ByteBuffer, byte[]>> iterator = mkeyCache.entrySet().iterator();
                    while(iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                        if(mkeyCache.size() < COUNT_NUM) {
                            break;
                        }
                    }
                }
                if(seckey != null) {
                    mkeyCache.put(ByteBuffer.wrap(seckey),key);
                }
                return seckey;
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Log.d(TAG,"ckmsencryptKey 未注册CallBack回调加解密 使用默认加密 倒序排列");
            byte[] bytes = new byte[keylen];
            for(int i = 0; i < keylen; i++) {
                bytes[i] = key[keylen - i - 1];
            }
            return bytes;

        }
    }

    @Override
    public byte[] ckmsdecryptKey(byte[] seckey, int seckeylen) {
        if (mIvsKeyCallback != null) {
            try {
                Log.d(TAG, "ckmsdecryptKey");
                if(mkeyCache.containsKey(ByteBuffer.wrap(seckey))) {
                    //Log.d(TAG,"get key from cache");//调试log
                    return mkeyCache.get(ByteBuffer.wrap(seckey));
                }
                byte[] plainkey = mIvsKeyCallback.decryptKey(seckey, seckeylen);
                if(mkeyCache.size() > KEYCACHE_SIZE) {
                    Iterator<Map.Entry<ByteBuffer, byte[]>> iterator = mkeyCache.entrySet().iterator();
                    while(iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                        if(mkeyCache.size() < COUNT_NUM) {
                            break;
                        }
                    }
                }
                if(plainkey != null) {
                    mkeyCache.put(ByteBuffer.wrap(seckey),plainkey);
                }
                return plainkey;
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Log.d(TAG,"ckmsdecryptKey 未注册CallBack回调加解密 使用默认解密 倒序排列");
            byte[] bytes = new byte[seckeylen];
            for(int i = 0; i < seckeylen; i++) {
                bytes[i] = seckey[seckeylen - i - 1];
            }
            return bytes;
        }
    }

    @Override
    public void registerCallback(IVSKeyCallback ivsKeyCallback) {
        if (ivsKeyCallback != null) {
            mIvsKeyCallback = ivsKeyCallback;
        } else {
            VLog.e(TAG, "VSCkms vsCallback is null, registerCallback failed");
        }
    }

    @Override
    public void unregisterCallback() {
        VLog.e(TAG, "VSCkms unregisterCallback");
        mIvsKeyCallback = null;
        mkeyCache.clear();
    }
}
