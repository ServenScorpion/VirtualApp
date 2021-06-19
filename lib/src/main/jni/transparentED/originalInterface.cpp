//
// Created by zhangsong on 17-11-14.
//
#include "originalInterface.h"
#include <asm/unistd.h>

ssize_t my_write(int fd, const void* buf, size_t count)
{
    return syscall(__NR_write, fd, buf, count);
}

ssize_t my_read(int fd, void *buf, size_t count)
{
    return syscall(__NR_read, fd, buf, count);
}

int my_openat(int dirfd, const char *pathname, int flags, mode_t mode)
{
    return syscall(__NR_openat, dirfd, pathname, flags, mode);
}

int my_close(int fd)
{
    return syscall(__NR_close, fd);
}

int my_unlinkat(int dirfd, const char *pathname, int flags)
{
    return syscall(__NR_unlinkat, dirfd, pathname, flags);
}

int (*originalInterface::original_close)(int ) = my_close;
int (*originalInterface::original_openat)(int dirfd, const char *pathname, int flags, mode_t mode) = my_openat;
ssize_t (*originalInterface::original_read)(int, void *, size_t) = my_read;
ssize_t (*originalInterface::original_write)(int , const void *, size_t ) = my_write;
int (*originalInterface::original_unlinkat)(int, const char *, int) = my_unlinkat;
off_t (*originalInterface::original_lseek)(int , off_t , int ) = 0;
int (*originalInterface::original_llseek)(unsigned int fd, unsigned long offset_high,
                                          unsigned long offset_low, loff_t *result,
                                          unsigned int whence) = 0;
int (*originalInterface::original_fstat)(int , struct stat *) = 0;
ssize_t (*originalInterface::original_pread64)(int fd, void *buf, size_t count, off64_t offset) = 0;
ssize_t (*originalInterface::original_pwrite64)(int fd, const void *buf, size_t count, off64_t offset) = 0;
int (*originalInterface::original_ftruncate)(int, off_t) = 0;
int (*originalInterface::original_ftruncate64)(int, off64_t) = 0;
ssize_t (*originalInterface::original_sendfile)(int out_fd, int in_fd, off_t* offset, size_t count) = 0;
int (*originalInterface::original_getaddrinfo)(const char* __node, const char* __service, const struct addrinfo* __hints, struct addrinfo** __result) = 0;
