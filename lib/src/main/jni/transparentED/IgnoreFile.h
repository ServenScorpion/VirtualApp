//
// Created by zhangsong on 17-11-30.
//

#ifndef VIRTUALAPP_IGNOREFILE_H
#define VIRTUALAPP_IGNOREFILE_H

#include <sys/types.h>

class ignoreFile {
public:
    static ssize_t read(int fd, char * buf, size_t len);
    static ssize_t write(int fd, char * buf, size_t len);

    static ssize_t pread64(int fd, void* buf, size_t len, off64_t offset);
    static ssize_t pwrite64(int fd, void* buf, size_t len, off64_t offset);

    static off_t lseek(int fd, off_t pos, int whence);
    static int llseek(int fd, unsigned long offset_high,
               unsigned long offset_low, loff_t *result,
               unsigned int whence);

    static int fstat(int fd, struct stat *buf);

    static int ftruncate(int fd, off_t length);
    static int ftruncate64(int fd, off64_t length);
};


#endif //VIRTUALAPP_IGNOREFILE_H
