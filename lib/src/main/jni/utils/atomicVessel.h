//
// Created by zhangsong on 18-4-5.
//

#ifndef VIRTUALAPP_ATOMICVESSEL_H
#define VIRTUALAPP_ATOMICVESSEL_H

/*
 * 容纳一个指针
 * 多该指针的操作 —— 设置、清除、获取，为原子操作
 */
class atomicVessel
{
private:
    mutable volatile uint64_t thing;

public:
    atomicVessel() : thing(0) {}
    ~atomicVessel() {}

    void set(uint64_t val) {
        __sync_fetch_and_or(&thing, val);
    }

    void reset() {
        __sync_fetch_and_and(&thing, 0);
    }

    uint64_t get() {
        return __sync_fetch_and_or(&thing, 0);
    }
};

#endif //VIRTUALAPP_ATOMICVESSEL_H
