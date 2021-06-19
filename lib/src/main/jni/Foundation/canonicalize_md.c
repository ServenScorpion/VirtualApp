/*
 * Copyright (c) 1994, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * Pathname canonicalization for Unix file systems
 * http://and.rlib.cf/9.0.0_r3/xref/libcore/ojluni/src/main/native/canonicalize_md.c
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#if !defined(_ALLBSD_SOURCE)
#include <alloca.h>

#endif

/* Note: The comments in this file use the terminology
         defined in the java.io.File class */


/* Check the given name sequence to see if it can be further collapsed.
   Return zero if not, otherwise return the number of names in the sequence. */

static inline int
collapsible(const char *const names, int *const duplicate) {
    const char *p = names;
    int dots = 0, n = 0;

    // e.g. '//data'
    while (*p == '/') {
        ++p;
        *duplicate = 1;
    }

    while (*p) {
        if ((p[0] == '.') && ((p[1] == '\0')
                              || (p[1] == '/')
                              || ((p[1] == '.') && ((p[2] == '\0')
                                                    || (p[2] == '/'))))) {
            dots = 1;
        }
        ++n;
        while (*p) {
            if (*p == '/') {
                while (*++p == '/') {
                    *duplicate = 1;
                }
                break;
            }
            ++p;
        }
    }
    return (dots ? n : 0);
}


/* Split the names in the given name sequence,
   replacing slashes with nulls and filling in the given index array */

static inline void
splitNames(char *p, char **ix)
{
    int i = 0;
    while (*p) {
        ix[i++] = p++;
        while (*p) {
            if (*p == '/') {
                *p = '\0';
                while (*(++p) == '/') {
                }
                break;
            }
            ++p;
        }
    }
}


/* Join the names in the given name sequence, ignoring names whose index
   entries have been cleared and replacing nulls with slashes as needed */

static inline void
joinNames(char *const names, int nc, char **ix)
{
    int i;
    char *p;

    for (i = 0, p = names; i < nc; ++i) {
        if (!ix[i]) continue;
        if (i > 0) {
            p[-1] = '/';
        }
        if (p == ix[i]) {
            p += strlen(p) + 1;
        } else {
            char *q = ix[i];
            while ((*p++ = *q++));
        }
    }
    *p = '\0';
}


/* Collapse "." and ".." names in the given path wherever possible.
   A "." name may always be eliminated; a ".." name may be eliminated if it
   follows a name that is neither "." nor "..".  This is a syntactic operation
   that performs no filesystem queries, so it should only be used to cleanup
   after invoking the realpath() procedure. */

static inline const char *
collapse(const char *const original, char *const path, size_t len) {
    int duplicate = 0;
    int nc = collapsible(original + 1, &duplicate);
    if (nc <= 1 && !duplicate) {
        return original; /* Nothing to do */
    }

    if (duplicate) {
        const char *p, *ls = original;
        char *s = path;
        while ((p = strstr(ls, "//")) != 0) {
            size_t slen = p - ls + 1u;
            if (len > slen) {
                memcpy(s, ls, slen * sizeof(char));
                len = len - slen;
            } else {
                break;
            }

            p = p + 2;
            while (*p == '/') {
                ++p;
            }
            s  = s + slen;
            ls = p;
        }

        if (ls[0] != '\0') {
            size_t slen = strlen(ls);
            if (len > slen) {
                memcpy(s, ls, slen * sizeof(char));
                s = s + slen;
            }
        }

        s[s != path && s[-1] == '/' ? -1 : 0] = '\0';
        if (nc <= 1) {
            return path;
        }
    } else {
        strncpy(path, original, len);
    }

    char *names = path + 1; /* Preserve first '/' */

    char **ix = (char **)alloca(nc * sizeof(char *));
    splitNames(names, ix);

    int i, j;
    for (i = 0; i < nc; ++i) {
        int dots = 0;

        /* Find next occurrence of "." or ".." */
        do {
            char *p = ix[i];
            if (p[0] == '.') {
                if (p[1] == '\0') {
                    dots = 1;
                    break;
                }
                if ((p[1] == '.') && (p[2] == '\0')) {
                    dots = 2;
                    break;
                }
            }
            ++i;
        } while (i < nc);
        if (i >= nc) break;

        /* At this point i is the index of either a "." or a "..", so take the
           appropriate action and then continue the outer loop */
        if (dots == 1) {
            /* Remove this instance of "." */
            ix[i] = 0;
        } else {
            /* If there is a preceding name, remove both that name and this
               instance of ".."; otherwise, leave the ".." as is!!! */
            for (j = i - 1; j >= 0; --j) {
                if (ix[j]) break;
            }
            if (j < 0) continue;
            ix[j] = 0;
            ix[i] = 0;
        }
        /* i will be incremented at the top of the loop */
    }

    joinNames(names, nc, ix);
    return path;
}


/* Convert a absolute pathname to canonical form.  The input path is assumed to contain
   no duplicate slashes. */

const char *
canonicalize_path(const char *const original, char *const resolved, size_t len)
{
    if (original[0] != '/') {
        return original;
    }

    return collapse(original, resolved, len);
}
