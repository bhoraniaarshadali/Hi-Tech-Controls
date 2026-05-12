package com.example.hi_tech_controls.mediaControl;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.abedelazizshe.lightcompressorlibrary.CompressionListener;
import com.abedelazizshe.lightcompressorlibrary.VideoQuality;
import com.abedelazizshe.lightcompressorlibrary.config.AppSpecificStorageConfiguration;
import com.abedelazizshe.lightcompressorlibrary.config.Configuration;

import java.io.File;
import java.util.Collections;

public class VideoCompressor {

    private static final String TAG = "VideoCompressor";
    private volatile boolean cancelled = false;

    public void cancel() {
        cancelled = true;
    }

    public void compress(Context ctx, Uri uri, File output, Callback cb) {
        Log.d(TAG, "Starting fast compression (LightCompressor) for uri: " + uri);

        AppSpecificStorageConfiguration storageConfig = new AppSpecificStorageConfiguration(
                "temp_videos"
        );

        Configuration config = new Configuration(
                VideoQuality.MEDIUM,
                true,    // isMinBitrateCheckEnabled
                null,    // videoBitrateInMbps
                false,   // disableAudio
                false,   // keepOriginalResolution
                Collections.singletonList(output.getName()) // videoNames
        );

        // Using fully qualified name to avoid conflict with this class name
        com.abedelazizshe.lightcompressorlibrary.VideoCompressor.start(
                ctx,
                Collections.singletonList(uri),
                false,
                storageConfig,
                config,
                new CompressionListener() {
                    @Override
                    public void onStart(int index) {
                        Log.d(TAG, "Compression started");
                    }

                    @Override
                    public void onProgress(int index, float percent) {
                    }

                    @Override
                    public void onSuccess(int index, long size, String path) {
                        Log.d(TAG, "Compression success: " + path);
                        if (cancelled) {
                            cb.onCancelled();
                        } else {
                            cb.onSuccess(new File(path));
                        }
                    }

                    @Override
                    public void onFailure(int index, String failureMessage) {
                        Log.e(TAG, "Compression failed: " + failureMessage);
                        cb.onError(new Exception(failureMessage));
                    }

                    @Override
                    public void onCancelled(int index) {
                        Log.d(TAG, "Compression cancelled");
                        cb.onCancelled();
                    }
                }
        );
    }

    public interface Callback {
        void onSuccess(File output);
        void onError(Exception e);
        void onCancelled();
    }
}
