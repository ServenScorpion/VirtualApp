//
// Created by zhangsong on 17-11-24.
//

#ifndef VIRTUALAPP_VIRTUALFILESYSTEM_H
#define VIRTUALAPP_VIRTUALFILESYSTEM_H

#include <map>
#include <string>
#include <pthread.h>
#include <utils/RefBase.h>
#include <utils/StrongPointer.h>
#include <utils/atomicVessel.h>
#include <utils/releaser.h>

#include "EncryptFile.h"
#include "TemplateFile.h"
#include "IgnoreFile.h"

/*
 * 虚拟文件描述符
 * 维护文件的状态 —— 文件游标等
 */
enum vfileState
{
    VFS_IGNORE = 0,
    VFS_TESTING = 1,
    VFS_ENCRYPT
};

class virtualFile;
class virtualFileDescribe : public xdja::zs::LightRefBase<virtualFileDescribe>
{
public:
    xdja::zs::sp<virtualFile> *_vf;
    vfileState cur_state;
    int _fd;

    virtualFileDescribe(int fd) : _vf(0), cur_state(VFS_IGNORE), _fd(fd)
    {
    }

    ~virtualFileDescribe() {
        if (_vf != NULL) {
            delete _vf;
            _vf = 0;
        }
    }
};

/*
 * 虚拟文件描述符集
 */
class virtualFileDescribeSet
{
    atomicVessel items[1024];
    atomicVessel flagItems[1024];
    releaser<virtualFileDescribe> rl;

public:
    void reset(int idx);
    void set(int idx, virtualFileDescribe * vfd);
    virtualFileDescribe * get(int idx);

    void release(virtualFileDescribe * vfd) { rl.release(vfd); }
    void setFlag(int idx, int flag);
    uint32_t getFlag(int idx);
    void clearFlag(int idx);

    virtualFileDescribeSet()
    {}
    virtual ~virtualFileDescribeSet() {
        rl.finish();
    }

    void dumpItems() {
        log("dumpItems items = %p", items);
        for (int i = 0; i < 1024; i++)
        {
            log("dumpItems item[%d] = %x", i, items[i].get());
        }

        int * p = (int *)items;
        for(int i = 0; i < 100; i++)
        {
            log("dumpItems address %x", *(p--));
        }
    }

    static virtualFileDescribeSet & getVFDSet();
};

/*
 *
 */

class virtualFileManager;
class TemplateFile;                  //占位
class virtualFile : public xdja::zs::LightRefBase<virtualFile>
{
private:
    char * _path;

    unsigned int refrence;                   //文件引用计数

    vfileState       _vfs;                //文件状态

    pthread_rwlock_t _rw_lock;
    EncryptFile * ef;                       //操作加密文件的对象
    TemplateFile * tf;                      //操作临时文件的对象

public:
    virtualFile(char * path)
    {
        _path = new char[strlen(path) + 1];
        memset(_path, 0, strlen(path) + 1);
        strcpy(_path, path);

        pthread_rwlock_init(&_rw_lock, NULL);

        ef = 0;
        tf = 0;

        refrence = 0;
    }

    ~virtualFile()
    {
        if(tf != NULL)
        {
            //delete tf;
            delete tf;
            tf = 0;
        }

        if(ef != NULL)
        {
            delete ef;
            ef = 0;
        }

        if(_path) {
            delete[]_path;
            _path = 0;
        }
        pthread_rwlock_destroy(&_rw_lock);

    }

    unsigned int addRef();
    unsigned int delRef();

    void setVFS(vfileState vfs);
    vfileState getVFS();

    void lockWhole();
    void unlockWhole();

public:
    int vpread64(virtualFileDescribe* pvfd, char * buf, size_t len, off64_t from);
    int vpwrite64(virtualFileDescribe* pvfd, char * buf, size_t len, off64_t from);

    int vread(virtualFileDescribe* pvfd, char * buf, size_t len);
    int vwrite(virtualFileDescribe* pvfd, char * buf, size_t len);

    int vfstat(virtualFileDescribe* pvfd, struct stat * buf);
    off_t vlseek(virtualFileDescribe* pvfd, off_t offset, int whence);
    int vllseek(virtualFileDescribe* pvfd, unsigned long offset_high,
               unsigned long offset_low, loff_t *result,
               unsigned int whence);

    int vftruncate(virtualFileDescribe* pvfd, off_t length);
    int vftruncate64(virtualFileDescribe* pvfd, off64_t length);

    char * getPath() {return _path;}
    void setPath(char * path)
    {
        if(_path)
        {
            delete []_path;
            _path = NULL;
        }

        if(_path == NULL)
        {
            _path = new char[strlen(path) + 1];
            memset(_path, 0, strlen(path) + 1);
        }
        strncpy(_path, path, strlen(path) + 1);
    }

    bool create(virtualFileDescribe* pvfd);


    int vclose(virtualFileDescribe* pvfd);

    void forceTranslate();

    int getHeaderOffSet();

private:

    friend class virtualFileManager;
};

/*
 * 虚拟文件管理器
 * 已文件绝对路径来管理文件
 * 每个linux file对应一个virtualFile
 */


typedef std::map<std::string, xdja::zs::sp<virtualFile>*> VFMap;

class virtualFileManager
{
private:
    pthread_mutex_t _lock;
    VFMap _vfmap;

public:
    virtualFileManager()
    {
        pthread_mutex_init(&_lock, NULL);
    }

    ~virtualFileManager()
    {
        pthread_mutex_destroy(&_lock);
    }

    virtualFile * getVF(virtualFileDescribe* pvfd, char * path, int * pErrno);

    xdja::zs::sp<virtualFile> * queryVF(char *path);
    void updateVF(virtualFile & vf);

    void releaseVF(char *path, virtualFileDescribe* pvfd);

    /*void forceClean(char * path);*/

    void deleted(char *path);

public:
    static virtualFileManager & getVFM();
};

#endif //VIRTUALAPP_VIRTUALFILESYSTEM_H
