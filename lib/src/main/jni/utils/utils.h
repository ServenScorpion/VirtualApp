//
// Created by zhangsong on 18-1-22.
//

#ifndef VIRTUALAPP_UTILS_H
#define VIRTUALAPP_UTILS_H

#include <string>
#include "zString.h"

#define getErr "0"

bool getPathFromFd(int fd, zString & path);
bool is_TED_Enable();
bool changeDecryptState(bool,int);
bool getDecryptState();
void doFileTrace(const char* path, char* operation);
bool isEncryptPath(const char *_path);
const char * getMagicPath();
bool closeAllSockets();
bool hasAppendFlag(int fd);
void delAppendFlag(int fd);
void addAppendFlag(int fd);
int getApiLevel();
bool configSafePkgName(char const ** name, int count);
void addEncryptPkgName(const char* name);
void delEncryptPkgName(const char* name);
bool configNetStrategy(char const ** netstrategy,int type,int count);
void configNetworkState(bool netonOroff);
void configWhiteOrBlack(bool WhiteOrBlack);
bool getNetWorkState();
bool isDomainEnable(const char * domain_node);
bool isContainsStr(std::string str,std::string contains_str);
void addWhiteIpStrategy(const char* ip);
bool isIpV4Enable(const char * ipv4);
bool isIpV6Enable(const char * ipv6);
bool judgeIpEqual(std::string netstrategy_ip,std::string real_ip);
bool judgeIpSection(std::string netstrategy_ip,std::string real_ip);
bool judgeSubnet(std::string netstrategy_ip,std::string real_ip);
long long getIp2Long(std::string ip);
void split(const std::string& src, const std::string& separator, std::vector<std::string>& dest);
int ipStrToInt(std::string ip);
bool isWhiteList();
void isNetworkControl(const char * ipOrdomain,bool isSuccessOrFail);
void cofingDomainToIp();
bool isIPAddress(const char* str);
#endif //VIRTUALAPP_UTILS_H
