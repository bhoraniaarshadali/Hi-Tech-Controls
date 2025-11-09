// UploadService.java - FIXED: Indeterminate progress, better logging

package com.example.hi_tech_controls;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.hi_tech_controls.supabaseMedia.SupabaseClient;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadService extends Service {

    private static final String TAG = "UploadService";
    private static final String CHANNEL_ID = "media_upload_channel";
    private static final int NOTIFICATION_ID = 42;
    public static final String ACTION_UPLOAD_FINISHED = "com.example.hi_tech_controls.UPLOAD_FINISHED";

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private SupabaseClient supabase;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        supabase = new SupabaseClient(this);
        Log.d(TAG, "UploadService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String clientId = intent.getStringExtra("clientId");
        String filePath = intent.getStringExtra("filePath");
        int index = intent.getIntExtra("index", -1);

        if (clientId == null || filePath == null || index < 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "File not found: " + filePath);
            stopSelf();
            return START_NOT_STICKY;
        }

        long fileSize = file.length();
        String fileName = file.getName();

        // FIXED: Indeterminate progress
        startForeground(NOTIFICATION_ID, buildNotification(fileName, index));

        // Execute upload
        executor.execute(() -> {
            Log.d(TAG, "Starting upload: " + fileName + " (" + (fileSize / 1024) + " KB)");

            supabase.uploadMedia(file, clientId, new SupabaseClient.UploadCallback() {
                @Override
                public void onSuccess(String url) {
                    Log.d(TAG, "Upload success: " + url);
                    sendBroadcastResult(true, url, index, clientId);
                    stopSelf();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Upload error: " + error);
                    sendBroadcastResult(false, null, index, clientId);
                    stopSelf();
                }
            });
        });

        return START_NOT_STICKY;
    }

    private void sendBroadcastResult(boolean success, String url, int index, String clientId) {
        Intent broadcast = new Intent(ACTION_UPLOAD_FINISHED)
                .putExtra("ok", success)
                .putExtra("url", url)
                .putExtra("index", index)
                .putExtra("clientId", clientId);
        sendBroadcast(broadcast);
    }

    private Notification buildNotification(String fileName, int index) {
        Intent intent = new Intent(this, MediaUpload.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // FIXED: Indeterminate progress
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Uploading Media #" + (index + 1))
                .setContentText(fileName)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(0, 0, true);  // Indeterminate

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Upload",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Uploads images and videos");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        Log.d(TAG, "UploadService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}