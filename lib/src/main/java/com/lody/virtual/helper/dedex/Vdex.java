package com.lody.virtual.helper.dedex;

import java.io.IOException;

import static com.lody.virtual.helper.dedex.DataReader.toInt;


// See art/runtime/vdex_file.cc
public class Vdex {

    private static final int VERSION_OREO_006 = 6;
    private static final int VERSION_OREO_MR1_010 = 10;
    private static final int VERSION_P_018 = 18;

    public static class Header {

        final char[] magic_ = new char[4];
        final char[] version_ = new char[4];
        public final int number_of_dex_files_;
        final int dex_size_;
        final int dex_shared_data_size_;
        final int verifier_deps_size_;
        final int quickening_info_size_;

        final int[] vdexCheckSums;
        public final int version;

        public Header(DataReader r) throws IOException {
            r.readBytes(magic_);
            String magic = new String(magic_);
            if (!"vdex".equals(magic)) {
                throw new IOException("Invalid dex magic '" + magic + "'");
            }
            r.readBytes(version_);
            version = toInt(new String(version_));
            number_of_dex_files_ = r.readInt();
            dex_size_ = r.readInt();
            dex_shared_data_size_ = versionNears(VERSION_P_018) ? r.readInt() : 0;
            verifier_deps_size_ = r.readInt();
            quickening_info_size_ = r.readInt();

            vdexCheckSums = new int[number_of_dex_files_];
            for (int i = 0; i < vdexCheckSums.length; i++) {
                vdexCheckSums[i] = r.readInt();
            }
        }

        public boolean versionNears(int version) {
            return Math.abs(this.version - version) <= 1;
        }
    }

    public static class QuickenDex extends Dex {

        QuickenDex(DataReader r) throws IOException {
            super(r);
        }
    }

    public final Header header;
    public final QuickenDex[] dexFiles;
    public final int[] quickeningTableOffsets;
    public final int dexBegin;


    public Vdex(DataReader r) throws Exception {
        header = new Header(r);
        dexBegin = r.position();
        r.position(dexBegin);
        quickeningTableOffsets = header.versionNears(VERSION_P_018)
                ? new int[header.number_of_dex_files_] : null;
        dexFiles = new QuickenDex[header.number_of_dex_files_];
        for (int i = 0; i < header.number_of_dex_files_; i++) {
            if (quickeningTableOffsets != null) {
                quickeningTableOffsets[i] = r.readInt();
            }
            final QuickenDex dex = new QuickenDex(r);
            dexFiles[i] = dex;
            r.position(dex.dexPosition + dex.header.file_size_);
        }
    }

}
