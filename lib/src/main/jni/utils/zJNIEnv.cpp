//
// Created by zhangsong on 18-1-23.
//

#include "zJNIEnv.h"

_JavaVM* zJNIEnv::_jvm = 0;

void zJNIEnv::initial(_JavaVM *vm) {
    _jvm = vm;
}

zJNIEnv::zJNIEnv() {
    isAttached = false;

    _jniEnv = 0;
    if(_jvm == 0)
      return ;

    int status = 0;
    _jniEnv = 0;

    status = zJNIEnv::_jvm->GetEnv((void **)&_jniEnv, JNI_VERSION_1_6);
    if(status < 0)
    {
        _jniEnv = NULL;
        if(status == JNI_EDETACHED) {
            status = zJNIEnv::_jvm->AttachCurrentThread(&_jniEnv, NULL);
            if (status < 0) {
                _jniEnv = NULL;
            } else {
                isAttached = true;
            }
        }
    }
}

zJNIEnv::~zJNIEnv() {
    if(isAttached && _jniEnv)
        zJNIEnv::_jvm->DetachCurrentThread();
}
