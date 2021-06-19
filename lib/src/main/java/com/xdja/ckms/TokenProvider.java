package com.xdja.ckms;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.xdja.utils.SignatureVerify;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TokenProvider extends ContentProvider {

    public static final String TAG = "xela-TokenProvider";

    public TokenProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO: Implement this to handle query requests from clients.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle result = new Bundle();
        int ret = -1;

        do {
            if ("getToken".equals(method)) {
                int pid = Integer.parseInt(arg);

                List<String> pkg_list = VActivityManager.get().getProcessPkgList(pid);
                if (pkg_list.isEmpty()) {
                    try {
                        InputStream is = getContext().getAssets().open("token.pro");
                        int totalBytes = is.available();
                        byte[] buffer = new byte[totalBytes];
                        int readBytes = 0;
                        int leftBytes = totalBytes - readBytes;
                        while (readBytes < totalBytes) {
                            leftBytes = totalBytes - readBytes;
                            readBytes += is.read(buffer, readBytes, leftBytes);
                        }
                        result.putByteArray("tokenBytes", buffer);
                        String signature = SignatureVerify.getHostSHA1Signature();
                        Log.e(TAG, VirtualCore.get().getHostPkg() + " : " + signature);
                        result.putString("signature", signature);
                        is.close();
                        ret = 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        Resources resources = VirtualCore.get().getResources(pkg_list.get(0));
                        if (resources != null) {
                            InputStream is = resources.getAssets().open("token.pro");
                            String filePath = getContext().getCacheDir() + "/" + pkg_list.get(0)+".token.pro";
                            File ckmsToken = new File(filePath);
                            //FileOutputStream fileOutputStream = new FileOutputStream(ckmsToken);
                            int totalBytes = is.available();
                            byte[] buffer = new byte[totalBytes];
                            int readBytes = 0;
                            int leftBytes = totalBytes - readBytes;
                            while (readBytes < totalBytes) {
                                leftBytes = totalBytes - readBytes;
                                readBytes += is.read(buffer, readBytes, leftBytes);
                            }

                            result.putByteArray("tokenBytes", buffer);
                            String signature = SignatureVerify.getSHA1Signature(pkg_list.get(0));
                            Log.e(TAG, pkg_list.get(0) + " : " + signature);
                            result.putString("signature", signature);
                            is.close();

                            ret = 0;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } while (false);

        result.putInt("ret", ret);
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
