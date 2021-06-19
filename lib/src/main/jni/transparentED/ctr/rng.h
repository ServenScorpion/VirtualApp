/*
 * =====================================================================================
 *
 *       Filename:  rng.h
 *
 *    Description:  
 *
 *        Created:  2017-11-13 20:33
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#ifndef RNG_H

#define RNG_H

#include <cstddef>
#include <cstdio>
#include <stdint.h>

namespace xdja {
    class rng_t {
    public:
        bool getBytes(size_t bytes, void *buffer);
        bool allocateBytes(size_t bytes, void **buffer);
        virtual ~rng_t ();
        static rng_t *getRNG(const char *file);
    
    private:
        rng_t (const char * file);
        int m_fd;
    };

} /* xdja */

#endif /* end of include guard: RNG_H */

