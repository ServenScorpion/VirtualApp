package com.lody.virtual.helper.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.os.VEnvironment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Lody
 */
public class FileUtils {

    public static int count(File file) {
        if(!file.exists()){
            return -1;
        }
        if(file.isFile()){
            return 1;
        }
        if (file.isDirectory()) {
            String[] fs = file.list();
            return fs == null ? 0 : fs.length;
        }
        return 0;
    }

    public static String getFilenameExt(String filename) {
        int dotPos = filename.lastIndexOf('.');
        if (dotPos == -1) {
            return "";
        }
        return filename.substring(dotPos + 1);
    }

    public static File changeExt(File f, String targetExt) {
        String outPath = f.getAbsolutePath();
        if (!getFilenameExt(outPath).equals(targetExt)) {
            int dotPos = outPath.lastIndexOf(".");
            if (dotPos > 0) {
                outPath = outPath.substring(0, dotPos + 1) + targetExt;
            } else {
                outPath = outPath + "." + targetExt;
            }
            return new File(outPath);
        }
        return f;
    }

    public static String readToString(String fileName) throws IOException {
        InputStream is = new FileInputStream(fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }
        return baos.toString();
    }

    /**
     * @param path
     * @param mode {@link FileMode}
     */
    public static void chmod(String path, int mode) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.chmod(path, mode);
                return;
            } catch (Exception e) {
                // ignore
            }
        }

        File file = new File(path);
        String cmd = "chmod ";
        if (file.isDirectory()) {
            cmd += " -R ";
        }
        String cmode = String.format("%o", mode);
        Runtime.getRuntime().exec(cmd + cmode + " " + path).waitFor();
    }

    public static void createSymlink(String oldPath, String newPath) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.link(oldPath, newPath);
                return;
            }catch (Throwable e){
                //ignore
            }
        }
        Runtime.getRuntime().exec("ln -s " + oldPath + " " + newPath).waitFor();
    }

    public static boolean isSymlink(File file) throws IOException {
        if (file == null)
            throw new NullPointerException("File must not be null");
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }

    public static void writeParcelToFile(Parcel p, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(p.marshall());
        fos.close();
    }

    public static byte[] toByteArray(InputStream inStream) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        return swapStream.toByteArray();
    }

    public static int deleteDir(File dir) {
        int count = 0;
        if (dir.isDirectory()) {
            boolean link = false;
            try {
                link = isSymlink(dir);
            } catch (Exception e) {
                //ignore
            }
            if (!link) {
                String[] children = dir.list();
                for (String file : children) {
                    count += deleteDir(new File(dir, file));
                }
            }
        }
        if (dir.delete()) {
            count++;
        }
        return count;
    }

    public static int deleteDir(String dir) {
        return deleteDir(new File(dir));
    }

    public static void writeToFile(InputStream dataIns, File target) throws IOException {
        final int BUFFER = 1024;
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
        int count;
        byte data[] = new byte[BUFFER];
        while ((count = dataIns.read(data, 0, BUFFER)) != -1) {
            bos.write(data, 0, count);
        }
        bos.close();
    }

    public static void writeToFile(byte[] data, File target) throws IOException {
        FileOutputStream fo = null;
        ReadableByteChannel src = null;
        FileChannel out = null;
        try {
            src = Channels.newChannel(new ByteArrayInputStream(data));
            fo = new FileOutputStream(target);
            out = fo.getChannel();
            out.transferFrom(src, 0, data.length);
        } finally {
            if (fo != null) {
                fo.close();
            }
            if (src != null) {
                src.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    public static void copyFile(InputStream inputStream, File target){
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(target);
            byte[] data = new byte[4096];
            int len;
            while ((len = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, len);
            }
            outputStream.flush();
        } catch (Throwable e) {
            //ignore
        } finally {
            closeQuietly(inputStream);
            closeQuietly(outputStream);
        }
    }

    public static void copyFile(File source, File target) throws IOException {

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(source);
            outputStream = new FileOutputStream(target);
            FileChannel iChannel = inputStream.getChannel();
            FileChannel oChannel = outputStream.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                buffer.clear();
                int r = iChannel.read(buffer);
                if (r == -1)
                    break;
                buffer.limit(buffer.position());
                buffer.position(0);
                oChannel.write(buffer);
            }
        } finally {
            closeQuietly(inputStream);
            closeQuietly(outputStream);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static int peekInt(byte[] bytes, int value, ByteOrder endian) {
        int v2;
        int v0;
        if (endian == ByteOrder.BIG_ENDIAN) {
            v0 = value + 1;
            v2 = v0 + 1;
            v0 = (bytes[v0] & 255) << 16 | (bytes[value] & 255) << 24 | (bytes[v2] & 255) << 8 | bytes[v2 + 1] & 255;
        } else {
            v0 = value + 1;
            v2 = v0 + 1;
            v0 = (bytes[v0] & 255) << 8 | bytes[value] & 255 | (bytes[v2] & 255) << 16 | (bytes[v2 + 1] & 255) << 24;
        }

        return v0;
    }

    private static boolean isValidExtFilenameChar(char c) {
        switch (c) {
            case '\0':
            case '/':
                return false;
            default:
                return true;
        }
    }

    /**
     * Check if given filename is valid for an ext4 filesystem.
     */
    public static boolean isValidExtFilename(String name) {
        return (name != null) && name.equals(buildValidExtFilename(name));
    }

    /**
     * Mutate the given filename to make it valid for an ext4 filesystem,
     * replacing any invalid characters with "_".
     */
    public static String buildValidExtFilename(String name) {
        if (TextUtils.isEmpty(name) || ".".equals(name) || "..".equals(name)) {
            return "(invalid)";
        }
        final StringBuilder res = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (isValidExtFilenameChar(c)) {
                res.append(c);
            } else {
                res.append('_');
            }
        }
        return res.toString();
    }

    public static boolean isExist(String path) {
        return new File(path).exists();
    }

    public static boolean canRead(String path) {
        return new File(path).canRead();
    }

    public interface FileMode {
        int MODE_ISUID = 04000;
        int MODE_ISGID = 02000;
        int MODE_ISVTX = 01000;
        int MODE_IRUSR = 00400;
        int MODE_IWUSR = 00200;
        int MODE_IXUSR = 00100;
        int MODE_IRGRP = 00040;
        int MODE_IWGRP = 00020;
        int MODE_IXGRP = 00010;
        int MODE_IROTH = 00004;
        int MODE_IWOTH = 00002;
        int MODE_IXOTH = 00001;

        int MODE_755 = MODE_IRUSR | MODE_IWUSR | MODE_IXUSR
                | MODE_IRGRP | MODE_IXGRP
                | MODE_IROTH | MODE_IXOTH;
    }

    /**
     * Lock the specified fle
     */
    public static class FileLock {
        private static FileLock singleton;
        private Map<String, FileLockCount> mRefCountMap = new ConcurrentHashMap<String, FileLockCount>();

        public static FileLock getInstance() {
            if (singleton == null) {
                singleton = new FileLock();
            }
            return singleton;
        }

        private int RefCntInc(String filePath, java.nio.channels.FileLock fileLock, RandomAccessFile randomAccessFile,
                              FileChannel fileChannel) {
            int refCount;
            if (this.mRefCountMap.containsKey(filePath)) {
                FileLockCount fileLockCount = this.mRefCountMap.get(filePath);
                int i = fileLockCount.mRefCount;
                fileLockCount.mRefCount = i + 1;
                refCount = i;
            } else {
                refCount = 1;
                this.mRefCountMap.put(filePath, new FileLockCount(fileLock, refCount, randomAccessFile, fileChannel));

            }
            return refCount;
        }

        private int RefCntDec(String filePath) {
            int refCount = 0;
            if (this.mRefCountMap.containsKey(filePath)) {
                FileLockCount fileLockCount = this.mRefCountMap.get(filePath);
                int i = fileLockCount.mRefCount - 1;
                fileLockCount.mRefCount = i;
                refCount = i;
                if (refCount <= 0) {
                    this.mRefCountMap.remove(filePath);
                }
            }
            return refCount;
        }

        public boolean LockExclusive(File targetFile) {

            if (targetFile == null) {
                return false;
            }
            try {
                File lockFile = new File(targetFile.getParentFile().getAbsolutePath().concat("/lock"));
                if (!lockFile.exists()) {
                    lockFile.createNewFile();
                }
                RandomAccessFile randomAccessFile = new RandomAccessFile(lockFile.getAbsolutePath(), "rw");
                FileChannel channel = randomAccessFile.getChannel();
                java.nio.channels.FileLock lock = channel.lock();
                if (!lock.isValid()) {
                    return false;
                }
                RefCntInc(lockFile.getAbsolutePath(), lock, randomAccessFile, channel);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * unlock odex file
         **/
        public void unLock(File targetFile) {

            File lockFile = new File(targetFile.getParentFile().getAbsolutePath().concat("/lock"));
            if (!lockFile.exists()) {
                return;
            }
            if (this.mRefCountMap.containsKey(lockFile.getAbsolutePath())) {
                FileLockCount fileLockCount = this.mRefCountMap.get(lockFile.getAbsolutePath());
                if (fileLockCount != null) {
                    java.nio.channels.FileLock fileLock = fileLockCount.mFileLock;
                    RandomAccessFile randomAccessFile = fileLockCount.fOs;
                    FileChannel fileChannel = fileLockCount.fChannel;
                    try {
                        if (RefCntDec(lockFile.getAbsolutePath()) <= 0) {
                            if (fileLock != null && fileLock.isValid()) {
                                fileLock.release();
                            }
                            if (randomAccessFile != null) {
                                randomAccessFile.close();
                            }
                            if (fileChannel != null) {
                                fileChannel.close();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private class FileLockCount {
            FileChannel fChannel;
            RandomAccessFile fOs;
            java.nio.channels.FileLock mFileLock;
            int mRefCount;

            FileLockCount(java.nio.channels.FileLock fileLock, int mRefCount, RandomAccessFile fOs,
                          FileChannel fChannel) {
                this.mFileLock = fileLock;
                this.mRefCount = mRefCount;
                this.fOs = fOs;
                this.fChannel = fChannel;
            }
        }
    }

    public static String getFilePathByUri(Context context, Uri uri) {
        String path = null;
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
            return path;
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                    String cardRootPath = "";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        List<StorageVolume> list = mStorageManager.getStorageVolumes();
                       if (list.size() > 0) {
                           for (StorageVolume storageVolume : list) {
                               File storage = Reflect.on(storageVolume).field("mPath").get();
                               Boolean removeable = Reflect.on(storageVolume).field("mRemovable").get();
                               if (removeable) {
                                   cardRootPath = storage.getPath();
                               }
                           }
                       }
                    }
                    if (type.startsWith("secondary")) {
                        path = cardRootPath + "/" + split[1];
                    } else if (type.startsWith("primary")) {
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];
                    } else {
                        path = cardRootPath + "/" + split[1];
                    }
                    return path;
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    path = getDataColumn(context, contentUri, null, null);
                    return path;
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                    return path;
                }
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
