#ifndef SANDBOX_FS_H
#define SANDBOX_FS_H

#include <string>
#include <errno.h>

#define KEY_MAX 256

typedef struct PathItem {
    char *path;
    bool is_folder;
    size_t size;
} PathItem;

typedef struct ReplaceItem {
    char *orig_path;
    size_t orig_size;
    char *new_path;
    size_t new_size;
    bool is_folder;
} ReplaceItem;

enum RelocateResult {
    MATCH,
    NOT_MATCH,
    FORBID,
    KEEP
};

bool isReadOnly(const char * path);

const char *relocate_path(const char *path, char *const buffer, const size_t size);

const char *reverse_relocate_path(const char *path, char *const buffer, const size_t size);

int reverse_relocate_path_inplace(char *const path, const size_t size);

int add_keep_item(const char *path);

int add_forbidden_item(const char *path);

int add_replace_item(const char *orig_path, const char *new_path);

int add_forbidden_item(const char *path);

int add_readonly_item(const char *path);

PathItem *get_keep_items();

PathItem *get_forbidden_items();

ReplaceItem *get_replace_items();

ReplaceItem *get_readonly_items();

int get_keep_item_count();

int get_forbidden_item_count();

int get_replace_item_count();


#endif //SANDBOX_FS_H
