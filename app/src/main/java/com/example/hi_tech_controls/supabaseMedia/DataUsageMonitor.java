package com.example.hi_tech_controls.supabaseMedia;

import android.util.Log;

public class DataUsageMonitor {
    private static final String TAG = "DataUsageMonitor";

    private static long totalUploadBytes = 0;
    private static long totalDownloadBytes = 0;
    private static long sessionStartTime = 0;

    public static void initialize() {
        sessionStartTime = System.currentTimeMillis();
        totalUploadBytes = 0;
        totalDownloadBytes = 0;
        Log.i(TAG, "Data usage monitoring initialized");
    }

    public static void addUploadBytes(long bytes) {
        totalUploadBytes += bytes;
        Log.d(TAG, "Uploaded: " + bytes + " bytes | Total upload: " + formatBytes(totalUploadBytes));
    }

    public static void addDownloadBytes(long bytes) {
        totalDownloadBytes += bytes;
        Log.d(TAG, "Downloaded: " + bytes + " bytes | Total download: " + formatBytes(totalDownloadBytes));
    }

    public static String getSessionSummary() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        return String.format(
                "Session Data Usage:\nUpload: %s\nDownload: %s\nTotal: %s\nDuration: %s",
                formatBytes(totalUploadBytes),
                formatBytes(totalDownloadBytes),
                formatBytes(totalUploadBytes + totalDownloadBytes),
                formatDuration(sessionDuration)
        );
    }

    public static void logSessionSummary() {
        Log.i(TAG, getSessionSummary());
    }

    public static void reset() {
        totalUploadBytes = 0;
        totalDownloadBytes = 0;
        sessionStartTime = System.currentTimeMillis();
        Log.i(TAG, "Data usage monitoring reset");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    // Getters for UI display
    public static long getTotalUploadBytes() {
        return totalUploadBytes;
    }

    public static long getTotalDownloadBytes() {
        return totalDownloadBytes;
    }

    public static long getTotalBytes() {
        return totalUploadBytes + totalDownloadBytes;
    }
}