//
// Created by zhangsong on 17-11-27.
//

#ifndef VIRTUALAPP_FILECODER1_H
#define VIRTUALAPP_FILECODER1_H

#include "ctr/ctr.h"
#include "EncryptFile.h"
//#include "ctr/caesar_cipher.h"
#include "ctr/sm4_cipher.h"
#include "originalInterface.h"
#include "utils/mylog.h"

#include <string>

using namespace xdja;

class fc1 : public fileCoder
{
#define ENCRYPT_BLOCK_SIZE 16
    //caesar_cipher cc;
    sm4_cipher cc;
    CTR ctr;

public:
    fc1() : ctr(cc)
    {
        ctr.setBlockSize(ENCRYPT_BLOCK_SIZE);
    }

    virtual const char * getName() { return "test file coder1"; }
    virtual int getIdx() { return 1; };

    virtual void setKey(char * key) { ctr.setKey(key); }

    virtual bool encrypt(char * input, int inputLen, char * output, int & outputLen, uint64_t offset){

        /*std::string ob = "";
        for(int i = 0; i < inputLen; i++)
        {
            char tmp[30] = {0};
            sprintf(tmp, "%02hhx ", input[i]);
            ob += tmp;

            if((i+1) % 20 == 0)
                ob += "\n";
        }
        slog("encrypt : inputlen [%d], offset 0x2[%llu] \n input :\n%s \n", inputLen, offset, ob.c_str());*/

        ctr.encrypt(input, output, inputLen, offset);

        /*ob = "";
        for(int i = 0; i < inputLen; i++)
        {
            char tmp[30] = {0};
            sprintf(tmp, "%02hhx ", output[i]);
            ob += tmp;

            if((i+1) % 20 == 0)
                ob += "\n";
        }
        slog("encrypt : outputLen [%d], offset [%llu] \n output :\n%s \n", outputLen, offset, ob.c_str());*/

        /*for(int i = 0; i < inputLen; i++)
          output[i] = input[i] + 3;*/

        outputLen = inputLen;

        return true;
    };
    virtual bool decrypt(char * input, int inputLen, char * output, int & outputLen, uint64_t offset){

        ctr.encrypt(input, output, inputLen, offset);

        /*for(int i = 0; i < inputLen; i++)
          output[i] = input[i] - 3;*/

        outputLen = inputLen;

        return true;
    };
};


class ckmsInfo : public EncryptInfo
{
    uint32_t group_id;

public:
    ckmsInfo() { group_id = 0x1; }

    int read(int fd);
    int write(int fd);
    int getSize();

    bool encrypt(char * input, uint32_t inputlen, char * output, uint32_t & outputlen);
    bool decrypt(char * input, uint32_t inputlen, char * output, uint32_t & outputlen);
};

class EncryptInfo_v1 : public EncryptInfo
{
    char * key;             //明文key
    uint32_t keyLen;

    char * key2;            //秘文key
    uint32_t keyLen2;

    ckmsInfo ci;

public:
    int read(int fd);
    int write(int fd);
    int getSize();

    EncryptInfo_v1();
    EncryptInfo_v1(EncryptInfo_v1 & ei);

    virtual ~EncryptInfo_v1();

    char * getKey();
    int getKeyLen();
};

class ckmsInfo_v2 : public EncryptInfo
{
    uint32_t group_id;
public:
    ckmsInfo_v2() {group_id = 0x1;}
    int read(int fd);
    int write(int fd);
    int getSize();
    char * ckmsEncryptKey(char * input, uint32_t inputlen, uint32_t & outputlen);
    char * ckmsDecryptKey(char * input, uint32_t inputlen, uint32_t & outputlen);
};

class EncryptInfo_v2 : public EncryptInfo
{
    char * key;             //明文key
    uint32_t keyLen;

    char * key2;            //秘文key
    uint32_t keyLen2;

    ckmsInfo_v2 ci;

public:
    int read(int fd);
    int write(int fd);
    int getSize();

    EncryptInfo_v2();
    EncryptInfo_v2(EncryptInfo_v2 &ei);

    virtual ~EncryptInfo_v2();

    char * getKey();
    int getKeyLen();
};

#endif //VIRTUALAPP_FILECODER1_H
