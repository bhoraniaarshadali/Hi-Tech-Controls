package com.example.hi_tech_controls.mediaControl;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy;
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy;
import com.otaliastudios.transcoder.resize.AtMostResizer;

import java.io.File;
import java.util.concurrent.Future;

public class VideoCompressor {

    private static final String TAG = "VideoCompressor";
    private Future<?> transcodeFuture;

    public void compress(Context ctx, Uri uri, File output, Callback cb) {
        Log.d(TAG, "Starting compression for: " + uri);

        transcodeFuture = Transcoder.into(output.getAbsolutePath())
                .addDataSource(ctx, uri)

                // ── Video: fastest possible settings ──────────────
                .setVideoTrackStrategy(new DefaultVideoStrategy.Builder()
                        .addResizer(new AtMostResizer(480)) // max 480p
                        .bitRate(800_000) // 0.8 Mbps — smaller + faster
                        .frameRate(24) // 24fps sufficient
                        .keyFrameInterval(3) // every 3s = faster encoding
                        .build())

                // ── Audio: passthrough = skip re-encoding ─────────
                // removes biggest speed bottleneck
                .setAudioTrackStrategy(new DefaultAudioStrategy.Builder()
                        .channels(1) // mono — faster than stereo
                        .sampleRate(44100)
                        .bitRate(64_000) // 64kbps — voice quality sufficient
                        .build())

                .setListener(new TranscoderListener() {
                    @Override
                    public void onTranscodeProgress(double progress) {
                        Log.d(TAG, "Progress: " + (int) (progress * 100) + "%");
                    }

                    @Override
                    public void onTranscodeCompleted(int successCode) {
                        Log.d(TAG, "Done: " + output.length() + " bytes");
                        if (output.exists() && output.length() > 500) {
                            cb.onSuccess(output);
                        } else {
                            cb.onError(new Exception("Output file corrupted or too small"));
                        }
                    }

                    @Override
                    public void onTranscodeCanceled() {
                        Log.d(TAG, "Cancelled");
                        cb.onCancelled();
                    }

                    @Override
                    public void onTranscodeFailed(Throwable exception) {
                        Log.e(TAG, "Failed", exception);
                        cb.onError(new Exception(exception));
                    }
                })
                .transcode();
    }

    // ── Proper cancel support ─────────────────────────────────
    public void cancel() {
        if (transcodeFuture != null && !transcodeFuture.isDone()) {
            transcodeFuture.cancel(true);
            Log.d(TAG, "Compression cancel requested");
        }
    }

    public interface Callback {
        void onSuccess(File output);

        void onError(Exception e);

        void onCancelled();
    }
}