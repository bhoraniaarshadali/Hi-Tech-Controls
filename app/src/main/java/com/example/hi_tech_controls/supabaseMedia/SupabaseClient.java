package com.example.hi_tech_controls.supabaseMedia;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static final String BUCKET = "uploads";

    // ✅ Keys ko BuildConfig se lo — hardcode mat karo
    // app/build.gradle mein add karo:
    // buildConfigField "String", "SUPABASE_URL", '"https://xxx.supabase.co"'
    // buildConfigField "String", "SUPABASE_ANON_KEY", '"eyJ..."'
    private static final String PROJECT_URL = com.example.hi_tech_controls.BuildConfig.SUPABASE_URL;
    private static final String ANON_KEY = com.example.hi_tech_controls.BuildConfig.SUPABASE_ANON_KEY;

    private static OkHttpClient sharedClient;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SupabaseClient(Context context) {
        if (sharedClient == null) {
            sharedClient = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS))
                    .build();
        }
        this.client = sharedClient;
    }

    // ── UPLOAD ────────────────────────────────────────────────────────────────
    public void uploadMedia(File file, String clientId, UploadCallback callback) {
        if (!file.exists() || file.length() == 0) {
            callback.onError("File missing or empty");
            return;
        }

        // ✅ Security: Sanitize clientId to prevent path traversal
        String sanitizedId = clientId.replaceAll("[^a-zA-Z0-9]", "");
        String ext = getFileExtension(file.getName());
        String objectPath = "clients/" + sanitizedId + "/" + UUID.randomUUID() + "." + ext;
        doUpload(file, objectPath, getMimeType(file), 0, callback);
    }

    private void doUpload(File file, String objectPath, String mime, int attempt, UploadCallback cb) {
        final int MAX_RETRY = 3;
        final long[] backoff = { 0, 2000, 4000, 8000 };

        String url = PROJECT_URL + "/storage/v1/object/" + BUCKET + "/" + objectPath;

        // ✅ PUT with raw body — Supabase Storage ka correct method
        // Multipart nahi — wo server side pe corrupt file deta tha
        RequestBody body = RequestBody.create(file, MediaType.parse(mime));

        Request req = new Request.Builder()
                .url(url)
                .put(body) // ✅ POST → PUT
                .header("Authorization", "Bearer " + ANON_KEY)
                .header("apikey", ANON_KEY)
                .header("Content-Type", mime)
                .header("x-upsert", "true") // ✅ duplicate name pe overwrite
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Upload failed attempt=" + attempt + ": " + e.getMessage());
                if (attempt < MAX_RETRY) {
                    mainHandler.postDelayed(
                            () -> doUpload(file, objectPath, mime, attempt + 1, cb),
                            backoff[attempt + 1]);
                } else {
                    mainHandler.post(() -> cb.onError("Upload failed after retries"));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Upload HTTP=" + response.code() + " body=" + body);
                response.close();

                if (response.isSuccessful()) {
                    String publicUrl = PROJECT_URL + "/storage/v1/object/public/"
                            + BUCKET + "/" + objectPath;
                    mainHandler.post(() -> cb.onSuccess(publicUrl));

                } else if (response.code() >= 500 && attempt < MAX_RETRY) {
                    mainHandler.postDelayed(
                            () -> doUpload(file, objectPath, mime, attempt + 1, cb),
                            backoff[attempt + 1]);
                } else {
                    mainHandler.post(() -> cb.onError("HTTP " + response.code() + ": " + body));
                }
            }
        });
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public void deleteMedia(String publicUrl, String clientId, DeleteCallback cb) {
        if (publicUrl == null || publicUrl.isEmpty()) {
            cb.onResult(false);
            return;
        }

        // ✅ Pipe-separated URL handle karo (video|thumb format)
        // Dono URLs delete karo agar video hai
        if (publicUrl.contains("|")) {
            String[] parts = publicUrl.split("\\|");
            java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(2);
            java.util.concurrent.atomic.AtomicBoolean overallSuccess = new java.util.concurrent.atomic.AtomicBoolean(true);

            Runnable checkDone = () -> {
                if (remaining.decrementAndGet() == 0) {
                    cb.onResult(overallSuccess.get());
                }
            };

            deleteSingle(parts[0], checkDone, () -> {
                overallSuccess.set(false);
                checkDone.run();
            });

            deleteSingle(parts[1], checkDone, () -> {
                overallSuccess.set(false);
                checkDone.run();
            });

            return;
        }

        deleteSingle(publicUrl, () -> cb.onResult(true), () -> cb.onResult(false));
    }

    private void deleteSingle(String publicUrl, Runnable onOk, Runnable onFail) {
        String prefix = "/storage/v1/object/public/" + BUCKET + "/";
        int idx = publicUrl.indexOf(prefix);
        String objectPath = (idx >= 0)
                ? publicUrl.substring(idx + prefix.length())
                : publicUrl;

        // Query params strip karo agar koi ho
        if (objectPath.contains("?")) {
            objectPath = objectPath.substring(0, objectPath.indexOf("?"));
        }

        String url = PROJECT_URL + "/storage/v1/object/" + BUCKET + "/" + objectPath;

        Request req = new Request.Builder()
                .url(url)
                .delete()
                .header("Authorization", "Bearer " + ANON_KEY)
                .header("apikey", ANON_KEY)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Delete failed: " + e.getMessage());
                mainHandler.post(onFail);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                boolean ok = response.isSuccessful();
                Log.d(TAG, "Delete HTTP=" + response.code());
                response.close();
                mainHandler.post(ok ? onOk : onFail);
            }
        });
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot > 0) ? fileName.substring(dot + 1).toLowerCase() : "jpg";
    }

    private String getMimeType(File file) {
        String ext = getFileExtension(file.getName());
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mime != null)
            return mime;
        if (ext.equals("mp4"))
            return "video/mp4";
        if (ext.equals("webp"))
            return "image/webp";
        return "image/jpeg";
    }

    // ── CALLBACKS ─────────────────────────────────────────────────────────────
    public interface UploadCallback {
        void onSuccess(String fileUrl);

        void onError(String error);
    }

    public interface DeleteCallback {
        void onResult(boolean success);
    }
}