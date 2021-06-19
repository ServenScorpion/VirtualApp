package com.xdja.zs;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.lody.virtual.client.core.VirtualCore;

public class BoxProvider {

    private static String PROVIDER_AUTH = null;

    /**
     * 当前是否是工作域
     */
    public static boolean isCurrentSpace() {
        Context context = VirtualCore.get().getContext();
        if (PROVIDER_AUTH == null) {
            PROVIDER_AUTH = ProviderInfoUtil.getProviderInfo(context);
        }
        if (PROVIDER_AUTH != null) {
            Uri CONTENT_URI = Uri.parse(PROVIDER_AUTH);
            try {
                Bundle bundle = new Bundle();
                Bundle result = context.getContentResolver().call(CONTENT_URI, "currentSpace", VirtualCore.get().getHostPkg(), bundle);
                return result != null && result.getBoolean("space");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
