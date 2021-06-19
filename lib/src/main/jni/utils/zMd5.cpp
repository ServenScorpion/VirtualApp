//
// Created by zhangsong on 18-1-27.
//

#include "zMd5.h"

zMd5::zMd5() {
    memset(signature, 0, 16);
}

zMd5::~zMd5() {

}

char* zMd5::getSig(char * buf, int len) {
    if(buf == NULL || len <= 0)
    {

    }
    else {
        MD5Init(&ctx);
        MD5Update(&ctx, (unsigned char *)buf, (unsigned)len);
        MD5Final(signature, &ctx);
    }

    char * tmp = output.getBuf();
    for(int i = 0; i < 16; i++)
    {
        sprintf(tmp + i*2, "%02X", signature[i]);
    }

    return tmp;
}