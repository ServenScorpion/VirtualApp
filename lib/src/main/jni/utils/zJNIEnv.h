//
// Created by zhangsong on 18-1-23.
//

#ifndef VIRTUALAPP_ZJNIENV_H
#define VIRTUALAPP_ZJNIENV_H


#include <jni.h>

class zJNIEnv {
private:
    static _JavaVM * _jvm;
public:
    static void initial(_JavaVM *vm);

private:
    bool isAttached;
    JNIEnv* _jniEnv;

public:
    zJNIEnv();
    virtual ~zJNIEnv();

    JNIEnv* get() { return _jniEnv; }
};


#endif //VIRTUALAPP_ZJNIENV_H
