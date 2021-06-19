package com.lody.virtual.helper.dedex;

import com.lody.virtual.helper.utils.FileUtils;

import java.io.File;
import java.io.IOException;

import static com.lody.virtual.helper.dedex.DataReader.toInt;

@SuppressWarnings("unused")
public class Oat {
    public final static String SECTION_RODATA = ".rodata";

    public enum Version {
        // https://android.googlesource.com/platform/art/+/lollipop-release/runtime/oat.cc#26
        L_50(21, 39),

        // https://android.googlesource.com/platform/art/+/lollipop-mr1-release/runtime/oat.cc#26
        L_MR1_51(22, 45),

        // https://android.googlesource.com/platform/art/+/marshmallow-release/runtime/oat.h#35
        M_60(23, 64),

        // https://android.googlesource.com/platform/art/+/nougat-release/runtime/oat.h#35
        N_70(24, 79),

        // https://android.googlesource.com/platform/art/+/nougat-mr1-release/runtime/oat.h#35
        N_MR1_71(25, 88),

        // https://android.googlesource.com/platform/art/+/oreo-release/runtime/oat.h#35
        O_80(26, 124),

        // https://android.googlesource.com/platform/art/+/oreo-mr1-release/runtime/oat.h#36
        O_MR1_81(27, 131);

        public final int api;
        public final int oat;

        Version(int api, int oat) {
            this.api = api;
            this.oat = oat;
        }
    }

    // See /art/runtime/instruction_set.h
    public final static class InstructionSet {
        public final static int kNone = 0;
        public final static int kArm = 1;
        public final static int kArm64 = 2;
        public final static int kThumb2 = 3;
        public final static int kX86 = 4;
        public final static int kX86_64 = 5;
        public final static int kMips = 6;
        public final static int kMips64 = 7;
    }

    // See /art/runtime/oat.h
    public static class Header {

        final char[] magic_ = new char[4];
        final char[] version_ = new char[4];
        final int adler32_checksum_;

        final int instruction_set_;
        final int instruction_set_features_; // instruction_set_features_bitmap_
        final int dex_file_count_;
        final int executable_offset_;
        final int interpreter_to_interpreter_bridge_offset_;
        final int interpreter_to_compiled_code_bridge_offset_;
        final int jni_dlsym_lookup_offset_;
        int portable_imt_conflict_trampoline_offset_;
        int portable_resolution_trampoline_offset_;
        int portable_to_interpreter_bridge_offset_;
        final int quick_generic_jni_trampoline_offset_;
        final int quick_imt_conflict_trampoline_offset_;
        final int quick_resolution_trampoline_offset_;
        final int quick_to_interpreter_bridge_offset_;

        final int image_patch_delta_;
        final int image_file_location_oat_checksum_;
        final int image_file_location_oat_data_begin_;
        final int key_value_store_size_;
        final char[] key_value_store_;

        int artVersion;

        public Header(DataReader r) throws IOException {
            r.readBytes(magic_);
            if (magic_[0] != 'o' || magic_[1] != 'a' || magic_[2] != 't') {
                throw new IOException(String.format("Invalid art magic %c%c%c", magic_[0], magic_[1], magic_[2]));
            }
            r.readBytes(version_);
            artVersion = toInt(new String(version_));

            adler32_checksum_ = r.readInt();
            instruction_set_ = r.readInt();
            instruction_set_features_ = r.readInt();

            dex_file_count_ = r.readInt();
            executable_offset_ = r.readInt();
            interpreter_to_interpreter_bridge_offset_ = r.readInt();
            interpreter_to_compiled_code_bridge_offset_ = r.readInt();

            jni_dlsym_lookup_offset_ = r.readInt();
            if (artVersion < 52) {
                // Remove portable. (since oat version 052)
                // https://android.googlesource.com/platform/art/+/956af0f0
                portable_imt_conflict_trampoline_offset_ = r.readInt();
                portable_resolution_trampoline_offset_ = r.readInt();
                portable_to_interpreter_bridge_offset_ = r.readInt();
            }
            quick_generic_jni_trampoline_offset_ = r.readInt();
            quick_imt_conflict_trampoline_offset_ = r.readInt();
            quick_resolution_trampoline_offset_ = r.readInt();
            quick_to_interpreter_bridge_offset_ = r.readInt();

            image_patch_delta_ = r.readInt();
            image_file_location_oat_checksum_ = r.readInt();
            image_file_location_oat_data_begin_ = r.readInt();
            key_value_store_size_ = r.readInt();
            key_value_store_ = new char[key_value_store_size_];
            r.readBytes(key_value_store_);
        }
    }


    // See art/compiler/oat_writer OatDexFile::Write
    public static class OatDexFile {
        public final int dex_file_location_size_;
        public final byte[] dex_file_location_data_;
        final int dex_file_location_checksum_;
        final int dex_file_offset_;
        File dex_file_pointer_; // If not null, dex_file_offset_ is the size of vdex header
        int class_offsets_offset_;
        int lookup_table_offset_;

        public OatDexFile(DataReader r, int version) throws IOException {
            dex_file_location_size_ = r.readInt();
            dex_file_location_data_ = new byte[dex_file_location_size_];
            r.readBytes(dex_file_location_data_);
            dex_file_location_checksum_ = r.readInt();
            dex_file_offset_ = r.readInt();

            final File vdex = FileUtils.changeExt(r.getFile(), "vdex");
            if (vdex.exists()) {
                dex_file_pointer_ = vdex;
            } else if (dex_file_offset_ == 0x1C) {
                throw new IOException("dex_file_offset_=" + dex_file_offset_
                        + ", does " + vdex.getName() + " miss?");
            }
            if (version >= Version.N_70.oat) {
                class_offsets_offset_ = r.readInt();
                lookup_table_offset_ = r.readInt();
            }
        }

        public String getLocation() {
            return new String(dex_file_location_data_);
        }
    }

    public final long oatPosition;
    public final Header header;
    public final OatDexFile[] oatDexFiles;
    public final Dex[] dexFiles;
    public final File srcFile;

    public Oat(DataReader reader) throws Exception {
        oatPosition = reader.position();
        if (oatPosition != 4096) {
            // Normally start from 4096(0x1000)
            throw new IOException("Strange oat position " + oatPosition);
        }
        srcFile = reader.getFile();
        header = new Header(reader);
        oatDexFiles = new OatDexFile[header.dex_file_count_];
        dexFiles = new Dex[header.dex_file_count_];
        for (int i = 0; i < oatDexFiles.length; i++) {
            final OatDexFile odf = new OatDexFile(reader, header.artVersion);
            oatDexFiles[i] = odf;
            final long curOatPos = reader.position();

            final Dex dex;
            if (odf.dex_file_pointer_ != null) {
                DataReader r = new DataReader(odf.dex_file_pointer_);
                reader.addAssociatedReader(r);
                r.seek(odf.dex_file_offset_);
                dex = new Dex(r);
            } else {
                reader.seek(oatPosition + odf.dex_file_offset_);
                dex = new Dex(reader);
            }
            dexFiles[i] = dex;

            if (header.artVersion < Version.N_70.oat) {
                int num_methods_offsets_ = dex.header.class_defs_size_;
                reader.seek(curOatPos + 4 * num_methods_offsets_);
                if (reader.previewInt() > 0xff) { // workaround for samsung offset
                    //num_methods_offsets_ += 4;
                    reader.readInt();
                }
                // No need to read method information
                // methods_offsets_ change to method_bitmap_ since N
                //odf.methods_offsets_ = new int[num_methods_offsets_];
                //reader.seek(thisOatPos);
                //reader.readIntArray(odf.methods_offsets_);

            } else {
                reader.seek(curOatPos);
            }
        }
    }

    public int getArtVersion() {
        return header.artVersion;
    }


}

