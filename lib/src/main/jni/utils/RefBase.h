//
// Created by zhangsong on 18-3-5.
//

#ifndef VIRTUALAPP_REFBASE_H
#define VIRTUALAPP_REFBASE_H

#include <cstdint>

namespace xdja
{
namespace zs
{

template <class T> class LightRefBase
{
public:
    inline LightRefBase() : mCount(0) {}

    inline void incStrong(__attribute__((unused)) const void* id) const {
        __sync_fetch_and_add(&mCount, 1);
    }
    inline void decStrong(__attribute__((unused)) const void* id) const {
        if (__sync_fetch_and_sub(&mCount, 1) == 1) {
            printf("LightRefBase self delete %p \n", static_cast<const T*>(this));
            delete static_cast<const T*>(this);
        }
    }

    inline int32_t getStrongCount() const {
        return mCount;
    }

protected:
    inline virtual ~LightRefBase() {}

private:
    mutable volatile int32_t mCount;
};

}
}

#endif //VIRTUALAPP_REFBASE_H
