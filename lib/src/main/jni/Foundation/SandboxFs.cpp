#include <stdlib.h>
#include <cstring>
#include "SandboxFs.h"
#include "canonicalize_md.h"
#include "Log.h"

PathItem *keep_items;
PathItem *forbidden_items;
PathItem *readonly_items;
ReplaceItem *replace_items;
int keep_item_count;
int forbidden_item_count;
int readonly_item_count;
int replace_item_count;

int add_keep_item(const char *path) {
    char keep_env_name[KEY_MAX];
    sprintf(keep_env_name, "V_KEEP_ITEM_%d", keep_item_count);
    setenv(keep_env_name, path, 1);
    keep_items = (PathItem *) realloc(keep_items,
                                      keep_item_count * sizeof(PathItem) + sizeof(PathItem));
    PathItem &item = keep_items[keep_item_count];
    item.path = strdup(path);
    item.size = strlen(path);
    item.is_folder = (path[strlen(path) - 1] == '/');
    return ++keep_item_count;
}

int add_forbidden_item(const char *path) {
    char forbidden_env_name[KEY_MAX];
    sprintf(forbidden_env_name, "V_FORBID_ITEM_%d", forbidden_item_count);
    setenv(forbidden_env_name, path, 1);
    forbidden_items = (PathItem *) realloc(forbidden_items,
                                           forbidden_item_count * sizeof(PathItem) +
                                           sizeof(PathItem));
    PathItem &item = forbidden_items[forbidden_item_count];
    item.path = strdup(path);
    item.size = strlen(path);
    item.is_folder = (path[strlen(path) - 1] == '/');
    return ++forbidden_item_count;
}

int add_readonly_item(const char *path) {
    char readonly_env_name[KEY_MAX];
    sprintf(readonly_env_name, "V_READONLY_ITEM_%d", readonly_item_count);
    setenv(readonly_env_name, path, 1);
    readonly_items = (PathItem *) realloc(readonly_items,
                                          readonly_item_count * sizeof(PathItem) +
                                          sizeof(PathItem));
    PathItem &item = readonly_items[readonly_item_count];
    item.path = strdup(path);
    item.size = strlen(path);
    item.is_folder = (path[strlen(path) - 1] == '/');
    return ++readonly_item_count;
}

int add_replace_item(const char *orig_path, const char *new_path) {
    ALOGE("add replace item : %s -> %s", orig_path, new_path);
    char src_env_name[KEY_MAX];
    char dst_env_name[KEY_MAX];
    sprintf(src_env_name, "V_REPLACE_ITEM_SRC_%d", replace_item_count);
    sprintf(dst_env_name, "V_REPLACE_ITEM_DST_%d", replace_item_count);
    setenv(src_env_name, orig_path, 1);
    setenv(dst_env_name, new_path, 1);

    replace_items = (ReplaceItem *) realloc(replace_items,
                                            replace_item_count * sizeof(ReplaceItem) +
                                            sizeof(ReplaceItem));
    ReplaceItem &item = replace_items[replace_item_count];
    item.orig_path = strdup(orig_path);
    item.orig_size = strlen(orig_path);
    item.new_path = strdup(new_path);
    item.new_size = strlen(new_path);
    item.is_folder = (orig_path[strlen(orig_path) - 1] == '/');
    return ++replace_item_count;
}

PathItem *get_keep_items() {
    return keep_items;
}

PathItem *get_forbidden_items() {
    return forbidden_items;
}

PathItem *get_readonly_item() {
    return readonly_items;
}

ReplaceItem *get_replace_items() {
    return replace_items;
}

int get_keep_item_count() {
    return keep_item_count;
}

int get_forbidden_item_count() {
    return forbidden_item_count;
}

int get_replace_item_count() {
    return replace_item_count;
}

inline bool
match_path(bool is_folder, size_t size, const char *item_path, const char *path, size_t path_len) {
    if (is_folder) {
        if (path_len < size) {
            // ignore the last '/'
            return strncmp(item_path, path, size - 1) == 0 && item_path[size - 1] == '/';
        } else {
            return strncmp(item_path, path, size) == 0;
        }
    } else {
        return strcmp(item_path, path) == 0;
    }
}

bool isReadOnly(const char *path) {
    for (int i = 0; i < readonly_item_count; ++i) {
        PathItem &item = readonly_items[i];
        if (match_path(item.is_folder, item.size, item.path, path, strlen(path))) {
            return true;
        }
    }
    return false;
}

const char *relocate_path_internal(const char *path, char *const buffer, const size_t size) {
    if (NULL == path) {
        return path;
    }
    const char *orig_path = path;
    path = canonicalize_path(path, buffer, size);

    const size_t len = strlen(path);

    for (int i = 0; i < keep_item_count; ++i) {
        PathItem &item = keep_items[i];
        if (match_path(item.is_folder, item.size, item.path, path, len)) {
            return orig_path;
        }
    }

    for (int i = 0; i < forbidden_item_count; ++i) {
        PathItem &item = forbidden_items[i];
        if (match_path(item.is_folder, item.size, item.path, path, len)) {
            return NULL;
        }
    }

    for (int i = 0; i < replace_item_count; ++i) {
        ReplaceItem &item = replace_items[i];
        if (match_path(item.is_folder, item.orig_size, item.orig_path, path, len)) {
            if (len < item.orig_size) {
                // remove last /
                std::string relocated_path(item.new_path, 0, item.new_size - 1);
                return strdup(relocated_path.c_str());
            } else {
                const size_t remain_size = len - item.orig_size + 1u;
                if (size < item.new_size + remain_size) {
                    ALOGE("buffer overflow %u", static_cast<unsigned int>(size));
                    return NULL;
                }

                const char *const remain = path + item.orig_size;
                if (path != buffer) {
                    memcpy(buffer, item.new_path, item.new_size);
                    memcpy(buffer + item.new_size, remain, remain_size);
                } else {
                    void *const remain_temp = alloca(remain_size);
                    memcpy(remain_temp, remain, remain_size);
                    memcpy(buffer, item.new_path, item.new_size);
                    memcpy(buffer + item.new_size, remain_temp, remain_size);
                }
                return buffer;
            }
        }
    }
    return orig_path;
}

const char *relocate_path(const char *path, char *const buffer, const size_t size) {
    const char *result = relocate_path_internal(path, buffer, size);
    return result;
}

const char *reverse_relocate_path(const char *path, char *const buffer, const size_t size) {
    if (path == NULL) {
        return NULL;
    }
    path = canonicalize_path(path, buffer, size);

    const size_t len = strlen(path);
    for (int i = 0; i < keep_item_count; ++i) {
        PathItem &item = keep_items[i];
        if (match_path(item.is_folder, item.size, item.path, path, len)) {
            return path;
        }
    }

    for (int i = 0; i < replace_item_count; ++i) {
        ReplaceItem &item = replace_items[i];
        if (match_path(item.is_folder, item.new_size, item.new_path, path, len)) {
            if (len < item.new_size) {
                return item.orig_path;
            } else {
                const size_t remain_size = len - item.new_size + 1u;
                if (size < item.orig_size + remain_size) {
                    ALOGE("reverse buffer overflow %u", static_cast<unsigned int>(size));
                    return NULL;
                }

                const char *const remain = path + item.new_size;
                if (path != buffer) {
                    memcpy(buffer, item.orig_path, item.orig_size);
                    memcpy(buffer + item.orig_size, remain, remain_size);
                } else {
                    void *const remain_temp = alloca(remain_size);
                    memcpy(remain_temp, remain, remain_size);
                    memcpy(buffer, item.orig_path, item.orig_size);
                    memcpy(buffer + item.orig_size, remain_temp, remain_size);
                }
                return buffer;
            }
        }
    }

    return path;
}

int reverse_relocate_path_inplace(char *const path, const size_t size) {
    char path_temp[PATH_MAX];
    const char *redirect_path = reverse_relocate_path(path, path_temp, sizeof(path_temp));
    if (redirect_path) {
        if (redirect_path != path) {
            const size_t len = strlen(redirect_path) + 1u;
            if (len <= size) {
                memcpy(path, redirect_path, len);
            }
        }
        return 0;
    }
    return -1;
}