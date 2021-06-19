package com.lody.virtual.client.stub.usb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.lody.virtual.R;
import com.lody.virtual.client.stub.ResolverActivity;

import java.util.ArrayList;

public class UsbListChooserActivity extends ResolverActivity {
    public static final String EXTRA_RESOLVE_INFOS = "rlist";

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Parcelable targetParcelable = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (!(targetParcelable instanceof Intent)) {
            superOnCreate(savedInstanceState);
            Log.w("UsbListChooserActivity", "Target is not an intent: " + targetParcelable);
            finish();
            return;
        }
        Intent target = (Intent) targetParcelable;
        ArrayList<ResolveInfo> rList = intent.getParcelableArrayListExtra(EXTRA_RESOLVE_INFOS);
        super.onCreate(savedInstanceState, target, getString(R.string.choose_usb_app), null, rList, false, 0);
    }
}