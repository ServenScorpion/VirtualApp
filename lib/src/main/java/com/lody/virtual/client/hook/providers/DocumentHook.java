package com.lody.virtual.client.hook.providers;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IInterface;
import android.provider.DocumentsContract;
import android.util.Log;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import mirror.android.content.IContentProvider;

public class DocumentHook extends ProviderHook {
    private interface DocumentsContract {
        String EXTRA_PARENT_URI = "parentUri";
        String EXTRA_PROMPT = "android.provider.extra.PROMPT";
        String EXTRA_RESULT = "result";
        String EXTRA_SHOW_ADVANCED = "android.content.extra.SHOW_ADVANCED";
        String EXTRA_SHOW_FILESIZE = "android.content.extra.SHOW_FILESIZE";
        String EXTRA_TARGET_URI = "android.content.extra.TARGET_URI";
        String EXTRA_URI = "uri";


        String METHOD_COMPRESS_DOCUMENT = "android:compressDocument";
        String METHOD_COPY_DOCUMENT = "android:copyDocument";
        String METHOD_CREATE_DOCUMENT = "android:createDocument";
        String METHOD_DELETE_DOCUMENT = "android:deleteDocument";
        String METHOD_IS_CHILD_DOCUMENT = "android:isChildDocument";
        String METHOD_MOVE_DOCUMENT = "android:moveDocument";
        String METHOD_REMOVE_DOCUMENT = "android:removeDocument";
        String METHOD_RENAME_DOCUMENT = "android:renameDocument";
        String METHOD_UNCOMPRESS_DOCUMENT = "android:uncompressDocument";
    }
    private static final String TAG = DocumentHook.class.getSimpleName();

    public DocumentHook(Object base) {
        super(base);
    }

    protected void processArgs(Method method, Object... args) {
        if (args != null && args.length > 0 && args[0] instanceof String) {
            String pkg = (String) args[0];
            if (VirtualCore.get().isAppInstalled(pkg)) {
                args[0] = VirtualCore.get().getHostPkg();
            }
        }
    }

    public static Uri getOutsideUri(Uri uri) {
        String url = uri.toString();
        if (!url.contains("/secondary") && !url.contains("/primary")) {
            return uri;
        }

        List<String> paths = uri.getPathSegments();
        String auth = uri.getAuthority();
        String scm = uri.getScheme();
        Uri.Builder newUri = new Uri.Builder().authority(auth).scheme(scm);
        for (String path : paths) {
            if (path.startsWith("secondary")) {
                path = path.substring("secondary".length());
                newUri.appendPath(path);
                break;
            }
            newUri.appendPath(path);
        }
        return newUri.build();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            processArgs(method, args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        String name = method.getName();
        int start = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? 1 : 0;
        if (name.equals("query") || name.equals("openTypedAssetFile") || name.equals("openAssetFile")) {
            Uri uri = (Uri) args[start];
            args[start] = getOutsideUri(uri);
            Log.d(TAG, "document uri:" + args[start]);
        } else if (name.equals("call")) {
            String methodName = (String) args[start];
            Bundle extras = (Bundle) args[start + 2];
            if (methodName.startsWith("android:")) {
                Uri documentUri = extras.getParcelable(DocumentsContract.EXTRA_URI);
                Uri targetUri = extras.getParcelable(DocumentsContract.EXTRA_TARGET_URI);
                Uri parentSourceUri = extras.getParcelable(DocumentsContract.EXTRA_PARENT_URI);

                if (documentUri != null) {
                    VLog.d(TAG, "call:methodName:%s, documentUri:%s", methodName, documentUri);
                    extras.putParcelable(DocumentsContract.EXTRA_URI, getOutsideUri(documentUri));
                }
                if (targetUri != null) {
                    VLog.d(TAG, "call:methodName:%s, targetUri:%s", methodName, targetUri);
                    extras.putParcelable(DocumentsContract.EXTRA_TARGET_URI, getOutsideUri(targetUri));
                }
                if (parentSourceUri != null) {
                    VLog.d(TAG, "call:methodName:%s, parentSourceUri:%s", methodName, parentSourceUri);
                    extras.putParcelable(DocumentsContract.EXTRA_PARENT_URI, getOutsideUri(parentSourceUri));
                }
                args[start + 2] = extras;
            } else {
                Log.d(TAG, "call:methodName:" + methodName);
            }
        }
        return method.invoke(mBase, args);
    }
}
