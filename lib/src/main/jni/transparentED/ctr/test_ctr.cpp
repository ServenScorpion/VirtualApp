#include <stdio.h>
#include "crypter.h"
#include "ctr.h"
#include "rng.h"
#include "caesar_cipher.h"
//#include "util.h"

#include <cstdlib>
#include <cstring>
using namespace xdja;

int main(int argc, const char *argv[])
{
    caesar_cipher cc;
    CTR ctr(cc);
    char *key = (char *)malloc(cc.getKeySize());
    //void *memset(void *s, int c, size_t n);
    memset(key, 3, cc.getKeySize());
    const char *p_key = key;
    ctr.setKey(p_key);
    ctr.setBlockSize(16);
  /*  
    rng_t *r = rng_t::getRNG("/dev/urandom");
    char *buf = NULL;
    size_t bytes = ctr.getCipherBlockSize() / 2;
    if (r->allocateBytes(bytes, (void **)&buf))
    {
        int i = 0;
        for (i = 0; i < bytes; i++) {
            printf("%02hhX ", buf[i]);
        }
        putchar('\n');
    };

    const char *nonce = buf;
    //ctr.setNonce(nonce);
    //ctr.output();
*/

    const char *plain = "abcdefghijklmnopqrstuvwxyz";
    //const char *plain = "aaaaaaaaaaaaaaaaaaaaaaaaaa";
    char *p = (char *)plain;

    size_t cipher_len = strlen(plain);
    char *cipher = (char *)calloc(1,cipher_len);
    char *decipher = (char *)calloc(1,cipher_len);
    {
        int i;
        for (i = 0; i < cipher_len; i++) {
            ctr.encrypt(plain+i, cipher+i, 1, i);
        }
    }

    //ctr.encrypt(plain, cipher, cipher_len, 0);

    {
        int i;
        for (i = 0; i < cipher_len; i++) {
            printf("%2c", cipher[i]);
        }
        putchar('\n');
    }

    printf("==================================\n");
    ctr.encrypt(cipher+4, decipher, cipher_len/2, 4);

    {
        int i;
        for (i = 0; i < cipher_len; i++) {
            printf("%2c", decipher[i]);
        }
        putchar('\n');
    }

    free(buf);
    free(cipher);
    free(decipher);
    return 0;
}
