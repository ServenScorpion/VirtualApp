/*
 * =====================================================================================
 *
 *       Filename:  rng.cpp
 *
 *    Description:  
 *
 *        Created:  2017-11-13 20:44
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#include "rng.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstdlib>
#include <cstdio>


namespace xdja {
    static rng_t *r = NULL;
    rng_t::rng_t(const char * file):m_fd(open(file, O_RDONLY))
    {
        //printf("m_fd: %d\n", m_fd);
    }

    bool rng_t::getBytes(size_t bytes, void *buffer)
    {
        size_t done;
        ssize_t got;

        done = 0;
        unsigned char *p = (unsigned char *)buffer;

        while (done < bytes)
        {
            got = read(this->m_fd, p + done, bytes - done);
            if (got <= 0)
            {
                sleep(1);
                continue;
            }
            done += got;
        }

        return true;
    }

    bool rng_t::allocateBytes(size_t bytes, void **buffer)
    {
        *buffer = calloc(1, bytes);
        return getBytes(bytes, *buffer);
    }

    rng_t::~rng_t()
    {
        //printf("close fd: %d\n",m_fd);
        close(m_fd);
    }


    rng_t * rng_t::getRNG(const char *file)
    {
        if (r == NULL)
        {
            r = new rng_t(file);
        }
        return r;
    }
} /* xdja */
