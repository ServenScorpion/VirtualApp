//
// Created by zhangsong on 18-1-27.
//

#ifndef VIRTUALAPP_ZMD5_H
#define VIRTUALAPP_ZMD5_H

#include "md5.h"
#include "zString.h"

class zMd5 {
private:
    MD5_CTX ctx;
    unsigned char signature[16];
    zString output;

public:
    zMd5();
    virtual ~zMd5();

    char * getSig(char * buf, int len);
};


#endif //VIRTUALAPP_ZMD5_H
