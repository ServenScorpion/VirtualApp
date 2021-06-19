package com.lody.virtual.helper.utils;

import android.util.Base64;

/**
 * @author Lody
 */
public class EncodeUtils {

    public static String decodeBase64(String base64) {
        return new String(Base64.decode(base64, Base64.DEFAULT));
    }
}
