#ifndef _SYMBOL_H_
#define _SYMBOL_H_

#include <elf.h>

#if defined(__arm__)
typedef Elf32_Word elf_word;
typedef Elf32_Half elf_half;
typedef Elf32_Ehdr elf_ehdr;
typedef Elf32_Shdr elf_shdr;
typedef Elf32_Sym elf_sym;
#elif defined(__i386__)
typedef Elf32_Word elf_word;
typedef Elf32_Half elf_half;
typedef Elf32_Ehdr elf_ehdr;
typedef Elf32_Shdr elf_shdr;
typedef Elf32_Sym elf_sym;
#elif defined(__aarch64__)
typedef Elf64_Word elf_word;
typedef Elf64_Half elf_half;
typedef Elf64_Ehdr elf_ehdr;
typedef Elf64_Shdr elf_shdr;
typedef Elf64_Sym elf_sym;
#else
#error "Unsupported architecture"
#endif

int resolve_symbol(const char *, const char *, intptr_t *);

intptr_t get_addr(const char *name);

#endif /* _SYMBOL_H_ */