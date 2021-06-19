//
// Created by zhangsong on 17-11-14.
//

#ifndef ZZY_TEST_ORIGINALINTERFACE_H
#define ZZY_TEST_ORIGINALINTERFACE_H

#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>

class originalInterface
{
public:
    static ssize_t (*original_read)(int fd, void *buf, size_t count);
    static ssize_t (*original_write)(int fd, const void *buf, size_t count);
//    static int (*original_open)(const char*, int, ...);
    static int (*original_close)(int fd);
    static off_t (*original_lseek)(int fd, off_t offset, int whence);
    static int (*original_llseek)(unsigned int fd, unsigned long offset_high,
                unsigned long offset_low, loff_t *result,
                unsigned int whence);
    static int (*original_fstat)(int fd, struct stat *buf);

    static ssize_t (*original_pread64)(int fd, void *buf, size_t count, off64_t offset);
    static ssize_t (*original_pwrite64)(int fd, const void *buf, size_t count, off64_t offset);

    static int (*original_openat)(int dirfd, const char *pathname, int flags, mode_t mode);
    static int (*original_unlinkat)(int dirfd, const char *pathname, int flags);

    static int (*original_ftruncate)(int fd, off_t length);
    static int (*original_ftruncate64)(int fd, off64_t length);

    static ssize_t (*original_sendfile)(int out_fd, int in_fd, off_t* offset, size_t count);
    static int (*original_getaddrinfo)(const char* __node, const char* __service, const struct addrinfo* __hints, struct addrinfo** __result);
};

#endif //ZZY_TEST_ORIGINALINTERFACE_H
