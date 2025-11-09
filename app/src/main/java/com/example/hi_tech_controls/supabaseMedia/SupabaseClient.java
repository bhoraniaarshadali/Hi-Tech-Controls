package com.example.hi_tech_controls.supabaseMedia;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
    private static final String PROJECT_URL = "https://bwbopiabddpxhacfscvq.supabase.co";
    private static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ3Ym9waWFiZGRweGhhY2ZzY3ZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE4MzI3MTcsImV4cCI6MjA3NzQwODcxN30.a7TLEki1NW0xpGp1iUYK45X0-WJlWhR3uE81Jbu3a6Q";
    private static final String BUCKET = "uploads";

    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SupabaseClient(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void uploadMedia(File file, String clientId, UploadCallback callback) {
        if (!file.exists() || file.length() == 0) {
            callback.onError("File missing");
            return;
        }
        String ext = getFileExtension(file.getName());
        String fileName = UUID.randomUUID() + "." + ext;
        String objectPath = "clients/" + clientId + "/" + fileName;

        doUpload(file, objectPath, getMimeType(file), 0, callback);
    }

    private void doUpload(File file, String objectPath, String mime, int attempt, UploadCallback cb) {
        final int MAX_RETRY = 3;
        final long[] backoff = {0, 2000, 4000, 8000};

        String url = PROJECT_URL + "/storage/v1/object/" + BUCKET + "/" + objectPath;
        RequestBody fileBody = RequestBody.create(file, MediaType.parse(mime != null ? mime : "application/octet-stream"));
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

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (attempt < MAX_RETRY) {
                    mainHandler.postDelayed(() -> doUpload(file, objectPath, mime, attempt + 1, cb), backoff[attempt + 1]);
                } else {
                    mainHandler.post(() -> cb.onError("Upload failed"));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    String publicUrl = PROJECT_URL + "/storage/v1/object/public/" + BUCKET + "/" + objectPath;
                    mainHandler.post(() -> cb.onSuccess(publicUrl));
                } else if (response.code() >= 500 && attempt < MAX_RETRY) {
                    mainHandler.postDelayed(() -> doUpload(file, objectPath, mime, attempt + 1, cb), backoff[attempt + 1]);
                } else {
                    mainHandler.post(() -> cb.onError("HTTP " + response.code() + ": " + body));
                }
                response.close();
            }
        });
    }

    public void deleteMedia(String publicUrl, String clientId, DeleteCallback cb) {
        // public: .../object/public/uploads/clients/{clientId}/{file}
        String prefix = "/storage/v1/object/public/" + BUCKET + "/";
        int idx = publicUrl.indexOf(prefix);
        String objectPath = (idx >= 0) ? publicUrl.substring(idx + prefix.length()) : publicUrl;

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
                mainHandler.post(() -> cb.onResult(false));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                boolean ok = response.isSuccessful();
                response.close();
                mainHandler.post(() -> cb.onResult(ok));
            }
        });
    }

    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase() : "jpg";
    }

    private String getMimeType(File file) {
        String ext = getFileExtension(file.getName());
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mime != null) return mime;
        return ext.equals("mp4") ? "video/mp4" : "image/jpeg";
    }

    public interface DeleteCallback {
        void onResult(boolean success);
    }

    public interface UploadCallback {
        void onSuccess(String fileUrl);

        void onError(String error);
    }
}
