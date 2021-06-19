// IVSafekey.aidl
package com.xdja.zs;

// Declare any non-default types here with import statements
import com.xdja.zs.IVSCallback;

interface IVSafekey {
    boolean checkCardState();
    String getCardId();
    int getPinTryCount();
    byte[] encryptKey(in byte[] key, int keylen);
    byte[] decryptKey(in byte[] seckey, int seckeylen);
    byte[] getRandom(int len);
    void registerCallback(IVSCallback vsCallback);
    void unregisterCallback();
    int initSafekeyCard();
}
