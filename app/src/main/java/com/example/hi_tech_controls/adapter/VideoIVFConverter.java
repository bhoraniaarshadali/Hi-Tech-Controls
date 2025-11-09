package com.example.hi_tech_controls.adapter;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class VideoIVFConverter {

    /**
     * STUB: Real WebM conversion requires FFMpeg or MediaCodec.
     * This stub will log the intent and use the onFallback method,
     * which copies the original file to the cache as requested.
     */
    public static void convertToWebM(Context ctx, Uri uri, File outFile, Callback cb) {
        // --- REAL CONVERSION LOGIC WOULD GO HERE ---
        // For example, using FFMpeg:
        // FFmpeg.execute("-i " + uri.toString() + " -c:v libvpx -b:v 1M -c:a libvorbis " + outFile.getPath());
        //
        // Since that is complex, we will just use the fallback path
        // to demonstrate the "store in cache" logic.

        System.out.println("WebMConverter: Real conversion not implemented. Using fallback.");

        try {
            // Copy the original video to the cache directory
            File fallbackFile = copyFileToCache(ctx, uri, ".mp4");
            cb.onFallback(fallbackFile);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }

    private static File copyFileToCache(Context ctx, Uri uri, String ext) throws IOException {
        File out = new File(ctx.getCacheDir(), "temp_vid_" + System.currentTimeMillis() + ext);
        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                fos.write(buf, 0, r);
            }
        }
        return out;
    }

    public interface Callback {
        void onSuccess(File f);

        void onFallback(File f); // Fallback to original

        void onError(String m);
    }
}