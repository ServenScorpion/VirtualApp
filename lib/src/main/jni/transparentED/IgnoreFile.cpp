//
// Created by zhangsong on 17-11-30.
//

#include "IgnoreFile.h"
#include "utils/mylog.h"
#include "originalInterface.h"

ssize_t ignoreFile::read(int fd, char *buf, size_t len) {
    //log("ignoreFile::read [fd %d] [len %zu]", fd, len);

    return originalInterface::original_read(fd, buf, len);
}

ssize_t ignoreFile::write(int fd, char *buf, size_t len) {
    //log("ignoreFile::write [fd %d] [len %zu]", fd, len);

    return originalInterface::original_write(fd, buf, len);
}

ssize_t ignoreFile::pread64(int fd, void *buf, size_t len, off64_t offset) {
    //log("ignoreFile::pread64 [fd %d] [len %zu] [offset %lld]", fd, len, offset);

    return originalInterface::original_pread64(fd, buf, len, offset);
}

ssize_t ignoreFile::pwrite64(int fd, void *buf, size_t len, off64_t offset) {
    //log("ignoreFile::pwrite64 [fd %d] [len %zu] [offset %lld]", fd, len, offset);

    return originalInterface::original_pwrite64(fd, buf, len, offset);
}

off_t ignoreFile::lseek(int fd, off_t pos, int whence) {
    //log("ignoreFile::lseek [fd %d] [offset %d] [whence %d]", fd, pos, whence);

    return originalInterface::original_lseek(fd, pos, whence);
}

int ignoreFile::llseek(int fd, unsigned long offset_high, unsigned long offset_low, loff_t *result,
                       unsigned int whence) {

    off64_t rel_offset = 0; rel_offset |= offset_high; rel_offset <<= 32; rel_offset |= offset_low;
    //log("ignoreFile::llseek [fd %d] [offset %lld] [whence %d]", fd, rel_offset, whence);

    return originalInterface::original_llseek(fd, offset_high, offset_low, result, whence);
}

int ignoreFile::fstat(int fd, struct stat *buf) {
    //log("ignoreFile::fstat [fd %d]", fd);

    return originalInterface::original_fstat(fd, buf);
}

int ignoreFile::ftruncate(int fd, off_t length) {
    //log("ignoreFile::ftruncate [fd %d] [length %ld", fd, length);

    return originalInterface::original_ftruncate(fd, length);
}

int ignoreFile::ftruncate64(int fd, off64_t length) {
    //log("ignoreFile::ftruncate64 [fd %d] [length %lld", fd, length);

    return originalInterface::original_ftruncate64(fd, length);
}