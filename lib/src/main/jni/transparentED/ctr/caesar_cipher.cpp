/*
 * =====================================================================================
 *
 *       Filename:  caesar_cipher.cpp
 *
 *    Description:  
 *
 *        Created:  2017-11-14 21:31
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#include "caesar_cipher.h"

namespace xdja {
    caesar_cipher::caesar_cipher()
    {
    }

    size_t caesar_cipher::getKeySize() const
    {
        return (size_t)16;
    }

    size_t caesar_cipher::getBlockSize() const
    {
        return (size_t)16;
    }

    bool caesar_cipher::encrypt(const char *from, char *to, const char *key) const
    {
        int i;
        size_t len = getBlockSize();
        for (i = 0; i < len; i++) {
            to[i] = from[i] + key[i];
        }

        return true;
    }

    caesar_cipher::~caesar_cipher()
    {
    }
    
} /* xdja */
