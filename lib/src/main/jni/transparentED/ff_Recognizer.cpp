//
// Created by zhangsong on 17-11-19.
//

#include "ff_Recognizer.h"
#include <dlfcn.h>
#include <cstring>

#include "utils/mylog.h"

ff_Recognizer g_ffr;

ff_Recognizer& ff_Recognizer::getFFR() {
    return g_ffr;
}

ff_Recognizer::ff_Recognizer() {
    p_getFormat = 0;
    p_init = 0;
    p_uninit = 0;

    handle = 0;
}

ff_Recognizer::~ff_Recognizer() {

}

bool ff_Recognizer::init(const char * magicPath) {
    handle = ::dlopen("libmyfile.so", RTLD_LAZY);
    if(handle == NULL)
    {
        LOGE(" ** ff_Recognizer::init fail !");
        return false;
    }

    p_init = (bool (*)(const char *))dlsym(handle, "init");
    p_uninit = (void (*)())dlsym(handle, "uninit");
    p_getFormat = (const char *(*)(char *, int))dlsym(handle, "get_type_by_buf");

    if(!p_init || !p_uninit || !p_getFormat)
    {
        LOGE(" ** ff_Recognizer::dlsym fail !");
        return false;
    }

    if(p_init(magicPath) != 0)
    {
        LOGE("** ff_Recognizer::init fail !");
        return false;
    }

    LOGE("** ff_Recognizer::init success !");
    return true;
}

void ff_Recognizer::uninit() {
    p_uninit();
}

const char * ff_Recognizer::getFormat(char *buf, int len) {

    if(buf == 0 || len <= 0)
    {
        return "UNKNOW";
    }

    return p_getFormat(buf, len);
}

const char * hit_item[] =
        {
                "UTF-8 Unicode (with BOM) text, with no line terminators",
                "UTF-8 Unicode (with BOM) text, with CRLF line terminators",
                "UTF-8 Unicode (with BOM) text, with CRLF, CR line terminators",
                "UTF-8 Unicode (with BOM) text, with CR line terminators",
                "Microsoft Office Document",
                "Zip archive data, at least v2.0 to extract",
                "PDF document, version 1.4",
                "JPEG image data, EXIF standard",
                "PNG image data",
                "JPEG image data, JFIF standard 1.01",
                "JPEG image data",
        };

bool ff_Recognizer::hit(const char *type) {
    LOGE("judge : TYPE [%s] ", type);

    if(type == NULL)
        return false;

    for(int i = 0; i < sizeof(hit_item)/sizeof(hit_item[0]); i++)
    {
        bool ret = strncmp(type, hit_item[i], strlen(hit_item[i])) == 0;

        slog("hit : %s -> %s ret %d", type, hit_item[i], ret);

        if(ret)
            return true;
    }

    return false;
}
