package com.example.hi_tech_controls.mediaControl;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy;
import com.otaliastudios.transcoder.resize.AtMostResizer;

import java.io.File;

public class VideoCompressor {

    private static final String TAG = "VideoCompressor";

    public void cancel() {
        // Implementation for cancellation can be added if needed
    }

    public void compress(Context ctx, Uri uri, File output, Callback cb) {
        Log.d(TAG, "Starting fast compression (Transcoder 0.11.2) for uri: " + uri);

        Transcoder.into(output.getAbsolutePath())
                .addDataSource(ctx, uri)
                .setVideoTrackStrategy(new DefaultVideoStrategy.Builder()
                        .addResizer(new AtMostResizer(480)) // 480p for Ultra-Fast speed
                        .bitRate(1_200_000) // 1.2 Mbps for maximum efficiency
                        .frameRate(24)
                        .build())
                .setListener(new TranscoderListener() {
                    @Override
                    public void onTranscodeProgress(double progress) {
                    }

                    @Override
                    public void onTranscodeCompleted(int successCode) {
                        Log.d(TAG, "Compression success: " + output.getAbsolutePath() + " | size=" + output.length());
                        if (output.exists() && output.length() > 500) {
                            cb.onSuccess(output);
                        } else {
                            Log.e(TAG, "Compressed file corrupted or too small! size=" + output.length());
                            cb.onError(new Exception("Output file corrupted"));
                        }
                    }

                    @Override
                    public void onTranscodeCanceled() {
                        Log.d(TAG, "Compression cancelled");
                        cb.onCancelled();
                    }

                    @Override
                    public void onTranscodeFailed(Throwable exception) {
                        Log.e(TAG, "Compression failed", exception);
                        cb.onError(new Exception(exception));
                    }
                })
                .transcode();
    }

    public interface Callback {
        void onSuccess(File output);
        void onError(Exception e);
        void onCancelled();
    }
}
