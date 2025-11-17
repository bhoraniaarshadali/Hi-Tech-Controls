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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {

    private static final String TAG = "SupabaseClient";

    private static final String PROJECT_URL = "https://bwbopiabddpxhacfscvq.supabase.co";
    private static final String ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ3Ym9waWFiZGRweGhhY2ZzY3ZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE4MzI3MTcsImV4cCI6MjA3NzQwODcxN30.a7TLEki1NW0xpGp1iUYK45X0-WJlWhR3uE81Jbu3a6Q";
    private static final String BUCKET = "uploads";

    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SupabaseClient(Context context) {
        Log.d(TAG, "Initializing OkHttpClient…");
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        Log.d(TAG, "OkHttpClient ready.");
    }

    // -------------------------------------------------------------------------
    // UPLOAD
    // -------------------------------------------------------------------------
    public void uploadMedia(File file, String clientId, UploadCallback callback) {
        Log.d(TAG, "uploadMedia() called → file=" + file + ", clientId=" + clientId);

        if (!file.exists() || file.length() == 0) {
            Log.e(TAG, "uploadMedia() → File missing or ZERO size");
            callback.onError("File missing");
            return;
        }

        String ext = getFileExtension(file.getName());
        String fileName = UUID.randomUUID() + "." + ext;
        String objectPath = "clients/" + clientId + "/" + fileName;

        Log.d(TAG, "uploadMedia() → ext=" + ext + ", objectPath=" + objectPath);

        doUpload(file, objectPath, getMimeType(file), 0, callback);
    }

    private void doUpload(File file, String objectPath, String mime, int attempt, UploadCallback cb) {
        final int MAX_RETRY = 3;
        final long[] backoff = {0, 2000, 4000, 8000};

        Log.d(TAG, "doUpload() attempt=" + attempt +
                ", file=" + file.getAbsolutePath() +
                ", object=" + objectPath +
                ", mime=" + mime);

        String url = PROJECT_URL + "/storage/v1/object/" + BUCKET + "/" + objectPath;
        Log.d(TAG, "Upload URL → " + url);

        RequestBody fileBody =
                RequestBody.create(file, MediaType.parse(mime != null ? mime : "application/octet-stream"));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Authorization", "Bearer " + ANON_KEY)
                .header("apikey", ANON_KEY)
                .build();

        Log.d(TAG, "Executing upload request…");

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Upload failure (attempt " + attempt + "): " + e.getMessage());

                if (attempt < MAX_RETRY) {
                    long wait = backoff[attempt + 1];
                    Log.w(TAG, "Retrying upload in " + wait + " ms");
                    mainHandler.postDelayed(() ->
                            doUpload(file, objectPath, mime, attempt + 1, cb), wait);
                } else {
                    Log.e(TAG, "Upload failed permanently after " + MAX_RETRY + " retries");
                    mainHandler.post(() -> cb.onError("Upload failed"));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = (response.body() != null) ? response.body().string() : "";
                Log.d(TAG, "Upload HTTP " + response.code() + " → response: " + body);

                if (response.isSuccessful()) {
                    String publicUrl = PROJECT_URL + "/storage/v1/object/public/" + BUCKET + "/" + objectPath;
                    Log.d(TAG, "Upload success → URL: " + publicUrl);
                    mainHandler.post(() -> cb.onSuccess(publicUrl));
                } else if (response.code() >= 500 && attempt < MAX_RETRY) {
                    Log.w(TAG, "Server error (5xx). Retrying upload…");
                    long wait = backoff[attempt + 1];
                    mainHandler.postDelayed(() ->
                            doUpload(file, objectPath, mime, attempt + 1, cb), wait);
                } else {
                    Log.e(TAG, "Upload error (no retry) → HTTP " + response.code());
                    mainHandler.post(() -> cb.onError("HTTP " + response.code() + ": " + body));
                }

                response.close();
            }
        });
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------
    public void deleteMedia(String publicUrl, String clientId, DeleteCallback cb) {
        Log.d(TAG, "deleteMedia() → url=" + publicUrl);

        String prefix = "/storage/v1/object/public/" + BUCKET + "/";
        int idx = publicUrl.indexOf(prefix);
        String objectPath = (idx >= 0)
                ? publicUrl.substring(idx + prefix.length())
                : publicUrl;

        Log.d(TAG, "Computed objectPath → " + objectPath);

        String url = PROJECT_URL + "/storage/v1/object/" + BUCKET + "/" + objectPath;
        Log.d(TAG, "Delete URL → " + url);

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
                mainHandler.post(() -> cb.onResult(false));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                boolean ok = response.isSuccessful();
                Log.d(TAG, "Delete response=" + ok + " HTTP=" + response.code());
                response.close();
                mainHandler.post(() -> cb.onResult(ok));
            }
        });
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String ext = (dot > 0) ? fileName.substring(dot + 1).toLowerCase() : "jpg";
        Log.d(TAG, "getFileExtension(" + fileName + ") → " + ext);
        return ext;
    }

    private String getMimeType(File file) {
        String ext = getFileExtension(file.getName());
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);

        if (mime != null) {
            Log.d(TAG, "Detected MIME from extension → " + mime);
            return mime;
        }

        if (ext.equals("mp4")) {
            Log.d(TAG, "MIME fallback → video/mp4");
            return "video/mp4";
        }

        Log.d(TAG, "MIME fallback → image/jpeg");
        return "image/jpeg";
    }

    // -------------------------------------------------------------------------
    // CALLBACKS
    // -------------------------------------------------------------------------
    public interface DeleteCallback {
        void onResult(boolean success);
    }

    public interface UploadCallback {
        void onSuccess(String fileUrl);

        void onError(String error);
    }
}
