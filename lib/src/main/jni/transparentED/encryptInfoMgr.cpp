//
// Created by zhangsong on 17-12-15.
//

#include "encryptInfoMgr.h"
#include "fileCoder1.h"

EncryptInfo * getEI(int version, EncryptInfo * ei)
{

    if(version == 0x01)
    {
        EncryptInfo_v1 * ei_v1 = 0;
        if(ei) {
            ei_v1 = new EncryptInfo_v1(*(EncryptInfo_v1 *)ei);
        } else {
            ei_v1 = new EncryptInfo_v1();
        }

        return ei_v1;
    }
    else if(version == 0x02)
    {
        EncryptInfo_v2 * ei_v2 = 0;
        if(ei) {
            ei_v2 = new EncryptInfo_v2(*(EncryptInfo_v2 *)ei);
        } else {
            ei_v2 = new EncryptInfo_v2();
        }

        return ei_v2;
    }

    return 0;
}

fileCoder * getFC(int version, EncryptInfo * ei)
{
    if(version == 0x01)
    {
        if(ei == 0)
            return 0;

        EncryptInfo_v1 * ei_v1 = (EncryptInfo_v1*)ei;
        fc1 * fc = new fc1;
        char * key = ei_v1->getKey();
        fc->setKey(key);

        return fc;
    }
    else if(version == 0x02)
    {
        if(ei == 0)
            return 0;

        EncryptInfo_v2 * ei_v2 = (EncryptInfo_v2*)ei;
        fc1 * fc = new fc1;
        char * key = ei_v2->getKey();
        fc->setKey(key);

        return fc;
    }

    return 0;
}