//
// Created by zhangsong on 18-4-26.
//

#ifndef VIRTUALAPP_RELEASER_H
#define VIRTUALAPP_RELEASER_H

#include "Autolock.h"
#include <map>

template <class T> class releaser
{
#define TIME_OUT 60
private:
    std::map<T*, time_t> record;
    Mutex lock;

    void realRelease(int to = TIME_OUT)
    {
        for(typename std::map<T*, time_t>::iterator iter = record.begin(); iter != record.end();)
        {
            if((timeStamp() - iter->second) > to)
            {
                printf("***** releaser::realRelease %p******\n", iter->first);
                iter->first->decStrong(0);
                iter = record.erase(iter);
            } else {
                iter++;
            }
        }
    }

    time_t timeStamp()
    {
        timespec time;
        clock_gettime(CLOCK_REALTIME, &time);

        return time.tv_sec;
    }

public:
    releaser() {
        pthread_mutex_init(&lock, NULL);
    }

    virtual ~releaser(){
        pthread_mutex_destroy(&lock);
    }

    void release(T* a) {
        Autolock al(lock);
        record.insert(std::pair<T*, time_t>(a, timeStamp()));

        realRelease();
    }

    void finish()
    {
        Autolock al(lock);

        realRelease(0);
    }
};

#endif //VIRTUALAPP_RELEASER_H
