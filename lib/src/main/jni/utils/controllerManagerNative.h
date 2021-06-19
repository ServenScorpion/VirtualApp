//
// Created by zhangsong on 18-1-23.
//

#ifndef VIRTUALAPP_CONTROLLERMANAGERNATIVE_H
#define VIRTUALAPP_CONTROLLERMANAGERNATIVE_H


#include <jni.h>

class controllerManagerNative {
    static JavaVM * _jvm;
    static jclass cmn_class;
    static jmethodID isNetworkEnable_method;
    static jmethodID isCameraEnable_method;
    static jmethodID isChangeConnect_method;
    static jmethodID isGatewayEnable_method;
    static jmethodID isSoundRecordEnable_method;
    static jmethodID isIpV4Enable_method;
    static jmethodID isIpV6Enable_method;
    static jmethodID isDomainEnable_method;
    static jmethodID getNetworkState_method;
    static jmethodID isWhiteList_method;
    static jmethodID addWhiteIpStrategy_method;
    static jmethodID isNetworkControl_method;
public:
    static bool initial();

public:
    static bool isNetworkEnable();
    static bool isCameraEnable();
    static bool isChangeConnect(int port, char *ip);
    static bool isGatewayEnable();
    static bool isSoundRecordEnable();
    static bool isIpV4Enable(char *ipv4);
    static bool isIpV6Enable(char *ipv6);
    static bool isDomainEnable(char * domain);
    static bool getNetworkState();
    static bool isWhiteList();
    static void addWhiteIpStrategy(char *ip);
    static void isNetworkControl(const char * ipOrdomain,bool isSuccessOrFail);
};


#endif //VIRTUALAPP_CONTROLLERMANAGERNATIVE_H
