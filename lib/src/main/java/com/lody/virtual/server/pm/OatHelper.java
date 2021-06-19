package com.lody.virtual.server.pm;

import com.lody.virtual.helper.DexOptimizer;
import com.lody.virtual.helper.dedex.DataReader;
import com.lody.virtual.helper.dedex.Dex;
import com.lody.virtual.helper.dedex.Elf;
import com.lody.virtual.helper.dedex.Oat;
import com.lody.virtual.helper.dedex.Vdex;
import com.lody.virtual.helper.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * After Android N, we cannot load framework jar file such as
 * '/system/framework/com.android.location.provider.jar' directly.
 * <p>
 * Most of time, our environment is 32bit but system only provided 64bit JAR.
 * We must dump the dex from the associated oat/vdex file.
 *
 * @author Lody
 */
public class OatHelper {

    private static final String[] ABIS_32BIT = {
            "arm",
            "mips",
            "x86"
    };

    private static final String[] ABIS_64BIT = {
            "arm64",
            "mips64",
            "x86_64"
    };

    public static boolean extractFrameworkFor32Bit(String name, File zipFile, File odexFile) {
        return extractFramework(name, ABIS_32BIT, ABIS_64BIT, zipFile, odexFile);
    }

    public static boolean extractFrameworkFor64Bit(String name, File zipFile, File odexFile) {
        return extractFramework(name, ABIS_64BIT, ABIS_32BIT, zipFile, odexFile);
    }

    public static boolean extractFramework(String name, String[] matchAbis, String[] dumpOatAbis, File zipFile, File odexFile) {
        File framework = new File("/system/framework/" + name + ".jar");
        if (!framework.exists()) {
            return false;
        }
        if (containDex(framework.getPath())) {
            return false;
        }
        boolean exist = false;
        for (String matchAbi : matchAbis) {
            File oatFile = new File(String.format("/system/framework/oat/%s/%s.oat", matchAbi, name));
            if (oatFile.exists()) {
                exist = true;
                break;
            }
        }
        if (exist) {
            return false;
        }
        for (String dumpOatAbi : dumpOatAbis) {
            File oatFile = new File(String.format("/system/framework/oat/%s/%s.oat", dumpOatAbi, name));
            if (oatFile.exists()) {
                return extractDexFromOatFile(oatFile, zipFile, odexFile);
            } else {
                File vdexFile = new File(String.format("/system/framework/oat/%s/%s.vdex", dumpOatAbi, name));
                if (vdexFile.exists()) {
                    return extractDexFromVDexFile(vdexFile, zipFile, odexFile);
                }
            }
        }
        return false;
    }

    private static void addDexToZip(ZipOutputStream os, int dexIdx, Dex dex) throws IOException {
        String dexName;
        if (dexIdx == 0) {
            dexName = "classes.dex";
        } else {
            dexName = "classes" + (dexIdx + 1) + ".dex";
        }
        os.putNextEntry(new ZipEntry(dexName));
        byte[] bytes = dex.getFixedBytes();
        os.write(bytes);
        os.closeEntry();
    }

    private static boolean extractDexFromVDexFile(File vdexFile, File zipFile, File odexFile) {
        DataReader reader = null;
        ZipOutputStream os = null;
        try {
            reader = new DataReader(vdexFile);
            Vdex vdex = new Vdex(reader);
            os = new ZipOutputStream(new FileOutputStream(zipFile));
            int dexIdx = 0;
            for (Dex dex : vdex.dexFiles) {
                addDexToZip(os, dexIdx, dex);
                dexIdx++;
            }
            generateOdex(zipFile, odexFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            FileUtils.closeQuietly(reader);
            FileUtils.closeQuietly(os);
        }
    }

    private static boolean extractDexFromOatFile(File oatFile, File zipFile, File odexFile) {
        Elf elf = null;
        ZipOutputStream os = null;
        try {
            elf = new Elf(oatFile);
            final Elf.Elf_Shdr rodata = elf.getSection(Oat.SECTION_RODATA);
            if (rodata == null) {
                return false;
            }
            DataReader reader = elf.getReader();
            reader.seek(rodata.getOffset());
            Oat oat = new Oat(reader);
            os = new ZipOutputStream(new FileOutputStream(zipFile));
            int dexIdx = 0;
            for (Dex dex : oat.dexFiles) {
                addDexToZip(os, dexIdx, dex);
                dexIdx++;
            }
            generateOdex(zipFile, odexFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            FileUtils.closeQuietly(elf);
            FileUtils.closeQuietly(os);
        }
    }

    private static void generateOdex(File zipFile, File odexFile) throws IOException {
        DexOptimizer.interpretDex2Oat(zipFile.getPath(), odexFile.getPath());
    }


    public static boolean containDex(String apkPath) {
        if (apkPath == null) {
            return false;
        }
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(apkPath);
            ZipEntry entry = zipfile.getEntry("classes.dex");
            if (entry != null && !entry.isDirectory()) {
                return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
