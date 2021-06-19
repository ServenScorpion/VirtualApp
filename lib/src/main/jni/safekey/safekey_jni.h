//
// Created by wxudong on 17-12-17.
//

#ifndef VIRTUALAPP_SAFEKEY_JNI_H
#define VIRTUALAPP_SAFEKEY_JNI_H

#include <jni.h>

class SafeKeyJni {

public:
    static int encryptKey(char *input, int inputlen, char *output, int outputlen);
    static int decryptKey(char *input, int inputlen, char *output, int outputlen);
    static int operatorKey(char *input, int inputlen, char *output, int outputlen,int mode);
    static int getRandom(int len, char *random);
    static char * ckmsencryptKey(char *input, int inputlen, uint32_t & outputlen);
    static char * ckmsdecryptKey(char *input, int inputlen, uint32_t & outputlen);
    static char * ckmsoperatorKey(char *input, int inputlen, uint32_t & outputlen,int mode);
};


#endif //VIRTUALAPP_SAFEKEY_JNI_H
