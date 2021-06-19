//
// Created by zhangsong on 18-1-23.
//

#include <cstring>
#include "controllerManagerNative.h"
#include "zJNIEnv.h"

jclass controllerManagerNative::cmn_class = 0;

jmethodID controllerManagerNative::isNetworkEnable_method = 0;
jmethodID controllerManagerNative::isCameraEnable_method = 0;
jmethodID controllerManagerNative::isChangeConnect_method = 0;
jmethodID controllerManagerNative::isGatewayEnable_method = 0;
jmethodID controllerManagerNative::isSoundRecordEnable_method = 0;
jmethodID controllerManagerNative::isIpV4Enable_method = 0;
jmethodID controllerManagerNative::isIpV6Enable_method = 0;
jmethodID controllerManagerNative::isDomainEnable_method = 0;
jmethodID controllerManagerNative::getNetworkState_method = 0;
jmethodID controllerManagerNative::isWhiteList_method = 0;
jmethodID controllerManagerNative::addWhiteIpStrategy_method = 0;
jmethodID controllerManagerNative::isNetworkControl_method = 0;
bool controllerManagerNative::initial() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    bool ret = false;
    do{
        controllerManagerNative::cmn_class = env.get()->FindClass("com/xdja/zs/controllerManager");
        if(controllerManagerNative::cmn_class == NULL)
            break;

        controllerManagerNative::cmn_class = (jclass )env.get()->NewGlobalRef((jobject )controllerManagerNative::cmn_class);

        controllerManagerNative::isNetworkEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isNetworkEnable", "()Z");
        controllerManagerNative::isCameraEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isCameraEnable", "()Z");
        controllerManagerNative::isGatewayEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isGatewayEnable", "()Z");
        controllerManagerNative::isChangeConnect_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isChangeConnect", "(ILjava/lang/String;)Z");
        controllerManagerNative::isSoundRecordEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isSoundRecordEnable", "()Z");
        controllerManagerNative::isIpV4Enable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class,"isIpV4Enable","(Ljava/lang/String;)Z");
        controllerManagerNative::isIpV6Enable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class,"isIpV6Enable","(Ljava/lang/String;)Z");
        controllerManagerNative::isDomainEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class,"isDomainEnable","(Ljava/lang/String;)Z");
        controllerManagerNative::getNetworkState_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class,"getNetworkState","()Z");
        controllerManagerNative::isWhiteList_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class,"isWhiteList","()Z");
        controllerManagerNative::addWhiteIpStrategy_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class,"addWhiteIpStrategy","(Ljava/lang/String;)V");
        controllerManagerNative::isNetworkControl_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class,"isNetworkControl","(Ljava/lang/String;Z)V");
        if (controllerManagerNative::isNetworkEnable_method == NULL
            || controllerManagerNative::isChangeConnect_method == NULL
            || controllerManagerNative::isGatewayEnable_method == NULL
            || controllerManagerNative::isCameraEnable_method == NULL
            || controllerManagerNative::isSoundRecordEnable_method == NULL
            || controllerManagerNative::isIpV4Enable_method == NULL
            || controllerManagerNative::isIpV6Enable_method == NULL
            || controllerManagerNative::isDomainEnable_method == NULL
            || controllerManagerNative::getNetworkState_method == NULL
            || controllerManagerNative::isWhiteList_method == NULL
            || controllerManagerNative::addWhiteIpStrategy_method == NULL
            || controllerManagerNative::isNetworkControl_method == NULL)
            break;

        ret = true;
    }while(false);

    return ret;
}

bool controllerManagerNative::isNetworkEnable() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::isNetworkEnable_method);
}

bool controllerManagerNative::isCameraEnable() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::isCameraEnable_method);
}

bool controllerManagerNative::isGatewayEnable() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::isGatewayEnable_method);
}

bool controllerManagerNative::isChangeConnect(int port, char *ip){
    zJNIEnv env;
    bool ret = false;
    if(env.get() == NULL)
        return false;

    jstring ips = env.get()->NewStringUTF(ip);
    ret = env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::isChangeConnect_method, port, ips);
    env.get()->DeleteLocalRef(ips);
    return ret;
}

bool controllerManagerNative::isSoundRecordEnable() {
    zJNIEnv env;
    if (env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class,
                                              controllerManagerNative::isSoundRecordEnable_method);
}

bool controllerManagerNative::isIpV4Enable(char *ipv4) {
    zJNIEnv env;
    bool ret = false;
    if (env.get() == NULL) {
        return false;
    }
    jstring ipstr = env.get()->NewStringUTF(ipv4);
    ret = env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class,controllerManagerNative::isIpV4Enable_method,ipstr);
    env.get()->DeleteLocalRef(ipstr);
    return ret;
}

bool controllerManagerNative::isIpV6Enable(char *ipv6)  {
    zJNIEnv env;
    bool ret = false;
    if (env.get() == NULL) {
        return false;
    }
    jstring ipv6str = env.get()->NewStringUTF(ipv6);
    ret = env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class,controllerManagerNative::isIpV6Enable_method,ipv6str);
    env.get()->DeleteLocalRef(ipv6str);
    return ret;
}

bool controllerManagerNative::isDomainEnable(char *domain) {
    zJNIEnv env;
    bool ret = false;
    if (env.get() == NULL) {
        return false;
    }
    jstring domain_name = env.get()->NewStringUTF(domain);
    ret = env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class,controllerManagerNative::isDomainEnable_method,domain_name);
    env.get()->DeleteLocalRef(domain_name);
    return ret;
}

bool controllerManagerNative::getNetworkState() {
    zJNIEnv env;
    if(env.get() == NULL) {
        return false;
    }
    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class,controllerManagerNative::getNetworkState_method);
}

bool controllerManagerNative::isWhiteList() {
    zJNIEnv env;
    if(env.get() == NULL) {
        return false;
    }
    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class,controllerManagerNative::isWhiteList_method);
}

void controllerManagerNative::addWhiteIpStrategy(char * ip) {
    zJNIEnv env;
    if(env.get() == NULL) {
        return;
    }
    jstring ipstr = env.get()->NewStringUTF(ip);
    env.get()->CallStaticVoidMethod(controllerManagerNative::cmn_class,controllerManagerNative::addWhiteIpStrategy_method,ipstr);
    env.get()->DeleteLocalRef(ipstr);
}

void
controllerManagerNative::isNetworkControl(const char *ipOrdomain,
                                          bool isSuccessOrFail) {
    zJNIEnv env;
    if(env.get() == NULL) {
        return;
    }
    jstring ip_domain =  env.get()->NewStringUTF(ipOrdomain);
    env.get()->CallStaticVoidMethod(controllerManagerNative::cmn_class,controllerManagerNative::isNetworkControl_method,ip_domain,isSuccessOrFail);
    env.get()->DeleteLocalRef(ip_domain);
}