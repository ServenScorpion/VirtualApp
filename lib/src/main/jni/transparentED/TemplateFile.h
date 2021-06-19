//
// Created by zhangsong on 17-11-30.
//

#ifndef VIRTUALAPP_TEMPLATEFILE_H
#define VIRTUALAPP_TEMPLATEFILE_H

#include <utils/zString.h>
#include "EncryptFile.h"

#define CHECK_BUF_SIZE 100

class TemplateFile {
private:
    EncryptFile* _ef_bk;      //同步写的备份文件
    int _ef_fd;
    char * _path;

public:
    ssize_t read(int fd, char * buf, size_t len);
    ssize_t write(int fd, char * buf, size_t len);

    ssize_t pread64(int fd, void* buf, size_t len, off64_t offset);
    ssize_t pwrite64(int fd, void* buf, size_t len, off64_t offset);

    off_t lseek(int fd, off_t pos, int whence);
    int llseek(int fd, unsigned long offset_high,
               unsigned long offset_low, loff_t *result,
               unsigned int whence);

    int ftruncate(int fd, off_t length);
    int ftruncate64(int fd, off64_t length);

    int fstat(int fd, struct stat *buf);

    bool create(const char * path);
    void close(bool checkWhenClose, int fd);
    void forceTranslate();

    TemplateFile ();
    virtual ~TemplateFile();

private:
    bool flag_for_check[CHECK_BUF_SIZE];
    char buf_for_check[CHECK_BUF_SIZE];

    int createTempFile(char * path, zString & tpath);

public:
    bool canCheck();
    bool doControl(int len = CHECK_BUF_SIZE);

    bool translate(int fd);

    EncryptFile* getBK() { return _ef_bk; }
};


#endif //VIRTUALAPP_TEMPLATEFILE_H
