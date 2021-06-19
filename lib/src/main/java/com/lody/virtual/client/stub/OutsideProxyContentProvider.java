package com.lody.virtual.client.stub;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.lody.virtual.client.core.VirtualCore;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class OutsideProxyContentProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    public static Uri toProxyUri(Uri uri) {
        if (!"content".equals(uri.getScheme())) {
            return uri;
        }
        List<String> paths = uri.getPathSegments();
        Uri.Builder builder = new Uri.Builder()
                .scheme(uri.getScheme())
                .authority(StubManifest.PROXY_CP_AUTHORITY_OUTSIDE);
        builder.appendPath(uri.getAuthority());
        for (String path : paths) {
            builder.appendPath(path);
        }
        builder.encodedQuery(uri.getEncodedQuery());
        return builder.build();
    }

    public static Uri toRealUri(Uri uri) {
        if (!"content".equals(uri.getScheme())) {
            return uri;
        }
        if (StubManifest.PROXY_CP_AUTHORITY_OUTSIDE.equals(uri.getAuthority())) {
            List<String> paths = uri.getPathSegments();
            Uri.Builder builder = new Uri.Builder()
                    .scheme(uri.getScheme());
            builder.authority(paths.get(0));
//            builder.appendPath(uri.getAuthority());
            int size = paths.size();
            for (int i = 1; i < size; i++) {
                builder.appendPath(paths.get(i));
            }
            builder.encodedQuery(uri.getEncodedQuery());
            return builder.build();
        }
        return uri;
    }

    private ContentResolver getContentResolver() {
        if (getContext() == null) {
            return VirtualCore.get().getContext().getContentResolver();
        }
        return getContext().getContentResolver();
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return getContentResolver().query(toRealUri(uri), projection, selection, selectionArgs, sortOrder);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return getContentResolver().getType(toRealUri(uri));
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return getContentResolver().insert(toRealUri(uri), values);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return getContentResolver().delete(toRealUri(uri), selection, selectionArgs);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return getContentResolver().update(toRealUri(uri), values, selection, selectionArgs);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder, @Nullable CancellationSignal cancellationSignal) {
        return getContentResolver().query(toRealUri(uri), projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable Bundle queryArgs, @Nullable CancellationSignal cancellationSignal) {
        return getContentResolver().query(toRealUri(uri), projection, queryArgs, cancellationSignal);
    }

    @Nullable
    @Override
    public Uri canonicalize(@NonNull Uri url) {
        return getContentResolver().canonicalize(toRealUri(url));
    }

    @Nullable
    @Override
    public Uri uncanonicalize(@NonNull Uri url) {
        return getContentResolver().uncanonicalize(toRealUri(url));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean refresh(Uri uri, @Nullable Bundle args, @Nullable CancellationSignal cancellationSignal) {
        return getContentResolver().refresh(toRealUri(uri), args, cancellationSignal);
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        return getContentResolver().bulkInsert(toRealUri(uri), values);
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        return getContentResolver().openFileDescriptor(toRealUri(uri), mode, signal);
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        return getContentResolver().openAssetFileDescriptor(toRealUri(uri), mode);
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        return getContentResolver().openAssetFileDescriptor(toRealUri(uri), mode, signal);
    }

    @Nullable
    @Override
    public String[] getStreamTypes(@NonNull Uri uri, @NonNull String mimeTypeFilter) {
        return getContentResolver().getStreamTypes(toRealUri(uri), mimeTypeFilter);
    }

    @Nullable
    @Override
    public AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri, @NonNull String mimeTypeFilter, @Nullable Bundle opts) throws FileNotFoundException {
        return getContentResolver().openTypedAssetFileDescriptor(toRealUri(uri), mimeTypeFilter, opts);
    }

    @Nullable
    @Override
    public AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri, @NonNull String mimeTypeFilter, @Nullable Bundle opts, @Nullable CancellationSignal signal) throws FileNotFoundException {
        return getContentResolver().openTypedAssetFileDescriptor(toRealUri(uri), mimeTypeFilter, opts, signal);
    }

    @NonNull
    @Override
    public <T> ParcelFileDescriptor openPipeHelper(@NonNull Uri uri, @NonNull String mimeType, @Nullable Bundle opts, @Nullable T args, @NonNull PipeDataWriter<T> func) throws FileNotFoundException {
        return super.openPipeHelper(uri, mimeType, opts, args, func);
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String _method, @Nullable String arg, @Nullable Bundle extras) {
        int index = _method.indexOf("@");
        Uri uri = Uri.parse(_method.substring(0, index));
        String method = _method.substring(index + 1);
        return getContentResolver().call(uri, method, arg, extras);
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        return getContentResolver().openFileDescriptor(toRealUri(uri), mode);
    }

}
