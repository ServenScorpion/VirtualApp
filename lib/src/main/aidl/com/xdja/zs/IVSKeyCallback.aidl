// IVSKeyCallback.aidl
package com.xdja.zs;

// Declare any non-default types here with import statements

interface IVSKeyCallback {
    byte[] encryptKey(in byte[] key,int keylen);
    byte[] decryptKey(in byte[] key, int seckeylen);
}
