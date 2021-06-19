/*
 * =====================================================================================
 *
 *       Filename:  util.cpp
 *
 *    Description:  
 *
 *        Created:  2017-11-14 11:10
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#include "util.h"
#include "SpookyV2.h"
#include <strings.h>

#include <cstdio>
bool copy_within_block(const void *from, void *to, size_t bytes, off_t offset, size_t block_size)
{
    bool ret = false;
    do {
        if (from == NULL || to == NULL || offset + bytes > block_size)
        {
            break;
        }

        const void *src = ((const char *)from + offset);
        void *dest = to;
        bcopy(src, dest, bytes);

        ret = true;
    } while (0);
    return ret;
}

void hash_to(const void *from, void *to, size_t to_length)
{
    uint64_t seq = *(uint64_t *)from;
    if (to_length == 4)
    {
        uint32_t h = SpookyHash::Hash32(from, 8, 0);
        {
            //printf("Hash32(%lu):%u\n", seq, h); 
        }
        *(uint32_t *)to = h;
    }
    else if (to_length == 8)
    {
        uint64_t h = SpookyHash::Hash64(from, 8, 0);
        {
            //printf("Hash64(%lu):%lu\n", seq, h); 
        }


        *(uint64_t *)to = h;
    }
    else
    {
        bcopy(from, to, to_length);
    }
}

void XOR_TO(const char *left, const char *right, char *to, size_t len)
{
    int i;
    for (i = 0; i < len; i++) {
        to[i] = left[i] ^ right[i];
    }
}
