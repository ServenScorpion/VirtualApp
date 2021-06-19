#ifndef AUTOLOCK_H

#define AUTOLOCK_H

#include <pthread.h>
#include "mylog.h"
typedef pthread_mutex_t Mutex;

class Autolock {
    public:
        inline Autolock(Mutex& mutex, char * func = 0, int line = 0) : mLock(mutex),mFunc(func),mLine(line)  {
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("trylock %p at %s：%d", &mLock, mFunc, mLine);
#endif
			pthread_mutex_lock(&mLock);
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("locked %p at %s：%d", &mLock, mFunc, mLine);
#endif
		}

        inline ~Autolock() {
			pthread_mutex_unlock(&mLock);
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("unlock %p at %s：%d", &mLock, mFunc, mLine);
#endif
		}
    private:
        Mutex& mLock;
        char * mFunc;
        int mLine;
};

class AutoRLock {
    typedef pthread_rwlock_t RWLock;
	public:
		inline AutoRLock(RWLock& rwlock, char * func = 0, int line = 0) : mLock(rwlock),mFunc(func),mLine(line)  {
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("trylock %p at %s：%d", &mLock, mFunc, mLine);
#endif
            pthread_rwlock_rdlock(&mLock);
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("locked %p at %s：%d", &mLock, mFunc, mLine);
#endif
        }
		inline ~AutoRLock() {
            pthread_rwlock_unlock(&mLock);
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("unlock %p at %s：%d", &mLock, mFunc, mLine);
#endif
        }
	private:
		RWLock& mLock;
        char * mFunc;
        int mLine;
};

class AutoWLock {
    typedef pthread_rwlock_t RWLock;
	public:
		inline AutoWLock(RWLock& rwlock, char * func = 0, int line = 0) : mLock(rwlock),mFunc(func),mLine(line)  {
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("trylock %p at %s：%d", &mLock, mFunc, mLine);
#endif
            pthread_rwlock_wrlock(&mLock);
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("locked %p at %s：%d", &mLock, mFunc, mLine);
#endif
        }
		inline ~AutoWLock() {
            pthread_rwlock_unlock(&mLock);
#ifdef _LOCK_DEBUG_
            if(mFunc) slog("unlock %p at %s：%d", &mLock, mFunc, mLine);
#endif
        }
	private:
		RWLock& mLock;
        char * mFunc;
        int mLine;
};

class ManualWLock {
	typedef pthread_rwlock_t RWLock;
public:
	static inline void lock(RWLock& mLock) { pthread_rwlock_wrlock(&mLock); }
	static inline void unlock(RWLock& mLock) { pthread_rwlock_unlock(&mLock); }
};

#endif /* end of include guard: AUTOLOCK_H */
