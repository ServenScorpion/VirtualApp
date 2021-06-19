package com.xdja.mms;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class LaunchConversationActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //只用来设置默认短信应用
        finish();
    }
}
