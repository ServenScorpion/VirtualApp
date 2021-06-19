package com.lody.virtual.client.stub;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.os.VUserHandle;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

import mirror.android.content.ContentProviderClientICS;
import mirror.android.content.ContentProviderClientJB;
import mirror.android.content.ContentProviderClientQ;

import static android.content.ContentResolver.SCHEME_FILE;

/**
 * @author Lody
 */
public class ContentProviderProxy extends ContentProvider {

    private class TargetProviderInfo {
        int userId;
        ProviderInfo info;
        Uri uri;

        TargetProviderInfo(int userId, ProviderInfo info, Uri uri) {
            this.userId = userId;
            this.info = info;
            this.uri = uri;
        }
    }

    // add by lml@xdja.com
    private static final String[] COLUMNS = {
            OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
    };

    // add by lml@xdja.com
    private static String[] copyOf(String[] original, int newLength) {
        final String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    // add by lml@xdja.com
    private static Object[] copyOf(Object[] original, int newLength) {
        final Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    public static Uri buildProxyUri(int userId, boolean is64bit, String authority, Uri uri) {
        String proxyAuthority = StubManifest.getProxyAuthority(is64bit);
        Uri proxyUriPrefix = Uri.parse(String.format(Locale.ENGLISH, "content://%1$s/%2$d/%3$s", proxyAuthority, userId, authority));
        return Uri.withAppendedPath(proxyUriPrefix, uri.toString());
    }

    // add by lml@xdja.com
    private static Uri getFileUri(Uri uri) {
        if (SCHEME_FILE.equals(uri.getQueryParameter("__va_scheme"))) {
            String path = uri.getEncodedPath();
            final int splitIndex = path.indexOf('/', 1);
            final String tag = Uri.decode(path.substring(1, splitIndex));
            path = Uri.decode(path.substring(splitIndex + 1));
            if ("external".equals(tag))
            {
                int userId = VUserHandle.myUserId();
                File root = VEnvironment.getExternalStorageDirectory(userId);
                File file = new File(root, path);
                Uri u = Uri.fromFile(file);
                return u;
            }
        }
        return null;
    }

    private TargetProviderInfo getProviderProviderInfo(Uri uri) {
        if (!VirtualCore.get().isEngineLaunched()) {
            return null;
        }
        List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() < 3) {
            return null;
        }
        int userId = -1;
        try {
            userId = Integer.parseInt(segments.get(0));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (userId == -1) {
            return null;
        }
        String authority = segments.get(1);
        ProviderInfo providerInfo = VPackageManager.get().resolveContentProvider(authority, 0, userId);
        if (providerInfo == null) {
            return null;
        }
        String uriContent = uri.toString();
        String realUri = uriContent.substring(authority.length() + uriContent.indexOf(authority, 1) + 1 + "content:".length());
        if (realUri.startsWith("/") &&!realUri.startsWith("//")) {
            realUri = "content://" + realUri.substring(realUri.indexOf("/" , 0) + "/".length());
        } else {
            realUri = "content://" + realUri.substring(realUri.indexOf("//" , 0) + "//".length());
        }

        return new TargetProviderInfo(
                userId,
                providerInfo,
                Uri.parse(realUri)
        );
    }

    private ContentProviderClient acquireProviderClient(TargetProviderInfo info) {
        try {
            IInterface provider = VActivityManager.get().acquireProviderClient(info.userId, info.info);
            if (provider != null) {
                if (BuildCompat.isQ()) {
                    String authority = info.uri.getAuthority();
                    return ContentProviderClientQ.ctor.newInstance(getContext().getContentResolver(), provider, authority, true);
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    return ContentProviderClientJB.ctor.newInstance(getContext().getContentResolver(), provider, true);
                } else {
                    return ContentProviderClientICS.ctor.newInstance(getContext().getContentResolver(), provider);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ContentProviderClient acquireTargetProviderClient(Uri uri) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            return acquireProviderClient(info);
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // add by lml@xdja.com
        {
            Uri a = getFileUri(uri);
            if (a != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    getContext().grantUriPermission(getCallingPackage(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                final File file = new File(a.getPath());
                if (projection == null) {
                    projection = COLUMNS;
                }

                String[] cols = new String[projection.length];
                Object[] values = new Object[projection.length];
                int i = 0;
                for (String col : projection) {
                    if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                        cols[i] = OpenableColumns.DISPLAY_NAME;
                        values[i++] = file.getName();
                    } else if (OpenableColumns.SIZE.equals(col)) {
                        cols[i] = OpenableColumns.SIZE;
                        values[i++] = file.length();
                    } else if (MediaStore.MediaColumns.DATA.equals(col)) {
                        cols[i] = MediaStore.MediaColumns.DATA;
                        values[i++] = file.getAbsolutePath();
                    }
                }
                cols = copyOf(cols, i);
                values = copyOf(values, i);
                final MatrixCursor cursor = new MatrixCursor(cols, 1);
                cursor.addRow(values);
                return cursor;
            }
        }
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.query(info.uri, projection, selection, selectionArgs, sortOrder);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        // add by lml@xdja.com
        {
            Uri a = getFileUri(uri);
            if (a != null) {
                final File file = new File(a.getPath());
                final int lastDot = file.getName().lastIndexOf('.');
                if (lastDot >= 0) {
                    final String extension = file.getName().substring(lastDot + 1);
                    final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    if (mime != null) {
                        return mime;
                    }
                }
                return "application/octet-stream";
            }
        }
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.getType(info.uri);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.insert(info.uri, values);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.delete(info.uri, selection, selectionArgs);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.update(info.uri, values, selection, selectionArgs);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public Uri canonicalize(Uri uri) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.canonicalize(info.uri);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public Uri uncanonicalize(Uri uri) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.uncanonicalize(info.uri);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return uri;
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean refresh(Uri uri, Bundle args, CancellationSignal cancellationSignal) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.refresh(info.uri, args, cancellationSignal);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    // add by lml@xdja.com
    private static int modeToMode(String mode) {
        int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        return modeBits;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        {
            Uri a = getFileUri(uri);
            if (a != null) {
                return ParcelFileDescriptor.open(new java.io.File(a.getPath()), modeToMode(mode));
            }
        }
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.openFile(info.uri, mode);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String str) throws FileNotFoundException {
        VLog.e("xela", "openAssetFile : " + uri);
        {
            Uri a = getFileUri(uri);
            if (a != null) {
                ParcelFileDescriptor fd = openFile(uri, str);
                return fd != null ? new AssetFileDescriptor(fd, 0, -1) : null;
            }
        }

        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.openAssetFile(info.uri, str);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.getStreamTypes(info.uri, mimeTypeFilter);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
