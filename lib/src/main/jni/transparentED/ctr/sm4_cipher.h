/*
 * =====================================================================================
 *
 *       Filename:  sm4_cipher.h
 *
 *    Description:
 *
 *        Created:  2017-12-20 11:18
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#ifndef SM4_CIPHER_H

#define SM4_CIPHER_H
#include "crypter.h"

namespace xdja {
    class sm4_cipher : public crypter {
    public:
        sm4_cipher ();
        size_t getKeySize() const;
        size_t getBlockSize() const;
        bool encrypt(const char *from, char *to, const char *key) const;
        virtual ~sm4_cipher ();

    private:
        /* data */
    };
} /* xdja */

#endif /* end of include guard: SM4_CIPHER_H */
