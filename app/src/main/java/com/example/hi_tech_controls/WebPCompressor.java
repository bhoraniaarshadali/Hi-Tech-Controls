package com.example.hi_tech_controls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class WebPCompressor {

    public static File compressToWebP(Context context, Uri imageUri, int quality) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (inputStream != null) {
                inputStream.close();
            }

            // Create output file
            File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File webpFile = new File(outputDir, "compressed_" + System.currentTimeMillis() + ".webp");

            // Compress to WebP
            FileOutputStream out = new FileOutputStream(webpFile);
            bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out);
            out.flush();
            out.close();

            bitmap.recycle();

            return webpFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}