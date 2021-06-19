//
// VirtualApp Native Project
//
#include <unistd.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <Substrate/CydiaSubstrate.h>
#include <Jni/VAJni.h>
#include <sys/stat.h>
#include <syscall.h>
#include <Foundation/syscall/BinarySyscallFinder.h>
#include <limits.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <sys/user.h>

#include <asm/mman.h>
#include <sys/mman.h>
#include <arpa/inet.h>
#include <utils/zMd5.h>
#include <utils/controllerManagerNative.h>
#include <linux/in6.h>
#include <netdb.h>

#include "IORelocator.h"
#include "SandboxFs.h"
#include "canonicalize_md.h"
#include "Symbol.h"
#include "Log.h"

#include "transparentED/originalInterface.h"
#include "transparentED/ff_Recognizer.h"

void startIOHook(JNIEnv *env);

bool need_load_env = true;

int g_api_level;

bool execve_process = false;

#include "utils/zString.h"
#include "utils/utils.h"
#include "utils/Autolock.h"
#include "transparentED/virtualFileSystem.h"
#include "utils/mylog.h"

std::map<uint32_t, MmapFileInfo *> MmapInfoMap;
using namespace xdja;

void startIOHook(int api_level);

char *get_process_name() {
    char *cmdline = (char *) calloc(0x400u, 1u);
    if (cmdline) {
        FILE *file = fopen("/proc/self/cmdline", "r");
        if (file) {
            int count = fread(cmdline, 1u, 0x400u, file);
            if (count) {
                if (cmdline[count - 1] == '\n') {
                    cmdline[count - 1] = '\0';
                }
            }
            fclose(file);
        } else {
            ALOGE("fail open cmdline.");
        }
    }
    return cmdline;
}

void IOUniformer::init_env_before_all() {
    if (!need_load_env) {
        return;
    }
    need_load_env = false;
    char *ld_preload = getenv("LD_PRELOAD");
    if (!ld_preload || !strstr(ld_preload, CORE_SO_NAME)) {
        return;
    }
    execve_process = true;
    char *process_name = get_process_name();
    ALOGI("Start init env : %s", process_name);
    free(process_name);
    char src_key[KEY_MAX];
    char dst_key[KEY_MAX];
    int i = 0;
    while (true) {
        memset(src_key, 0, sizeof(src_key));
        memset(dst_key, 0, sizeof(dst_key));
        sprintf(src_key, "V_REPLACE_ITEM_SRC_%d", i);
        sprintf(dst_key, "V_REPLACE_ITEM_DST_%d", i);
        char *src_value = getenv(src_key);
        if (!src_value) {
            break;
        }
        char *dst_value = getenv(dst_key);
        add_replace_item(src_value, dst_value);
        i++;
    }
    i = 0;
    while (true) {
        memset(src_key, 0, sizeof(src_key));
        sprintf(src_key, "V_KEEP_ITEM_%d", i);
        char *keep_value = getenv(src_key);
        if (!keep_value) {
            break;
        }
        add_keep_item(keep_value);
        i++;
    }
    i = 0;
    while (true) {
        memset(src_key, 0, sizeof(src_key));
        sprintf(src_key, "V_FORBID_ITEM_%d", i);
        char *forbid_value = getenv(src_key);
        if (!forbid_value) {
            break;
        }
        add_forbidden_item(forbid_value);
        i++;
    }
    char *api_level_char = getenv("V_API_LEVEL");
    if (api_level_char != NULL) {
        int api_level = atoi(api_level_char);
        startIOHook(api_level);
    }
}

static inline void
hook_function(void *handle, const char *symbol, void *new_func, void **old_func) {
    void *addr = dlsym(handle, symbol);
    if (addr == NULL) {
        ALOGE("Not found symbol : %s", symbol);
        return;
    }
    MSHookFunction(addr, new_func, old_func);
}

void onSoLoaded(const char *name, void *handle);

void IOUniformer::relocate(const char *orig_path, const char *new_path) {
    add_replace_item(orig_path, new_path);
}

const char *IOUniformer::query(const char *orig_path, char *const buffer, const size_t size) {
    return relocate_path(orig_path, buffer, size);
}

void IOUniformer::whitelist(const char *_path) {
    add_keep_item(_path);
}

void IOUniformer::forbid(const char *_path) {
    add_forbidden_item(_path);
}

void IOUniformer::readOnly(const char *_path) {
    add_readonly_item(_path);
}

const char *IOUniformer::reverse(const char *_path, char *const buffer, const size_t size) {
    return reverse_relocate_path(_path, buffer, size);
}


__BEGIN_DECLS

// int faccessat(int dirfd, const char *pathname, int mode, int flags);
HOOK_DEF(int, faccessat, int dirfd, const char *pathname, int mode, int flags) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path && !(mode & W_OK && isReadOnly(relocated_path))) {
        return syscall(__NR_faccessat, dirfd, relocated_path, mode, flags);
    }
    errno = EACCES;
    return -1;
}

// int fchmodat(int dirfd, const char *pathname, mode_t mode, int flags);
HOOK_DEF(int, fchmodat, int dirfd, const char *pathname, mode_t mode, int flags) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_fchmodat, dirfd, relocated_path, mode, flags);
    }
    errno = EACCES;
    return -1;
}

// int fstatat64(int dirfd, const char *pathname, struct stat *buf, int flags);
HOOK_DEF(int, fstatat64, int dirfd, const char *pathname, struct stat *buf, int flags) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        long ret = syscall(__NR_fstatat64, dirfd, relocated_path, buf, flags);
        if (is_TED_Enable()) {
            int fd = originalInterface::original_openat(AT_FDCWD, relocated_path, O_RDONLY, 0);

            if (fd > 0) {
                if (EncryptFile::isEncryptFile(fd)) {
                    EncryptFile ef(relocated_path);
                    if (ef.create(fd, ENCRYPT_READ)) {
                        ef.fstat(fd, buf);
                    }
                }
                originalInterface::original_close(fd);
            }
        }
        return ret;
    }
    errno = EACCES;
    return -1;
}

// int kill(pid_t pid, int sig);
HOOK_DEF(int, kill, pid_t pid, int sig) {
    ALOGE("kill >>> pid : %d, sig : %d", pid, sig);
    return syscall(__NR_kill, pid, sig);
}

// int __statfs64(const char *path, size_t size, struct statfs *stat);
HOOK_DEF(int, __statfs64, const char *pathname, size_t size, struct statfs *stat) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_statfs64, relocated_path, size, stat);
    }
    errno = EACCES;
    return -1;
}

// int __open(const char *pathname, int flags, int mode);
HOOK_DEF(int, __open, const char *pathname, int flags, int mode) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path && !((flags & O_WRONLY || flags & O_RDWR) && isReadOnly(relocated_path))) {
//        int fake_fd = redirect_proc_maps(relocated_path, flags, mode);
//        if (fake_fd != 0) {
//            return fake_fd;
//        }
        return syscall(__NR_open, relocated_path, flags, mode);
    }
    errno = EACCES;
    return -1;
}

// ssize_t readlink(const char *path, char *buf, size_t bufsiz);
HOOK_DEF(ssize_t, readlink, const char *pathname, char *buf, size_t bufsiz) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        long ret = syscall(__NR_readlink, relocated_path, buf, bufsiz);
        if (ret < 0) {
            return ret;
        } else {
            // relocate link content
            if (reverse_relocate_path_inplace(buf, bufsiz) != -1) {
                return ret;
            }
        }
    }
    errno = EACCES;
    return -1;
}

// int mkdir(const char *pathname, mode_t mode);
HOOK_DEF(int, mkdir, const char *pathname, mode_t mode) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_mkdir, relocated_path, mode);
    }
    errno = EACCES;
    return -1;
}

// int rmdir(const char *pathname);
HOOK_DEF(int, rmdir, const char *pathname) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_rmdir, relocated_path);
    }
    errno = EACCES;
    return -1;
}

// int lchown(const char *pathname, uid_t owner, gid_t group);
HOOK_DEF(int, lchown, const char *pathname, uid_t owner, gid_t group) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_lchown, relocated_path, owner, group);
    }
    errno = EACCES;
    return -1;
}

// int utimes(const char *filename, const struct timeval *tvp);
HOOK_DEF(int, utimes, const char *pathname, const struct timeval *tvp) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_utimes, relocated_path, tvp);
    }
    errno = EACCES;
    return -1;
}

// int link(const char *oldpath, const char *newpath);
HOOK_DEF(int, link, const char *oldpath, const char *newpath) {
    char temp[PATH_MAX];
    const char *relocated_path_old = relocate_path(oldpath, temp, sizeof(temp));
    if (relocated_path_old) {
        return syscall(__NR_link, relocated_path_old, newpath);
    }
    errno = EACCES;
    return -1;
}

// int access(const char *pathname, int mode);
HOOK_DEF(int, access, const char *pathname, int mode) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path && !(mode & W_OK && isReadOnly(relocated_path))) {
        return syscall(__NR_access, relocated_path, mode);
    }
    errno = EACCES;
    return -1;
}

// int chmod(const char *path, mode_t mode);
HOOK_DEF(int, chmod, const char *pathname, mode_t mode) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_chmod, relocated_path, mode);
    }
    errno = EACCES;
    return -1;
}

// int chown(const char *path, uid_t owner, gid_t group);
HOOK_DEF(int, chown, const char *pathname, uid_t owner, gid_t group) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_chown, relocated_path, owner, group);
    }
    errno = EACCES;
    return -1;
}

// int lstat(const char *path, struct stat *buf);
HOOK_DEF(int, lstat, const char *pathname, struct stat *buf) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        long ret = syscall(__NR_lstat64, relocated_path, buf);

        if (is_TED_Enable()) {
            int fd = originalInterface::original_openat(AT_FDCWD, relocated_path, O_RDONLY, 0);

            if (fd > 0) {
                if (EncryptFile::isEncryptFile(fd)) {
                    EncryptFile ef(relocated_path);
                    if (ef.create(fd, ENCRYPT_READ)) {
                        ef.fstat(fd, buf);
                    }
                }
                originalInterface::original_close(fd);
            }
        }

        return ret;
    }
    errno = EACCES;
    return -1;
}

// int stat(const char *path, struct stat *buf);
HOOK_DEF(int, stat, const char *pathname, struct stat *buf) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        long ret = syscall(__NR_stat64, relocated_path, buf);
        if (isReadOnly(relocated_path)) {
            buf->st_mode &= ~S_IWGRP;
        }

        if (is_TED_Enable()) {
            int fd = originalInterface::original_openat(AT_FDCWD, relocated_path, O_RDONLY, 0);

            if (fd > 0) {
                if (EncryptFile::isEncryptFile(fd)) {
                    EncryptFile ef(relocated_path);
                    if (ef.create(fd, ENCRYPT_READ)) {
                        ef.fstat(fd, buf);
                    }
                }
                originalInterface::original_close(fd);
            }
        }
        return ret;
    }
    errno = EACCES;
    return -1;
}

// int symlink(const char *oldpath, const char *newpath);
HOOK_DEF(int, symlink, const char *oldpath, const char *newpath) {
    char temp[PATH_MAX];
    const char *relocated_path_old = relocate_path(oldpath, temp, sizeof(temp));
    if (relocated_path_old) {
        return syscall(__NR_symlink, relocated_path_old, newpath);
    }
    errno = EACCES;
    return -1;
}

// int unlink(const char *pathname);
HOOK_DEF(int, unlink, const char *pathname) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path && !isReadOnly(relocated_path)) {
        return syscall(__NR_unlink, relocated_path);
    }
    errno = EACCES;
    return -1;
}

// int fchmod(const char *pathname, mode_t mode);
HOOK_DEF(int, fchmod, const char *pathname, mode_t mode) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_fchmod, relocated_path, mode);
    }
    errno = EACCES;
    return -1;
}


// int fstatat(int dirfd, const char *pathname, struct stat *buf, int flags);
HOOK_DEF(int, fstatat, int dirfd, const char *pathname, struct stat *buf, int flags) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_fstatat64, dirfd, relocated_path, buf, flags);
    }
    errno = EACCES;
    return -1;
}

HOOK_DEF(int, fstat, int fd, struct stat *buf)
{
    int ret;
    bool flag = false;

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("fstat fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vfstat(vfd.get(), buf);
            flag = true;
        }
    }
    if(!flag) {
        ret = orig_fstat(fd, buf);
    }
    return ret;
}

// int mknod(const char *pathname, mode_t mode, dev_t dev);
HOOK_DEF(int, mknod, const char *pathname, mode_t mode, dev_t dev) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_mknod, relocated_path, mode, dev);
    }
    errno = EACCES;
    return -1;
}

// int rename(const char *oldpath, const char *newpath);
HOOK_DEF(int, rename, const char *oldpath, const char *newpath) {
    char temp_old[PATH_MAX], temp_new[PATH_MAX];
    const char *relocated_path_old = relocate_path(oldpath, temp_old, sizeof(temp_old));
    const char *relocated_path_new = relocate_path(newpath, temp_new, sizeof(temp_new));
    if (relocated_path_old && relocated_path_new) {
        return syscall(__NR_rename, relocated_path_old, relocated_path_new);
    }
    errno = EACCES;
    return -1;
}

// int mknodat(int dirfd, const char *pathname, mode_t mode, dev_t dev);
HOOK_DEF(int, mknodat, int dirfd, const char *pathname, mode_t mode, dev_t dev) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_mknodat, dirfd, relocated_path, mode, dev);
    }
    errno = EACCES;
    return -1;
}

// int utimensat(int dirfd, const char *pathname, const struct timespec times[2], int flags);
HOOK_DEF(int, utimensat, int dirfd, const char *pathname, const struct timespec times[2],
         int flags) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_utimensat, dirfd, relocated_path, times, flags);
    }
    errno = EACCES;
    return -1;
}

// int fchownat(int dirfd, const char *pathname, uid_t owner, gid_t group, int flags);
HOOK_DEF(int, fchownat, int dirfd, const char *pathname, uid_t owner, gid_t group, int flags) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_fchownat, dirfd, relocated_path, owner, group, flags);
    }
    errno = EACCES;
    return -1;
}

// int chroot(const char *pathname);
HOOK_DEF(int, chroot, const char *pathname) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_chroot, relocated_path);
    }
    errno = EACCES;
    return -1;
}

// int renameat(int olddirfd, const char *oldpath, int newdirfd, const char *newpath);
HOOK_DEF(int, renameat, int olddirfd, const char *oldpath, int newdirfd, const char *newpath) {
    char temp_old[PATH_MAX], temp_new[PATH_MAX];
    const char *relocated_path_old = relocate_path(oldpath, temp_old, sizeof(temp_old));
    const char *relocated_path_new = relocate_path(newpath, temp_new, sizeof(temp_new));
    if (relocated_path_old && relocated_path_new) {
        xdja::zs::sp <virtualFile> *vf2 = virtualFileManager::getVFM().queryVF(
                (char *) relocated_path_old);
        if (vf2 != NULL) {
            slog(" *** need to force translate virtual File [%s] *** ", vf2->get()->getPath());

            xdja::zs::sp <virtualFile> pvf2(vf2->get());
            pvf2->lockWhole();
            pvf2->forceTranslate();
            pvf2->unlockWhole();
            pvf2->delRef();
        }

        {
            /**？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？**/
            virtualFileManager::getVFM().deleted((char *) relocated_path_old);
        }

        int ret = syscall(__NR_renameat, olddirfd, relocated_path_old, newdirfd,
                          relocated_path_new);

        xdja::zs::sp <virtualFile> *vf3 = virtualFileManager::getVFM().queryVF(
                (char *) relocated_path_new);
        if (vf3 != NULL) {
            xdja::zs::sp <virtualFile> pvf3(vf3->get());
            slog(" *** update virtual file [%s] *** ", pvf3->getPath());
            pvf3->lockWhole();
            virtualFileManager::getVFM().updateVF(*pvf3.get());
            pvf3->unlockWhole();
            pvf3->delRef();
        }

        /*zString op("renameat to %s ret %d err %s", redirect_path_new, ret, getErr);
        doFileTrace(redirect_path_old, op.toString());*/

        return ret;
    }
    errno = EACCES;
    return -1;
}

// int statfs64(const char *__path, struct statfs64 *__buf) __INTRODUCED_IN(21);
HOOK_DEF(int, statfs64, const char *filename, struct statfs64 *buf) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(filename, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_statfs, relocated_path, buf);
    }
    errno = EACCES;
    return -1;
}

// int unlinkat(int dirfd, const char *pathname, int flags);
HOOK_DEF(int, unlinkat, int dirfd, const char *pathname, int flags) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path && !isReadOnly(relocated_path)) {
        int ret = syscall(__NR_unlinkat, dirfd, relocated_path, flags);

        if (ret == 0) {
            /***？？？？？？？？？？？？？？？？？？？？？？？？？？？？？***/
            virtualFileManager::getVFM().deleted((char *) relocated_path);
        }

        return ret;
    }
    errno = EACCES;
    return -1;
}

// int symlinkat(const char *oldpath, int newdirfd, const char *newpath);
HOOK_DEF(int, symlinkat, const char *oldpath, int newdirfd, const char *newpath) {
    char temp[PATH_MAX];
    const char *relocated_path_old = relocate_path(oldpath, temp, sizeof(temp));
    if (relocated_path_old) {
        return syscall(__NR_symlinkat, relocated_path_old, newdirfd, newpath);
    }
    errno = EACCES;
    return -1;
}

// int linkat(int olddirfd, const char *oldpath, int newdirfd, const char *newpath, int flags);
HOOK_DEF(int, linkat, int olddirfd, const char *oldpath, int newdirfd, const char *newpath,
         int flags) {
    char temp[PATH_MAX];
    const char *relocated_path_old = relocate_path(oldpath, temp, sizeof(temp));
    if (relocated_path_old) {
        return syscall(__NR_linkat, olddirfd, relocated_path_old, newdirfd, newpath,
                       flags);
    }
    errno = EACCES;
    return -1;
}

// int mkdirat(int dirfd, const char *pathname, mode_t mode);
HOOK_DEF(int, mkdirat, int dirfd, const char *pathname, mode_t mode) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_mkdirat, dirfd, relocated_path, mode);
    }
    errno = EACCES;
    return -1;
}

// int readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz);
HOOK_DEF(int, readlinkat, int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        long ret = syscall(__NR_readlinkat, dirfd, relocated_path, buf, bufsiz);
        if (ret < 0) {
            return ret;
        } else {
            // relocate link content
            if (reverse_relocate_path_inplace(buf, bufsiz) != -1) {
                return ret;
            }
        }
    }
    errno = EACCES;
    return -1;
}


// int truncate(const char *path, off_t length);
HOOK_DEF(int, truncate, const char *pathname, off_t length) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_truncate, relocated_path, length);
    }
    errno = EACCES;
    return -1;
}

// int chdir(const char *path);
HOOK_DEF(int, chdir, const char *pathname) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_chdir, relocated_path);
    }
    errno = EACCES;
    return -1;
}

// int truncate64(const char *pathname, off_t length);
HOOK_DEF(int, truncate64, const char *pathname, off_t length) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        int ret = syscall(__NR_truncate64, relocated_path, length);

        /*zString op("truncate64 length %ld ret %d err %s", length, ret, getErr);
        doFileTrace(relocated_path, op.toString());*/
        return ret;
    }

    errno = EACCES;
    return -1;
}


// int __getcwd(char *buf, size_t size);
HOOK_DEF(int, __getcwd, char *buf, size_t size) {
    int ret = syscall(__NR_getcwd, buf, size);
    if (!ret) {
        if (reverse_relocate_path_inplace(buf, size) < 0) {
            errno = EACCES;
            return -1;
        }
    }
    return ret;
}

// int __openat(int fd, const char *pathname, int flags, int mode);
HOOK_DEF(int, __openat, int fd, const char *pathname, int flags, int mode) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        if ((flags & O_ACCMODE) == O_WRONLY) {
            flags &= ~O_ACCMODE;
            flags |= O_RDWR;
        }

        int ret = syscall(__NR_openat, fd, relocated_path, flags, mode);
        /*zString op("openat fd = %d err = %s", ret, strerror(errno));
        doFileTrace(relocated_path, op.toString());*/

        if (getApiLevel() >= 29) {
            xdja::zs::sp<virtualFileDescribe> oldVfd(
                    virtualFileDescribeSet::getVFDSet().get(ret));
            if (oldVfd.get() != nullptr) {
                virtualFileDescribeSet::getVFDSet().reset(ret);
                xdja::zs::sp<virtualFile> vf(oldVfd->_vf->get());
                if (vf.get() != nullptr) {
                    virtualFileManager::getVFM().releaseVF(vf->getPath(), oldVfd.get());
                }
                oldVfd.get()->decStrong(0);
            }
        }

        if (ret > 0 && (is_TED_Enable() || changeDecryptState(false, 1)) &&
            isEncryptPath(relocated_path)) {

            /*******************only here**********************/
            virtualFileDescribe *pvfd = new virtualFileDescribe(ret);
            pvfd->incStrong(0);
            /***************************************************/
            xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

            int _Errno;
            xdja::zs::sp <virtualFile> vf(
                    virtualFileManager::getVFM().getVF(vfd.get(), (char *) relocated_path, &_Errno));

            virtualFileDescribeSet::getVFDSet().set(ret, pvfd);

            if (vf.get() != nullptr) {
                LOGE("judge : open vf [PATH %s] [VFS %d] [FD %d] [VFD %p]", vf->getPath(),
                     vf->getVFS(), ret, vfd.get());
                if ((flags & O_APPEND) == O_APPEND) {
                    vf->vlseek(vfd.get(), 0, SEEK_END);
                } else {
                    vf->vlseek(vfd.get(), 0, SEEK_SET);
                }
            } else {
                virtualFileDescribeSet::getVFDSet().reset(ret);
                /******through this way to release vfd *********/
                virtualFileDescribeSet::getVFDSet().release(pvfd);
                /***********************************************/

                if (_Errno < 0) {
                    //这种情况需要让openat 返回失败
                    originalInterface::original_close(ret);
                    ret = -1;
                    errno = EACCES;

                    if (flags & O_CREAT) {
                        originalInterface::original_unlinkat(AT_FDCWD, relocated_path, 0);
                    }

                    LOGE("judge : **** force openat fail !!! ****");
                }
            }
        }
        return ret;
    }
    errno = EACCES;
    return -1;
}

HOOK_DEF(int, close, int __fd) {
    int ret;
    LOGE("close fd[%d]", __fd);
    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(__fd));
    if (vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(__fd)) {
            log("close fd[%d] flag is closing", __fd);
            return -1;
        }
    } else {
        virtualFileDescribeSet::getVFDSet().setFlag(__fd, FD_CLOSING);

        virtualFileDescribeSet::getVFDSet().reset(__fd);
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            log("trace_close fd[%d]path[%s]vfd[%p]", __fd, vf->getPath(), vfd.get());
            virtualFileManager::getVFM().releaseVF(vf->getPath(), vfd.get());
        }

        /******through this way to release vfd *********/
        virtualFileDescribeSet::getVFDSet().release(vfd.get());
        /***********************************************/
    }

    ret = syscall(__NR_close, __fd);
    virtualFileDescribeSet::getVFDSet().clearFlag(__fd);
    return ret;
}


// int __statfs (__const char *__file, struct statfs *__buf);
HOOK_DEF(int, __statfs, __const char *__file, struct statfs *__buf) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(__file, temp, sizeof(temp));
    if (__predict_true(relocated_path)) {
        return syscall(__NR_statfs, relocated_path, __buf);
    }
    errno = EACCES;
    return -1;
}

static char **relocate_envp(const char *pathname, char *const envp[]) {
    if (strstr(pathname, "libweexjsb.so")) {
        return const_cast<char **>(envp);
    }
    char *soPath = getenv("V_SO_PATH");
    char *soPath64 = getenv("V_SO_PATH_64");

    char *env_so_path = NULL;
    FILE *fd = fopen(pathname, "r");
    if (!fd) {
        return const_cast<char **>(envp);
    }
    for (int i = 0; i < 4; ++i) {
        fgetc(fd);
    }
    int type = fgetc(fd);
    if (type == ELFCLASS32) {
        env_so_path = soPath;
    } else if (type == ELFCLASS64) {
        env_so_path = soPath64;
    }
    fclose(fd);
    if (env_so_path == NULL) {
        return const_cast<char **>(envp);
    }
    int len = 0;
    int ld_preload_index = -1;
    int self_so_index = -1;
    while (envp[len]) {
        /* find LD_PRELOAD element */
        if (ld_preload_index == -1 && !strncmp(envp[len], "LD_PRELOAD=", 11)) {
            ld_preload_index = len;
        }
        if (self_so_index == -1 && !strncmp(envp[len], "V_SO_PATH=", 10)) {
            self_so_index = len;
        }
        ++len;
    }
    /* append LD_PRELOAD element */
    if (ld_preload_index == -1) {
        ++len;
    }
    /* append V_env element */
    if (self_so_index == -1) {
        // V_SO_PATH
        // V_API_LEVEL
        // V_PREVIEW_API_LEVEL
        // V_NATIVE_PATH
        len += 4;
        if (soPath64) {
            // V_SO_PATH_64
            len++;
        }
        len += get_keep_item_count();
        len += get_forbidden_item_count();
        len += get_replace_item_count() * 2;
    }

    /* append NULL element */
    ++len;

    char **relocated_envp = (char **) malloc(len * sizeof(char *));
    memset(relocated_envp, 0, len * sizeof(char *));
    for (int i = 0; envp[i]; ++i) {
        if (i != ld_preload_index) {
            relocated_envp[i] = strdup(envp[i]);
        }
    }
    char LD_PRELOAD_VARIABLE[PATH_MAX];
    if (ld_preload_index == -1) {
        ld_preload_index = len - 2;
        sprintf(LD_PRELOAD_VARIABLE, "LD_PRELOAD=%s", env_so_path);
    } else {
        const char *orig_ld_preload = envp[ld_preload_index] + 11;
        sprintf(LD_PRELOAD_VARIABLE, "LD_PRELOAD=%s:%s", env_so_path, orig_ld_preload);
    }
    relocated_envp[ld_preload_index] = strdup(LD_PRELOAD_VARIABLE);
    int index = 0;
    while (relocated_envp[index]) index++;
    if (self_so_index == -1) {
        char element[PATH_MAX] = {0};
        sprintf(element, "V_SO_PATH=%s", soPath);
        relocated_envp[index++] = strdup(element);
        if (soPath64) {
            sprintf(element, "V_SO_PATH_64=%s", soPath64);
            relocated_envp[index++] = strdup(element);
        }
        sprintf(element, "V_API_LEVEL=%s", getenv("V_API_LEVEL"));
        relocated_envp[index++] = strdup(element);
        sprintf(element, "V_PREVIEW_API_LEVEL=%s", getenv("V_PREVIEW_API_LEVEL"));
        relocated_envp[index++] = strdup(element);
        sprintf(element, "V_NATIVE_PATH=%s", getenv("V_NATIVE_PATH"));
        relocated_envp[index++] = strdup(element);

        for (int i = 0; i < get_keep_item_count(); ++i) {
            PathItem &item = get_keep_items()[i];
            char env[PATH_MAX] = {0};
            sprintf(env, "V_KEEP_ITEM_%d=%s", i, item.path);
            relocated_envp[index++] = strdup(env);
        }

        for (int i = 0; i < get_forbidden_item_count(); ++i) {
            PathItem &item = get_forbidden_items()[i];
            char env[PATH_MAX] = {0};
            sprintf(env, "V_FORBID_ITEM_%d=%s", i, item.path);
            relocated_envp[index++] = strdup(env);
        }

        for (int i = 0; i < get_replace_item_count(); ++i) {
            ReplaceItem &item = get_replace_items()[i];
            char src[PATH_MAX] = {0};
            char dst[PATH_MAX] = {0};
            sprintf(src, "V_REPLACE_ITEM_SRC_%d=%s", i, item.orig_path);
            sprintf(dst, "V_REPLACE_ITEM_DST_%d=%s", i, item.new_path);
            relocated_envp[index++] = strdup(src);
            relocated_envp[index++] = strdup(dst);
        }
    }
    return relocated_envp;
}


// int (*origin_execve)(const char *pathname, char *const argv[], char *const envp[]);
HOOK_DEF(int, execve, const char *pathname, char *argv[], char *const envp[]) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (!relocated_path) {
        errno = EACCES;
        return -1;
    }

    char **relocated_envp = relocate_envp(relocated_path, envp);
    int ret = syscall(__NR_execve, relocated_path, argv, relocated_envp);
    if (relocated_envp != envp) {
        int i = 0;
        while (relocated_envp[i] != NULL) {
            free(relocated_envp[i]);
            ++i;
        }
        free(relocated_envp);
    }
    return ret;
}

HOOK_DEF(void *, dlopen_CI, const char *filename, int flag) {
    char temp[PATH_MAX];
    const char *redirect_path = relocate_path(filename, temp, sizeof(temp));
    void *ret = orig_dlopen_CI(redirect_path, flag);
    onSoLoaded(filename, ret);
    return ret;
}

HOOK_DEF(void*, do_dlopen_CIV, const char *filename, int flag, const void *extinfo) {
    char temp[PATH_MAX];
    const char *redirect_path = relocate_path(filename, temp, sizeof(temp));
    void *ret = orig_do_dlopen_CIV(redirect_path, flag, extinfo);
    onSoLoaded(filename, ret);
    return ret;
}

HOOK_DEF(void*, do_dlopen_CIVV, const char *name, int flags, const void *extinfo,
         void *caller_addr) {
    char temp[PATH_MAX];
    const char *redirect_path = relocate_path(name, temp, sizeof(temp));
    void *ret = orig_do_dlopen_CIVV(redirect_path, flags, extinfo, caller_addr);
    onSoLoaded(name, ret);
    return ret;
}

//void *dlsym(void *handle, const char *symbol)
HOOK_DEF(void*, dlsym, void *handle, char *symbol) {
    return orig_dlsym(handle, symbol);
}

HOOK_DEF(pid_t, vfork) {
    return fork();
}

HOOK_DEF(ssize_t, pread64, int fd, void* buf, size_t count, off64_t offset) {
    ssize_t ret = 0;
    bool flag = false;

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("pread64 fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vpread64(vfd.get(), (char *) buf, count, offset);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_pread64(fd, buf, count, offset);

    return ret;
}

HOOK_DEF(ssize_t, pwrite64, int fd, const void *buf, size_t count, off64_t offset) {
    ssize_t ret = 0;
    bool flag = false;

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("pwrite64 fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vpwrite64(vfd.get(), (char *) buf, count, offset);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_pwrite64(fd, buf, count, offset);

    return ret;
}

HOOK_DEF(ssize_t, read, int fd, void *buf, size_t count) {
    ssize_t ret = 0;
    bool flag = false;

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("read fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vread(vfd.get(), (char *) buf, count);
            flag = true;
        }
    }

    if(!flag)
        ret = syscall(__NR_read, fd, buf, count);

    return ret;
}

HOOK_DEF(ssize_t, write, int fd, const void* buf, size_t count) {
    ssize_t ret = 0;
    bool flag = false;

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("write fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vwrite(vfd.get(), (char *) buf, count);
            flag = true;
        }
    }

    if(!flag)
        ret = syscall(__NR_write, fd, buf, count);

    return ret;
}

HOOK_DEF(int, munmap, void *addr, size_t length) {
    int ret = -1;

    MmapFileInfo *fileInfo = 0;
    std::map<uint32_t , MmapFileInfo *>::iterator iter = MmapInfoMap.find(std::uint32_t(addr));
    if (iter != MmapInfoMap.end()) {
        MmapInfoMap.erase(iter);
        fileInfo = iter->second;
        if ((fileInfo->_flag & MAP_SHARED)) {
            int fd = syscall(__NR_openat, AT_FDCWD, fileInfo->_path, O_RDWR, 0);

            if (fd > 0 && isEncryptPath(fileInfo->_path)) {
                virtualFileDescribe *pvfd = new virtualFileDescribe(fd);
                xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

                int _Errno;
                xdja::zs::sp<virtualFile> vf(virtualFileManager::getVFM().getVF(vfd.get(), fileInfo->_path,
                                                                     &_Errno));
                virtualFileDescribeSet::getVFDSet().set(fd, pvfd);
                if (vf.get() != nullptr) {
                    vf->vpwrite64(vfd.get(), (char *) addr, length, fileInfo->_offsize * 4096);
                }

                virtualFileDescribeSet::getVFDSet().reset(fd);
                vf->delRef();
            }
            syscall(__NR_close, fd);
        }
    }

    ret = syscall(__NR_munmap, addr, length);

    return ret;
}

HOOK_DEF(int, msync, void *addr, size_t size, int flags) {
    int ret = -1;

    MmapFileInfo *fileInfo = 0;
    std::map<uint32_t , MmapFileInfo *>::iterator iter = MmapInfoMap.find(std::uint32_t(addr));
    if (iter != MmapInfoMap.end()) {
        MmapInfoMap.erase(iter);
        fileInfo = iter->second;
        if ((fileInfo->_flag & MAP_SHARED)) {
            int fd = syscall(__NR_openat, AT_FDCWD, fileInfo->_path, O_RDWR, 0);

            if (fd > 0 && isEncryptPath(fileInfo->_path)) {
                virtualFileDescribe *pvfd = new virtualFileDescribe(fd);
                xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

                int _Errno;
                xdja::zs::sp<virtualFile> vf(virtualFileManager::getVFM().getVF(vfd.get(), fileInfo->_path,
                                                                     &_Errno));
                virtualFileDescribeSet::getVFDSet().set(fd, pvfd);
                if (vf.get() != nullptr) {
                    vf->vpwrite64(vfd.get(), (char *) addr, size, fileInfo->_offsize * 4096);
                }

                virtualFileDescribeSet::getVFDSet().reset(fd);
                vf->delRef();
            }
            syscall(__NR_close, fd);
        }
    }

    ret = syscall(__NR_msync, addr, size, flags);

    return ret;
}

HOOK_DEF(void *, __mmap2, void *addr, size_t length, int prot,int flags, int fd, size_t pgoffset) {
    void * ret = 0;
    bool flag = false;

    do {
        if (fd == -1) break;

        xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));

        if (vfd.get() == nullptr) {
            if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
                log("__mmap2 fd[%d] flag is closing", fd);
                return MAP_FAILED;
            }
        } else {
            xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
            if (vf.get() != nullptr) {
                if (vf->getVFS() == VFS_ENCRYPT) {
                    flags |= MAP_ANONYMOUS;     //申请匿名内存
                    ret = (void *) syscall(__NR_mmap2, addr, length, prot, flags, 0, 0);

                    bool nowrite = (prot & PROT_WRITE) == 0;
                    if (nowrite && -1 == mprotect(ret, length, prot | PROT_WRITE)) {
                        LOGE("__mmap2 mprotect failed.");
                    } else {
                        vf->vpread64(vfd.get(), (char *) ret, length, pgoffset * 4096);

                        if (nowrite) {
                            if (0 != mprotect(ret, length, prot)) {
                                LOGE("__mmap2 mprotect restore prot fails.");
                            }
                        }
                        MmapFileInfo *fileInfo = new MmapFileInfo(vf->getPath(), pgoffset,
                                                                  flags);
                        MmapInfoMap.insert(
                                std::pair<uint32_t, MmapFileInfo *>(uint32_t(ret), fileInfo));
                        flag = true;
                    }
                }
            }
        }
    }while(false);

    if(fd > 0)
    {
        /*zString path;
        getPathFromFd(fd, path);

        zString op("%c__mmap2 length %d flags %p pgoffset %p", flag?'v':' ', length, flags, pgoffset);
        doFileTrace(path.toString(), op.toString());*/
    }

    if(!flag)
        ret = (void *) syscall(__NR_mmap2, addr, length, prot, flags, fd, pgoffset);

    return ret;
}

HOOK_DEF(off_t, lseek, int fd, off_t offset, int whence)
{
    off_t ret;
    bool flag = false;

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("lseek fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vlseek(vfd.get(), offset, whence);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_lseek(fd, offset, whence);

    return ret;
}

HOOK_DEF(int, __llseek, unsigned int fd, unsigned long offset_high,
         unsigned long offset_low, off64_t *result,
         unsigned int whence)
{
    bool flag = false;

    int ret;

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("__llseek fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vllseek(vfd.get(), offset_high, offset_low, result, whence);
            flag = true;
        }
    }

    if(!flag)
        ret = orig___llseek(fd, offset_high, offset_low, result, whence);

    return ret;
}

//int ftruncate64(int, off_t)
HOOK_DEF(int, ftruncate64, int fd, off64_t length)
{
    int ret;
    bool flag = false;

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("ftruncate64 fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vftruncate64(vfd.get(), length);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_ftruncate64(fd, length);

    return ret;
}

//ssize_t sendfile(int out_fd, int in_fd, off_t* offset, size_t count)
HOOK_DEF(ssize_t, sendfile, int out_fd, int in_fd, off_t* offset, size_t count)
{
    ssize_t ret;

    off_t off = 0;
    if(offset != 0)
        off = *offset;

    struct stat st;
    originalInterface::original_fstat(in_fd,&st);

    xdja::zs::sp<virtualFileDescribe> in_vfd(virtualFileDescribeSet::getVFDSet().get(in_fd));
    xdja::zs::sp<virtualFileDescribe> out_vfd(virtualFileDescribeSet::getVFDSet().get(out_fd));
    if(in_vfd.get() == nullptr && out_vfd.get() == nullptr) {
        if((virtualFileDescribeSet::getVFDSet().getFlag(out_fd)) &&
           (virtualFileDescribeSet::getVFDSet().getFlag(in_fd))) {
            log("sendfile out_fd[%d] and in_fd[%d] flag is closing", out_fd, in_fd);
            return -1;
        }
        //完全不管
        ret = orig_sendfile(out_fd, in_fd, offset, count);
    } else {
        if(in_vfd.get() != nullptr && out_vfd.get() != nullptr) //完全管理
        {
            xdja::zs::sp<virtualFile> in_vf(in_vfd->_vf->get());
            xdja::zs::sp<virtualFile> out_vf(out_vfd->_vf->get());

            size_t real_count = 0;
            int encryptFileHeadLength = in_vf.get()->getHeaderOffSet();
            if(off + count > (st.st_size - encryptFileHeadLength)) {
                real_count = (size_t)(st.st_size - encryptFileHeadLength - off);
            } else {
                real_count = count;
            }

            if(offset != 0)
            {
                in_vf->vlseek(in_vfd.get(), off, SEEK_SET);
            } else {
                in_vf->vlseek(in_vfd.get(), 0, SEEK_CUR);
            }

            char * buf = new char[1024]{0};
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = in_vf->vread(in_vfd.get(),buf,real_count % 1024);
                } else {
                    rl = in_vf->vread(in_vfd.get(),buf,1024);
                }
                out_vf->vwrite(out_vfd.get(),buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                in_vf->vlseek(in_vfd.get(), off, SEEK_SET);
            }
        }
        else if(in_vfd.get() == nullptr && out_vfd.get() != nullptr)
        {
            if (virtualFileDescribeSet::getVFDSet().getFlag(in_fd)) {
                log("sendfile in_fd[%d] flag is closing", in_fd);
                return -1;
            }

            xdja::zs::sp<virtualFile> out_vf(out_vfd->_vf->get());

            size_t real_count = 0;
            if(off + count > st.st_size) {
                real_count = (size_t)(st.st_size - off);
            } else {
                real_count = count;
            }

            if(offset != 0)
            {
                ignoreFile::lseek(in_fd, off, SEEK_SET);
            } else {
                ignoreFile::lseek(in_fd, 0, SEEK_CUR);
            }

            char * buf = new char[1024]{0};
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = ignoreFile::read(in_fd,buf,real_count % 1024);
                } else {
                    rl = ignoreFile::read(in_fd,buf,1024);
                }
                out_vf->vwrite(out_vfd.get(),buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                ignoreFile::lseek(in_fd, off, SEEK_SET);
            }
        }
        else if(in_vfd.get() != nullptr && out_vfd.get() == nullptr)
        {
            if (virtualFileDescribeSet::getVFDSet().getFlag(out_fd)) {
                log("sendfile out_fd[%d] flag is closing", out_fd);
                return -1;
            }

            xdja::zs::sp<virtualFile> in_vf(in_vfd->_vf->get());

            size_t real_count = 0;
            int encryptFileHeadLength = in_vf.get()->getHeaderOffSet();
            if(off + count > (st.st_size - encryptFileHeadLength)) {
                real_count = (size_t)(st.st_size - encryptFileHeadLength - off);
            } else {
                real_count = count;
            }

            if(offset != 0)
            {
                in_vf->vlseek(in_vfd.get(), off, SEEK_SET);
            } else {
                in_vf->vlseek(in_vfd.get(), 0, SEEK_CUR);
            }

            char * buf = new char[1024];
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = in_vf->vread(in_vfd.get(),buf,real_count % 1024);
                } else {
                    rl = in_vf->vread(in_vfd.get(),buf,1024);
                }
                ignoreFile::write(out_fd,buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                in_vf->vlseek(in_vfd.get(), off, SEEK_SET);
            }
        }
    }

    return ret;
}

//ssize_t sendfile64(int out_fd, int in_fd, off64_t* offset, size_t count)
HOOK_DEF(ssize_t, sendfile64, int out_fd, int in_fd, off64_t* offset, size_t count)
{
    ssize_t ret;

    off64_t off = 0;
    if(offset != 0)
        off = *offset;

    struct stat st;
    originalInterface::original_fstat(in_fd,&st);

    unsigned long off_hi = static_cast<unsigned long>(off >> 32);
    unsigned long off_lo = static_cast<unsigned long>(off);

    xdja::zs::sp<virtualFileDescribe> in_vfd(virtualFileDescribeSet::getVFDSet().get(in_fd));
    xdja::zs::sp<virtualFileDescribe> out_vfd(virtualFileDescribeSet::getVFDSet().get(out_fd));
    if(in_vfd.get() == nullptr && out_vfd.get() == nullptr) {
        if((virtualFileDescribeSet::getVFDSet().getFlag(out_fd)) &&
           (virtualFileDescribeSet::getVFDSet().getFlag(in_fd))) {
            log("sendfile64 out_fd[%d] and in_fd[%d] flag is closing", out_fd, in_fd);
            return -1;
        }
        //完全不管
        ret = orig_sendfile64(out_fd, in_fd, offset, count);
    } else {
        if(in_vfd.get() != nullptr && out_vfd.get() != nullptr) //完全管理
        {
            xdja::zs::sp<virtualFile> in_vf(in_vfd->_vf->get());
            xdja::zs::sp<virtualFile> out_vf(out_vfd->_vf->get());

            size_t real_count = 0;
            int encryptFileHeadLength = in_vf.get()->getHeaderOffSet();
            if(off + count > (st.st_size - encryptFileHeadLength)) {
                real_count = (size_t)(st.st_size - encryptFileHeadLength - off);
            } else {
                real_count = count;
            }

            if(offset != 0)
            {
                loff_t result;
                in_vf->vllseek(in_vfd.get(), off_hi, off_lo, &result, SEEK_SET);
            } else {
                in_vf->vlseek(in_vfd.get(), 0, SEEK_CUR);
            }

            char * buf = new char[1024]{0};
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = in_vf->vread(in_vfd.get(),buf,real_count % 1024);
                } else {
                    rl = in_vf->vread(in_vfd.get(),buf,1024);
                }
                out_vf->vwrite(out_vfd.get(),buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                loff_t result;
                in_vf->vllseek(in_vfd.get(), off_hi, off_lo, &result, SEEK_SET);
            }
        }
        else if(in_vfd.get() == nullptr && out_vfd.get() != nullptr)
        {
            if (virtualFileDescribeSet::getVFDSet().getFlag(in_fd)) {
                log("sendfile64 in_fd[%d] flag is closing", in_fd);
                return -1;
            }
            xdja::zs::sp<virtualFile> out_vf(out_vfd->_vf->get());

            size_t real_count = 0;
            if(off + count > st.st_size) {
                real_count = (size_t)(st.st_size - off);
            } else {
                real_count = count;
            }

            if(offset != 0)
            {
                loff_t result;
                ignoreFile::llseek(in_fd, off_hi, off_lo, &result, SEEK_SET);
            } else {
                ignoreFile::lseek(in_fd, 0, SEEK_CUR);
            }

            char * buf = new char[1024]{0};
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = ignoreFile::read(in_fd,buf,real_count % 1024);
                } else {
                    rl = ignoreFile::read(in_fd,buf,1024);
                }
                out_vf->vwrite(out_vfd.get(),buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                loff_t result;
                ignoreFile::llseek(in_fd, off_hi, off_lo, &result, SEEK_SET);
            }
        }
        else if(in_vfd.get() != nullptr && out_vfd.get() == nullptr)
        {
            if (virtualFileDescribeSet::getVFDSet().getFlag(out_fd)) {
                log("sendfile64 out_fd[%d] flag is closing", out_fd);
                return -1;
            }

            xdja::zs::sp<virtualFile> in_vf(in_vfd->_vf->get());

            size_t real_count = 0;
            int encryptFileHeadLength = in_vf.get()->getHeaderOffSet();
            if(off + count > (st.st_size - encryptFileHeadLength)) {
                real_count = (size_t)(st.st_size - encryptFileHeadLength - off);
            } else {
                real_count = count;
            }

            if(offset != 0)
            {
                loff_t result;
                in_vf->vllseek(in_vfd.get(), off_hi, off_lo, &result, SEEK_SET);
            } else {
                in_vf->vlseek(in_vfd.get(), 0, SEEK_CUR);
            }

            char * buf = new char[1024];
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = in_vf->vread(in_vfd.get(),buf,real_count % 1024);
                } else {
                    rl = in_vf->vread(in_vfd.get(),buf,1024);
                }
                ignoreFile::write(out_fd,buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                loff_t result;
                in_vf->vllseek(in_vfd.get(), off_hi, off_lo, &result, SEEK_SET);
            }
        }
    }

    return ret;
}

//int dup(int oldfd);
HOOK_DEF(int, dup, int oldfd)
{
    int ret = syscall(__NR_dup, oldfd);

    zString path, path2;
    getPathFromFd(oldfd, path);
    getPathFromFd(ret, path2);

    if (getApiLevel() >= 29) {
        xdja::zs::sp<virtualFileDescribe> oldVfd(
                virtualFileDescribeSet::getVFDSet().get(ret));
        if (oldVfd.get() != nullptr) {
            virtualFileDescribeSet::getVFDSet().reset(ret);
            xdja::zs::sp<virtualFile> vf(oldVfd->_vf->get());
            if (vf.get() != nullptr) {
                virtualFileManager::getVFM().releaseVF(vf->getPath(), oldVfd.get());
            }
            oldVfd.get()->decStrong(0);
        }
    }

    if(ret > 0 && (is_TED_Enable()||changeDecryptState(false,1)) && isEncryptPath(path2.toString())) {
        /*******************only here**********************/
        virtualFileDescribe *pvfd = new virtualFileDescribe(ret);
        pvfd->incStrong(0);
        /***************************************************/
        xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

        int _Errno;
        xdja::zs::sp<virtualFile> vf(virtualFileManager::getVFM().getVF(vfd.get(), path2.toString(), &_Errno));

        virtualFileDescribeSet::getVFDSet().set(ret, pvfd);

        if (vf.get() != nullptr) {
            LOGE("judge : dup vf [PATH %s] [VFS %d] [FD %d]", vf->getPath(), vf->getVFS(), ret);
            vf->vlseek(vfd.get(), 0, SEEK_SET);
        } else {
            virtualFileDescribeSet::getVFDSet().reset(ret);
            /******through this way to release vfd *********/
            virtualFileDescribeSet::getVFDSet().release(pvfd);
            /***********************************************/

            if(_Errno < 0)
            {
                //这种情况需要让openat 返回失败
                /*originalInterface::original_close(ret);
                ret = -1;
                errno = EACCES;

                if(flags & O_CREAT)
                {
                    originalInterface::original_unlinkat(AT_FDCWD, relocated_path, 0);
                }

                LOGE("judge : **** force openat fail !!! ****");*/
            }
        }
    }

    return ret;
}

//int dup3(int oldfd, int newfd, int flags);
HOOK_DEF(int, dup3, int oldfd, int newfd, int flags)
{
    return syscall(__NR_dup3, oldfd, newfd, flags);
}

HOOK_DEF(int, fcntl, int fd, int cmd, ...) {
    va_list arg;
    int ret = -1;
    va_start (arg, cmd);
    switch (cmd) {
        case F_DUPFD:
        case F_DUPFD_CLOEXEC: {
            int target = va_arg (arg, int);
            ret = syscall(__NR_fcntl, fd, cmd, target);

            if (getApiLevel() >= 29 && ret > 0) {
                xdja::zs::sp<virtualFileDescribe> oldVfd(
                        virtualFileDescribeSet::getVFDSet().get(ret));
                if (oldVfd.get() != nullptr) {
                    virtualFileDescribeSet::getVFDSet().reset(ret);
                    xdja::zs::sp<virtualFile> vf(oldVfd->_vf->get());
                    if (vf.get() != nullptr) {
                        virtualFileManager::getVFM().releaseVF(vf->getPath(), oldVfd.get());
                    }
                    oldVfd.get()->decStrong(0);
                }
            }

            zString path;
            getPathFromFd(ret, path);

            if (ret > 0 && (is_TED_Enable() || changeDecryptState(false, 1)) &&
                isEncryptPath(path.toString())) {
                /*******************only here**********************/
                virtualFileDescribe *pvfd = new virtualFileDescribe(ret);
                pvfd->incStrong(0);
                /***************************************************/
                xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

                int _Errno;
                xdja::zs::sp<virtualFile> vf(
                        virtualFileManager::getVFM().getVF(vfd.get(), path.toString(), &_Errno));

                virtualFileDescribeSet::getVFDSet().set(ret, pvfd);

                if (vf.get() != nullptr) {
                    LOGE("judge : fcntl vf [PATH %s] [VFS %d] [FD %d]", vf->getPath(), vf->getVFS(),
                         ret);
                    vf->vlseek(vfd.get(), 0, SEEK_SET);
                } else {
                    virtualFileDescribeSet::getVFDSet().reset(ret);
                    /******through this way to release vfd *********/
                    virtualFileDescribeSet::getVFDSet().release(pvfd);
                    /***********************************************/
                }
            }
            va_end(arg);
        }
            break;
        default:
            void * target = va_arg(arg, void*);
            ret = orig_fcntl(fd, cmd, target);
            va_end(arg);
            break;
    }

    return ret;
}

HOOK_DEF(int, getaddrinfo,const char *__node, const char *__service, const struct addrinfo *__hints,
         struct addrinfo **__result) {
    int ret = -1;
    if (__node != nullptr) {
        if (getNetWorkState()) {
            if(isWhiteList()) {
                if(isIPAddress(__node) || isContainsStr(__node,":")) {
                    if(isIPAddress(__node)) {
                        if(isIpV4Enable(__node)) {
                            ret = originalInterface::original_getaddrinfo(__node, __service, __hints, __result);
                            //log("wkw getaddrinfo: node %s ret %d",__node,ret);
                            return ret;
                        }
                    } else if(isContainsStr(__node,":")) {
                        if(isIpV6Enable(__node)) {
                            ret = originalInterface::original_getaddrinfo(__node, __service, __hints, __result);
                            //log("wkw getaddrinfo: node %s ret %d",__node,ret);
                            return ret;
                        }
                    }
                    errno = EAI_FAIL;
                    //log("wkw getaddrinfo: node %s ret %d",__node,ret);
                    return ret;
                }

                if (!isDomainEnable(__node)) {
                    errno = EAI_FAIL;
                    return ret;
                }
                ret = originalInterface::original_getaddrinfo(__node, __service, __hints, __result);
                if (ret == 0) {
                    struct addrinfo *add_result = (*__result);
                    do {
                        if (add_result->ai_addr->sa_family == AF_INET) {
                            sockaddr_in *pSin = (sockaddr_in *) (add_result->ai_addr);
                            char *ipv4 = inet_ntoa(pSin->sin_addr);
                            //log("wkw getaddrinfo ipv4 %s domain %s", ipv4, __node);
                            addWhiteIpStrategy(ipv4);
                        } else if (add_result->ai_addr->sa_family == AF_INET6) {
                            sockaddr_in6 sin6;
                            memcpy(&sin6, add_result->ai_addr, sizeof(sin6));
                            char ipv6[INET6_ADDRSTRLEN];
                            inet_ntop(AF_INET6, &sin6.sin6_addr, ipv6, sizeof(ipv6));
                            //log("wkw getaddrinfo ipv6 %s domain %s", ipv6, __node);
                            addWhiteIpStrategy(ipv6);
                        }
                        add_result = add_result->ai_next;
                    } while (add_result != nullptr);
                }
                return ret;
            } else {
                if(isIPAddress(__node) || isContainsStr(__node,":")) {
                    if(isIPAddress(__node)) {
                        if(!isIpV4Enable(__node)) {
                            errno = EAI_FAIL;
                            //log("wkw getaddrinfo: node %s ret %d",__node,ret);
                            return ret;
                        }
                    } else if(isContainsStr(__node,":")) {
                        if(!isIpV6Enable(__node)) {
                            errno = EAI_FAIL;
                            //log("wkw getaddrinfo: node %s ret %d",__node,ret);
                            return ret;
                        }
                    }
                    ret = originalInterface::original_getaddrinfo(__node, __service, __hints, __result);
                    //log("wkw getaddrinfo: node %s ret %d",__node,ret);
                    return ret;
                }

                ret = originalInterface::original_getaddrinfo(__node, __service, __hints, __result);
                if(!isDomainEnable(__node)) {
                    struct addrinfo *add_result = (*__result);
                    do {
                        if (add_result->ai_addr->sa_family == AF_INET) {
                            sockaddr_in *pSin = (sockaddr_in *) (add_result->ai_addr);
                            char *ipv4 = inet_ntoa(pSin->sin_addr);
                            //log("wkw getaddrinfo ipv4 %s domain %s", ipv4, __node);
                            addWhiteIpStrategy(ipv4);
                        } else if (add_result->ai_addr->sa_family == AF_INET6) {
                            sockaddr_in6 sin6;
                            memcpy(&sin6, add_result->ai_addr, sizeof(sin6));
                            char ipv6[INET6_ADDRSTRLEN];
                            inet_ntop(AF_INET6, &sin6.sin6_addr, ipv6, sizeof(ipv6));
                            //log("wkw getaddrinfo ipv6 %s domain %s", ipv6, __node);
                            addWhiteIpStrategy(ipv6);
                        }
                        add_result = add_result->ai_next;
                    } while (add_result != nullptr);
                    errno = EAI_FAIL;
                    return -1;
                }
                return ret;
            }
        }
    }
    ret = originalInterface::original_getaddrinfo(__node, __service, __hints, __result);
    return ret;
}

HOOK_DEF(ssize_t, sendto, int fd, const void *buf, size_t n, int flags, struct sockaddr *dst_addr,
         socklen_t dst_addr_length) {
    ssize_t ret = -1;
    if (getNetWorkState()) {
        if (nullptr != dst_addr) {
            if (dst_addr->sa_family == AF_INET) {
                sockaddr_in *pSin = (sockaddr_in *) dst_addr;
                char *ipv4 = inet_ntoa(pSin->sin_addr);
                //log("wkw sendto [ipv4 %s]", ipv4);
                if (!isIpV4Enable(ipv4)) {
                    //log("return [ret %d] ",ret);
                    errno = EACCES;
                    return ret;
                }
            } else if (dst_addr->sa_family == AF_INET6) {
                sockaddr_in6 sin6;
                memcpy(&sin6, dst_addr, sizeof(sin6));
                char ipv6[INET6_ADDRSTRLEN];
                inet_ntop(AF_INET6, &sin6.sin6_addr, ipv6, sizeof(ipv6));
                //log("wkw sendto ipv6:%s", ipv6);
                if (!isIpV6Enable(ipv6)) {
                    errno = EACCES;
                    return ret;
                }
            }
        }
    }
    ret = syscall(__NR_sendto, fd, buf, n, flags, dst_addr, dst_addr_length);
    return ret;
}

HOOK_DEF(int, connect, int sd, struct sockaddr *addr, socklen_t socklen) {
    int ret = -1;
    if (getNetWorkState()) {
        if (addr->sa_family == AF_INET) {
            sockaddr_in *pSin = (sockaddr_in *) addr;
            char *ipv4 = inet_ntoa(pSin->sin_addr);
            //int port = pSin->sin_port;
            //log("wkw connect [ipv4 %s]", ipv4);
            if (!isIpV4Enable(ipv4)) {
                //log("return [ret %d] ENETUNREACH",ret);
                errno = ENETUNREACH;//无法传送数据包至指定的主机.
                return ret;
            }
        } else if (addr->sa_family == AF_INET6) {
            sockaddr_in6 sin6;
            memcpy(&sin6, addr, sizeof(sin6));
            char ipv6[INET6_ADDRSTRLEN];
            inet_ntop(AF_INET6, &sin6.sin6_addr, ipv6, sizeof(ipv6));
            //log("wkw connect ipv6:%s", ipv6);
            if (!isIpV6Enable(ipv6)) {
                errno = ENETUNREACH;//无法传送数据包至指定的主机.
                return ret;
            }
        }
    }

    ret = syscall(__NR_connect, sd, addr, socklen);
    return ret;
}

HOOK_DEF(void, xlogger_Write, void* _info, const char* _log)
{
    slog_wx("%s", _log);

    orig_xlogger_Write(_info, _log);
}


__END_DECLS
// end IO DEF


void onSoLoaded(const char *name, void *handle) {
}

bool relocate_linker(const char* LINKER_PATH) {
    intptr_t linker_addr, dlopen_off, symbol;
    if ((linker_addr = get_addr(LINKER_PATH)) == 0) {
        ALOGE("Cannot found linker addr.");
        return false;
    }
    if (resolve_symbol(LINKER_PATH, "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv",
                       &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_do_dlopen_CIVV,
                       (void **) &orig_do_dlopen_CIVV);
        return true;
    } else if (resolve_symbol(LINKER_PATH, "__dl__Z9do_dlopenPKciPK17android_dlextinfoPv",
                              &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_do_dlopen_CIVV,
                       (void **) &orig_do_dlopen_CIVV);
        return true;
    } else if (resolve_symbol(LINKER_PATH, "__dl__ZL10dlopen_extPKciPK17android_dlextinfoPv",
                              &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_do_dlopen_CIVV,
                       (void **) &orig_do_dlopen_CIVV);
        return true;
    } else if (
            resolve_symbol(LINKER_PATH, "__dl__Z20__android_dlopen_extPKciPK17android_dlextinfoPKv",
                           &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_do_dlopen_CIVV,
                       (void **) &orig_do_dlopen_CIVV);
        return true;
    } else if (
            resolve_symbol(LINKER_PATH, "__dl___loader_android_dlopen_ext",
                           &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_do_dlopen_CIVV,
                       (void **) &orig_do_dlopen_CIVV);
        return true;
    } else if (resolve_symbol(LINKER_PATH, "__dl__Z9do_dlopenPKciPK17android_dlextinfo",
                              &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_do_dlopen_CIV,
                       (void **) &orig_do_dlopen_CIV);
        return true;
    } else if (resolve_symbol(LINKER_PATH, "__dl__Z8__dlopenPKciPKv",
                              &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_do_dlopen_CIV,
                       (void **) &orig_do_dlopen_CIV);
        return true;
    } else if (resolve_symbol(LINKER_PATH, "__dl___loader_dlopen",
                              &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_do_dlopen_CIV,
                       (void **) &orig_do_dlopen_CIV);
        return true;
    } else if (resolve_symbol(LINKER_PATH, "__dl_dlopen",
                              &dlopen_off) == 0) {
        symbol = linker_addr + dlopen_off;
        MSHookFunction((void *) symbol, (void *) new_dlopen_CI,
                       (void **) &orig_dlopen_CI);
        return true;
    }
    return false;
}

bool on_found_linker_syscall_arm(const char *path, int num, void *func) {
    switch (num) {
        case __NR_openat:
            MSHookFunction(func, (void *) new___openat, (void **) &orig___openat);
            break;
        case __NR_open:
            MSHookFunction(func, (void *) new___open, (void **) &orig___open);
            break;
    }
    return CONTINUE_FIND_SYSCALL;
}

void startIOHook(int api_level) {
    void *handle = dlopen("libc.so", RTLD_NOW);

    if (handle) {
        HOOK_SYMBOL(handle, faccessat);
        HOOK_SYMBOL(handle, __openat);
        HOOK_SYMBOL(handle, fchmodat);
        HOOK_SYMBOL(handle, fchownat);
        HOOK_SYMBOL(handle, renameat);
        HOOK_SYMBOL(handle, fstatat64);
        HOOK_SYMBOL(handle, __statfs);
        HOOK_SYMBOL(handle, __statfs64);
        HOOK_SYMBOL(handle, mkdirat);
        HOOK_SYMBOL(handle, mknodat);
        HOOK_SYMBOL(handle, truncate);
        HOOK_SYMBOL(handle, linkat);
        HOOK_SYMBOL(handle, readlinkat);
        HOOK_SYMBOL(handle, unlinkat);
        HOOK_SYMBOL(handle, symlinkat);
        HOOK_SYMBOL(handle, utimensat);
        HOOK_SYMBOL(handle, __getcwd);
        HOOK_SYMBOL(handle, chdir);
        HOOK_SYMBOL(handle, execve);
        HOOK_SYMBOL(handle, kill);
        HOOK_SYMBOL(handle, vfork);
        HOOK_SYMBOL(handle, access);
        HOOK_SYMBOL(handle, stat);
        HOOK_SYMBOL(handle, lstat);
        HOOK_SYMBOL(handle, fstatat);
//        HOOK_SYMBOL(handle, __getdents64);
        HOOK_SYMBOL(handle, close);
        HOOK_SYMBOL(handle, read);
        HOOK_SYMBOL(handle, write);
        HOOK_SYMBOL(handle, __mmap2);
        HOOK_SYMBOL(handle, munmap);
        HOOK_SYMBOL(handle, pread64);
        HOOK_SYMBOL(handle, pwrite64);
        HOOK_SYMBOL(handle, fstat);
        HOOK_SYMBOL(handle, __llseek);
        HOOK_SYMBOL(handle, lseek);
        HOOK_SYMBOL(handle, ftruncate64);
        HOOK_SYMBOL(handle, sendfile);
        HOOK_SYMBOL(handle, sendfile64);
        HOOK_SYMBOL(handle, dup);
        HOOK_SYMBOL(handle, dup3);
        HOOK_SYMBOL(handle, fcntl);
        HOOK_SYMBOL(handle,getaddrinfo);
        HOOK_SYMBOL(handle,sendto);
#if defined(__i386__) || defined(__x86_64__)
        HOOK_SYMBOL2(handle, connect, new_connect2, orig_connect2);
#else
        HOOK_SYMBOL(handle, connect);
#endif
        HOOK_SYMBOL(handle, msync);
        if (api_level <= 20) {
            HOOK_SYMBOL(handle, access);
            HOOK_SYMBOL(handle, __open);
            HOOK_SYMBOL(handle, chmod);
            HOOK_SYMBOL(handle, chown);
            HOOK_SYMBOL(handle, rename);
            HOOK_SYMBOL(handle, rmdir);
            HOOK_SYMBOL(handle, mkdir);
            HOOK_SYMBOL(handle, mknod);
            HOOK_SYMBOL(handle, link);
            HOOK_SYMBOL(handle, unlink);
            HOOK_SYMBOL(handle, readlink);
            HOOK_SYMBOL(handle, symlink);
        }
#ifdef __arm__
        const char* LINKER_PATH = api_level > 28 ? LINKER_PATH_Q : LINKER_PATH_L;
        if (!relocate_linker(LINKER_PATH)) {
            findSyscalls(LINKER_PATH, on_found_linker_syscall_arm);
        }
#endif
        dlclose(handle);
    }
    originalInterface::original_lseek = orig_lseek;
    originalInterface::original_llseek = orig___llseek;
    originalInterface::original_fstat = orig_fstat;
    originalInterface::original_pwrite64 = orig_pwrite64;
    originalInterface::original_pread64 = orig_pread64;
    originalInterface::original_ftruncate64 = orig_ftruncate64;
    originalInterface::original_sendfile = orig_sendfile;
    originalInterface::original_getaddrinfo = orig_getaddrinfo;
}


void
IOUniformer::startUniformer(const char *so_path, const char *so_path_64, const char *native_path,
                            int api_level,
                            int preview_api_level) {
    bool ret = ff_Recognizer::getFFR().init(getMagicPath());
    LOGE("FFR path %s init %s", getMagicPath(), ret ? "success" : "fail");
    char api_level_chars[56];
    char pre_api_level_chars[56];
    setenv("V_SO_PATH", so_path, 1);
//    setenv("V_SO_PATH_64", so_path_64, 1);
    sprintf(api_level_chars, "%i", api_level);
    setenv("V_API_LEVEL", api_level_chars, 1);
    sprintf(pre_api_level_chars, "%i", preview_api_level);
    setenv("V_PREVIEW_API_LEVEL", pre_api_level_chars, 1);
    setenv("V_API_LEVEL", api_level_chars, 1);
    setenv("V_NATIVE_PATH", native_path, 1);
    startIOHook(api_level);
}
