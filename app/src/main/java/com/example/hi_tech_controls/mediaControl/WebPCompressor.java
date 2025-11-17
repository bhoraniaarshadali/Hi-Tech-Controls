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

    /**
     * Faster + memory-safe + adaptive scaling WebP encoder.
     */
    public static File compressToWebP(Context context, Uri inputUri, int quality) throws Exception {

        // -------- Step 1: Decode bounds only (no bitmap in memory yet)
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        InputStream is1 = context.getContentResolver().openInputStream(inputUri);
        BitmapFactory.decodeStream(is1, null, bounds);
        if (is1 != null) is1.close();

        int w = bounds.outWidth;
        int h = bounds.outHeight;

        Log.d(TAG, "Input image size: " + w + "x" + h);

        // -------- Step 2: Choose scaling ratio (adaptive)
        float scale = chooseScale(w, h);

        int targetW = Math.max(1, Math.round(w * scale));
        int targetH = Math.max(1, Math.round(h * scale));

        Log.d(TAG, "Target scaled size: " + targetW + "x" + targetH);

        // Decode bitmap with inSampleSize (fast, memory-safe)
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;   // low memory
        opts.inSampleSize = calcSampleSize(w, h, targetW, targetH);
        opts.inJustDecodeBounds = false;

        InputStream is2 = context.getContentResolver().openInputStream(inputUri);
        Bitmap decoded = BitmapFactory.decodeStream(is2, null, opts);
        if (is2 != null) is2.close();

        if (decoded == null)
            throw new Exception("Image decode failed");

        // -------- Step 3: Scale to exact target resolution
        Bitmap finalBmp = Bitmap.createScaledBitmap(decoded, targetW, targetH, true);
        if (!decoded.isRecycled()) decoded.recycle();

        // -------- Step 4: Encode to WebP
        File outFile = new File(
                context.getCacheDir(),
                "IMG_WEBP_" + System.currentTimeMillis() + ".webp"
        );

        FileOutputStream out = new FileOutputStream(outFile);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            finalBmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, out);
        } else {
            finalBmp.compress(Bitmap.CompressFormat.WEBP, quality, out);
        }

        out.flush();
        out.close();
        finalBmp.recycle();

        Log.d(TAG, "WEBP saved: " + outFile.getAbsolutePath());

        return outFile;
    }

    /**
     * Decide how much to reduce resolution based on megapixels.
     */
    private static float chooseScale(int w, int h) {
        int mp = (w * h);

        if (mp > 12_000_000) return 0.3f;   // huge image
        if (mp > 8_000_000) return 0.5f;   // large
        if (mp > 4_000_000) return 0.7f;   // medium
        return 1.0f;                         // small (no scaling)
    }

    /**
     * Compute inSampleSize for decoding (power-of-two reduction)
     */
    private static int calcSampleSize(int srcW, int srcH, int tgtW, int tgtH) {
        int sample = 1;

        while ((srcW / sample) > tgtW * 2 || (srcH / sample) > tgtH * 2) {
            sample *= 2;
        }

        Log.d(TAG, "inSampleSize = " + sample);
        return sample;
    }
}
