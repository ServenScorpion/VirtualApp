/*
 * =====================================================================================
 *
 *       Filename:  util.h
 *
 *    Description:  
 *
 *        Created:  2017-11-14 11:09
 *
 *         Author:  limingle
 *          Email:  lml@xdja.com
 *   Organization:  XDJA Tech.CO.ltd.
 *
 * =====================================================================================
 */
#ifndef UTIL_H

#define UTIL_H

//#include <cstddef>
#include <cstdlib>
bool copy_within_block(const void *from, void *to, size_t bytes, off_t offset, size_t block_size);
void hash_to(const void *from, void *to, size_t from_len);
void XOR_TO(const char *left, const char *right, char *to, size_t len);
#endif /* end of include guard: UTIL_H */
