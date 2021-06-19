package com.lody.virtual.helper.dedex;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
public class Elf implements Closeable {
    // art/runtime/elf.h
    // Elf32 [Addr, Off, Word, Sword]=(int), [Half]=(short)
    // Elf64 [Addr, Off, Xword, Sxword]=(long), [Word]=(int), [Half]=(short)

    final static char ELF_MAGIC[] = {0x7f, 'E', 'L', 'F'};
    final static int EI_CLASS = 4; // File class
    final static int EI_DATA = 5; // Data encoding
    final static int EI_NIDENT = 16;
    final char[] e_ident = new char[EI_NIDENT]; // ELF Identification bytes

    final boolean checkMagic() {
        return e_ident[0] == ELF_MAGIC[0] && e_ident[1] == ELF_MAGIC[1]
                && e_ident[2] == ELF_MAGIC[2] && e_ident[3] == ELF_MAGIC[3];
    }

    final char getFileClass() {
        return e_ident[EI_CLASS];
    }

    final char getDataEncoding() {
        return e_ident[EI_DATA];
    }

    public final boolean isLittleEndian() {
        return getDataEncoding() == 1;
    }

    public static abstract class Ehdr {
        short e_type;      // Type of file (see ET_* below)
        short e_machine;   // Required architecture for this file (see EM_*)
        int e_version;     // Must be equal to 1

        int e_flags;       // Processor-specific flags
        short e_ehsize;    // Size of ELF header, in bytes
        short e_phentsize; // Size of an entry in the program header table
        short e_phnum;     // Number of entries in the program header table
        short e_shentsize; // Size of an entry in the section header table
        short e_shnum;     // Number of entries in the section header table
        short e_shstrndx;  // Sect hdr table index of sect name string table

        abstract long getSectionOffset();

        abstract long getProgramOffset();
    }

    static class Elf32_Ehdr extends Ehdr {
        int e_entry;     // Address to jump to in order to start program
        int e_phoff;     // Program header table's file offset, in bytes
        int e_shoff;     // Section header table's file offset, in bytes

        @Override
        long getSectionOffset() {
            return e_shoff;
        }

        @Override
        long getProgramOffset() {
            return e_phoff;
        }
    }

    static class Elf64_Ehdr extends Ehdr {
        long e_entry;
        long e_phoff;
        long e_shoff;

        @Override
        long getSectionOffset() {
            return e_shoff;
        }

        @Override
        long getProgramOffset() {
            return e_phoff;
        }
    }

    // --- Begin section header ---
    public static final String SHN_DYNSYM = ".dynsym";
    public static final String SHN_DYNSTR = ".dynstr";
    public static final String SHN_HASH = ".hash";
    public static final String SHN_RODATA = ".rodata";
    public static final String SHN_TEXT = ".text";
    public static final String SHN_DYNAMIC = ".dynamic";
    public static final String SHN_SHSTRTAB = ".shstrtab";

    // Special section indices.
    final static int SHN_UNDEF = 0; // Undefined, missing, irrelevant, or meaningless

    // Section types
    final static int SHT_PROGBITS = 1; // Program-defined contents.
    final static int SHT_SYMTAB = 2;   // Symbol table.
    final static int SHT_STRTAB = 3;   // String table.
    final static int SHT_RELA = 4;     // Relocation entries; explicit addends.
    final static int SHT_HASH = 5;     // Symbol hash table.
    final static int SHT_DYNAMIC = 6;  // Information for dynamic linking.
    final static int SHT_DYNSYM = 11;  // Symbol table.

    // Section header
    public static abstract class Elf_Shdr {
        int sh_name;      // Section name (index into string table)
        int sh_type;      // Section type (SHT_*)
        int sh_link;      // Section type-specific header table index link
        int sh_info;      // Section type-specific extra information

        public abstract int getSize();

        public abstract long getOffset();
    }

    static class Elf32_Shdr extends Elf_Shdr {
        int sh_flags;     // Section flags (SHF_*)
        int sh_addr;      // Address where section is to be loaded
        int sh_offset;    // File offset of section data, in bytes
        int sh_size;      // Size of section, in bytes
        int sh_addralign; // Section address alignment
        int sh_entsize;   // Size of records contained within the section

        @Override
        public int getSize() {
            return sh_size;
        }

        @Override
        public long getOffset() {
            return sh_offset;
        }
    }

    // Section header for ELF64 - same fields as ELF32, different types.
    static class Elf64_Shdr extends Elf_Shdr {
        long sh_flags;
        long sh_addr;
        long sh_offset;
        long sh_size;
        long sh_addralign;
        long sh_entsize;

        @Override
        public int getSize() {
            return (int) sh_size;
        }

        @Override
        public long getOffset() {
            return sh_offset;
        }
    }

    // --- Begin symbol table ---
    public static abstract class Elf_Sym {
        int st_name;  // Symbol name (index into string table)
        char st_info;  // Symbol's type and binding attributes
        char st_other; // Must be zero; reserved
        short st_shndx; // Which section (header table index) it's defined in

        // These accessors and mutators correspond to the ELF32_ST_BIND,
        // ELF32_ST_TYPE, and ELF32_ST_INFO macros defined in the ELF specification:
        char getBinding() {
            return (char) ((int) st_info >> 4);
        }

        char getType() {
            return (char) ((int) st_info & 0x0f);
        }

        void setBinding(char b) {
            setBindingAndType(b, getType());
        }

        void setType(char t) {
            setBindingAndType(getBinding(), t);
        }

        void setBindingAndType(char b, char t) {
            st_info = (char) (((int) b << 4) + ((int) t & 0x0f));
        }

        abstract long getSize();

        public long getOffset(Elf elf) {
            for (int i = 0; i < elf.mSectionHeaders.length; i++) {
                if (st_shndx == i) {
                    return elf.mSectionHeaders[i].getOffset();
                }
            }
            return -1;
        }
    }

    // Symbol table entries for ELF32.
    static class Elf32_Sym extends Elf_Sym {
        int st_value; // Value or address associated with the symbol
        int st_size;  // Size of the symbol

        @Override
        long getSize() {
            return st_size;
        }
    }

    // Symbol table entries for ELF64.
    static class Elf64_Sym extends Elf_Sym {
        long st_value; // Value or address associated with the symbol
        long st_size;  // Size of the symbol

        @Override
        long getSize() {
            return st_size;
        }
    }

    // --- Begin program header ---
    // Segment types.
    final static int PT_NULL = 0; // Unused segment.
    final static int PT_LOAD = 1; // Loadable segment.
    final static int PT_DYNAMIC = 2; // Dynamic linking information.
    final static int PT_INTERP = 3; // Interpreter pathname.
    final static int PT_NOTE = 4; // Auxiliary information.
    final static int PT_SHLIB = 5; // Reserved.
    final static int PT_PHDR = 6; // The program header table itself.
    final static int PT_TLS = 7; // The thread-local storage template.

    // Segment flag bits.
    final static int PF_X = 1; // Execute
    final static int PF_W = 2; // Write
    final static int PF_R = 4; // Read
    final static int PF_MASKOS = 0x0ff00000;// Bits for operating system-specific semantics.
    final static int PF_MASKPROC = 0xf0000000; // Bits for processor-specific semantics.

    static abstract class Elf_Phdr {
        int p_type;   // Type of segment
        int p_offset; // File offset where segment is located, in bytes

        abstract long getFlags();

        String flagsString() {
            return "("
                    + ((getFlags() & PF_R) != 0 ? "R" : "_")
                    + ((getFlags() & PF_W) != 0 ? "W" : "_")
                    + ((getFlags() & PF_X) != 0 ? "X" : "_")
                    + ")";
        }

        String programType() {
            switch (p_type) {
                case PT_NULL:
                    return "NULL";
                case PT_LOAD:
                    return "Loadable Segment";
                case PT_DYNAMIC:
                    return "Dynamic Segment";
                case PT_INTERP:
                    return "Interpreter Path";
                case PT_NOTE:
                    return "Note";
                case PT_SHLIB:
                    return "PT_SHLIB";
                case PT_PHDR:
                    return "Program Header";
                default:
                    return "Unknown Section";
            }
        }
    }

    // Program header for ELF32.
    static class Elf32_Phdr extends Elf_Phdr {
        int p_vaddr;  // Virtual address of beginning of segment
        int p_paddr;  // Physical address of beginning of segment (OS-specific)
        int p_filesz; // Num. of bytes in file image of segment (may be zero)
        int p_memsz;  // Num. of bytes in mem image of segment (may be zero)
        int p_flags;  // Segment flags
        int p_align;  // Segment alignment constraint

        @Override
        public long getFlags() {
            return p_flags;
        }
    }

    // Program header for ELF64.
    static class Elf64_Phdr extends Elf_Phdr {
        long p_vaddr;  // Virtual address of beginning of segment
        long p_paddr;  // Physical address of beginning of segment (OS-specific)
        long p_filesz; // Num. of bytes in file image of segment (may be zero)
        long p_memsz;  // Num. of bytes in mem image of segment (may be zero)
        long p_flags;  // Segment flags
        long p_align;  // Segment alignment constraint

        @Override
        public long getFlags() {
            return p_flags;
        }
    }

    public DataReader getReader() {
        return mReader;
    }

    public Ehdr getHeader() {
        return mHeader;
    }

    public Elf_Shdr[] getSectionHeaders() {
        return mSectionHeaders;
    }

    private final DataReader mReader;
    private final Ehdr mHeader;
    private final Elf_Shdr[] mSectionHeaders;
    private byte[] mStringTable;

    public final boolean is64bit;
    boolean mReadFull;
    Elf_Phdr[] mProgHeaders;
    Elf_Sym[] mDynamicSymbols;
    byte[] mDynStringTable;

    public Elf(String file, boolean closeNow) throws Exception {
        this(file);
        if (closeNow) {
            mReader.close();
        }
    }

    public Elf(String file) throws Exception {
        this(new File(file));
    }

    public Elf(File file) throws Exception {
        final DataReader r = mReader = new DataReader(file);
        r.readBytes(e_ident);
        if (!checkMagic()) {
            throw new IOException("Invalid elf magic: " + file);
        }
        r.setLittleEndian(isLittleEndian());

        is64bit = getFileClass() == 2;
        if (is64bit) {
            Elf64_Ehdr header = new Elf64_Ehdr();
            header.e_type = r.readShort();
            header.e_machine = r.readShort();
            header.e_version = r.readInt();
            header.e_entry = r.readLong();
            header.e_phoff = r.readLong();
            header.e_shoff = r.readLong();
            mHeader = header;
        } else {
            Elf32_Ehdr header = new Elf32_Ehdr();
            header.e_type = r.readShort();
            header.e_machine = r.readShort();
            header.e_version = r.readInt();
            header.e_entry = r.readInt();
            header.e_phoff = r.readInt();
            header.e_shoff = r.readInt();
            mHeader = header;
        }
        final Ehdr h = mHeader;
        h.e_flags = r.readInt();
        h.e_ehsize = r.readShort();
        h.e_phentsize = r.readShort();
        h.e_phnum = r.readShort();
        h.e_shentsize = r.readShort();
        h.e_shnum = r.readShort();
        h.e_shstrndx = r.readShort();

        mSectionHeaders = new Elf_Shdr[h.e_shnum];
        for (int i = 0; i < h.e_shnum; i++) {
            final long offset = h.getSectionOffset() + (i * h.e_shentsize);
            r.seek(offset);
            if (is64bit) {
                Elf64_Shdr secHeader = new Elf64_Shdr();
                secHeader.sh_name = r.readInt();
                secHeader.sh_type = r.readInt();
                secHeader.sh_flags = r.readLong();
                secHeader.sh_addr = r.readLong();
                secHeader.sh_offset = r.readLong();
                secHeader.sh_size = r.readLong();
                secHeader.sh_link = r.readInt();
                secHeader.sh_info = r.readInt();
                secHeader.sh_addralign = r.readLong();
                secHeader.sh_entsize = r.readLong();
                mSectionHeaders[i] = secHeader;
            } else {
                Elf32_Shdr secHeader = new Elf32_Shdr();
                secHeader.sh_name = r.readInt();
                secHeader.sh_type = r.readInt();
                secHeader.sh_flags = r.readInt();
                secHeader.sh_addr = r.readInt();
                secHeader.sh_offset = r.readInt();
                secHeader.sh_size = r.readInt();
                secHeader.sh_link = r.readInt();
                secHeader.sh_info = r.readInt();
                secHeader.sh_addralign = r.readInt();
                secHeader.sh_entsize = r.readInt();
                mSectionHeaders[i] = secHeader;
            }
        }
        if (h.e_shstrndx > -1 && h.e_shstrndx < mSectionHeaders.length) {
            final Elf_Shdr strSec = mSectionHeaders[h.e_shstrndx];
            if (strSec.sh_type == SHT_STRTAB) {
                int strSecSize = strSec.getSize();
                mStringTable = new byte[strSecSize];
                r.seek(strSec.getOffset());
                r.readBytes(mStringTable);
            } else {
                throw new IOException("Wrong string section e_shstrndx=" + h.e_shstrndx);
            }
        } else {
            throw new IOException("Invalid e_shstrndx=" + h.e_shstrndx);
        }

        if (mReadFull) {
            readSymbolTables();
            readProgramHeaders();
        }
    }

    private void readSymbolTables() {
        final DataReader r = mReader;
        final Elf_Shdr dynsym = getSection(SHN_DYNSYM);
        if (dynsym != null) {
            r.seek(dynsym.getOffset());
            int len = dynsym.getSize() / (is64bit ? 24 : 16); // sizeof Elf_Sym
            mDynamicSymbols = new Elf_Sym[len];
            char cbuf[] = new char[1];
            for (int i = 0; i < len; i++) {
                if (is64bit) {
                    Elf64_Sym dsym = new Elf64_Sym();
                    dsym.st_name = r.readInt();
                    r.readBytes(cbuf);
                    dsym.st_info = cbuf[0];
                    r.readBytes(cbuf);
                    dsym.st_other = cbuf[0];
                    dsym.st_value = r.readLong();
                    dsym.st_size = r.readLong();
                    dsym.st_shndx = r.readShort();
                    mDynamicSymbols[i] = dsym;
                } else {
                    Elf32_Sym dsym = new Elf32_Sym();
                    dsym.st_name = r.readInt();
                    dsym.st_value = r.readInt();
                    dsym.st_size = r.readInt();
                    r.readBytes(cbuf);
                    dsym.st_info = cbuf[0];
                    r.readBytes(cbuf);
                    dsym.st_other = cbuf[0];
                    dsym.st_shndx = r.readShort();
                    mDynamicSymbols[i] = dsym;
                }
            }

            final Elf_Shdr dynLinkSec = mSectionHeaders[dynsym.sh_link];
            r.seek(dynLinkSec.getOffset());
            mDynStringTable = new byte[dynLinkSec.getSize()];
            r.readBytes(mDynStringTable);
        }
    }

    private void readProgramHeaders() {
        final Ehdr h = mHeader;
        final DataReader r = mReader;
        mProgHeaders = new Elf_Phdr[h.e_phnum];
        for (int i = 0; i < h.e_phnum; i++) {
            final long offset = h.getProgramOffset() + (i * h.e_phentsize);
            r.seek(offset);
            if (is64bit) {
                Elf64_Phdr progHeader = new Elf64_Phdr();
                progHeader.p_type = r.readInt();
                progHeader.p_offset = r.readInt();
                progHeader.p_vaddr = r.readLong();
                progHeader.p_paddr = r.readLong();
                progHeader.p_filesz = r.readLong();
                progHeader.p_memsz = r.readLong();
                progHeader.p_flags = r.readLong();
                progHeader.p_align = r.readLong();
                mProgHeaders[i] = progHeader;
            } else {
                Elf32_Phdr progHeader = new Elf32_Phdr();
                progHeader.p_type = r.readInt();
                progHeader.p_offset = r.readInt();
                progHeader.p_vaddr = r.readInt();
                progHeader.p_paddr = r.readInt();
                progHeader.p_filesz = r.readInt();
                progHeader.p_memsz = r.readInt();
                progHeader.p_flags = r.readInt();
                progHeader.p_align = r.readInt();
                mProgHeaders[i] = progHeader;
            }
        }
    }


    public final Elf_Shdr getSection(String name) {
        for (Elf_Shdr sec : mSectionHeaders) {
            if (name.equals(getString(sec.sh_name))) {
                return sec;
            }
        }
        return null;
    }


    public final Elf_Sym getSymbolTable(String name) {
        if (mDynamicSymbols != null) {
            for (Elf_Sym sym : mDynamicSymbols) {
                if (name.equals(getDynString(sym.st_name))) {
                    return sym;
                }
            }
        }
        return null;
    }


    public final String getString(int index) {
        if (index == SHN_UNDEF) {
            return "SHN_UNDEF";
        }
        int end = index;
        while (mStringTable[end] != '\0') {
            end++;
        }
        return new String(mStringTable, index, end - index);
    }


    public final String getDynString(int index) {
        if (index == SHN_UNDEF) {
            return "SHN_UNDEF";
        }
        int end = index;
        while (mDynStringTable[end] != '\0') {
            end++;
        }
        return new String(mDynStringTable, index, end - index);
    }

    @Override
    public void close() {
        mReader.close();
    }
}