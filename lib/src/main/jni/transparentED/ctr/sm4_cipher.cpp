/*
 * =====================================================================================
 *
 *       Filename:  sm4_cipher.cpp
 *
 *    Description:
 *
 *        Created:  2017-12-20 11:32
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */

#include <stdlib.h>
#include <string.h>
#include "sm4_cipher.h"
#include "sm4.h"

namespace xdja {
    sm4_cipher::sm4_cipher()
    {
    }

    size_t sm4_cipher::getKeySize() const
    {
        return (size_t)16;
    }

    size_t sm4_cipher::getBlockSize() const
    {
        return (size_t)16;
    }

    bool sm4_cipher::encrypt(const char *from, char *to, const char *key) const
    {
        bool ret = false;
        do {
            if (from == NULL || to == NULL || key == NULL)
            {
                break;
            }

            sm4_context ctx;
            sm4_setkey_enc(&ctx, (unsigned char *)key);
            sm4_crypt_ecb(&ctx, 1, 16, (unsigned char *)from, (unsigned char *)to);
            ret = true;
        } while (false);

        return ret;
    }

    sm4_cipher::~sm4_cipher()
    {
    }
} /* xdja */
