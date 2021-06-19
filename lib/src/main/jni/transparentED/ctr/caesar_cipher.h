/*
 * =====================================================================================
 *
 *       Filename:  caesar_cipher.h
 *
 *    Description:  
 *
 *        Created:  2017-11-14 21:25
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#ifndef CAESAR_CIPHER_H

#define CAESAR_CIPHER_H

#include "crypter.h"

namespace xdja {
    class caesar_cipher : public crypter {
    public:
        caesar_cipher ();
        size_t getKeySize() const;
        size_t getBlockSize() const;
        bool encrypt(const char *from, char *to, const char *key) const;
        virtual ~caesar_cipher ();
    
    private:
        /* data */
    };
} /* xdja */
#endif /* end of include guard: CAESAR_CIPHER_H */
