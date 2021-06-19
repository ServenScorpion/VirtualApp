//
// Created by zhangsong on 17-11-24.
//

#include <cassert>
#include "virtualFileSystem.h"
#include "originalInterface.h"
#include "utils/Autolock.h"
#include "utils/mylog.h"

virtualFileDescribeSet g_VFDS;
virtualFileDescribeSet& virtualFileDescribeSet::getVFDSet() {
    return g_VFDS;
}

void virtualFileDescribeSet::reset(int idx) {
    if (idx < 0 || idx > 1023) {
        return;
    }

    items[idx].reset();
}
void virtualFileDescribeSet::set(int idx, virtualFileDescribe *vfd) {
    if (idx < 0 || idx > 1023) {
        return;
    }

    items[idx].set(reinterpret_cast<uint64_t>(vfd));
}

virtualFileDescribe* virtualFileDescribeSet::get(int idx) {
    if (idx < 0 || idx > 1023) {
        return 0;
    }

    virtualFileDescribe * vfd = reinterpret_cast<virtualFileDescribe *>(items[idx].get());

    return vfd;
}

void virtualFileDescribeSet::setFlag(int idx, int flag) {
    if (idx < 0 || idx > 1023) {
        return;
    }

    flagItems[idx].set(static_cast<uint64_t>(flag));
}

uint32_t virtualFileDescribeSet::getFlag(int idx) {
    if (idx < 0 || idx > 1023) {
        return 0;
    }

    return static_cast<uint32_t>(flagItems[idx].get());
}

void virtualFileDescribeSet::clearFlag(int idx) {
    if (idx < 0 || idx > 1023) {
        return;
    }

    flagItems[idx].reset();
}
////////////////////////////////////////////////////////////////////////////////////////////////////
virtualFileManager g_VFM;
/*
void virtualFileManager::forceClean(char * path) {
    virtualFileDescribeSet &vfds = virtualFileDescribeSet::getVFDSet();

    Autolock at_lock(_lock);
    VFMap::iterator iter = _vfmap.find(std::string(path));
    if(iter != _vfmap.end())
    {
        virtualFile * vf = iter->second;
        for(int i = 0; i < 1024; i++)
        {
            virtualFileDescribe * vfd = vfds.get(i);
            if(vfd != 0 && vfd->_vf == vf)
            {
                delete vfd;
                vfds.set(i, 0);
            }
        }
        {
            log("judge : force clean [path %s]", vf->getPath());
            delete vf;
            _vfmap.erase(iter);
        }
    }
}
*/
void virtualFileManager::deleted(char *path) {
    Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);
    VFMap::iterator iter = _vfmap.find(std::string(path));
    if(iter != _vfmap.end())
    {
        xdja::zs::sp<virtualFile> * vf = iter->second;
        {
            xdja::zs::sp<virtualFile> pvf(vf->get());
            log("judge : [path %s] deleted", pvf->getPath());

            int len = strlen(path) + 20;

            char *tmp = new char[len];
            memset(tmp, 0, len);

            snprintf(tmp, len, "%s deleted", pvf->getPath());
            pvf->setPath(tmp);

            _vfmap.erase(iter);     //删掉原来的节点
            _vfmap.insert(std::pair<std::string, xdja::zs::sp<virtualFile> *>(std::string(tmp), vf)); //以新文件名从新插入

            delete []tmp;
        }
    }
}

virtualFileManager & virtualFileManager::getVFM() {
    return g_VFM;
}

xdja::zs::sp<virtualFile>* virtualFileManager::queryVF(char *path) {
    xdja::zs::sp<virtualFile> *vf = 0;

    Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);
    do {
        VFMap::iterator iterator = _vfmap.find(std::string(path));
        if(iterator != _vfmap.end()) {
            vf = iterator->second;
            if (vf == NULL) {
                return NULL;
            }
            vf->get()->addRef();

            LOGE("judge : query virtualFile ");

            break;
        }
    }while(false);

    return vf;
}

void virtualFileManager::updateVF(virtualFile &vf) {

    vfileState vfs = VFS_IGNORE;
    xdja::zs::sp<virtualFile> pvf(&vf);
    do {
        int fd = originalInterface::original_openat(AT_FDCWD, pvf->getPath(), O_RDONLY, 0);
        if(fd <= 0)
        {
            slog("judge : updateVF openat [%s] fail", pvf->getPath());
            break;
        }

        struct stat sb;
        originalInterface::original_fstat(fd, &sb);

        if (!S_ISREG(sb.st_mode)) {
            //LOGE("judge : S_ISREG return false");
            break;      //不处理
        }

        if (sb.st_size == 0) {
            //设置为 ‘待判断’
            vfs = VFS_TESTING;

            slog("judge : updateVF file size = 0");
        } else if (sb.st_size > 0) {
            //是加密文件 是遏制为 ‘处理’
            //不是加密文件 不管

            if (EncryptFile::isEncryptFile(fd)) {
                vfs = VFS_ENCRYPT;

                slog("judge : updateVF find Encrypt File ");
            } else {
                slog("judge : updateVF not EF ignore");
            }

        }

        virtualFileDescribe * vfd = new virtualFileDescribe(fd);
        pvf->setVFS(vfs);
        if(!pvf->create(vfd))
        {
            slog("judge :  **** updateVF  [%s] fail **** ", pvf->getPath());
            slog("judge :  **** updateVF  [%s] fail **** ", pvf->getPath());
            slog("judge :  **** updateVF  [%s] fail **** ", pvf->getPath());

            pvf->setVFS(VFS_IGNORE);
        }
        delete vfd;
        originalInterface::original_close(fd);
    } while (false);
}

virtualFile* virtualFileManager::getVF(virtualFileDescribe* pvfd, char *path, int * pErrno) {

    xdja::zs::sp<virtualFile> *vf = 0;
    vfileState vfs = VFS_IGNORE;
    *pErrno = 0;                                //默认无错误发生

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    do {
        vf = queryVF(path);
        if (vf != NULL) {
            vfd->_vf = new xdja::zs::sp<virtualFile>(vf->get());
            vfd->cur_state = vf->get()->getVFS();  //记录最初的状态
            break;
        }

        do {
            /*if (strncmp(path, "/data", 5) != 0
                && strncmp(path, "/sdcard", 7) != 0
                && strncmp(path, "/storage", 8) != 0
                    ) {
                break;
            }*/
            struct stat sb;
            originalInterface::original_fstat(vfd->_fd, &sb);

            if (!S_ISREG(sb.st_mode)) {
                //LOGE("judge : S_ISREG return false");
                break;      //不处理
            }

            if (sb.st_size == 0) {
                //设置为 ‘待判断’
                vfs = VFS_TESTING;

                LOGE("judge : file size = 0");
            } else if (sb.st_size > 0) {
                //是加密文件 是遏制为 ‘处理’
                //不是加密文件 不管

                if (EncryptFile::isEncryptFile(vfd->_fd)) {
                    vfs = VFS_ENCRYPT;

                    LOGE("judge : find Encrypt File ");
                } else {
                    LOGE("judge : not EF ignore");
                }

            }

            if(vfs != VFS_IGNORE)
            {
                {
                    virtualFile *_vf = new virtualFile(path);
                    _vf->addRef();
                    _vf->setVFS(vfs);

                    if(!_vf->create(vfd.get()))
                    {
                        delete _vf;
                        _vf = 0;

                        *pErrno = -1;

                        LOGE("************* virtualFile::create fail ! *************");
                        LOGE("************* virtualFile::create fail ! *************");
                        LOGE("************* virtualFile::create fail ! *************");
                        LOGE("************* virtualFile::create fail ! *************");
                    }
                    else {
                        LOGE("judge : create virtualFile [%s]", _vf->getPath());

                        xdja::zs::sp<virtualFile> *pvf = new xdja::zs::sp<virtualFile>(_vf);

                        Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);
                        _vfmap.insert(std::pair<std::string, xdja::zs::sp<virtualFile> *>(std::string(path), pvf));

                        vfd->_vf = new xdja::zs::sp<virtualFile>(pvf->get());
                        vfd->cur_state = pvf->get()->getVFS();  //记录最初的状态
                    }
                }
            }

        } while (false);

    }while(false);

    return (vfd->_vf != NULL) ? vfd->_vf->get() : NULL;
}

void virtualFileManager::releaseVF(char *path, virtualFileDescribe* pvfd) {
    Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    VFMap::iterator iter = _vfmap.find(std::string(path));
    if(iter != _vfmap.end()) {
        xdja::zs::sp<virtualFile> *vf = iter->second;
        if (vf != NULL) {
            if (vf->get()->delRef() == 0) {
//            struct stat buf;
//            buf.st_size = 0;
//
//            int fd = originalInterface::original_openat(AT_FDCWD, vf->getPath(), O_RDONLY, 0);
//            if(fd > 0) {
//                originalInterface::original_fstat(fd, &buf);
//                originalInterface::original_close(fd);
//            }
//            log("judge : file [path %s] [size %lld] real closed", vf->getPath(), buf.st_size);
                vf->get()->vclose(vfd.get());
                delete vf;
                _vfmap.erase(iter);
            }
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
void virtualFile::lockWhole() {
    ManualWLock::lock(_rw_lock);
}

void virtualFile::unlockWhole() {
    ManualWLock::unlock(_rw_lock);
}

unsigned int virtualFile::addRef() {
    __sync_add_and_fetch(&refrence, 1);

//    log("virtualFile::addRef [refrence %u][%s]", refrence, _path);

    return refrence;
}

unsigned int virtualFile::delRef() {
    if(refrence > 0)
        __sync_sub_and_fetch(&refrence, 1);

//    log("virtualFile::delRef [refrence %u] [%s]", refrence, _path);

    return refrence;
}

vfileState virtualFile::getVFS() {
    return _vfs;
}

void virtualFile::setVFS(vfileState vfs) {
    __sync_lock_test_and_set(&_vfs, vfs);
}

bool virtualFile::create(virtualFileDescribe* pvfd) {
    vfileState vfs = getVFS();

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT) {
        if(ef != NULL) {
            delete ef;
            ef = NULL;
        }
        if(ef == NULL)
        {
            ef = new EncryptFile(_path);
            bool ret = ef->create(vfd->_fd, ENCRYPT_READ);
            if(!ret) {
                delete ef;
                ef = 0;
            }
            return ret;
        }
    } else if(vfs == VFS_TESTING) {
        if(tf != NULL)
        {
            delete tf;
            tf = NULL;
        }
        if(tf == NULL)
        {
            tf = new TemplateFile();
            bool ret = tf->create(_path);
            if(!ret)
            {
                delete tf;
                tf = 0;
            }

            return ret;
        }
    } else if (vfs == VFS_IGNORE) {

    } else {
        slog("virtualFile::create vfs UNKNOW");
        slog("virtualFile::create vfs UNKNOW");
        slog("virtualFile::create vfs UNKNOW");
    }

    return false;
}

int virtualFile::vclose(virtualFileDescribe* pvfd) {
    /*
     * 如果VFS == VFS_TESTING
     * 那么这里需要做最后一次检查
     */
    vfileState vfs = getVFS();

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT) {
    } else if(vfs == VFS_TESTING) {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        if(tf != NULL)
        {
            tf->close(true, vfd->_fd);
        }
    } else {
    }

    return 0;
}

void virtualFile::forceTranslate() {
    vfileState vfs = getVFS();
    if(vfs == VFS_TESTING) {
        if(tf != NULL)
        {
            tf->forceTranslate();
        }
    }
}

int virtualFile::vpread64(virtualFileDescribe* pvfd, char * buf, size_t len, off64_t from) {
    vfileState vfs = getVFS();

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->pread64(vfd->_fd, buf, len, from);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::pread64(vfd->_fd, buf, len, from);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::pread64(vfd->_fd, buf, len, from);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->pread64(vfd->_fd, buf, len, from);
            }
            case VFS_TESTING:
                return tf->pread64(vfd->_fd, buf, len, from);
        }
    }

    return 0;
}

int virtualFile::vpwrite64(virtualFileDescribe* pvfd, char * buf, size_t len, off64_t offset) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->pwrite64(vfd->_fd, buf, len, offset);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::pwrite64(vfd->_fd, buf, len, offset);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::pwrite64(vfd->_fd, buf, len, offset);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->pwrite64(vfd->_fd, buf, len, offset);
            }
            case VFS_TESTING: {
                int ret =  tf->pwrite64(vfd->_fd, buf, len, offset);

                if(tf->canCheck())
                {
                    if(tf->doControl())
                    {
                        tf->translate(vfd->_fd);

                        if(ef == NULL) {
                            ef = new EncryptFile(*tf->getBK());
                        }
                        setVFS(VFS_ENCRYPT);
                    }
                    else {
                        setVFS(VFS_IGNORE);
                    }

                    tf->close(false, vfd->_fd);
                    delete tf;
                    tf = 0;
                }

                return ret;
            }
        }
    }

    return 0;
}

int virtualFile::vread(virtualFileDescribe* pvfd, char * buf, size_t len) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->read(vfd->_fd, buf, len);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::read(vfd->_fd, buf, len);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::read(vfd->_fd, buf, len);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->read(vfd->_fd, buf, len);
            }
            case VFS_TESTING:
                return tf->read(vfd->_fd, buf, len);
        }
    }

    return 0;
}

int virtualFile::vwrite(virtualFileDescribe* pvfd, char * buf, size_t len) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->write(vfd->_fd, buf, len);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::write(vfd->_fd, buf, len);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::write(vfd->_fd, buf, len);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->write(vfd->_fd, buf, len);
            }
            case VFS_TESTING: {
                int ret =  tf->write(vfd->_fd, buf, len);

                if(tf->canCheck())
                {
                    if(tf->doControl())
                    {
                        tf->translate(vfd->_fd);

                        if(ef == NULL) {
                            ef = new EncryptFile(*tf->getBK());
                        }
                        setVFS(VFS_ENCRYPT);
                    }
                    else {
                        setVFS(VFS_IGNORE);
                    }

                    tf->close(false, vfd->_fd);
                    delete tf;
                    tf = 0;
                }

                return ret;
            }
        }
    }

    return 0;
}

int virtualFile::vfstat(virtualFileDescribe* pvfd, struct stat *buf) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->fstat(vfd->_fd, buf);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::fstat(vfd->_fd, buf);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::fstat(vfd->_fd, buf);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->fstat(vfd->_fd, buf);
            }
            case VFS_TESTING:
                return tf->fstat(vfd->_fd, buf);
        }
    }

    return 0;
}

off_t virtualFile::vlseek(virtualFileDescribe* pvfd, off_t offset, int whence){
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->lseek(vfd->_fd, offset, whence);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::lseek(vfd->_fd, offset, whence);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::lseek(vfd->_fd, offset, whence);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->lseek(vfd->_fd, offset, whence);
            }
            case VFS_TESTING:
                return tf->lseek(vfd->_fd, offset, whence);
        }
    }

    return 0;
}

int virtualFile::vllseek(virtualFileDescribe* pvfd, unsigned long offset_high, unsigned long offset_low, loff_t *result,
                         unsigned int whence) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->llseek(vfd->_fd, offset_high, offset_low, result, whence);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::llseek(vfd->_fd, offset_high, offset_low, result, whence);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::llseek(vfd->_fd, offset_high, offset_low, result, whence);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->llseek(vfd->_fd, offset_high, offset_low, result, whence);
            }
            case VFS_TESTING:
                return tf->llseek(vfd->_fd, offset_high, offset_low, result, whence);
        }
    }

    return 0;
}

int virtualFile::vftruncate(virtualFileDescribe* pvfd, off_t length) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->ftruncate(vfd->_fd, length);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::ftruncate(vfd->_fd, length);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::ftruncate(vfd->_fd, length);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->ftruncate(vfd->_fd, length);
            }
            case VFS_TESTING:
                return tf->ftruncate(vfd->_fd, length);
        }
    }

    return 0;
}

int virtualFile::vftruncate64(virtualFileDescribe* pvfd, off64_t length) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->ftruncate64(vfd->_fd, length);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::ftruncate64(vfd->_fd, length);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::ftruncate64(vfd->_fd, length);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->ftruncate64(vfd->_fd, length);
            }
            case VFS_TESTING:
                return tf->ftruncate64(vfd->_fd, length);
        }
    }

    return 0;
}

int virtualFile::getHeaderOffSet() {
    if(ef) {
        return ef->getHeadOffset();
    } else {
        return 0;
    }
}
