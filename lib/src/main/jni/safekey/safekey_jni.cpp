//
// Created by wxudong on 17-12-16.
//


#include <cstring>
#include <utils/mylog.h>
#include <stdlib.h>
#include <utils/zString.h>
#include <linux/time.h>
#include <time.h>

#include "safekey_jni.h"
#include "utils/zJNIEnv.h"

extern jclass vskmClass;
extern jclass vsckmsClass;

int SafeKeyJni::encryptKey(char *input, int inputlen, char *output, int outputlen){
    return operatorKey(input, inputlen, output, outputlen, 0);
   /* for(int i = 0; i < inputlen; i++)
        output[i] = input[i] + (char)3;

    return 0;*/
}

int SafeKeyJni::decryptKey(char *input, int inputlen, char *output, int outputlen){
    return operatorKey(input, inputlen, output, outputlen, 1);
   /* for(int i = 0; i < inputlen; i++)
        output[i] = input[i] - (char)3;

    return 0;*/
}

int SafeKeyJni::operatorKey(char *input, int inputlen, char *output, int outputlen, int mode) {

    log("SafeKeyJni operatorKey start mode %d keylen %d", mode, inputlen);
    int ret = 0;
    zJNIEnv env;
    if(env.get() == NULL) {
        log("JNIEnv is NULL");
        return -1;
    }

    jbyteArray _input = env.get()->NewByteArray(inputlen);
    env.get()->SetByteArrayRegion(_input, 0, inputlen, (jbyte*)input);
    jbyteArray _output;
    jmethodID mid = NULL;
    if(mode == 0){
        mid = env.get()->GetStaticMethodID(vskmClass, "encryptKey", "([BI)[B");
    }else{
        mid = env.get()->GetStaticMethodID(vskmClass, "decryptKey", "([BI)[B");
    }
    _output = (jbyteArray)env.get()->CallStaticObjectMethod(vskmClass, mid ,_input, inputlen);
    jbyte* a = env.get()->GetByteArrayElements(_output, JNI_FALSE);
    memcpy(output, a, (size_t)inputlen);

    for(int i=0; i<inputlen; i++){
        if(output[i] != 0){
            ret = 0;
            break;
        }
        ret = -1;
    }
    log("SafeKeyJni operatorKey ret = %d", ret);

    env.get()->ReleaseByteArrayElements(_output, a, 0);
    env.get()->DeleteLocalRef(_input);
    env.get()->DeleteLocalRef(_output);

    /*zString tmp;
    char * p = tmp.getBuf();
    for(int i = 0; i < inputlen; i++)
    {
        sprintf(p + i*2, "%02hhx", output[i]);
    }
    log("SafeKeyJni operatorKey end return %d [%s]", ret, p);*/
    return ret;
}

int SafeKeyJni::getRandom(int len, char *random) {

    int ret = 0;
    log("SafeKeyJni getRandom start keylen %d", len);
    zJNIEnv env;
    if(env.get() == NULL) {
        log("JNIEnv is NULL");
        return -1;
    }

    jbyteArray _output;
    jmethodID mid = env.get()->GetStaticMethodID(vskmClass, "getRandom", "(I)[B");
    _output = (jbyteArray)env.get()->CallStaticObjectMethod(vskmClass, mid, len);
    jbyte* a = env.get()->GetByteArrayElements(_output, JNI_FALSE);
    memcpy(random, a, (size_t)len);

    for(int i=0; i<len; i++){
        if(random[i] != 0){
            ret = 0;
            break;
        }
        ret = -1;
    }
    log("SafeKeyJni getRandom ret = %d", ret);

    env.get()->ReleaseByteArrayElements(_output, a, 0);
    env.get()->DeleteLocalRef(_output);
    return ret;

   /* timespec time;
    clock_gettime(CLOCK_REALTIME, &time);  //获取相对于1970到现在的秒数
    srand48(time.tv_nsec);
    lrand48();

    for(int i = 0; i < len; i++)
    {
        random[i] = 'a' + (char)(lrand48() / 26);
    }

    zString tmp;
    char * p = tmp.getBuf();
    for(int i = 0; i < len; i++)
    {
        sprintf(p + i*2, "%02hhx", random[i]);
    }

    log("SafeKeyJni getRandom end return %d [%s]", ret, p);*/
}

char *SafeKeyJni::ckmsencryptKey(char *input, int inputlen, uint32_t &outputlen) {
        return ckmsoperatorKey(input, inputlen, outputlen, 0);
    }

char *SafeKeyJni::ckmsdecryptKey(char *input, int inputlen, uint32_t &outputlen) {
        return ckmsoperatorKey(input, inputlen, outputlen, 1);
    }


char *SafeKeyJni::ckmsoperatorKey(char *input, int inputlen, uint32_t &outputlen,
                                  int mode) {
    zJNIEnv env;
    if (env.get() == NULL) {
        log("JNIEnv is NULL");
        return nullptr;
    }

    jbyteArray _input = env.get()->NewByteArray(inputlen);
    env.get()->SetByteArrayRegion(_input, 0, inputlen, (jbyte *) input);
    jbyteArray _output;
    jmethodID mid = NULL;
    if (mode == 0) {
        mid = env.get()->GetStaticMethodID(vsckmsClass, "ckmsencryptKey", "([BI)[B");
    } else if (mode == 1) {
        mid = env.get()->GetStaticMethodID(vsckmsClass, "ckmsdecrypeKey", "([BI)[B");
    }

    _output = (jbyteArray) env.get()->CallStaticObjectMethod(vsckmsClass, mid, _input, inputlen);
    if(NULL == _output) {
        log("ckms exception return null");
        return NULL;
    }
    jbyte *out = env.get()->GetByteArrayElements(_output, JNI_FALSE);
    outputlen = static_cast<uint32_t>(env.get()->GetArrayLength(_output));
    char *temp = (char *) malloc(outputlen);
    if (temp == NULL) {
        return NULL;
    }
    memcpy(temp, out, (size_t) outputlen);

    log("SafeKeyJni ckmsoperatorKey length = %d mode=%d", outputlen,mode);

    env.get()->ReleaseByteArrayElements(_output, out, 0);
    env.get()->DeleteLocalRef(_input);
    env.get()->DeleteLocalRef(_output);
    return temp;
}
