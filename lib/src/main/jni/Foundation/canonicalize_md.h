//
// VirtualApp Native Project
//

#ifndef FOUNDATION_PATH
#define FOUNDATION_PATH

#include <unistd.h>
#include <stdlib.h>
#include <limits.h>
#include <string.h>
#include <sys/stat.h>
#include <syscall.h>

extern "C" const char *
canonicalize_path(const char *original, char *resolved, size_t len);

#endif // FOUNDATION_PATH
