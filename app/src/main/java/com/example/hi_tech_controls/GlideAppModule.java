package com.example.hi_tech_controls;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

@GlideModule
public final class GlideAppModule extends AppGlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // 50 MB disk cache for media files
        int diskCacheSizeBytes = 50 * 1024 * 1024;
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, diskCacheSizeBytes));

        // 10 MB memory cache for faster loads
        int memoryCacheSizeBytes = 10 * 1024 * 1024;
        builder.setMemoryCache(new LruResourceCache(memoryCacheSizeBytes));

        // FIXED: Set default decode format to PREFER_RGB_565 (saves ~50% memory for photos)
        // Use RequestOptions.format(DecodeFormat) – no decodeTypeOf needed here
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565)
        );

        // Unlimited source executor for parallel thumbnail loads
        builder.setDiskCacheExecutor(
                com.bumptech.glide.load.engine.executor.GlideExecutor.newUnlimitedSourceExecutor()
        );
    }
}