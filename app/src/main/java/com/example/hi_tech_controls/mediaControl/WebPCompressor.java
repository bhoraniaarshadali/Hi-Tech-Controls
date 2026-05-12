package com.example.hi_tech_controls.mediaControl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class WebPCompressor {

    private static final String TAG = "WebPCompressor";

    public static File compressToWebP(Context context, Uri inputUri, int quality) throws Exception {

        // Step 1: Decode bounds only
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is1 = context.getContentResolver().openInputStream(inputUri)) {
            BitmapFactory.decodeStream(is1, null, bounds);
        }

        int w = bounds.outWidth;
        int h = bounds.outHeight;
        Log.d(TAG, "Input: " + w + "x" + h);

        // Step 2: Adaptive scale — repair photos ke liye max 1080p
        float scale = chooseScale(w, h);
        int targetW = Math.max(1, Math.round(w * scale));
        int targetH = Math.max(1, Math.round(h * scale));
        Log.d(TAG, "Target: " + targetW + "x" + targetH);

        // Step 3: Decode with inSampleSize
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // ARGB_8888: WebP lossy ke saath better compression ratio
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inSampleSize = calcSampleSize(w, h, targetW, targetH);
        opts.inJustDecodeBounds = false;

        Bitmap decoded = null;
        try (InputStream is2 = context.getContentResolver().openInputStream(inputUri)) {
            try {
                decoded = BitmapFactory.decodeStream(is2, null, opts);
            } catch (OutOfMemoryError e) {
                Log.w(TAG, "OOM during decode, retrying with higher sampleSize");
                opts.inSampleSize *= 2;
                // Re-open stream for second attempt
                try (InputStream is3 = context.getContentResolver().openInputStream(inputUri)) {
                    decoded = BitmapFactory.decodeStream(is3, null, opts);
                }
            }
        }

        if (decoded == null) throw new Exception("Image decode failed");

        // Step 4: Scale — filter=false for speed (compress use case)
        Bitmap finalBmp;
        if (scale < 1.0f) {
            finalBmp = Bitmap.createScaledBitmap(decoded, targetW, targetH, false);
            if (finalBmp != decoded) decoded.recycle();
        } else {
            finalBmp = decoded; // no scaling needed
        }

        // Step 5: Encode to WebP
        File outFile = new File(
                context.getCacheDir(),
                "IMG_WEBP_" + System.currentTimeMillis() + ".webp"
        );

        try (FileOutputStream out = new FileOutputStream(outFile)) {
            Bitmap.CompressFormat fmt = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    ? Bitmap.CompressFormat.WEBP_LOSSY
                    : Bitmap.CompressFormat.WEBP;
            finalBmp.compress(fmt, quality, out);
            out.flush();
        }

        finalBmp.recycle();
        Log.d(TAG, "WebP saved: " + outFile.length() / 1024 + " KB → " + outFile.getAbsolutePath());

        return outFile;
    }

    public static File compressBitmapToWebP(Context context, Bitmap bmp,
                                            int quality, String prefix) throws Exception {
        File outFile = new File(
                context.getCacheDir(),
                prefix + "_" + System.currentTimeMillis() + ".webp"
        );
        Bitmap.CompressFormat fmt = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                ? Bitmap.CompressFormat.WEBP_LOSSY
                : Bitmap.CompressFormat.WEBP;
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            bmp.compress(fmt, quality, out);
            out.flush();
        }
        return outFile;
    }

    /**
     * Repair photos ke liye: max 1080p maintain karo, quality mat girao zyada
     */
    private static float chooseScale(int w, int h) {
        long mp = (long) w * h;

        if (mp > 15_000_000) return 0.4f;  // 15MP+ (flagship cameras)
        if (mp > 10_000_000) return 0.55f; // 10-15MP
        if (mp > 6_000_000)  return 0.70f; // 6-10MP
        if (mp > 3_000_000)  return 0.85f; // 3-6MP
        return 1.0f;                        // under 3MP — no scaling
    }

    private static int calcSampleSize(int srcW, int srcH, int tgtW, int tgtH) {
        int sample = 1;
        while ((srcW / sample) > tgtW * 2 || (srcH / sample) > tgtH * 2) {
            sample *= 2;
        }
        Log.d(TAG, "inSampleSize = " + sample);
        return sample;
    }
}