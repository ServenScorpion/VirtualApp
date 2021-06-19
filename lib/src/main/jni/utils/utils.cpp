//
// Created by zhangsong on 18-1-22.
//

#include <unistd.h>
#include <sys/syscall.h>
#include <linux/fcntl.h>
#include <string>
#include <list>
#include <fcntl.h>
#include <dirent.h>
#include <sys/socket.h>
#include <cstdlib>
#include <string.h>
#include <map>
#include <errno.h>
#include <vector>
#include <set>
#include <netdb.h>
#include <arpa/inet.h>
#include <Foundation/Log.h>

#include "transparentED/originalInterface.h"
#include "utils.h"
#include "mylog.h"
#include "controllerManagerNative.h"
#if defined(__LP64__)
#define my_fcntl __NR_fcntl
#else
#define my_fcntl __NR_fcntl64
#endif

static inline bool startWith(const std::string &str, const std::string &prefix) {
    return str.compare(0, prefix.length(), prefix) == 0;
}


static int my_readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    int ret = static_cast<int>(syscall(__NR_readlinkat, dirfd, pathname, buf, bufsiz));
    return ret;
}

bool getSelfProcessName(zString & name)
{
    int fd = originalInterface::original_openat(AT_FDCWD, "/proc/self/cmdline", O_RDONLY, 0);
    if(!fd)
        return false;

    int len = static_cast<int>(originalInterface::original_read(fd, name.getBuf(),
                                                                static_cast<size_t>(name.getSize())));
    if(len <= 0) {
        originalInterface::original_close(fd);
        return false;
    }

    originalInterface::original_close(fd);

    return true;
}

bool getPathFromFd(int fd, zString & path) {
    zString fd_path("/proc/self/fd/%d", fd);
    int ret = my_readlinkat(AT_FDCWD, fd_path.toString(), path.getBuf(), (size_t )path.getSize());

    if (ret < 0) {
        path.format("readlinkat fail : %s", strerror(errno));
    }

    return ret > 0;
}

static std::vector<std::string> TED_packageVector;

bool configSafePkgName(char const** name, int count) {
    if (name != nullptr) {
        TED_packageVector.clear();
        for(int i = 0; i < count; i++) {
            TED_packageVector.push_back(name[i]);
        }
        return true;
    } else {
        return false;
    }
}

void addEncryptPkgName(const char* name) {
    if (name != nullptr) {
        std::vector<std::string>::iterator iter = std::find(TED_packageVector.begin(), TED_packageVector.end(), name);
        if (iter != TED_packageVector.end()) {
            return;
        }
        TED_packageVector.push_back(name);
    }
}

void delEncryptPkgName(const char* name) {
    if (name != nullptr) {
        std::vector<std::string>::iterator iter = std::find(TED_packageVector.begin(), TED_packageVector.end(), name);
        if (iter != TED_packageVector.end()) {
            TED_packageVector.erase(iter);
        }
    }
}
bool is_TED_Enable()
{
//    return false;
    static int temp_result = -1;
    zString pname;

    if(!getSelfProcessName(pname))
    {
        slog("getSelfProcessName fail !");
        return false;
    }

    if(temp_result == -1)
    {
        temp_result = 0;

        if(TED_packageVector.empty()) {
            slog("%s vector is empty is_TED_Enable false", pname.toString());
            return temp_result == 1;
        }
        for(int i = 0; i < TED_packageVector.size(); i++) {
            if (startWith(std::string(pname.toString()), TED_packageVector[i].data())) {
                temp_result = 1;
                break;
            }
        }

        slog("%s is_TED_Enable %s", pname.toString(), temp_result == 1 ? "true" : "false");
    }

    return temp_result == 1;
}
static bool decryptState = false;
bool changeDecryptState(bool state,int mode){

    if(mode==0){
        decryptState = state;
        return false;
    }else if(mode==1) {
        return decryptState;
    }
}
bool getDecryptState(){
    return is_TED_Enable()||decryptState;
}

const char * FT_packageVector[] =
        {
                /*"com.tencent.mm",
                "cn.wps.moffice",
                "com.android.gallery3d"*/
        };

static bool is_FT_Enable()
{
    static int temp_result = -1;
    zString pname;

    if(!getSelfProcessName(pname))
    {
        slog("getSelfProcessName fail !");
        return false;
    }

    if(temp_result == -1)
    {
        temp_result = 0;

        for(int i = 0; i < sizeof(FT_packageVector)/sizeof(FT_packageVector[0]); i++) {
            if (startWith(std::string(pname.toString()), std::string(FT_packageVector[i]))) {
                temp_result = 1;
                break;
            }
        }

        slog("%s is_FT_Enable %s", pname.toString(), temp_result == 1 ? "true" : "false");
    }

    return temp_result == 1;
}

static bool is_output_FT(const char * path)
{
    const char * paths[] =
            {
                "anon_inode:[eventfd]",
                "anon_inode:sync_fence",
                "/dev/mali0"
            };

    for(int i = 0; i < sizeof(paths) / sizeof(paths[0]); i++)
    {
        bool ret = strcmp(paths[i], path) == 0;

        //slog("is_output_FT %s|%s %s", paths[i], path, ret ? "TRUE" : "FALSE");

        if(ret == true)
            return false;
    }

    return true;
}

void doFileTrace(const char* path, char* operation)
{
    if(is_FT_Enable() && is_output_FT(path))
        slog("%s %s", path, operation);
}

const char* EncryptPathMap[] =
        {
                "/data/data/io.busniess.va/virtual/storage",
                "/data/user/0/io.busniess.va/virtual/storage/emulated",
                "/data/data/com.xdja.safetybox/virtual/storage",
                "/data/user/0/com.xdja.safetybox/virtual/storage/emulated",
                "/data/data/com.xdja.safetysandbox/virtual/storage",
                "/data/user/0/com.xdja.safetysandbox/virtual/storage/emulated",
                "/data/data/com.xdja.safetysandbox.system/virtual/storage",
                "/data/user/0/com.xdja.safetysandbox.system/virtual/storage/emulated",
                "/storage",
                "/mnt"
        };

bool isEncryptPath(const char *_path) {

    bool result = false;
    for(int i = 0; i < sizeof(EncryptPathMap)/sizeof(EncryptPathMap[0]); i++)
    {
        if(startWith(std::string(_path), std::string(EncryptPathMap[i]))) {
            result = true;
            break;
        }
    }

    //slog("%s isEncryptPath %s", _path, result == 1 ? "true" : "false");

    return result;
}

const char * magicPath[] = {
        "/data/user/0/io.busniess.va/files/magic.mgc",
        "/data/user/0/com.xdja.safetybox/files/magic.mgc",
        "/data/user/0/com.xdja.safetysandbox.system/files/magic.mgc",
        "/data/user/0/com.xdja.safetysandbox/files/magic.mgc"
};

const char * getMagicPath()
{
    for(int i = 0; i < sizeof(magicPath) / sizeof(magicPath[0]); i++)
    {
        int fd = originalInterface::original_openat(AT_FDCWD, magicPath[i], O_RDONLY, 0);
        if( fd > 0)
        {
            originalInterface::original_close(fd);

            return magicPath[i];
        }
    }

    slog("magic file not found !");

    return "unknow";
}

void getStrMidle(char* buf,char* inote){
    bool start = false;
    int a = 0;
    for(int i=0; i<30; i++){
        if(buf[i] == '['){
            start = true;
        }else if(buf[i] == ']'){
            inote[a] = '\0';
            break;
        }else{
            if(start == true){
                inote[a] = buf[i];
                a++;
            }

        }
    }
}

bool checkSocketFromTcp(char* path){

    bool ret = false;
    char dest[20]={0};

    getStrMidle(path, dest);
    //LOGE("wxd %s path %s dest %s ", __FUNCTION__, path, dest);
    zString filename("/proc/net/tcp");
    FILE *fp;
    char StrLine[1024];             //每行最大读取的字符数
    if((fp = fopen(filename.toString(), "r")) == NULL) //判断文件是否存在及可读
    {
        printf("error!");
        return ret;
    }

    while (!feof(fp))
    {
        fgets(StrLine, 1024, fp);  //读取一行
        if(strstr(StrLine, dest) != NULL){
            ret = true;
            break;
        }
    }
    fclose(fp);
    return ret;
}


bool checkSocketFromTcp6(char* path){

    bool ret = false;
    char dest[20]={0};

    getStrMidle(path, dest);
    //LOGE("wxd %s path %s dest %s ", __FUNCTION__, path, dest);
    zString filename("/proc/net/tcp6");
    FILE *fp;
    char StrLine[1024];             //每行最大读取的字符数
    if((fp = fopen(filename.toString(), "r")) == NULL) //判断文件是否存在及可读
    {
        printf("error!");
        return ret;
    }

    while (!feof(fp))
    {
        fgets(StrLine, 1024, fp);  //读取一行
        if(strstr(StrLine, dest) != NULL){
            ret = true;
            break;
        }
    }
    fclose(fp);
    return ret;
}

bool closeAllSockets(){
    bool isclose = false;
    int i = 0;
    do{
        zString *path = new zString();
        bool ret = getPathFromFd(i, *path);
        if(ret && strncmp("socket", path->toString(), 6)==0
           && checkSocketFromTcp(path->toString())){
            shutdown(i, SHUT_RDWR);
            int ret = close(i);
            LOGE("lxf %s tcp socket close fd %d ret %d", __FUNCTION__, i, ret);
            isclose = true;
        }
        if(ret && strncmp("socket", path->toString(), 6)==0
           && checkSocketFromTcp6(path->toString())){
            shutdown(i, SHUT_RDWR);
            int ret = close(i);
            LOGE("lxf %s tcp6 socket6 close fd %d ret %d", __FUNCTION__, i, ret);
            isclose = true;
        }
       ++i;
   }while (i<1024);

   return isclose;
}

bool hasAppendFlag(int fd) {
    int val = syscall(my_fcntl, fd, F_GETFL, 0);

    if (val == -1){
        LOGE("fcntl error for F_GETFL");
        return false;
    }

    LOGE("fcntl FD %d, F_GETFL value: %d", fd, val);
    return (val & O_APPEND) == O_APPEND;
}

void delAppendFlag(int fd) {
    int val = syscall(my_fcntl, fd, F_GETFL, 0);

    if (val == -1){
        LOGE("fcntl error for F_GETFL");
        return ;
    }

    val &= ~O_APPEND;

    LOGE("fcntl FD %d, F_SETFL value: %d", fd, val);
    if (syscall(my_fcntl, fd, F_SETFL, val) < 0) {
        LOGE("fcntl error for F_SETFL");
        return ;
    }
}

void addAppendFlag(int fd) {
    int val = syscall(my_fcntl, fd, F_GETFL, 0);

    if (val == -1) {
        LOGE("fcntl error for F_GETFL");
        return;
    }

    val |= O_APPEND;

    LOGE("fcntl FD %d, F_SETFL value: %d", fd, val);
    if (syscall(my_fcntl, fd, F_SETFL, val) < 0) {
        LOGE("fcntl error for F_SETFL");
        return;
    }
}

int getApiLevel() {
    char * api = getenv("V_API_LEVEL");
    return atoi(api);
}

static bool networkStragegyOnorOff;
static bool isWhiteOrBlackFlag;

void configNetworkState(bool netonOroff) {
    networkStragegyOnorOff = netonOroff;
}

bool getNetWorkState() {
    return networkStragegyOnorOff;
}

void configWhiteOrBlack(bool isWhiteOrBlack) {
    isWhiteOrBlackFlag = isWhiteOrBlack;
}

static std::set<std::string> ipstrategyset;
static std::set<std::string> domainstrategyset;
static const int IP_STRATEGY = 1;
static const int DOMAIN_STRATEGY = 2;

bool configNetStrategy(char const **netstrategy, int type, int count) {
    if (netstrategy != nullptr) {
        if (type == IP_STRATEGY) {
            ipstrategyset.clear();
            for (int i = 0; i < count; i++) {
                ipstrategyset.insert(netstrategy[i]);
                ALOGE("add ip strategy: %s",netstrategy[i]);
            }
            return true;
        } else if (type == DOMAIN_STRATEGY) {
            domainstrategyset.clear();
            for (int i = 0; i < count; i++) {
                domainstrategyset.insert(netstrategy[i]);
                ALOGE("add domain strategy: %s",netstrategy[i]);
            }
            return true;
        }
    }
    return false;
}

bool isDomainEnable(const char *domain_node) {
    if (networkStragegyOnorOff) {
        std::string domain = domain_node;
        std::set<std::string>::iterator it_domain;
        if (isWhiteOrBlackFlag) { //handle white domain list
            if (domainstrategyset.empty()) {
                isNetworkControl(domain_node,false);
                return false;
            }
            for (it_domain = domainstrategyset.begin();
                 it_domain != domainstrategyset.end(); it_domain++) {
                if (isContainsStr((*it_domain), "*.")) {
                    std::string str = (*it_domain);
                    str = str.replace(str.find("*"), 1, "");
                    str = str.replace(str.find("."), 1, "");
                    if (isContainsStr(str, domain) || isContainsStr(domain, str)) {
                        isNetworkControl(domain_node,true);
                        return true;
                    }
                } else {
                    if ((*it_domain) == domain) {
                        isNetworkControl(domain_node,true);
                        return true;
                    }
                }
            }
            isNetworkControl(domain_node,false);
            return false;
        } else { // handle black domain list
            if (domainstrategyset.empty()) {
                isNetworkControl(domain_node,true);
                return true;
            }
            for (it_domain = domainstrategyset.begin();
                 it_domain != domainstrategyset.end(); it_domain++) {
                if (isContainsStr((*it_domain), "*")) {
                    std::string str = (*it_domain);
                    str = str.replace(str.find("*"), 1, "");
                    str = str.replace(str.find("."), 1, "");
                    if (isContainsStr(str, domain) || isContainsStr(domain, str)) {
                        isNetworkControl(domain_node,false);
                        return false;
                    }
                } else {
                    if ((*it_domain) == domain) {
                        isNetworkControl(domain_node,false);
                        return false;
                    }
                }
            }
            isNetworkControl(domain_node,true);
            return true;
        }
    }
    return true;
}

void addWhiteIpStrategy(const char* ip) {
    ipstrategyset.insert(ip);
}

bool isIpV4Enable(const char *ipv4) {
    if (networkStragegyOnorOff) {
        std::string ip = ipv4;
        std::set<std::string>::iterator it_ip;

        if (isWhiteOrBlackFlag) {// handle white ipv4 list
            if (ipstrategyset.empty()) {
                isNetworkControl(ipv4, false);
                return false;
            }
            for (it_ip = ipstrategyset.begin(); it_ip != ipstrategyset.end(); it_ip++) {
                if (isContainsStr((*it_ip), "-")) {
                    if (judgeIpSection((*it_ip), ip)) {
                        isNetworkControl(ipv4, true);
                        return true;
                    }
                } else if (isContainsStr((*it_ip), "/")) {
                    if (judgeSubnet((*it_ip), ip)) {
                        isNetworkControl(ipv4, true);
                        return true;
                    }
                } else {
                    if (judgeIpEqual((*it_ip), ip)) {
                        isNetworkControl(ipv4, true);
                        return true;
                    }
                }
            }
            isNetworkControl(ipv4, false);
            return false;
        } else {// handle black ipv4 list
            if (ipstrategyset.empty()) {
                isNetworkControl(ipv4, true);
                return true;
            }
            for (it_ip = ipstrategyset.begin(); it_ip != ipstrategyset.end(); it_ip++) {
                if (isContainsStr((*it_ip), "-")) {
                    if (judgeIpSection((*it_ip), ip)) {
                        isNetworkControl(ipv4, false);
                        return false;
                    }
                } else if (isContainsStr((*it_ip), "/")) {
                    if (judgeSubnet((*it_ip), ip)) {
                        isNetworkControl(ipv4, false);
                        return false;
                    }
                } else {
                    if (judgeIpEqual((*it_ip), ip)) {
                        isNetworkControl(ipv4, false);
                        return false;
                    }
                }
            }
            isNetworkControl(ipv4, true);
            return true;
        }
    }
    return true;
}

bool isIpV6Enable(const char *ipv6) {
    if (networkStragegyOnorOff) {
        std::string ip = ipv6;
        std::set<std::string>::iterator it_ip;

        if (isWhiteOrBlackFlag) {//handle white ipv6 list
            if (ipstrategyset.empty()) {
                isNetworkControl(ipv6,false);
                return false;
            }
            for (it_ip = ipstrategyset.begin(); it_ip != ipstrategyset.end(); it_ip++) {
                if ((*it_ip) == ip) {
                    isNetworkControl(ipv6,true);
                    return true;
                } else {
                    if (isContainsStr(ip, ".")) {//IPV6兼容IPV4格式
                        std::vector<std::string> vector;
                        split(ip, ":", vector);
                        std::string ipv4 = vector.back();
                        if (isContainsStr((*it_ip), "-")) {
                            if (judgeIpSection((*it_ip), ipv4)) {
                                isNetworkControl(ipv6,true);
                                return true;
                            }
                        } else if (isContainsStr((*it_ip), "/")) {
                            if (judgeSubnet((*it_ip), ipv4)) {
                                isNetworkControl(ipv6,true);
                                return true;
                            }
                        } else {
                            if (judgeIpEqual((*it_ip), ipv4)) {
                                isNetworkControl(ipv6,true);
                                return true;
                            }
                        }
                    }
                }
            }
            isNetworkControl(ipv6,false);
            return false;
        } else {//handle black ipv6 list
            if (ipstrategyset.empty()) {
                isNetworkControl(ipv6,true);
                return true;
            }
            for (it_ip = ipstrategyset.begin(); it_ip != ipstrategyset.end(); it_ip++) {
                if ((*it_ip) == ip) {
                    isNetworkControl(ipv6,false);
                    return false;
                } else {
                    if (isContainsStr(ip, ".")) {//IPV6兼容IPV4格式
                        std::vector<std::string> vector;
                        split(ip, ":", vector);
                        std::string ipv4 = vector.back();
                        if (isContainsStr((*it_ip), "-")) {
                            if (judgeIpSection((*it_ip), ipv4)) {
                                isNetworkControl(ipv6,false);
                                return false;
                            }
                        } else if (isContainsStr((*it_ip), "/")) {
                            if (judgeSubnet((*it_ip), ipv4)) {
                                isNetworkControl(ipv6,false);
                                return false;
                            }
                        } else {
                            if (judgeIpEqual((*it_ip), ipv4)) {
                                isNetworkControl(ipv6,false);
                                return false;
                            }
                        }
                    }
                }
            }
            isNetworkControl(ipv6,true);
            return true;
        }
    }
    return true;
}


bool judgeIpEqual(std::string netstrategy_ip,std::string real_ip) {
    return netstrategy_ip == real_ip;
}

bool judgeIpSection(std::string netstrategy_ip,std::string real_ip) {
    int index = netstrategy_ip.find("-");
    std::string beginIp = netstrategy_ip.substr(0,index);
    std::string endIp = netstrategy_ip.substr(index+1);
    return  (getIp2Long(beginIp)<=getIp2Long(real_ip) && getIp2Long(real_ip)<=getIp2Long(endIp));
}

bool judgeSubnet(std::string netstrategy_ip, std::string real_ip) {
    int index = netstrategy_ip.find("/");
    std::string ip1 = netstrategy_ip.substr(0, index);
    std::string subnet = netstrategy_ip.substr(index + 1);
    int subnetInt = ipStrToInt(subnet);
    int a = ipStrToInt(ip1);
    int b = ipStrToInt(real_ip);
    //IP1与IP2属于同一子网络
    if ((a & subnetInt) == (b & subnetInt)) {
        //log("wkw ip1 %s real_ip %s in subnet", ip1.c_str(), real_ip.c_str());
        return true;
    } else { //IP1与IP2不属于同一子网络
        //log("wkw ip1 %s real_ip %s not in subnet", ip1.c_str(), real_ip.c_str());
        return false;
    }
}

int ipStrToInt(std::string ip) {
    int intIp = 0;
    std::vector<std::string> vector;
    split(ip, ".", vector);
    for (int i = 0; i < vector.size(); i++) {
        intIp += atoi(vector[i].c_str()) << (24 - 8 * i);
    }
    return intIp;
}

long long getIp2Long(std::string ip) {
    long long ip2long = 0;
    std::vector<std::string> vector;
    split(ip, ".", vector);
    for (int i = 0; i < vector.size(); i++) {
        ip2long = ip2long << 8 | atoi(vector[i].c_str());
    }
    return ip2long;
}

void split(const std::string &src, const std::string &separator,
           std::vector<std::string> &dest) {
    std::string::size_type pos1, pos2;
    pos1 = 0;
    pos2 = src.find(separator);
    while (std::string::npos != pos2) {
        dest.push_back(src.substr(pos1, pos2 - pos1));
        pos1 = pos2 + separator.size();
        pos2 = src.find(separator, pos1);
    }
    if (pos1 != src.length())
        dest.push_back(src.substr(pos1));
}

bool isContainsStr(std::string str, std::string contains_str) {
    std::string::size_type idx = str.find(contains_str);
    if (idx != std::string::npos) {
        return true;
    } else {
        return false;
    }
}

bool isWhiteList() {
    return isWhiteOrBlackFlag;
}

void isNetworkControl(const char * ipOrdomain, bool isSuccessOrFail) {
    controllerManagerNative::isNetworkControl(ipOrdomain,
                                              isSuccessOrFail);
}

void cofingDomainToIp() {
    if (!domainstrategyset.empty()) {
        //查询域名对应IPV4 添加IPV4名单
        std::set<std::string>::iterator it_domain;
        struct addrinfo hints_ipv4, *result_ipv4, *rp_ipv4;
        int err_ipv4;
        in_addr addr;
        memset(&hints_ipv4, 0, sizeof(addrinfo));
        hints_ipv4.ai_socktype = SOCK_STREAM;
        hints_ipv4.ai_family = AF_INET;
        //log("wkw cofingDomainToIp");
        for (it_domain = domainstrategyset.begin();
             it_domain != domainstrategyset.end(); it_domain++) {
            if (isContainsStr((*it_domain), "*")) {
                std::string str = (*it_domain);
                str = str.replace(str.find("*"), 1, "");
                str = str.replace(str.find("."), 1, "");
                if ((err_ipv4 = originalInterface::original_getaddrinfo(str.c_str(), NULL, &hints_ipv4, &result_ipv4)) == 0) {
                    for (rp_ipv4 = result_ipv4; rp_ipv4 != NULL; rp_ipv4 = rp_ipv4->ai_next) {
                        addr.s_addr = ((sockaddr_in *) (rp_ipv4->ai_addr))->sin_addr.s_addr;
                        addWhiteIpStrategy(inet_ntoa(addr));
                        //log("wkw get addr domain %s ip_v4 %s",(*it_domain).c_str(), inet_ntoa(addr));
                    }
                }
                freeaddrinfo(result_ipv4);
            } else {
                if ((err_ipv4 = originalInterface::original_getaddrinfo((*it_domain).c_str(), NULL, &hints_ipv4,
                                            &result_ipv4)) == 0) {
                    for (rp_ipv4 = result_ipv4; rp_ipv4 != NULL; rp_ipv4 = rp_ipv4->ai_next) {
                        addr.s_addr = ((sockaddr_in *) (rp_ipv4->ai_addr))->sin_addr.s_addr;
                        addWhiteIpStrategy(inet_ntoa(addr));
                        //log("wkw get addr domain %s ip_v4 %s", (*it_domain).c_str(), inet_ntoa(addr));
                    }
                }
                freeaddrinfo(result_ipv4);
            }
        }
        //查询域名对应IPV6 添加IPV6名单
        struct addrinfo hints_ipv6, *result_ipv6, *rp_ipv6;
        int err_ipv6;
        sockaddr_in6 sin6;
        memset(&hints_ipv6, 0, sizeof(addrinfo));
        hints_ipv6.ai_socktype = SOCK_STREAM;
        hints_ipv6.ai_family = AF_INET6;
        for (it_domain = domainstrategyset.begin();
             it_domain != domainstrategyset.end(); it_domain++) {
            if (isContainsStr((*it_domain), "*")) {
                std::string str = (*it_domain);
                str = str.replace(str.find("*"), 1, "");
                str = str.replace(str.find("."), 1, "");
                if ((err_ipv6 = originalInterface::original_getaddrinfo(str.c_str(), NULL, &hints_ipv6, &result_ipv6)) == 0) {
                    for (rp_ipv6 = result_ipv6; rp_ipv6 != NULL; rp_ipv6 = rp_ipv6->ai_next) {
                        memcpy(&sin6, rp_ipv6->ai_addr, sizeof(sin6));
                        char ip_v6[INET6_ADDRSTRLEN];
                        inet_ntop(AF_INET6, &sin6.sin6_addr, ip_v6, sizeof(ip_v6));
                        addWhiteIpStrategy(ip_v6);
                        //log("wkw get addr6 domain %s ip_v6 %s",(*it_domain).c_str(),ip_v6);
                    }
                }
                freeaddrinfo(result_ipv6);
            } else {
                if ((err_ipv6 = originalInterface::original_getaddrinfo((*it_domain).c_str(), NULL, &hints_ipv6,
                                            &result_ipv6)) == 0) {
                    for (rp_ipv6 = result_ipv6; rp_ipv6 != NULL; rp_ipv6 = rp_ipv6->ai_next) {
                        memcpy(&sin6, rp_ipv6->ai_addr, sizeof(sin6));
                        char ip_v6[INET6_ADDRSTRLEN];
                        inet_ntop(AF_INET6, &sin6.sin6_addr, ip_v6, sizeof(ip_v6));
                        addWhiteIpStrategy(ip_v6);
                        //log("wkw get addr6 domain %s ip_v6 %s", (*it_domain).c_str(), ip_v6);
                    }
                }
                freeaddrinfo(result_ipv6);
            }
        }
    }
}

bool isIPAddress(const char *str) {
    int a, b, c, d;
    char temp[100];
    if ((sscanf(str, "%d.%d.%d.%d", &a, &b, &c, &d)) != 4) {
        return false;
    }
    sprintf(temp, "%d.%d.%d.%d", a, b, c, d);
    if (strcmp(temp, str) != 0) {
        return false;
    }
    if (!((a <= 255 && a >= 0) && (b <= 255 && b >= 0) && (c <= 255 && c >= 0))) {
        return false;
    } else {
        return true;
    }
}