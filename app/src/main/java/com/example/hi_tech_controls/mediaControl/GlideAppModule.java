// File: app/src/main/java/com/example/hi_tech_controls/GlideAppModule.java
package com.example.hi_tech_controls.mediaControl;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

@GlideModule
public final class GlideAppModule extends AppGlideModule {

    private static final String TAG = "GlideAppModule";

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {

        Log.d(TAG, "applyOptions() → Initializing enhanced Glide configuration");

        // ---------------------------------------------------------
        // 1) HIGH PERFORMANCE DISK CACHE (200MB internal)
        // ---------------------------------------------------------
        int diskCacheSizeBytes = 200 * 1024 * 1024; // 200 MB
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, diskCacheSizeBytes));
        Log.d(TAG, "Disk cache set → 200MB internal");

        // ---------------------------------------------------------
        // 2) EXECUTORS OPTIMIZED FOR MEDIA GRID
        // ---------------------------------------------------------
        builder.setSourceExecutor(
                GlideExecutor.newUnlimitedSourceExecutor()
        );
        builder.setDiskCacheExecutor(
                GlideExecutor.newUnlimitedSourceExecutor()
        );
        builder.setAnimationExecutor(
                GlideExecutor.newUnlimitedSourceExecutor()
        );
        Log.d(TAG, "Executors → Unlimited source/disk/anim");

        // ---------------------------------------------------------
        // 3) DEFAULT REQUEST OPTIONS → AUTO-DOWNSAMPLE + FAST RENDER
        // ---------------------------------------------------------
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565) // lower memory footprint
                        .disallowHardwareConfig()            // better compatibility
                        .skipMemoryCache(false)
        );
        Log.d(TAG, "Default Request Options → RGB_565, HW disabled");

        // ---------------------------------------------------------
        // 4) DEBUGGING: VERBOSE LOGS FOR ALL STATES
        // ---------------------------------------------------------
        builder.setLogLevel(Log.DEBUG);
        Log.d(TAG, "Glide LogLevel → DEBUG");

        Log.d(TAG, "Glide configuration applied successfully");
    }

    @Override
    public boolean isManifestParsingEnabled() {
        Log.d(TAG, "isManifestParsingEnabled() → false (faster startup)");
        return false; // Prevent manifest scanning → faster startup
    }
}
