// UploadWorker.java - FIXED: Added missing import, better error handling

package com.example.hi_tech_controls;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.example.hi_tech_controls.supabaseMedia.SupabaseClient;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

public class UploadWorker extends ListenableWorker {

    public static final String ACTION_UPLOAD_FINISHED = "com.example.hi_tech_controls.UPLOAD_FINISHED";
    private static final String TAG = "UploadWorker";
    private final SupabaseClient supabase;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        supabase = new SupabaseClient(context);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        String clientId = getInputData().getString("clientId");
        String filePath = getInputData().getString("filePath");
        int index = getInputData().getInt("index", -1);

        if (clientId == null || filePath == null || index < 0) {
            Log.e(TAG, "Invalid input data");
            return CallbackToFutureAdapter.getFuture(completer -> {
                completer.set(Result.failure());
                return "UploadWorker-Invalid";
            });
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "File not found: " + filePath);
            return CallbackToFutureAdapter.getFuture(completer -> {
                completer.set(Result.failure());
                return "UploadWorker-NoFile";
            });
        }

        Log.d(TAG, "Starting background upload: " + file.getName());

        return CallbackToFutureAdapter.getFuture(completer -> {
            supabase.uploadMedia(file, clientId, new SupabaseClient.UploadCallback() {
                @Override
                public void onSuccess(String url) {
                    Log.d(TAG, "Upload success: " + url);
                    sendLocalBroadcast(true, url, index, clientId);
                    completer.set(Result.success());
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Upload error: " + error);
                    sendLocalBroadcast(false, null, index, clientId);

                    // FIXED: Use getRunAttemptCount() correctly
                    if (getRunAttemptCount() < 3) {
                        completer.set(Result.retry());
                    } else {
                        completer.set(Result.failure());
                    }
                }
            });

            return "UploadWorker-" + index;
        });
    }

    private void sendLocalBroadcast(boolean success, String url, int index, String clientId) {
        Intent broadcast = new Intent(ACTION_UPLOAD_FINISHED)
                .putExtra("ok", success)
                .putExtra("url", url)
                .putExtra("index", index)
                .putExtra("clientId", clientId);
        getApplicationContext().sendBroadcast(broadcast);
    }
}