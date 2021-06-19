//
// Created by zhangsong on 17-11-30.
//

#ifndef VIRTUALAPP_MYLOG_H_H
#define VIRTUALAPP_MYLOG_H_H

#ifndef HOST
#include <android/log.h>
#else
#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/syscall.h>
#endif

namespace xdja {
#ifdef _DEBUG_
#define log(format, ...) __android_log_print(ANDROID_LOG_ERROR, "VFS-INFO-version 0x3", format, ## __VA_ARGS__)
#else
#define log(format, ...)
#endif

#define LOGE log

#ifndef HOST
#define slog(format, ...) __android_log_print(ANDROID_LOG_ERROR, "VFS-FATAL-version 0x3", format, ## __VA_ARGS__)
#define slog_wx(format, ...) __android_log_print(ANDROID_LOG_ERROR, "WX_LOG", format, ## __VA_ARGS__)
#else
#define slog(format, ...) fprintf(stderr, "[tid %ld]"format"\n", syscall(__NR_gettid), ## __VA_ARGS__);
#endif
}

#endif //VIRTUALAPP_MYLOG_H_H
