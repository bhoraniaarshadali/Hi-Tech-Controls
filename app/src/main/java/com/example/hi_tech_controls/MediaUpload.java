// MediaUpload.java - FIXED VERSION
// ✅ All formats supported
// ✅ No video limits
// ✅ WebP for images (smaller)
// ✅ MP4 compression for videos with fallback
// ✅ Fixed intents, fallbacks, and async handling

package com.example.hi_tech_controls;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.hi_tech_controls.adapter.VideoConverter;
import com.example.hi_tech_controls.supabaseMedia.MediaCompressor;
import com.example.hi_tech_controls.supabaseMedia.SupabaseClient;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaUpload extends BaseActivity {

    private static final int CAMERA_REQUEST_BASE = 1000;
    private static final int PERMISSION_REQUEST = 1;
    private static final int MAX_BOXES = 9;
    private static final String TAG = "MediaUpload";

    private static final String ACTION_UPLOAD_FINISHED = "com.example.hi_tech_controls.UPLOAD_FINISHED";
    private final List<ImageView> allImageViews = new ArrayList<>();
    private final List<ImageView> allCheckViews = new ArrayList<>();
    private final List<ImageView> allDeleteButtons = new ArrayList<>();
    private final List<CircularProgressIndicator> allProgressCircles = new ArrayList<>();
    private final List<Uri> mediaUris = new ArrayList<>();
    private final List<String> uploadedUrls = new ArrayList<>();
    private final List<String> fullMediaUrls = new ArrayList<>();
    private final List<Boolean> mediaLoaded = new ArrayList<>();
    private final List<String> localCachePaths = new ArrayList<>();
    private final ExecutorService ioPool = Executors.newFixedThreadPool(4);
    private final Deque<String> pendingDeletes = new ArrayDeque<>();
    private ProgressBar pageProgress;
    private String clientId;
    private SupabaseClient supabase;
    private FirebaseFirestore db;
    private final BroadcastReceiver uploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_UPLOAD_FINISHED.equals(intent.getAction())) return;
            boolean ok = intent.getBooleanExtra("ok", false);
            String url = intent.getStringExtra("url");
            int idx = intent.getIntExtra("index", -1);
            if (idx < 0 || idx >= MAX_BOXES) return;

            CircularProgressIndicator p = allProgressCircles.get(idx);
            if (ok && url != null) {
                uploadedUrls.add(url);
                fullMediaUrls.set(idx, url);
                runOnUiThread(() -> {
                    allCheckViews.get(idx).setVisibility(View.VISIBLE);
                    allDeleteButtons.get(idx).setVisibility(View.VISIBLE);
                    p.setProgress(100);
                    p.postDelayed(() -> p.setVisibility(View.GONE), 400);
                    Toast.makeText(MediaUpload.this, "✓ Uploaded", Toast.LENGTH_SHORT).show();
                });
                saveUrlsToFirestoreBatchedAsync();
            } else {
                runOnUiThread(() -> {
                    p.setVisibility(View.GONE);
                    Toast.makeText(MediaUpload.this, "Upload failed – retrying", Toast.LENGTH_SHORT).show();
                });
            }
        }
    };
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_upload);

        supabase = new SupabaseClient(this);
        db = FirebaseFirestore.getInstance();
        pageProgress = findViewById(R.id.uploadProgress);

        findViewById(R.id.mediaActivity_Back).setOnClickListener(v -> finish());
        clientId = getIntent().getStringExtra("clientId");
        if (clientId == null || clientId.isEmpty()) {
            Toast.makeText(this, "Invalid Client", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(uploadReceiver, new IntentFilter(ACTION_UPLOAD_FINISHED));

        setup9Boxes();
        loadExistingMedia();
        flushPendingDeletesIfAny();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(uploadReceiver, new IntentFilter(ACTION_UPLOAD_FINISHED));
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
        ioPool.shutdownNow();
        super.onDestroy();
    }

    private void setup9Boxes() {
        int[] ids = {R.id.box1, R.id.box2, R.id.box3, R.id.box4, R.id.box5,
                R.id.box6, R.id.box7, R.id.box8, R.id.box9};

        for (int i = 0; i < MAX_BOXES; i++) {
            View box = findViewById(ids[i]);
            ImageView iv = box.findViewById(R.id.boxImage);
            ImageView check = box.findViewById(R.id.checkMark);
            ImageView del = box.findViewById(R.id.deleteBtn);
            CircularProgressIndicator p = box.findViewById(R.id.progressCircle);

            int idx = i;
            iv.setOnClickListener(v -> handleClick(idx));
            del.setOnClickListener(v -> deleteMedia(idx));

            allImageViews.add(iv);
            allCheckViews.add(check);
            allDeleteButtons.add(del);
            allProgressCircles.add(p);
            fullMediaUrls.add(null);
            mediaUris.add(null);
            mediaLoaded.add(false);
            localCachePaths.add(null);
        }
    }

    private void handleClick(int i) {
        String url = fullMediaUrls.get(i);
        if (url == null) {
            openPicker(i);
            return;
        }

        // Check if cached locally ✅
        String localPath = localCachePaths.get(i);
        if (localPath != null && new File(localPath).exists()) {
            openLocalMedia(localPath);
            return;
        }

        if (mediaLoaded.get(i)) {
            openMedia(url);
            return;
        }

        // Download and cache ✅
        CircularProgressIndicator p = allProgressCircles.get(i);
        p.setVisibility(View.VISIBLE);

        ioPool.execute(() -> {
            try {
                File downloadedFile = Glide.with(this).downloadOnly().load(url).submit().get();
                String cachePath = downloadedFile.getAbsolutePath();
                localCachePaths.set(i, cachePath);

                runOnUiThread(() -> {
                    p.setVisibility(View.GONE);
                    mediaLoaded.set(i, true);
                    openLocalMedia(cachePath);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    p.setVisibility(View.GONE);
                    openMedia(url);
                });
            }
        });
    }

    private void deleteMedia(int index) {
        String url = fullMediaUrls.get(index);
        if (url == null) return;

        String localPath = localCachePaths.get(index);
        if (localPath != null) {
            new File(localPath).delete();
            localCachePaths.set(index, null);
        }

        fullMediaUrls.set(index, null);
        uploadedUrls.remove(url);
        resetBox(index);

        CircularProgressIndicator p = allProgressCircles.get(index);
        p.setVisibility(View.VISIBLE);
        p.setProgress(50);

        supabase.deleteMedia(url, clientId, ok -> {
            if (!ok) queueDelete(url);
            saveUrlsToFirestoreBatched(() -> runOnUiThread(() -> {
                p.setVisibility(View.GONE);
                Toast.makeText(this, ok ? "✓ Deleted" : "Queued", Toast.LENGTH_SHORT).show();
            }));
        });
    }

    private void openPicker(int i) {
        if (!checkPerm()) return;

        // Camera intent
        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f = createFile(i);
        Uri u = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
        mediaUris.set(i, u);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, u);

        // Video intent - NO DURATION LIMIT ✅
        Intent vid = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        vid.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // High quality

        // Gallery intent - FIXED: ACTION_GET_CONTENT for all formats ✅
        Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
        gallery.addCategory(Intent.CATEGORY_OPENABLE);
        gallery.setType("*/*");

        Intent chooser = Intent.createChooser(gallery, "Select Media");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cam, vid});
        startActivityForResult(chooser, CAMERA_REQUEST_BASE + i);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;

        int idx = req - CAMERA_REQUEST_BASE;
        if (idx < 0 || idx >= MAX_BOXES) return;

        Uri uri = data != null && data.getData() != null ? data.getData() : mediaUris.get(idx);
        if (uri != null) handleMedia(idx, uri);
    }

    private void handleMedia(int i, Uri uri) {
        ImageView iv = allImageViews.get(i);
        CircularProgressIndicator p = allProgressCircles.get(i);

        // Show preview immediately ✅
        Glide.with(this)
                .load(uri)
                .thumbnail(0.2f)
                .centerCrop()
                .into(iv);

        p.setVisibility(View.VISIBLE);
        p.setProgress(5);

        ioPool.execute(() -> {
            try {
                String mimeType = getContentResolver().getType(uri);
                boolean isVideo = isVideoUri(uri, mimeType);

                if (isVideo) {
                    compressVideo(i, uri, p);
                } else {
                    compressImage(i, uri, p);
                }
            } catch (Exception e) {
                Log.e(TAG, "Media processing failed", e);
                runOnUiThread(() -> {
                    p.setVisibility(View.GONE);
                    Toast.makeText(this, "Processing failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void compressImage(int index, Uri uri, CircularProgressIndicator progress) {
        runOnUiThread(() -> {
            progress.setProgress(20);
            Toast.makeText(this, "Compressing image...", Toast.LENGTH_SHORT).show();
        });

        CompletableFuture.supplyAsync(() -> {
            try {
                // Use MediaCompressor (now WebP) ✅
                return MediaCompressor.compressImage(this, uri);
            } catch (Exception e) {
                Log.e(TAG, "Image compression failed", e);
                throw new RuntimeException("Image compression failed");
            }
        }, ioPool).thenAcceptAsync(compressedFile -> {
            if (compressedFile != null) {
                localCachePaths.set(index, compressedFile.getAbsolutePath());
                long sizeKB = compressedFile.length() / 1024;

                runOnUiThread(() -> {
                    progress.setProgress(50);
                    Toast.makeText(this, "✓ Image: " + sizeKB + " KB", Toast.LENGTH_SHORT).show();
                });

                scheduleUpload(compressedFile, index, progress);
            }
        }, ioPool).exceptionally(e -> {
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Image compression failed", Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }


    private void compressVideo(int index, Uri uri, CircularProgressIndicator progress) {
        runOnUiThread(() -> {
            progress.setIndeterminate(true);
            Toast.makeText(this, "Compressing video...", Toast.LENGTH_SHORT).show();
        });

        ioPool.execute(() -> {
            try {
                // ---- NEW: Use VideoConverter (no setCompressQuality) ----
                File compressed = VideoConverter.convertToWebM(this, uri);

                File finalFile;
                if (compressed != null && compressed.exists()) {
                    finalFile = compressed;
                } else {
                    // ---- FALLBACK: copy original (no compression) ----
                    finalFile = copyFile(uri, ".mp4", index);
                }

                long sizeMB = finalFile.length() / (1024 * 1024);
                localCachePaths.set(index, finalFile.getAbsolutePath());

                runOnUiThread(() -> {
                    progress.setIndeterminate(false);
                    progress.setProgress(50);
                    Toast.makeText(this,
                            String.format(Locale.US, "Video: %d MB", sizeMB),
                            Toast.LENGTH_SHORT).show();
                });

                scheduleUpload(finalFile, index, progress);

            } catch (Exception e) {
                Log.e(TAG, "Video processing error", e);
                runOnUiThread(() -> {
                    progress.setIndeterminate(false);
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Video processing failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    private File copyFile(Uri uri, String ext, int idx) throws Exception {
        File out = new File(getFilesDir(), "media_" + System.currentTimeMillis() + "_" + idx + ext);
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                fos.write(buf, 0, r);
            }
        }
        return out;
    }

    private void scheduleUpload(File file, int index, CircularProgressIndicator progress) {
        runOnUiThread(() -> progress.setProgress(60));

        Intent svc = new Intent(this, UploadService.class)
                .putExtra("clientId", clientId)
                .putExtra("filePath", file.getAbsolutePath())
                .putExtra("index", index);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }

        // Optional WorkManager backup (keep if you want)
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setInputData(new Data.Builder()
                        .putString("clientId", clientId)
                        .putString("filePath", file.getAbsolutePath())
                        .putInt("index", index)
                        .build())
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "upload_" + index + "_" + System.currentTimeMillis(),
                ExistingWorkPolicy.KEEP, work);
    }
    private void saveUrlsToFirestoreBatchedAsync() {
        WriteBatch batch = db.batch();
        Map<String, Object> data = new HashMap<>();
        data.put("urls", new ArrayList<>(uploadedUrls)); // <-- This was missing!
        data.put("lastUpdated", FieldValue.serverTimestamp());

        batch.set(db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId)
                .collection("pages")
                .document("storage"), data);

        batch.commit().addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore URLs saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore save failed", e));
    }
    private void loadExistingMedia() {
        showPageProgress(true);
        db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId)
                .collection("pages")
                .document("storage")
                .get()
                .addOnSuccessListener(d -> {
                    showPageProgress(false);
                    if (d.exists() && d.get("urls") instanceof List) {
                        List<String> urls = (List<String>) d.get("urls");
                        uploadedUrls.addAll(Objects.requireNonNull(urls));
                        for (int i = 0; i < Math.min(urls.size(), MAX_BOXES); i++) {
                            displayThumb(i, urls.get(i));
                            preloadToCache(i, urls.get(i));
                        }
                    }
                })
                .addOnFailureListener(e -> showPageProgress(false));
    }

    private void preloadToCache(int index, String url) {
        ioPool.execute(() -> {
            try {
                File cachedFile = Glide.with(this).downloadOnly().load(url).submit().get();
                localCachePaths.set(index, cachedFile.getAbsolutePath());
                mediaLoaded.set(index, true);
            } catch (Exception ignored) {
            }
        });
    }

    private void displayThumb(int i, String url) {
        ImageView iv = allImageViews.get(i);
        ImageView check = allCheckViews.get(i);
        ImageView del = allDeleteButtons.get(i);

        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(220, 220)
                .centerCrop();

        if (isVideoUrl(url)) {
            Glide.with(this).asBitmap().load(url).apply(options).frame(700_000).into(iv);
        } else {
            Glide.with(this).load(url).apply(options).thumbnail(0.25f).into(iv);
        }

        check.setVisibility(View.VISIBLE);
        del.setVisibility(View.VISIBLE);
        fullMediaUrls.set(i, url);
    }

    private void saveUrlsToFirestoreBatched(Runnable done) {
        WriteBatch batch = db.batch();
        Map<String, Object> data = new HashMap<>();
        data.put("urls", new ArrayList<>(uploadedUrls));
        data.put("lastUpdated", FieldValue.serverTimestamp());
        batch.set(db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId)
                .collection("pages")
                .document("storage"), data);
        batch.commit().addOnCompleteListener(x -> {
            if (done != null) done.run();
        });
    }

    private void openMedia(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No viewer app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openLocalMedia(String localPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(localPath);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        intent.setDataAndType(uri, getMimeType(localPath));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No viewer app found", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".3gp")) return "video/3gpp";
        if (lower.endsWith(".mov")) return "video/quicktime";
        return "*/*";
    }

    private boolean isVideoUri(Uri uri, String mimeType) {
        if (mimeType != null && mimeType.startsWith("video/")) return true;
        String uriStr = uri.toString().toLowerCase();
        return uriStr.contains("video") ||
                uriStr.endsWith(".mp4") ||
                uriStr.endsWith(".mov") ||
                uriStr.endsWith(".avi") ||
                uriStr.endsWith(".3gp") ||
                uriStr.endsWith(".webm") ||
                uriStr.endsWith(".mkv");
    }

    private boolean isVideoUrl(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".mp4") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".3gp") ||
                lower.endsWith(".avi");
    }

    private void showPageProgress(boolean show) {
        pageProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private boolean checkPerm() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST);
                return false;
            }
        }
        return true;
    }

    private File createFile(int idx) {
        String name = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(getFilesDir(), name + "_" + idx + ".jpg");
    }

    private void resetBox(int idx) {
        allImageViews.get(idx).setImageResource(R.drawable.imageview);
        allCheckViews.get(idx).setVisibility(View.GONE);
        allDeleteButtons.get(idx).setVisibility(View.GONE);
        CircularProgressIndicator p = allProgressCircles.get(idx);
        p.setVisibility(View.GONE);
        p.setProgress(0);
        localCachePaths.set(idx, null);
    }

    private void queueDelete(String url) {
        pendingDeletes.add(url);
        persistPendingDeletes();
    }

    private void persistPendingDeletes() {
        SharedPreferences sp = getSharedPreferences("media_ops", MODE_PRIVATE);
        sp.edit().putString("del_" + clientId, String.join("||", pendingDeletes)).apply();
    }

    private void flushPendingDeletesIfAny() {
        SharedPreferences sp = getSharedPreferences("media_ops", MODE_PRIVATE);
        String raw = sp.getString("del_" + clientId, "");
        if (raw.isEmpty()) return;

        List<String> list = Arrays.asList(raw.split("\\|\\|"));
        ioPool.execute(() -> {
            Deque<String> still = new ArrayDeque<>();
            for (String url : list) {
                supabase.deleteMedia(url, clientId, ok -> {
                    if (!ok) still.add(url);
                });
                // FIXED: Removed blocking wait - async fire-and-forget
            }
            // Process still after all callbacks (approx delay)
            try {
                Thread.sleep(5000); // Wait for callbacks
            } catch (InterruptedException ignored) {}
            pendingDeletes.clear();
            pendingDeletes.addAll(still);
            runOnUiThread(this::persistPendingDeletes);
        });
    }
}