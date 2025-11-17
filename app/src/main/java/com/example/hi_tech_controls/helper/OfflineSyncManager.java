package com.example.hi_tech_controls.helper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.hi_tech_controls.ui.activity.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OfflineSyncManager {
    private static OfflineSyncManager instance;
    private final List<Runnable> pendingTasks = new ArrayList<>();
    private boolean isSyncing = false;

    public static OfflineSyncManager getInstance() {
        if (instance == null) instance = new OfflineSyncManager();
        return instance;
    }

    public void addTask(Runnable task) {
        pendingTasks.add(task);
    }

    public void syncNow(Context context) {
        if (isSyncing || pendingTasks.isEmpty()) return;
        isSyncing = true;
        Toast.makeText(context, "Syncing offline changes...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            for (Runnable task : new ArrayList<>(pendingTasks)) {
                task.run();
            }
            pendingTasks.clear();
            isSyncing = false;
            ((BaseActivity) context).runOnUiThread(() ->
                    Toast.makeText(context, "Sync complete!", Toast.LENGTH_SHORT).show());
        }).start();
    }

    public void queuePendingUpdate(String collection, String documentId, Map<String, Object> data) {
        // For now, just log or ignore (no offline queue implemented)
        Log.d("OfflineSync", "Queued update for " + collection + "/" + documentId + ": " + data);
    }


}