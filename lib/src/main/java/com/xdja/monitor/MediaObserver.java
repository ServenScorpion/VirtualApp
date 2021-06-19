package com.xdja.monitor;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.compat.StrictModeCompat;
import com.lody.virtual.os.VEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MediaObserver {
    private static final MediaObserver ourInstance = new MediaObserver();
    private static final String TAG = "xela-" + new Object() {
    }.getClass().getEnclosingClass().getSimpleName();

    private boolean isMonitoring = false;
    private ContentResolver mContentResolver;

    static MediaObserver getInstance() {
        return ourInstance;
    }

    private ContentObserver mContentObserver;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private MediaObserver() {
        mHandlerThread = new HandlerThread("Screenshot_Observer");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mContentObserver = new ScreenShotObserver(mHandler);
    }

    private final class ScreenShotObserver extends ContentObserver {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri != null) {
                handleMediaContentChange(uri);
            }
        }

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        private ScreenShotObserver(Handler handler) {
            super(handler);
        }

        private void processMedia(@NonNull final Uri uri) {
            long currTime = System.currentTimeMillis();
            final String[] MEDIA_PROJECTIONS = {
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
            };

            final String[] KEYWORDS = {
                    "screenshot", "screen_shot", "screen-shot", "screen shot",
                    "screencapture", "screen_capture", "screen-capture", "screen capture",
                    "screencap", "screen_cap", "screen-cap", "screen cap"
            };

            Cursor cursor = mContentResolver.query(uri, MEDIA_PROJECTIONS, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    final String file_path = cursor.getString(dataIndex);

                    do {
                        boolean found = false;
                        String data = file_path.toLowerCase();
                        for (String keyWork : KEYWORDS) {
                            if (data.contains(keyWork)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            cursor.close();
                            // Log.d(TAG, "this is not a screenshot, pass it");
                            return;
                        }
                    } while (false);

                    int displayIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    String display = cursor.getString(displayIndex);

                    int dateAddedIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED);
                    long dateAdded = cursor.getLong(dateAddedIndex);

                    boolean isTransferOK = false;
                    if (Math.abs(currTime - dateAdded * 1000) <= 1000) {
                        File target = new File(getScreenShotDir(), display);
                        String rootPath = VEnvironment.getDataDirectory().getParentFile().getPath();
                        String internalTarget = target.getPath().substring(rootPath.length());

                        Log.d(TAG, "processMesdi, internalTarget: " + internalTarget);
                        try {
                            // final InputStream inputStream = mContentResolver.openInputStream(uri);
//                            try {
//                                Thread.sleep(500);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
                            File origFile = new File(file_path);
                            final InputStream inputStream = new FileInputStream(origFile);
                            OutputStream outputStream = new FileOutputStream(target);
                            byte[] buf = new byte[2048];
                            int len;
                            while ((len = inputStream.read(buf)) > 0) {
                                outputStream.write(buf, 0, len);
                            }
                            inputStream.close();
                            outputStream.close();
                            scanMediaFile(Uri.parse("file://" + internalTarget));
                            isTransferOK = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // not taken just now
                    }

                    cursor.close();

                    if (isTransferOK) {
                        mContentResolver.delete(uri, null, null);
                        final File sourceFile = new File(file_path);
                        if (sourceFile != null && sourceFile.exists()) {
                            Log.d(TAG, "delete raw file: " + file_path);
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (sourceFile.exists()) {
                                        if (sourceFile.isFile()) {
                                            if (sourceFile.canWrite()) {
                                                boolean del_result = sourceFile.delete();
                                                Log.d(TAG, "delete result : " + del_result);
                                            } else {
                                                Log.d(TAG, "file " + file_path + " can not write");
                                            }
                                        } else {
                                            Log.d(TAG, "file " + file_path + " is not file");
                                        }
                                    } else {
                                        Log.d(TAG, "file " + sourceFile.getAbsolutePath() + " not exists");
                                    }
                                }
                            }, 2000);
                        } else {
                            Log.d(TAG, "file not exist ");
                        }
                    }
                } else {
                    Log.d(TAG, "cursor can not move to first");
                }
            } else {
                Log.d(TAG, "cursor get from query is null");
            }
        }

        private void scanMediaFile(@NonNull Uri uri) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(uri);
            ComponentName componentName = new ComponentName("com.android.providers.media", "com.android.providers.media.MediaScannerReceiver");
            intent.setPackage("com.android.providers.media");
            VActivityManager.get().sendBroadcast(intent, 0);
        }

        private void scanMediaFile(@NonNull File file) {
            scanMediaFile(Uri.fromFile(file));
        }

        private void handleMediaContentChange(@NonNull Uri uri) {
            final String uri_string = uri.toString();
            final String img_internal = MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString();
            final String img_external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString();
            final String video_internal = MediaStore.Video.Media.INTERNAL_CONTENT_URI.toString();
            final String video_external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString();
            if (uri_string.startsWith(img_external) || uri_string.startsWith(img_internal) || uri_string.startsWith(video_internal) || uri_string.startsWith(video_external)) {
                processMedia(uri);
            } else {
                Log.d(TAG, "I don't care a media file like this");
            }
        }
    }

    public static boolean observe(Context context) {
        boolean result = false;

        do {
            if (!StrictModeCompat.disableDeathOnFileUriExposure()) {
                Log.d(TAG, "can't exposed file uri yet");
                break;
            }
            if (context == null) {
                break;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, " I do not have write external storage permission");
                    break;
                }
            }
            result = getInstance().doObserver(context);
        } while (false);

        return result;
    }

    private File getScreenShotDir() {
        File file = VEnvironment.buildPath(VEnvironment.getExternalStorageDirectory(0), "Pictures", "Screenshots");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    private boolean doObserver(Context context) {
        boolean result = false;
        do {
            if (context == null) {
                Log.d(TAG, " monitoring screenshots with a context that is null");
                break;
            }

            if (isMonitoring) {
                Log.d(TAG, context.getPackageName() + " == already monitoring screenshot ==");
                break;
            }
            mContentResolver = context.getContentResolver();
            if (mContentResolver == null) {
                Log.d(TAG, "guess what, I get a NULL ContentResolver");
                break;
            }
            {
                mContentResolver.registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, mContentObserver);
                mContentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mContentObserver);
                mContentResolver.registerContentObserver(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true, mContentObserver);
                mContentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mContentObserver);
                isMonitoring = true;
            }
            result = true;
        } while (false);

        return result;
    }

    public static void unObserve() {
        getInstance().doUnObserve();
    }

    private void doUnObserve() {
        if (isMonitoring) {
            if (mContentResolver != null) {
                mContentResolver.unregisterContentObserver(mContentObserver);
                isMonitoring = false;
            } else {
                Log.e(TAG, " unmonitoring screenshots came with something wrong");
            }
        } else {
            Log.e(TAG, "oops, you haven't been monitoring screenshot yet.");
        }
    }
}
