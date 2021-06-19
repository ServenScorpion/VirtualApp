package com.xdja.zs;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import java.net.URLDecoder;
import java.net.URLEncoder;


/*适配统一认证，使用scheme方式，盒内外应用相互调用*/
public class UacProxyActivity extends Activity {

    public static String IAM_URI = "xdja";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("wxd", "UacProxyActivity");
        Intent intent = getIntent();
        if(intent != null && intent.getAction().equals("android.intent.action.VIEW")) {
            Uri uri = intent.getData();
            if (uri != null) {
                try {
                    String value_uri = uri.getQueryParameter("real_uri");
                    if(value_uri != null){
                        String real_uri = URLDecoder.decode(value_uri, "UTF-8");
                        Log.e("wxd", "real_uri : " + real_uri);
                        Intent newIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(real_uri));
                        VActivityManager.get().startActivity(newIntent, 0);
                    }else{
                        Log.e("wxd", "Not found uri key real_uri");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        finish();
    }

    public static Intent isHook(Intent intent){
        Uri uri = intent.getData();
        Log.e("wxd", "UacProxyActivity ishook intent " + intent);
        try {
            if (uri.getHost() != null && uri.getHost().equals("iam")) {
                Uri.Builder builder = Uri.parse(uri.toString()).buildUpon();

                String head = "xdja://" + VirtualCore.get().getHostPkg() + "/authorize?";
                String head_enc = URLEncoder.encode(head, "UTF-8");

                builder.appendQueryParameter("agent", head_enc);
                uri = builder.build();
                intent.setData(uri);
                Log.e("wxd", "UacProxyActivity hooked intent " + intent);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return intent;
    }
}
