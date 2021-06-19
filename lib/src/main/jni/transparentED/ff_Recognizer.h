//
// Created by zhangsong on 17-11-19.
//

#ifndef VIRTUALAPP_FF_RECOGNIZER_H
#define VIRTUALAPP_FF_RECOGNIZER_H


class ff_Recognizer {
public:
    ff_Recognizer();
    virtual ~ff_Recognizer();


    bool init(const char *);
    void uninit();

    const char * getFormat(char * buf, int len);

    bool hit(const char * type);

    static ff_Recognizer & getFFR();

private:
    bool (* p_init)(const char *);
    void (* p_uninit)();
    const char * (* p_getFormat)(char * , int );

    void * handle;
};


#endif //VIRTUALAPP_FF_RECOGNIZER_H
