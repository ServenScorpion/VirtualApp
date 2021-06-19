package com.lody.virtual.client.hook.providers;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.MethodBox;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.VLog;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * @author Lody
 */

class DownloadProviderHook extends ExternalProviderHook {

    private static final String TAG = DownloadProviderHook.class.getSimpleName();

    private static final String COLUMN_NOTIFICATION_PACKAGE = "notificationpackage";
    private static final String COLUMN_IS_PUBLIC_API = "is_public_api";
    private static final String COLUMN_OTHER_UID = "otheruid";
    private static final String COLUMN_COOKIE_DATA = "cookiedata";
    private static final String COLUMN_NOTIFICATION_CLASS = "notificationclass";
    private static final String INSERT_KEY_PREFIX = "http_header_";


    private static final String[] ENFORCE_REMOVE_COLUMNS = {
            COLUMN_OTHER_UID,
            COLUMN_NOTIFICATION_CLASS
    };

    DownloadProviderHook(Object base) {
        super(base);
    }

    @Override
    public Uri insert(MethodBox methodBox, Uri url, ContentValues initialValues) throws InvocationTargetException {
        VLog.e("DownloadManager", "insert: " + initialValues);
        String notificationPkg = initialValues.getAsString(COLUMN_NOTIFICATION_PACKAGE);
        if (notificationPkg == null) {
            return methodBox.call();
        }else if("mark.via".equals(notificationPkg)){
            initialValues.put("url","");
        }
        initialValues.put(COLUMN_NOTIFICATION_PACKAGE, VirtualCore.get().getHostPkg());
        if (initialValues.containsKey(COLUMN_COOKIE_DATA)) {
            String cookie = initialValues.getAsString(COLUMN_COOKIE_DATA);
            initialValues.remove(COLUMN_COOKIE_DATA);
            // retrieve the next free INSERT_KEY_PREFIX
            int headerIndex = 0;
            while (initialValues.containsKey(INSERT_KEY_PREFIX + headerIndex)) {
                headerIndex++;
            }
            // add the cookie
            initialValues.put(INSERT_KEY_PREFIX + headerIndex, "Cookie" + ": " + cookie);
        }
        if (!initialValues.containsKey(COLUMN_IS_PUBLIC_API)) {
            initialValues.put(COLUMN_IS_PUBLIC_API, true);
        }
        for (String column : ENFORCE_REMOVE_COLUMNS) {
            initialValues.remove(column);
        }
        initialValues.put("description", VirtualCore.get().getHostPkg());
        return super.insert(methodBox, url, initialValues);
    }

    @Override
    public Cursor query(MethodBox methodBox, Uri url, String[] projection, String selection, String[] selectionArgs, String sortOrder, Bundle originQueryArgs) throws InvocationTargetException {
        VLog.e("DownloadManager", "query : selection: " + selection + ", args: " + Arrays.toString(selectionArgs));
        if ("notificationclass=?".equals(selection)) {
            selection = "description=?";
            selectionArgs = new String[]{VirtualCore.get().getHostPkg()};
            if (BuildCompat.isOreo()) {
                originQueryArgs.remove(QUERY_ARG_SQL_SELECTION);
                originQueryArgs.remove(QUERY_ARG_SQL_SELECTION_ARGS);
                originQueryArgs.putString(QUERY_ARG_SQL_SELECTION, selection);
                originQueryArgs.putStringArray(QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
            } else {
                int start = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? 1 : 0;
                methodBox.args[start + 2] = selection;
                methodBox.args[start + 3] = selectionArgs;
            }
        }
        return super.query(methodBox, url, projection, selection, selectionArgs, sortOrder, originQueryArgs);
    }

}
