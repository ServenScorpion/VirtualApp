package com.lody.virtual.helper.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;


/**
 * 判断文件的类型  视频 音频  图片  word xls ppt pdf apk txt zip
 */
public class MediaFileUtil {
    public static String sFileExtensions;

    // Audio
    public static final int FILE_TYPE_MP3     = 1;
    public static final int FILE_TYPE_M4A     = 2;
    public static final int FILE_TYPE_WAV     = 3;
    public static final int FILE_TYPE_AMR     = 4;
    public static final int FILE_TYPE_AWB     = 5;
    public static final int FILE_TYPE_WMA     = 6;
    public static final int FILE_TYPE_OGG     = 7;
    private static final int FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3;
    private static final int LAST_AUDIO_FILE_TYPE = FILE_TYPE_OGG;

    // MIDI
    public static final int FILE_TYPE_MID     = 11;
    public static final int FILE_TYPE_SMF     = 12;
    public static final int FILE_TYPE_IMY     = 13;
    private static final int FIRST_MIDI_FILE_TYPE = FILE_TYPE_MID;
    private static final int LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY;

    // Video
    public static final int FILE_TYPE_MP4     = 21;
    public static final int FILE_TYPE_M4V     = 22;
    public static final int FILE_TYPE_3GPP    = 23;
    public static final int FILE_TYPE_3GPP2   = 24;
    public static final int FILE_TYPE_WMV     = 25;
    private static final int FIRST_VIDEO_FILE_TYPE = FILE_TYPE_MP4;
    private static final int LAST_VIDEO_FILE_TYPE = FILE_TYPE_WMV;

    // Image
    public static final int FILE_TYPE_JPEG    = 31;
    public static final int FILE_TYPE_GIF     = 32;
    public static final int FILE_TYPE_PNG     = 33;
    public static final int FILE_TYPE_BMP     = 34;
    public static final int FILE_TYPE_WBMP    = 35;
    private static final int FIRST_IMAGE_FILE_TYPE = FILE_TYPE_JPEG;
    private static final int LAST_IMAGE_FILE_TYPE = FILE_TYPE_WBMP;

    // Playlist
    public static final int FILE_TYPE_M3U     = 41;
    public static final int FILE_TYPE_PLS     = 42;
    public static final int FILE_TYPE_WPL     = 43;
    private static final int FIRST_PLAYLIST_FILE_TYPE = FILE_TYPE_M3U;
    private static final int LAST_PLAYLIST_FILE_TYPE = FILE_TYPE_WPL;

    //静态内部类
    static class MediaFileType {

        int fileType;
        String mimeType;

        MediaFileType(int fileType, String mimeType) {
            this.fileType = fileType;
            this.mimeType = mimeType;
        }
    }

    private static HashMap<String, MediaFileType> sFileTypeMap
            = new HashMap<String, MediaFileType>();
    private static HashMap<String, Integer> sMimeTypeMap
            = new HashMap<String, Integer>();
    static void addFileType(String extension, int fileType, String mimeType) {
        sFileTypeMap.put(extension, new MediaFileType(fileType, mimeType));
        sMimeTypeMap.put(mimeType, Integer.valueOf(fileType));
    }
    //office 文档格式
    //pdf

    public static final int FILE_TYPE_PDF = 51;

    //ppt
    public static final int FILE_TYPE_PPS=52;
    public static final int FILE_TYPE_PPT =53 ;
    public static final int FILE_TYPE_PPTX =54 ;
    public static final int FIRST_PPT_FILE_TYPE=FILE_TYPE_PPS;
    public static final int LAST_PPT_FILE_TYPE=FILE_TYPE_PPTX;
    //doc
    public static final int FILE_TYPE_WPS = 55;
    public static final int FILE_TYPE_DOC =56 ;
    public static final int FILE_TYPE_DOCX =57 ;
    public static final int FIRST_DOC_FILE_TYPE=FILE_TYPE_WPS;
    public static final int LAST_DOC_FILE_TYPE=FILE_TYPE_DOCX;
    //xls
    public static final int FILE_TYPE_XLS = 58;
    public static final int FILE_TYPE_XLSX =59 ;
    public static final int FIRST_XLS_FILE_TYPE=FILE_TYPE_XLS;
    public static final int LAST_XLS_FILE_TYPE=FILE_TYPE_XLSX;

    //文本文件
    public static final int FILE_TYPE_PROP = 61;
    public static final int FILE_TYPE_RC = 62;
    public static final int FILE_TYPE_SH = 63;
    public static final int FILE_TYPE_TXT = 64;
    public static final int FILE_TYPE_XML = 65;
    public static final int FILE_TYPE_RTF = 66;
    public static final int FIRST_TXT_FILE_TYPE=FILE_TYPE_PROP;
    public static final int LAST_TXT_FILE_TYPE=FILE_TYPE_RTF;
    //压缩文件
    public static final int FILE_TYPE_TAR = 71;
    public static final int FILE_TYPE_TGZ = 72;
    public static final int FILE_TYPE_Z = 73;
    public static final int FILE_TYPE_ZIP = 74;
    public static final int FIRST_ZIP_FILE_TYPE=FILE_TYPE_TAR;
    public static final int LAST_ZIP_FILE_TYPE=FILE_TYPE_ZIP;
    //apk文件
    private static final int FILE_TYPE_APK = 81;
    //exe文件
    private static final int FILE_TYPE_EXE = 82;

    static {
        addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg");
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4");
        addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav");
        addFileType("AMR", FILE_TYPE_AMR, "audio/amr");
        addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb");
        addFileType("WMA", FILE_TYPE_WMA, "audio/x-ms-wma");
        addFileType("OGG", FILE_TYPE_OGG, "application/ogg");

        addFileType("MID", FILE_TYPE_MID, "audio/midi");
        addFileType("XMF", FILE_TYPE_MID, "audio/midi");
        addFileType("RTTTL", FILE_TYPE_MID, "audio/midi");
        addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi");
        addFileType("IMY", FILE_TYPE_IMY, "audio/imelody");

        addFileType("MP4", FILE_TYPE_MP4, "video/mp4");
        addFileType("M4V", FILE_TYPE_M4V, "video/mp4");
        addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp");
        addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp");
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2");
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2");
        addFileType("WMV", FILE_TYPE_WMV, "video/x-ms-wmv");

        addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg");
        addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg");
        addFileType("GIF", FILE_TYPE_GIF, "image/gif");
        addFileType("PNG", FILE_TYPE_PNG, "image/png");
        addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp");
        addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp");

        addFileType("M3U", FILE_TYPE_M3U, "audio/x-mpegurl");
        addFileType("PLS", FILE_TYPE_PLS, "audio/x-scpls");
        addFileType("WPL", FILE_TYPE_WPL, "application/vnd.ms-wpl");


        addFileType("PDF",FILE_TYPE_PDF,"application/pdf");

        addFileType("PPS",FILE_TYPE_PPS,"application/vnd.ms-powerpoint");
        addFileType("PPT",FILE_TYPE_PPT,"application/vnd.ms-powerpoint");
        addFileType("PPTX",FILE_TYPE_PPTX,"application/vnd.openxmlformats-officedocument.presentationml.presentation");

        addFileType("WPS",FILE_TYPE_WPS,"application/vnd.ms-works");
        addFileType("DOC",FILE_TYPE_DOC,"application/msword");
        addFileType("DOCX",FILE_TYPE_DOCX,"application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        addFileType("XLS",FILE_TYPE_XLS,"application/vnd.ms-excel");
        addFileType("XLSX",FILE_TYPE_XLSX,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");


        addFileType("PROP",FILE_TYPE_PROP,"text/plain");
        addFileType("RC",FILE_TYPE_RC,"text/plain");
        addFileType("SH",FILE_TYPE_SH,"text/plain");
        addFileType("TXT",FILE_TYPE_TXT,"text/plain");
        addFileType("XML",FILE_TYPE_XML,"text/plain");


        addFileType("RTF",FILE_TYPE_RTF,"application/rtf");


        addFileType("TAR",FILE_TYPE_TAR,"application/x-tar");
        addFileType("TGZ",FILE_TYPE_TGZ,"application/x-compressed");
        addFileType("Z",FILE_TYPE_Z,"application/x-compress");
        addFileType("ZIP",FILE_TYPE_ZIP,"application/x-zip-compressed");

        addFileType("APK",FILE_TYPE_APK,"application/vnd.android.package-archive");
        addFileType("EXE",FILE_TYPE_EXE,"application/octet-stream");





        // compute file extensions list for native Media Scanner
        StringBuilder builder = new StringBuilder();
        Iterator<String> iterator = sFileTypeMap.keySet().iterator();

        while (iterator.hasNext()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(iterator.next());
        }
        sFileExtensions = builder.toString();
    }

    public static final String UNKNOWN_STRING = "<unknown>";

    public static boolean isAudioFileType(int fileType) {
        return ((fileType >= FIRST_AUDIO_FILE_TYPE &&
                fileType <= LAST_AUDIO_FILE_TYPE) ||
                (fileType >= FIRST_MIDI_FILE_TYPE &&
                        fileType <= LAST_MIDI_FILE_TYPE));
    }

    public static boolean isVideoFileType(int fileType) {
        return (fileType >= FIRST_VIDEO_FILE_TYPE &&
                fileType <= LAST_VIDEO_FILE_TYPE);
    }

    public static boolean isImageFileType(int fileType) {
        return (fileType >= FIRST_IMAGE_FILE_TYPE &&
                fileType <= LAST_IMAGE_FILE_TYPE);
    }

    public static boolean isPlayListFileType(int fileType) {
        return (fileType >= FIRST_PLAYLIST_FILE_TYPE &&
                fileType <= LAST_PLAYLIST_FILE_TYPE);
    }

    public static boolean isDocFileType(int fileType) {
        return (fileType >= FIRST_DOC_FILE_TYPE &&
                fileType <= LAST_DOC_FILE_TYPE);
    }

    public static boolean isPDFFileType(int fileType) {
        return (fileType ==FILE_TYPE_PDF);
    }
    public static boolean isPptFileType(int fileType) {
        return (fileType >= FIRST_PPT_FILE_TYPE &&
                fileType <= LAST_PPT_FILE_TYPE);
    }
    public static boolean isXlsFileType(int fileType) {
        return (fileType >= FIRST_XLS_FILE_TYPE &&
                fileType <= LAST_XLS_FILE_TYPE);
    }

    public static boolean isApkFileType(int fileType) {
        return (fileType ==FILE_TYPE_APK);
    }

    public static boolean isTxtFileType(int fileType){
        return (fileType ==FILE_TYPE_TXT);
    }

    public static boolean isZipFileType(int fileType){
        return (fileType >= FIRST_ZIP_FILE_TYPE &&
                fileType <= LAST_ZIP_FILE_TYPE);
    }
    public static MediaFileType getFileType(String path) {
        int lastDot = path.lastIndexOf(".");
        if (lastDot < 0)
            return null;
        Log.e("lyq","lastdot "+path.substring(lastDot + 1).toUpperCase());
        return sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase());
    }

    //根据视频文件路径判断文件类型
    public static boolean isVideoFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isVideoFileType(type.fileType);
        }
        return false;
    }

    //根据音频文件路径判断文件类型
    public static boolean isAudioFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isAudioFileType(type.fileType);
        }
        return false;
    }

    //根据mime类型查看文件类型
    public static int getFileTypeForMimeType(String mimeType) {
        Integer value = sMimeTypeMap.get(mimeType);
        return (value == null ? 0 : value.intValue());
    }

    //根据图片文件路径判断文件类型
    public static boolean isImageFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isImageFileType(type.fileType);
        }
        return false;
    }

    //根据文件路径判断文件类型是否是doc
    public static boolean isDocFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isDocFileType(type.fileType);
        }
        return false;
    }

    //根据文件路径判断文件类型是否是pdf
    public static boolean isPdfFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isPDFFileType(type.fileType);
        }
        return false;
    }

    //根据文件路径判断文件类型是否是ppt
    public static boolean isPPTFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isPptFileType(type.fileType);
        }
        return false;
    }

    //根据文件路径判断文件类型是否是xls
    public static boolean isXlsFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isXlsFileType(type.fileType);
        }
        return false;
    }

    //根据文件路径判断文件类型是否是txt
    public static boolean isTxtFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isTxtFileType(type.fileType);
        }
        return false;
    }

    //根据文件路径判断文件类型是否是zip
    public static boolean isZipFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isZipFileType(type.fileType);
        }
        return false;
    }

    //根据文件路径判断文件类型是否是apk
    public static boolean isApkFileTypeForPath(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isApkFileType(type.fileType);
        }
        return false;
    }

    public static String getMimeTypeForFile(String path){
        MediaFileType mediaFileType = getFileType(path);
        return mediaFileType==null?"*/*":mediaFileType.mimeType;
    }
}