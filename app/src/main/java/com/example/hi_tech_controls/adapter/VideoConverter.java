package com.example.hi_tech_controls.adapter;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;

/**
 * SiliCompressor **does NOT have** setCompressQuality().
 * It only compresses to a reasonable size automatically.
 * This helper just forwards the call and returns the compressed file.
 */
public class VideoConverter {

    private static final String TAG = "VideoConverter";

    public static File convertToWebM(Context context, Uri videoUri) {
        try {
            File cacheDir = context.getCacheDir();
            File outFile = new File(cacheDir,
                    "compressed_" + System.currentTimeMillis() + ".mp4");

            // SiliCompressor only needs input URI string + output path
            String resultPath = SiliCompressor.with(context)
                    .compressVideo(videoUri.toString(), outFile.getAbsolutePath());

            File result = new File(resultPath);
            if (result.exists() && result.length() > 0) {
                Log.d(TAG, "Video compressed → " + (result.length() / 1024) + " KB");
                return result;
            } else {
                Log.w(TAG, "Compression produced empty file");
            }
        } catch (Exception e) {
            Log.e(TAG, "Video compression failed", e);
        }
        return null;   // caller should fall back to the original file
    }
}