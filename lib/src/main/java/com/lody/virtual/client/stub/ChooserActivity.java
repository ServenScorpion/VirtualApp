package com.lody.virtual.client.stub;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;

import com.lody.virtual.R;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.xdja.zs.UacProxyActivity;

public class ChooserActivity extends ResolverActivity {
    public static final String EXTRA_DATA = "android.intent.extra.virtual.data";
    public static final String EXTRA_WHO = "android.intent.extra.virtual.who";
    public static final String EXTRA_INTENT = "android.intent.extra.virtual.intent";
    public static final String EXTRA_REQUEST_CODE = "android.intent.extra.virtual.request_code";
    public static final String EXTRA_IGNORE_DEFAULT = "android.intent.extra.virtual.ignore_default";
    public static final String ACTION;
    public static final String EXTRA_RESULTTO = "_va|ibinder|resultTo";

    static {
        Intent target = new Intent();
        Intent intent = Intent.createChooser(target, "");
        ACTION = intent.getAction();
    }
    public static boolean check(Intent intent) {
        if(intent!=null&&intent.getBooleanExtra("_VA_CHOOSER",false)){
            return false;
        }
        try {

            if(Intent.ACTION_VIEW.equals(intent.getAction())
                    && (intent.getData().toString().startsWith("scheme:") || intent.getData().toString().startsWith(UacProxyActivity.IAM_URI))){
                return false;

            }

            return TextUtils.equals(ACTION, intent.getAction())
                    ||TextUtils.equals(Intent.ACTION_CHOOSER, intent.getAction())
                    ||TextUtils.equals(Intent.ACTION_VIEW,intent.getAction());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        Intent intent = getIntent();
        int userId = extras.getInt(Constants.EXTRA_USER_HANDLE, VUserHandle.getCallingUserId());
        //va api
        mOptions = extras.getParcelable(EXTRA_DATA);
        mResultWho = extras.getString(EXTRA_WHO);
        mRequestCode = extras.getInt(EXTRA_REQUEST_CODE, 0);
        mResultTo = BundleCompat.getBinder(extras, EXTRA_RESULTTO);
        mIgnoreDefault = extras.getBoolean(EXTRA_IGNORE_DEFAULT, false);
        //system api
        Parcelable targetParcelable = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (!(targetParcelable instanceof Intent)) {
            superOnCreate(savedInstanceState);
            VLog.w(TAG, "Target is not an intent: %s", targetParcelable);
            finish();
            return;
        }
        Intent target = (Intent) targetParcelable;
        CharSequence title = intent.getCharSequenceExtra(Intent.EXTRA_TITLE);
        if (title == null) {
            title = getString(R.string.choose);
        }
        Parcelable[] pa = intent.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
        Intent[] initialIntents = null;
        if (pa != null) {
            initialIntents = new Intent[pa.length];
            for (int i = 0; i < pa.length; i++) {
                if (!(pa[i] instanceof Intent)) {
                    VLog.w(TAG, "Initial intent #" + i
                            + " not an Intent: %s", pa[i]);
                    finish();
                    return;
                }
                initialIntents[i] = (Intent) pa[i];
            }
        }
        super.onCreate(savedInstanceState, target, title, initialIntents, null, false, userId);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }
}
