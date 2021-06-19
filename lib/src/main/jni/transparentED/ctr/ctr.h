/*
 * =====================================================================================
 *
 *       Filename:  ctr.h
 *
 *    Description:  
 *
 *        Created:  2017-11-13 17:39
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */

#ifndef CTR_H

#define CTR_H
#include "crypter.h"
#include <stdint.h>

namespace xdja {
    class CTR {
    public:
        CTR (const crypter &c);
        bool encrypt(const char *from, char *to, size_t n, uint64_t offset);
        bool setBlockSize(size_t block_size);
        bool setNonce(const char *nonce);
        bool setKey(const char *key);
        size_t getCipherBlockSize();
        virtual ~CTR ();

        void output();
    
    private:
        const crypter &m_crypter;
        void *m_nonce;
        size_t m_block_size;
        void *m_key;
    };
} /* xdja */

#endif /* end of include guard: CTR_H */
