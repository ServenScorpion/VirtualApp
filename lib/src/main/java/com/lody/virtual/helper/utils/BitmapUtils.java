package com.lody.virtual.helper.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Lody
 */
public class BitmapUtils {

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
            return bitmapDrawable.getBitmap();
        } else {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }

    public static Bitmap getBitmapByStream(InputStream drawinput) {
        BitmapFactory.Options moptions = new BitmapFactory.Options();
        moptions.inJustDecodeBounds = false;
        moptions.inPurgeable = true;
        moptions.inInputShareable = true;
        return BitmapFactory.decodeStream(drawinput, null, moptions);
    }

    public static Bitmap getBitmapByFile(String file) {
        InputStream input = null;
        Bitmap b = null;
        try {
            input = new FileInputStream(file);
            b = getBitmapByStream(input);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return b;
    }


    public static Bitmap warrperIcon(Bitmap bitmap, int newWidth, int newHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width < newWidth || height < newHeight) {
            return bitmap;
        }
        float scaleWidth = ((float) newWidth) / (float)width;
        float scaleHeight = ((float) newHeight) / (float)height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}
