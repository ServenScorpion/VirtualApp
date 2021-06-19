#include <cstring>
#include <cstdio>
#include <limits.h>
#include <unistd.h>
#include <stdlib.h>
#include <syscall.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include "MapsRedirector.h"
#include "Log.h"
#include "SandboxFs.h"

static int create_temp_file() {
    // TODO: O_RDONLY FAKE_PATH
    char pattern[PATH_MAX] = {0};
    char *cache_dir = getenv("V_NATIVE_PATH");
    int fd = open(pattern, O_RDWR | O_CLOEXEC | O_TMPFILE | O_EXCL, NULL);
    if (fd != -1) {
        return fd;
    }

    snprintf(pattern, sizeof(pattern), "%s/dev_maps_%d_%d", cache_dir, getpid(), gettid());
    fd = open(pattern, O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC, NULL);
    if (fd == -1) {
        ALOGE("fake_maps: cannot create tmp file, errno = %d", errno);
        return -1;
    }
    unlink(pattern);
    return fd;
}

static char *match_maps_item(char *line) {
    char *p = strstr(line, " /data/");
    return p;
}

static bool match_host_pkg(const char *path) {
    // TODO Host Pkg
    return strstr(path, "io.busniess.va") != NULL;
}

static void redirect_proc_maps_internal(const int fd, const int fake_fd) {
    char line[PATH_MAX];
    char *p = line, *e;
    size_t n = PATH_MAX - 1;
    ssize_t r;
    while ((r = TEMP_FAILURE_RETRY(read(fd, p, n))) > 0) {
        p[r] = '\0';
        p = line; // search begin at line start

        while ((e = strchr(p, '\n')) != NULL) {
            e[0] = '\0';

            char *path = match_maps_item(p);
            if (path != NULL) {
                ++path; // skip blank

                char temp[PATH_MAX];
                const char *real_path = reverse_relocate_path(path, temp, sizeof(temp));
                if (real_path != NULL && match_host_pkg(real_path)) {
                    ALOGE("remove map item: %s", p);
                    real_path = NULL;
                }

                write(fake_fd, p, path - p);
                if (real_path != NULL && !match_host_pkg(real_path)) {
                    write(fake_fd, real_path, strlen(real_path));
                }
                write(fake_fd, "\n", 1);
            } else {
                e[0] = '\n';
                write(fake_fd, p, e - p + 1);
            }

            p = e + 1;
        }
        if (p == line) { // !any_entry
            ALOGE("fake_maps: cannot process line larger than %u bytes!", PATH_MAX);
            goto __break;
        } //if

        const size_t remain = strlen(p);
        if (remain <= (PATH_MAX / 2)) {
            memcpy(line, p, remain * sizeof(p[0]));
        } else {
            memmove(line, p, remain * sizeof(p[0]));
        } //if

        p = line + remain;
        n = PATH_MAX - 1 - remain;
    }

    __break:
    return;
}

int redirect_proc_maps(const char *const pathname, const int flags, const int mode) {
    // '/proc/self/maps'
    if (strncmp(pathname, "/proc/", sizeof("/proc/") - 1) != 0) {
        return 0;
    }

    // 'self/maps'
    const char *s = pathname + sizeof("/proc/") - 1;

    // '/maps'
    const char *p = strstr(s, "/maps");
    if (p == NULL || *(p + sizeof("/maps") - 1) != '\0') {
        p = strstr(s, "/smaps");
        if (p == NULL || *(p + sizeof("/smaps") - 1) != '\0') {
            return 0;
        }
    }
    ALOGE("start redirect: %s", pathname);

    int fd = syscall(__NR_openat, AT_FDCWD, pathname, flags, mode);
    if (fd == -1) {
        errno = EACCES;
        return -1;
    }

    int fake_fd = create_temp_file();
    if (fake_fd == -1) {
        ALOGE("fake_maps: create_temp_file failed, errno = %d", errno);
        errno = EACCES;
        return -1;
    }

    redirect_proc_maps_internal(fd, fake_fd);
    lseek(fake_fd, 0, SEEK_SET);
    syscall(__NR_close, fd);

    ALOGI("fake_maps: faked %s -> fd %d", pathname, fake_fd);
    return fake_fd;
}