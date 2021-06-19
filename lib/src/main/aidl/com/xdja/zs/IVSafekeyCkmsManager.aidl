// IVSafekeyCkmsManager.aidl
package com.xdja.zs;

// Declare any non-default types here with import statements
import com.xdja.zs.IVSKeyCallback;

interface IVSafekeyCkmsManager {
    byte[] ckmsencryptKey(in byte[] key,int keylen);
    byte[] ckmsdecryptKey(in byte[] seckey,int seckeylen);
    void registerCallback(IVSKeyCallback ivsKeyCallback);
    void unregisterCallback();
}
