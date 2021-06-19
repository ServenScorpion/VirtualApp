//
// Created by zhangsong on 17-11-14.
//

#include <stdio.h>
#include <time.h>
#include <safekey/safekey_jni.h>

#include <stdlib.h>

#include "EncryptFile.h"
#include "originalInterface.h"
#include "encryptInfoMgr.h"
#include "utils/mylog.h"
using namespace xdja;

/********************************************************************/

char * keyGenerator::generate(size_t keylen) {
    char * key = (char *)malloc(keylen);
    memset(key, 0, keylen);
    SafeKeyJni::getRandom(keylen, key);
    /*timespec time;
    clock_gettime(CLOCK_REALTIME, &time);  //获取相对于1970到现在的秒数
    struct tm nowTime;
    localtime_r(&time.tv_sec, &nowTime);

    sprintf(key, "%04d-%02d-%02d %02d:%02d:%02d", nowTime.tm_year + 1900, nowTime.tm_mon+1, nowTime.tm_mday,
            nowTime.tm_hour, nowTime.tm_min, nowTime.tm_sec);*/
    //sprintf(key, "2017120211:19:18");
    return key;
}

/********************************************************************/
/********************************************************************/
bool EncryptFile::isEncryptFile(int fd) {
    char magic[sizeof(MAGIC)];
    off_t cur_pos = originalInterface::original_lseek(fd, 0, SEEK_CUR);
    originalInterface::original_lseek(fd, 0, SEEK_SET);

    bool flag = true;
    do {

        if(originalInterface::original_read(fd, magic, sizeof(MAGIC)) != sizeof(MAGIC))
        {
            //log("EncryptFile::isEncryptFile readHeader fail !");
            flag = false;
            break;
        }

        if (strncmp(magic, MAGIC, strlen(MAGIC)) != 0)
        {
            //log("EncryptFile::isEncryptFile match magic fail !");
            flag = false;
            break;
        }

    }while(false);

    originalInterface::original_lseek(fd, cur_pos, SEEK_SET);   //恢复

    return flag;
}

EncryptFile::EncryptFile(const char * path) {
    this->path = new char[strlen(path) + 1];
    memset(this->path, 0, strlen(path) + 1);

    strcpy(this->path, path);

    fc2 = 0;
}

EncryptFile::EncryptFile(EncryptFile &ef) {
    this->path = new char[strlen(ef.path) + 1];
    memset(this->path, 0, strlen(ef.path) + 1);
    strcpy(path, ef.path);

    memcpy(this->header.magic, ef.header.magic, sizeof(header.magic));
    this->header.version1 = ef.header.version1;
    this->header.version2 = ef.header.version2;

    header.ei = getEI(header.version1, ef.header.ei);
    fc2 = getFC(header.version2, header.ei);
}

EncryptFile::~EncryptFile() {

    if(fc2) {
        delete fc2;
        fc2 = 0;
    }

    if(path) {
        delete []path;
        path = 0;
    }
}

/*

#include <string>
void dump(char * addr, int len)
{
    std::string output = "";
    char tmp[10] = {0};
    for(int i = 0; i < len; i++)
    {
        memset(tmp, 0, 10);
        sprintf(tmp, "%02hhX ", addr[i]);
        output += tmp;

        if((i + 1) % 10 == 0) {
            output += "\n";
            log("%s", (char *)output.c_str());
            output = "";
        }
    }
}
*/

int EncryptFile::getHeaderLen() {
    return
            sizeof(MAGIC) +
            sizeof(EncryptFileHeader::version1) +
            sizeof(EncryptFileHeader::version2);
}

bool EncryptFile::writeHeader(int fd) {

    off_t cur_pos = originalInterface::original_lseek(fd, 0, SEEK_CUR);
    originalInterface::original_lseek(fd, 0, SEEK_SET);

    bool ret = false;
    do{
        if(originalInterface::original_write(fd, &header.magic, sizeof(MAGIC)) != sizeof(MAGIC))
            break;

        if(originalInterface::original_write(fd, &header.version1, sizeof(EncryptFileHeader::version1)) != sizeof(EncryptFileHeader::version1))
            break;

        if(originalInterface::original_write(fd, &header.version2, sizeof(EncryptFileHeader::version2)) != sizeof(EncryptFileHeader::version2))
            break;

        header.ei = getEI(header.version1);
        if(header.ei == 0) {
            log("getEI return 0");
            break;
        }

        if(header.ei->write(fd)) {
            log("EI::write fail");
            break;
        }

        fc2 = getFC(header.version2, header.ei);
        if(fc2 == 0) {
            log("getFC return 0");
            break;
        }

        ret = true;
    }while(false);

    originalInterface::original_lseek(fd, cur_pos, SEEK_SET);   //恢复

    return ret;
}

bool EncryptFile::readHeader(int fd) {

    off_t cur_pos = originalInterface::original_lseek(fd, 0, SEEK_CUR);
    originalInterface::original_lseek(fd, 0, SEEK_SET);

    bool ret = false;
    do{
        if(originalInterface::original_read(fd, &header.magic, sizeof(MAGIC)) != sizeof(MAGIC))
            break;

        if(originalInterface::original_read(fd, &header.version1, sizeof(EncryptFileHeader::version1)) != sizeof(EncryptFileHeader::version1))
            break;

        if(originalInterface::original_read(fd, &header.version2, sizeof(EncryptFileHeader::version2)) != sizeof(EncryptFileHeader::version2))
            break;

        header.ei = getEI(header.version1);
        if(header.ei == 0)
            break;

        if(header.ei->read(fd))                         //读取加密信息
            break;

        fc2 = getFC(header.version2, header.ei);
        if(fc2 == 0)
            break;

        ret = true;
    }while(false);

    originalInterface::original_lseek(fd, cur_pos, SEEK_SET);   //恢复

    return ret;
}

bool EncryptFile::create(int fd, ef_mode mode) {

    _mode = mode;

    if(_mode == ENCRYPT_READ) {
        if (!EncryptFile::isEncryptFile(fd)) {
            log("EncryptFile::isEncryptFile return false !");
            return false;
        }
        if(!readHeader(fd))    //读取文件头
            return false;
    }
    else{
        header.version1 = header.version2 = 0x01;           //设置加密文件版本
//        header.version1 = header.version2 = 0x02;
        if(!writeHeader(fd))
            return false;
    }

    lseek(fd, 0, SEEK_SET);     /*将文件游标设置为0 ———— 透明游标*/

    return true;
}

/*
 * read write原则：
 *  不干预系统返回的FD所维护的文件偏移
 *  文件偏移在lseek/__llseek中维护
 */
ssize_t EncryptFile::read(int fd, char *buf, size_t len) {
    //log("EncryptFile::read [fd %d] [len %zu]", fd, len);

    off_t offset = this->lseek(fd, 0, SEEK_CUR);

    int ret = originalInterface::original_read(fd, buf, len);
    int tmp;
    if(ret > 0 && fc2 != NULL) {
        fc2->decrypt(buf, ret, buf, tmp, offset);
    }
    return ret;
}

ssize_t EncryptFile::write(int fd, char *buf, size_t len) {
    //log("EncryptFile::write [fd %d] [len %zu]", fd, len);

    if(len == 0)
        return 0;

    char * ebuf = new char[len];
    int elen;
    if(len > 0 && fc2 != NULL) {
        fc2->encrypt((char *)buf, len, (char *)ebuf, elen, lseek(fd, 0, SEEK_CUR));
    }
    int ret = originalInterface::original_write(fd, ebuf, len);

    delete []ebuf;

    return ret;
}

ssize_t EncryptFile::pread64(int fd, void *buf, size_t len, off64_t offset) {
    //log("EncryptFile::pread64 [fd %d] [len %zu] [offset %lld]", fd, len, offset);

    int ret = originalInterface::original_pread64(fd, buf, len, offset + (getHeadOffset() & 0x0000ffff)/*透明文件游标*/);
    int tmp;
    if(ret > 0 && fc2 != NULL) {
        fc2->decrypt((char *)buf, ret, (char *)buf, tmp, offset);
    }

    return ret;
}

ssize_t EncryptFile::pwrite64(int fd, void *buf, size_t len, off64_t offset) {
    //log("EncryptFile::pwrite64 [fd %d] [len %zu] [offset %lld]", fd, len, offset);

    if(len == 0)
        return 0;

    char * ebuf = new char[len];
    int elen;
    if(len > 0 && fc2 != NULL) {
        fc2->encrypt((char *)buf, len, (char *)ebuf, elen, offset);
    }
    int ret = originalInterface::original_pwrite64(fd, ebuf, len, offset +  (getHeadOffset() & 0x0000ffff)/*透明文件游标*/);

    delete []ebuf;

    return ret;
}

/*
 *  seek 原则：
 *  1. 设置进来的偏移 需要加上加密头长
 *  2. 返回的偏移 需要减去加密头长
 */
off_t EncryptFile::lseek(int fd, off_t offset, int whence) {
    //log("EncryptFile::lseek [fd %d] [offset %d] [whence %d]", fd, offset, whence);

    off_t ret = 0;
    LOGE("lseek fd : %d, offset %d whence %d", fd, offset, whence);
    switch (whence)
    {
        case SEEK_SET:
        {
            if(offset >= 0) {
                offset += getHeadOffset()/* 加上加密文件头*/;
                ret = originalInterface::original_lseek(fd, offset, SEEK_SET);
                ret -= getHeadOffset()/*减去加密头长度*/;
            }
        }
        break;

        case SEEK_END: /* if ABS(offset) > lenof(file) WRONG !!! */
        {
            ret = originalInterface::original_lseek(fd, offset, SEEK_END);
            ret -= getHeadOffset()/*减去加密头长度*/;
        }
        break;

        case SEEK_CUR:
        {
            ret = originalInterface::original_lseek(fd, offset, SEEK_CUR);
            ret -= getHeadOffset()/*减去加密头长度*/;
        }
        break;

        default:
        {
            log("****************** EncryptFile::lseek UNKNOW WHENCE ********************");
            log("****************** EncryptFile::lseek UNKNOW WHENCE ********************");
            log("****************** EncryptFile::lseek UNKNOW WHENCE ********************");
        }
        break;
    }

    return ret;
}

int EncryptFile::llseek(int fd, unsigned long offset_high, unsigned long offset_low,
                        loff_t *result, unsigned int whence) {
    //log("EncryptFile::llseek [fd %d] [offset %lld] [whence %d]", fd, rel_offset, whence);

    int ret;

    switch (whence)
    {
        case SEEK_SET:
        {
            off64_t offset = 0;
            offset |= offset_high;
            offset <<= 32;
            offset |= offset_low;

            if(offset >= 0) {
                offset += getHeadOffset()/* 加上加密文件头*/;

                unsigned long off_hi = static_cast<unsigned long>(offset >> 32);
                unsigned long off_lo = static_cast<unsigned long>(offset);

                ret = originalInterface::original_llseek(fd, off_hi, off_lo, result, SEEK_SET);
                *result -= (getHeadOffset() & 0x0000ffff)/*减去加密头长度*/;
            }
        }
            break;

        case SEEK_END: /* if ABS(offset) > lenof(file) WRONG !!! */
        {
            ret = originalInterface::original_llseek(fd, offset_high, offset_low, result, SEEK_END);
            *result -= (getHeadOffset() & 0x0000ffff)/*减去加密头长度*/;
        }
            break;

        case SEEK_CUR:
        {
            ret = originalInterface::original_llseek(fd, offset_high, offset_low, result, SEEK_CUR);
            *result -= (getHeadOffset() & 0x0000ffff)/*减去加密头长度*/;
        }
            break;

        default:
        {
            log("****************** EncryptFile::llseek UNKNOW WHENCE ********************");
            log("****************** EncryptFile::llseek UNKNOW WHENCE ********************");
            log("****************** EncryptFile::llseek UNKNOW WHENCE ********************");
        }
            break;
    }

    return ret;
}

/*
 * fstat原则：
 *  文件长度去掉加密头长
 */
int EncryptFile::fstat(int fd, struct stat *buf) {
    //log("EncryptFile::fstat [fd %d]", fd);

    if(buf == NULL)
        return -1;

    int ret = originalInterface::original_fstat(fd, buf);
    if(ret == 0)
    {
        if(buf->st_size >= getHeadOffset()) {
            buf->st_size -= (getHeadOffset() & 0x0000ffff);
        }
        else {
            log("EncryptFile::fstat file size < head offset\n");
            log("EncryptFile::fstat file size < head offset\n");
            log("EncryptFile::fstat file size < head offset\n");
            log("EncryptFile::fstat file size < head offset\n");
        }
    }

    return ret;
}

int EncryptFile::ftruncate(int fd, off_t length) {
    //log("EncryptFile::ftruncate [fd %d] [length %ld", fd, length);

    if(length > 0)
    {
        length += getHeadOffset();
    }

    return originalInterface::original_ftruncate(fd, length);
}

int EncryptFile::ftruncate64(int fd, off64_t length) {
    //log("EncryptFile::ftruncate64 [fd %d] [length %lld", fd, length);

    if(length > 0)
    {
        length += (getHeadOffset()&0x0000ffff);
    }

    struct stat st;
    originalInterface::original_fstat(fd,&st);

    if(length <= st.st_size)
    {
        return originalInterface::original_ftruncate64(fd,length);
    } else {
        size_t ext_length = static_cast<size_t>(length - st.st_size);
        int elen = 0;

        char * buf = new char[ext_length];
        char * ebuf = new char[ext_length];
        memset(buf,0,sizeof(char) * ext_length);
        memset(ebuf,0,sizeof(char) * ext_length);
        if (ext_length > 0)
            fc2->encrypt(buf, ext_length, ebuf, elen, static_cast<uint64_t>(lseek(fd, 0, SEEK_END)));

        int ret = originalInterface::original_write(fd, ebuf, ext_length);

        delete []buf;
        delete []ebuf;
        if (ret > 0) {
            return 0;
        } else {
            return -1;
        }


    }

}
