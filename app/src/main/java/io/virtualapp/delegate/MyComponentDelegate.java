package io.virtualapp.delegate;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.widget.Toast;

import com.lody.virtual.client.core.AppCallback;

import java.lang.reflect.Field;


import io.virtualapp.delegate.hook.plugin.HttpPlugin;

/**
 * 用于hook应用
 */
public class MyComponentDelegate implements AppCallback {
    private static final String TAG = "MyComponentDelegate";

    Context mContext;

    @Override
    public void beforeStartApplication(String packageName, String processName, Context context) {
    }

    @Override
    public void beforeApplicationCreate(String packageName, String processName, Application application) {
    }

    @Override
    public void afterApplicationCreate(String packageName, String processName, Application application) {
    }

}
