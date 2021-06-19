//
// Created by zhangsong on 17-11-14.
//

#ifndef ZZY_TEST_ENCRYPTFILE_H
#define ZZY_TEST_ENCRYPTFILE_H

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <stdint.h>

#include <map>

class fileCoderManager;

class fileCoder
{
#define DEF_SELF fileCoderManager::addCoder(this->getIdx(), this);
public:
    virtual const char * getName() = 0;
    virtual int getIdx() = 0;
    virtual bool encrypt(char * input, int inputLen, char * output, int & outputLen, uint64_t offset) = 0;
    virtual bool decrypt(char * input, int inputLen, char * output, int & outputLen, uint64_t offset) = 0;
    virtual void setKey(char  *key) = 0;

    fileCoder(){};
    virtual ~fileCoder(){};
};

#pragma pack(push)
#pragma pack(4)

class EncryptInfo
{
public:
    virtual int read(int fd) = 0;
    virtual int write(int fd) = 0;
    virtual int getSize() = 0;

    virtual ~EncryptInfo() {};
};

class EncryptFileHeader
{
#define MAGIC "XDJA_ENCRYPT_FILE"
public:
    char magic[sizeof(MAGIC)];
    int version1;
    int version2;
    EncryptInfo * ei;

    EncryptFileHeader()
    {
        memcpy(magic, MAGIC, strlen(MAGIC));
        version1 = version2 = 0x0;
        ei = 0;
    }

    virtual ~EncryptFileHeader()
    {
        if(ei)
            delete ei;
    }
};
#pragma pack(pop)

enum ef_mode{
    ENCRYPT_WRITE,
    ENCRYPT_READ
};

class EncryptFile {
private:
    char * path;
    ef_mode _mode;
    EncryptFileHeader header;

public:

public:
    ssize_t read(int fd, char * buf, size_t len);
    ssize_t write(int fd, char * buf, size_t len);

    ssize_t pread64(int fd, void* buf, size_t len, off64_t offset);
    ssize_t pwrite64(int fd, void* buf, size_t len, off64_t offset);

    bool create(int fd, ef_mode mode);

    off_t lseek(int fd, off_t pos, int whence);
    int llseek(int fd, unsigned long offset_high,
                           unsigned long offset_low, loff_t *result,
                           unsigned int whence);

    int ftruncate(int fd, off_t length);
    int ftruncate64(int fd, off64_t length);

    int fstat(int fd, struct stat *buf);

    EncryptFile(const char * path);
    EncryptFile(EncryptFile& ef);
    virtual ~EncryptFile();

    static bool isEncryptFile(int fd);

    char * getPath() { return path; }
    int getHeadOffset() { return getHeaderLen() + header.ei->getSize(); }

private:
    fileCoder * fc2;

private:
    static int getHeaderLen();

    bool readHeader(int fd);
    bool writeHeader(int fd);
};

class keyGenerator
{
public:
    static char * generate(size_t);
};

#endif //ZZY_TEST_ENCRYPTFILE_H
