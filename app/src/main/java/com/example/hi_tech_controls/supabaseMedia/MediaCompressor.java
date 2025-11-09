// app/src/main/java/com/example/hi_tech_controls/supabaseMedia/MediaCompressor.java
package com.example.hi_tech_controls.supabaseMedia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MediaCompressor {

    public static File compressImage(Context context, Uri uri) throws Exception {
        // Decode with inSampleSize to reduce memory
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, opts);
        }

        int width = opts.outWidth;
        int height = opts.outHeight;
        int sample = 1;
        while (width / sample > 1080 || height / sample > 1080) {
            sample *= 2;
        }
        opts.inSampleSize = sample;
        opts.inJustDecodeBounds = false;

        Bitmap bmp;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }

        File out = new File(context.getCacheDir(), "img_" + System.currentTimeMillis() + ".webp");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.WEBP, 80, fos); // 80% quality WebP
        }
        bmp.recycle();
        return out;
    }
}