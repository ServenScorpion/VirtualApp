/*
 * =====================================================================================
 *
 *       Filename:  crypter.h
 *
 *    Description:  
 *
 *        Created:  2017-11-13 18:29
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#ifndef CRYPTER_H
 
#define CRYPTER_H
 
#include <cstddef>

namespace xdja {
    class crypter {
    public:
        crypter ();
        virtual size_t getKeySize() const = 0 ;
        virtual size_t getBlockSize() const = 0;
        virtual bool encrypt(const char *from, char *to, const char *key) const = 0;
        virtual ~crypter ();

    private:

    };
} /* xdja */

#endif /* end of include guard: CRYPTER_H */
