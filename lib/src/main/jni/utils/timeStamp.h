//
// Created by zhangsong on 18-1-15.
//

#ifndef VIRTUALAPP_TIMESTAMP_H
#define VIRTUALAPP_TIMESTAMP_H

#include <sys/time.h>

class timeStamp {
private:
    struct timeval tv;

public:
    timeStamp()
    {
        gettimeofday(&tv, 0);
    }

    long getET()
    {
        struct timeval now;
        gettimeofday(&now, 0);

        long begin = tv.tv_sec * 1000 + tv.tv_usec / 1000;
        long end = now.tv_sec * 1000 + now.tv_usec / 1000;

        return end -begin;
    }

    long get() { return tv.tv_sec * 1000 + tv.tv_usec / 1000; }
};


#endif //VIRTUALAPP_TIMESTAMP_H
