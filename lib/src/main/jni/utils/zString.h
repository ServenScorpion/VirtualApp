//
// Created by zhangsong on 18-1-5.
//

#ifndef ZSTRING_H
#define ZSTRING_H


#include <jni.h>
#include <cstdio>
#include <cstring>

class zString {
#define BUF_SIZE 256 * 3
private:
    char *buf;

public:
    zString(const char *format, ...) {
        buf = 0;
        buf = new char[BUF_SIZE];
        memset(buf, 0, BUF_SIZE);

        va_list ap;
        va_start(ap, format);
        vsnprintf(buf, BUF_SIZE, format, ap);
        va_end(ap);
    }

    zString() {
        buf = 0;
        buf = new char[BUF_SIZE];
        memset(buf, 0, BUF_SIZE);
    }

    char *format(const char *format, ...) {
        memset(buf, 0, BUF_SIZE);

        va_list ap;
        va_start(ap, format);
        vsnprintf(buf, BUF_SIZE, format, ap);
        va_end(ap);

        return buf;
    }

    char *getBuf() {
        memset(buf, 0, BUF_SIZE);
        return buf;
    }

    int getSize() { return BUF_SIZE; }

    virtual ~zString() {
        if (buf) {
            delete[]buf;
            buf = 0;
        }
    }

    char *toString() { return buf; }
};


#endif //ZSTRING_H
