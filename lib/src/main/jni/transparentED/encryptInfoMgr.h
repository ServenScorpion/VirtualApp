//
// Created by zhangsong on 17-12-15.
//

#ifndef VIRTUALAPP_ENCRYPTINFOMGR_H
#define VIRTUALAPP_ENCRYPTINFOMGR_H

class EncryptInfo;
class fileCoder;

EncryptInfo * getEI(int version, EncryptInfo * ei = 0);
fileCoder * getFC(int version, EncryptInfo * ei);

#endif //VIRTUALAPP_ENCRYPTINFOMGR_H