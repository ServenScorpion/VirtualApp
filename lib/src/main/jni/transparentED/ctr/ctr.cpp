/*
 * =====================================================================================
 *
 *       Filename:  ctr.cpp
 *
 *    Description:  
 *
 *        Created:  2017-11-13 20:10
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#include "ctr.h"
#include "util.h"
#include <strings.h>

#include <cstdio>
namespace xdja {
    CTR::CTR(const crypter &c):m_crypter(c),m_block_size(c.getBlockSize())
    {
        m_nonce = calloc(1, m_crypter.getBlockSize() / 2);
        m_key = calloc(1, c.getKeySize());
    }

    bool CTR::setNonce(const char *nonce)
    {
        bool ret = false;
        size_t nonce_len = m_crypter.getBlockSize() / 2;
        size_t nonce_offset = 0;
        size_t counter_offset = m_crypter.getBlockSize() / 2;
        size_t nonce_block_size = m_crypter.getBlockSize() / 2;
        const void *from = (const void *)nonce;
        void *to = m_nonce;
        if (copy_within_block(from, to, nonce_len, nonce_offset, nonce_block_size))
        {
            ret = true;
        }
        else
        {
            ret = false;
        }
        return ret;
    }

    bool CTR::setKey(const char *key)
    {
        size_t key_len = m_crypter.getKeySize();
        size_t key_offset = 0;
        size_t key_block_size = key_len;
        const void *from = (const void *)key;
        void *to = m_key;
        return copy_within_block(from, to, key_len, key_offset, key_block_size);
    }

    bool CTR::encrypt(const char *from, char *to, size_t n, uint64_t offset)
    {
        bool ret = false;

        size_t counter_len = m_crypter.getBlockSize();
        char *counter = (char *)calloc(1, counter_len);
        if (counter == NULL)
        {
            return false;
        }
        do {
            size_t left_bytes = n;
            char *result = (char *)calloc(1, m_crypter.getBlockSize());
            if (result == NULL)
            {
                return false;
            }

            while (left_bytes > 0)
            {
                uint64_t curr_block_num = offset / m_block_size;
                uint64_t offset_in_curr_block = offset - curr_block_num * m_block_size;
                size_t need_bytes = m_block_size - offset_in_curr_block;
                if (left_bytes < need_bytes)
                {
                    need_bytes = left_bytes;
                }
                // set nonce
                {
                    size_t nonce_len = counter_len / 2;
                    size_t nonce_offset = 0;
                    size_t counter_offset = counter_len / 2;
                    size_t nonce_block_size = counter_len / 2;
                    const void *from = (const void *)m_nonce;
                    void *to = (void *)(counter + counter_offset);
                    if (!copy_within_block(from, to, nonce_len, nonce_offset, nonce_block_size))
                    {
                        if (counter != NULL)
                        {
                            free(counter);
                        }
                        if (result != NULL)
                        {
                            free(result);
                        }
                        return false;
                    }
                }

                // set block num
                {
                    size_t block_num_len = counter_len / 2;
                    size_t counter_offset = 0;
                    uint64_t block_num = (offset / m_block_size);
                    const void *from = &block_num;
                    void *to = (void *)(counter + counter_offset);
                    hash_to(from, to, block_num_len);
                }

                m_crypter.encrypt(counter, result, (const char *)m_key);
                {
                    size_t cipher_block_size = m_crypter.getBlockSize();
                    size_t cipher_left_bytes = need_bytes;
                    while (cipher_left_bytes > 0)
                    {
                        size_t cipher_block_num = offset_in_curr_block / cipher_block_size;
                        uint64_t offset_in_cipher_block = offset_in_curr_block - cipher_block_num * cipher_block_size;
                        size_t cipher_need_bytes = cipher_block_size - offset_in_cipher_block;
                        if (cipher_left_bytes < cipher_need_bytes)
                        {
                            cipher_need_bytes = cipher_left_bytes;
                        }
                        XOR_TO(result + offset_in_cipher_block, from, to, cipher_need_bytes);
                        to = to + cipher_need_bytes;
                        from = from + cipher_need_bytes;
                        cipher_left_bytes = cipher_left_bytes - cipher_need_bytes;
                        offset_in_curr_block += cipher_need_bytes;

                        left_bytes = left_bytes - cipher_need_bytes;
                        offset = offset + cipher_need_bytes;
                    }
                }
               ret = true;
            }
            if (result != NULL)
            {
                free(result);
            }
        } while (0);

        if (counter != NULL)
        {
            free(counter);
        }

        return ret;
    }

    bool CTR::setBlockSize(size_t block_size)
    {
        if ((block_size & (block_size - 1)) != 0)
        {
            return false;
        }
        m_block_size = block_size;
        return true;
    }


    size_t CTR::getCipherBlockSize()
    {
        return m_crypter.getBlockSize();
    }

    CTR::~CTR()
    {
        if (m_key)
        {
            free(m_key);
        }

        if (m_nonce)
        {
            free(m_nonce);
        }
    }
} /* xdja */
