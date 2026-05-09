package com.example.hi_tech_controls.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.mediaControl.VideoCompressor;
import com.example.hi_tech_controls.mediaControl.WebPCompressor;
import com.example.hi_tech_controls.supabaseMedia.SupabaseClient;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaUploadActivity extends BaseActivity {

    private static final String TAG = "MediaUploadActivity";
    private static final int CAMERA_REQUEST_BASE = 1000;
    private static final int PERMISSION_REQUEST = 1;
    private static final int MAX_BOXES = 9;

    private final List<ImageView> allImageViews = new ArrayList<>();
    private final List<ImageView> allCheckViews = new ArrayList<>();
    private final List<ImageView> allDeleteButtons = new ArrayList<>();
    private final List<CircularProgressIndicator> allProgressCircles = new ArrayList<>();
    private final List<Uri> mediaUris = new ArrayList<>();
    private final List<String> uploadedUrls = new ArrayList<>();
    private final List<String> fullMediaUrls = new ArrayList<>();
    private final List<Boolean> mediaLoaded = new ArrayList<>();

    private final List<ImageView> allPlayIcons = new ArrayList<>();

    private final ExecutorService ioPool = Executors.newFixedThreadPool(4);
    private final Deque<String> pendingDeletes = new ArrayDeque<>();
    private final List<VideoCompressor> activeCompressors = new ArrayList<>();
    private android.os.PowerManager.WakeLock wakeLock;
    private int currentCaptureIndex = -1;
    private final java.util.concurrent.atomic.AtomicInteger activeTasks = new java.util.concurrent.atomic.AtomicInteger(0);

    private final ActivityResultLauncher<Intent> captureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && currentCaptureIndex != -1) {
                    Intent data = result.getData();
                    Uri uri = (data != null && data.getData() != null) ? data.getData() : mediaUris.get(currentCaptureIndex);
                    if (uri != null) {
                        handleMedia(currentCaptureIndex, uri);
                    }
                }
                currentCaptureIndex = -1;
            }
    );

    private ProgressBar pageProgress;
    private String clientId;
    private SupabaseClient supabase;
    private FirebaseFirestore db;
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_media_upload);

        supabase = new SupabaseClient(this);
        db = FirebaseFirestore.getInstance();
        pageProgress = findViewById(R.id.uploadProgress);

        // Keep CPU awake during media processing
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "HiTech:MediaUpload");
        }

        findViewById(R.id.mediaActivity_Back).setOnClickListener(v -> finish());
        clientId = getIntent().getStringExtra("clientId");
        if (clientId == null || clientId.isEmpty()) {
            Log.e(TAG, "Invalid clientId in intent, finishing activity");
            Toast.makeText(this, "Invalid Client", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView dashTv = findViewById(R.id.dash_tv);
        TextView clientIdTv = findViewById(R.id.clientId_tv);

        if (dashTv != null) {
            dashTv.setText("Images & Videos");
        }
        if (clientIdTv != null) {
            clientIdTv.setText("Client ID: " + clientId);
        }

        Log.d(TAG, "clientId=" + clientId);
        setup9Boxes();
        loadExistingMedia();
        flushPendingDeletesIfAny();
    }

    @Override
    public void onBackPressed() {
        if (activeTasks.get() > 0) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Processing Media...")
                    .setMessage("Your images/videos are being compressed or uploaded. If you leave now, the process may be interrupted. Please wait for it to finish.")
                    .setPositiveButton("STAY & FINISH", null)
                    .setNegativeButton("LEAVE ANYWAY", (dialog, which) -> super.onBackPressed())
                    .setCancelable(false)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private void safeRunOnUiThread(Runnable r) {
        if (isDestroyed || isFinishing()) return;
        runOnUiThread(r);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        isDestroyed = true;

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        // cancel active compressors
        for (VideoCompressor c : activeCompressors) {
            try {
                c.cancel();
            } catch (Exception ignored) {
            }
        }
        activeCompressors.clear();

        ioPool.shutdownNow();
        super.onDestroy();
    }

    private final List<Integer> selectedIndices = new ArrayList<>();
    private View selectionBar;
    private TextView selectionCountTv;
    private boolean isSelectionMode = false;

    private void setup9Boxes() {
        Log.d(TAG, "setup9Boxes()");
        int[] ids = { R.id.box1, R.id.box2, R.id.box3, R.id.box4, R.id.box5, R.id.box6, R.id.box7, R.id.box8,
                R.id.box9 };

        selectionBar = findViewById(R.id.selectionBar);
        selectionCountTv = findViewById(R.id.selectionCountTv);
        findViewById(R.id.selectionCancel).setOnClickListener(v -> exitSelectionMode());
        findViewById(R.id.selectionDelete).setOnClickListener(v -> deleteSelectedMedia());

        findViewById(R.id.mediaActivity_Edit).setOnClickListener(v -> {
            if (isSelectionMode)
                exitSelectionMode();
            else
                enterSelectionMode();
        });

        for (int i = 0; i < MAX_BOXES; i++) {
            View box = findViewById(ids[i]);
            ImageView iv = box.findViewById(R.id.boxImage);
            ImageView check = box.findViewById(R.id.checkMark);
            ImageView del = box.findViewById(R.id.deleteBtn);
            CircularProgressIndicator p = box.findViewById(R.id.progressCircle);
            ImageView play = box.findViewById(R.id.playIcon);

            int idx = i;
            iv.setOnClickListener(v -> {
                if (isSelectionMode) {
                    toggleSelection(idx);
                } else {
                    handleClick(idx);
                }
            });

            iv.setOnLongClickListener(v -> {
                if (fullMediaUrls.get(idx) != null) {
                    if (!isSelectionMode)
                        enterSelectionMode();
                    toggleSelection(idx);
                    return true;
                }
                return false;
            });

            // Hide old delete buttons entirely
            del.setVisibility(View.GONE);

            allImageViews.add(iv);
            allCheckViews.add(check);
            allDeleteButtons.add(del);
            allProgressCircles.add(p);
            allPlayIcons.add(play);
            fullMediaUrls.add(null);
            mediaUris.add(null);
            mediaLoaded.add(false);
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        selectionBar.setVisibility(View.VISIBLE);
        refreshSelectionUI();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedIndices.clear();
        selectionBar.setVisibility(View.GONE);
        refreshSelectionUI();
    }

    private void toggleSelection(int idx) {
        if (fullMediaUrls.get(idx) == null)
            return;

        if (selectedIndices.contains(idx)) {
            selectedIndices.remove(Integer.valueOf(idx));
        } else {
            selectedIndices.add(idx);
        }

        if (selectedIndices.isEmpty() && isSelectionMode) {
            // Optional: stay in mode even if empty, or exit. Let's stay.
        }
        refreshSelectionUI();
    }

    private void refreshSelectionUI() {
        for (int i = 0; i < MAX_BOXES; i++) {
            ImageView check = allCheckViews.get(i);
            boolean isSelected = selectedIndices.contains(i);
            String url = fullMediaUrls.get(i);

            if (url != null) {
                check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                allImageViews.get(i).setAlpha(isSelected ? 0.5f : 1.0f);
            } else {
                check.setVisibility(View.GONE);
                allImageViews.get(i).setAlpha(1.0f);
            }
        }

        selectionCountTv.setText(selectedIndices.size() + " items selected");

        ImageView editIcon = findViewById(R.id.mediaActivity_Edit);
        if (isSelectionMode) {
            editIcon.setAlpha(0.5f); // Indicate active mode
        } else {
            editIcon.setAlpha(1.0f);
        }
        updateEditIconVisibility();
    }

    private void updateEditIconVisibility() {
        ImageView editIcon = findViewById(R.id.mediaActivity_Edit);
        boolean hasMedia = false;
        for (String url : fullMediaUrls) {
            if (url != null) {
                hasMedia = true;
                break;
            }
        }
        editIcon.setVisibility(hasMedia ? View.VISIBLE : View.GONE);
    }

    private void deleteSelectedMedia() {
        if (selectedIndices.isEmpty()) {
            Toast.makeText(this, "Select items to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Media")
                .setMessage("Are you sure you want to delete " + selectedIndices.size() + " selected item(s)?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    List<Integer> targets = new ArrayList<>(selectedIndices);
                    exitSelectionMode(); // Reset UI first

                    for (int idx : targets) {
                        deleteMedia(idx);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleClick(int i) {
        Log.d(TAG, "handleClick() index=" + i);
        String url = fullMediaUrls.get(i);
        if (url == null) {
            Log.d(TAG, "No URL found for index " + i + " — opening picker");
            openPicker(i);
            return;
        }

        if (mediaLoaded.get(i)) {
            Log.d(TAG, "Media already loaded for index " + i + " — opening media");
            openMedia(url);
            return;
        }

        CircularProgressIndicator p = allProgressCircles.get(i);
        p.setVisibility(View.VISIBLE);

        Log.d(TAG, "Fast preload for index " + i + " url=" + url);

        // Glide preload is async. We just update UI and open.
        safeRunOnUiThread(() -> {
            p.setVisibility(View.GONE);
            mediaLoaded.set(i, true);
            openMedia(url);
        });
    }

    private void deleteMedia(int index) {
        Log.d(TAG, "deleteMedia() index=" + index);
        String url = fullMediaUrls.get(index);
        if (url == null) {
            Log.d(TAG, "No media to delete at index " + index);
            return;
        }

        safeRunOnUiThread(() -> {
            fullMediaUrls.set(index, null);
            syncUploadedUrlsWithFullMediaUrls();
            resetBox(index);

            CircularProgressIndicator p = allProgressCircles.get(index);
            p.setVisibility(View.VISIBLE);
            p.setProgress(50);

            Log.d(TAG, "Calling supabase.deleteMedia for url=" + url);
            supabase.deleteMedia(url, clientId, ok -> {
                Log.d(TAG, "supabase.deleteMedia callback ok=" + ok + " for url=" + url);
                if (!ok) {
                    Log.w(TAG, "Delete failed, queuing for later: " + url);
                    queueDelete(url);
                }
                safeRunOnUiThread(() -> {
                    saveUrlsToFirestoreBatched(() -> safeRunOnUiThread(() -> {
                        p.setVisibility(View.GONE);
                        updateEditIconVisibility();
                        Toast.makeText(this, ok ? "Deleted" : "Offline delete queued", Toast.LENGTH_SHORT).show();
                    }));
                });
            });
        });
    }



    private void openPicker(int i) {
        Log.d(TAG, "openPicker() index=" + i);
        if (!checkPerm()) {
            Log.d(TAG, "Permissions not granted — request started");
            return;
        }

        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f = createFile(i);
        Uri u = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
        mediaUris.set(i, u);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, u);

        Intent vid = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        Intent chooser = Intent.createChooser(cam, "Capture");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { vid });
        try {
            currentCaptureIndex = i;
            captureLauncher.launch(chooser);
            Log.d(TAG, "Started capture launcher for index " + i);
        } catch (Exception e) {
            currentCaptureIndex = -1;
            Log.e(TAG, "Failed to launch capture: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    // Removed onActivityResult as we are using captureLauncher

    private void handleMedia(int i, Uri uri) {
        Log.d(TAG, "handleMedia() index=" + i + " uri=" + uri);
        ImageView iv = allImageViews.get(i);
        CircularProgressIndicator p = allProgressCircles.get(i);

        Glide.with(this).load(uri).thumbnail(0.2f).centerCrop().into(iv);

        p.setVisibility(View.VISIBLE);
        p.setProgress(5);

        // Keep CPU awake while processing
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L /* 10 minutes max */);
        }
        activeTasks.incrementAndGet();

        ioPool.execute(() -> {
            try {
                String type = getContentResolver().getType(uri);
                boolean isVideo = type != null && type.startsWith("video/");
                if (!isVideo && uri.toString().contains("video")) isVideo = true; // Fallback
                
                Log.d(TAG, "isVideo=" + isVideo + " type=" + type + " for uri=" + uri);

                if (isVideo) {
                    File outFile = new File(getExternalFilesDir(null),
                            "vid_c_" + System.currentTimeMillis() + "_" + i + ".mp4");

                    safeRunOnUiThread(() -> {
                        p.setIndeterminate(true);
                        Toast.makeText(this, "Compressing video...", Toast.LENGTH_SHORT).show();
                    });

                    VideoCompressor compressor = new VideoCompressor();
                    synchronized (activeCompressors) {
                        activeCompressors.add(compressor);
                    }

                    compressor.compress(this, uri, outFile, new VideoCompressor.Callback() {
                        @Override
                        public void onSuccess(File output) {
                            synchronized (activeCompressors) {
                                activeCompressors.remove(compressor);
                            }
                            Log.d(TAG, "Compression success for index " + i + " -> " + output.getAbsolutePath());
                            safeRunOnUiThread(() -> {
                                p.setIndeterminate(false);
                                p.setProgress(50);
                            });
                            upload(output, i, p);
                        }

                        @Override
                        public void onError(Exception e) {
                            synchronized (activeCompressors) {
                                activeCompressors.remove(compressor);
                            }
                            Log.e(TAG, "Compression error for index " + i + " : " + e.getMessage(), e);
                            safeRunOnUiThread(() -> {
                                p.setIndeterminate(false);
                                p.setProgress(15);
                                Toast.makeText(MediaUploadActivity.this,
                                        "Compression failed, uploading original", Toast.LENGTH_SHORT).show();
                            });

                            try {
                                File fallback = copyFile(uri, ".mp4", i);
                                Log.d(TAG, "Copied MP4 fallback file for index " + i + " path="
                                        + fallback.getAbsolutePath());
                                upload(fallback, i, p);
                            } catch (Exception ex) {
                                Log.e(TAG, "MP4 fallback failed for index " + i + " : " + ex.getMessage(), ex);
                                safeRunOnUiThread(() -> {
                                    p.setVisibility(View.GONE);
                                    Toast.makeText(MediaUploadActivity.this, "Prepare fail", Toast.LENGTH_SHORT).show();
                                    checkReleaseWakeLock();
                                    activeTasks.decrementAndGet();
                                });
                            }
                        }

                        @Override
                        public void onCancelled() {
                            synchronized (activeCompressors) {
                                activeCompressors.remove(compressor);
                            }
                            Log.d(TAG, "Compression cancelled for index " + i);
                            safeRunOnUiThread(() -> {
                                p.setVisibility(View.GONE);
                                Toast.makeText(MediaUploadActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                                checkReleaseWakeLock();
                                activeTasks.decrementAndGet();
                            });
                        }
                    });

                    return;
                }

                Log.d(TAG, "Compressing image to WebP for index " + i);
                File webp = WebPCompressor.compressToWebP(this, uri, 85);
                Log.d(TAG, "WebP compressed for index " + i + " file=" + webp.getAbsolutePath());
                upload(webp, i, p);

            } catch (Exception e) {
                Log.e(TAG, "handleMedia error index " + i + " : " + e.getMessage(), e);
                safeRunOnUiThread(() -> {
                    p.setVisibility(View.GONE);
                    Toast.makeText(this, "Prepare fail", Toast.LENGTH_SHORT).show();
                    checkReleaseWakeLock();
                    activeTasks.decrementAndGet();
                });
            }
        });
    }

    private void upload(File file, int i, CircularProgressIndicator p) {
        Log.d(TAG, "upload() index=" + i + " file=" + (file != null ? file.getName() : "null"));
        runOnUiThread(() -> p.setProgress(40));

        supabase.uploadMedia(file, clientId, new SupabaseClient.UploadCallback() {
            @Override
            public void onSuccess(String url) {
                Log.d(TAG, "upload.onSuccess index=" + i + " url=" + url);
                safeRunOnUiThread(() -> {
                    fullMediaUrls.set(i, url);
                    syncUploadedUrlsWithFullMediaUrls();
                    p.setProgress(90);
                    saveUrlsToFirestoreBatched(() -> safeRunOnUiThread(() -> {
                        boolean isVideo = url.toLowerCase().contains(".mp4");
                        allPlayIcons.get(i).setVisibility(isVideo ? View.VISIBLE : View.GONE);
                        allCheckViews.get(i).setVisibility(View.GONE);
                        updateEditIconVisibility();
                        p.setProgress(100);
                        p.postDelayed(() -> {
                            p.setVisibility(View.GONE);
                            checkReleaseWakeLock();
                            activeTasks.decrementAndGet();
                        }, 400);
                        Toast.makeText(MediaUploadActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                    }));
                });
            }

            @Override
            public void onError(String err) {
                Log.e(TAG, "upload.onError index=" + i + " err=" + err);
                safeRunOnUiThread(() -> {
                    p.setVisibility(View.GONE);
                    Toast.makeText(MediaUploadActivity.this, "Upload fail", Toast.LENGTH_SHORT).show();
                    checkReleaseWakeLock();
                    activeTasks.decrementAndGet();
                });
            }
        });
    }

    private void checkReleaseWakeLock() {
        // If no boxes are showing progress, we can release the wakelock
        boolean anyActive = false;
        for (CircularProgressIndicator p : allProgressCircles) {
            if (p.getVisibility() == View.VISIBLE) {
                anyActive = true;
                break;
            }
        }
        if (!anyActive && wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private File copyFile(Uri uri, String ext, int idx) throws Exception {
        File out = new File(getExternalFilesDir(null), "vid_" + System.currentTimeMillis() + "_" + idx + ext);

        try (InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1)
                fos.write(buf, 0, r);
        }
        Log.d(TAG, "copyFile created " + out.getAbsolutePath());
        return out;
    }

    private void loadExistingMedia() {
        Log.d(TAG, "loadExistingMedia() clientId=" + clientId);
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
                        Log.d(TAG, "Found existing urls count=" + urls.size());
                        // Clear current state and rebuild from Firestore
                        uploadedUrls.clear();
                        uploadedUrls.addAll(urls);
                        for (int i = 0; i < Math.min(urls.size(), MAX_BOXES); i++) {
                            fullMediaUrls.set(i, urls.get(i));
                            displayThumb(i, urls.get(i));
                        }
                        updateEditIconVisibility();
                    } else {
                        Log.d(TAG, "No storage doc or urls empty");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadExistingMedia failed: " + e.getMessage(), e);
                    showPageProgress(false);
                });
    }

    private void syncUploadedUrlsWithFullMediaUrls() {
        uploadedUrls.clear();
        for (String url : fullMediaUrls) {
            if (url != null && !url.isEmpty()) {
                uploadedUrls.add(url);
            }
        }
    }

    private void displayThumb(int i, String url) {
        Log.d(TAG, "displayThumb index=" + i + " url=" + url);
        ImageView iv = allImageViews.get(i);
        ImageView check = allCheckViews.get(i);
        ImageView play = allPlayIcons.get(i);

        RequestOptions o = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).override(220, 220)
                .centerCrop();

        boolean isVideo = url.toLowerCase().contains(".mp4");
        play.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        try {
            if (isVideo)
                Glide.with(this).asBitmap().load(url).apply(o).frame(700_000).into(iv);
            else
                Glide.with(this).load(url).apply(o).thumbnail(.25f).into(iv);
        } catch (Exception e) {
            Log.w(TAG, "Glide displayThumb failed for url=" + url + " : " + e.getMessage());
        }

        check.setVisibility(View.GONE);
        fullMediaUrls.set(i, url);
    }

    private void saveUrlsToFirestoreBatched(Runnable done) {
        Log.d(TAG, "saveUrlsToFirestoreBatched() saving " + uploadedUrls.size() + " urls");
        WriteBatch b = db.batch();
        Map<String, Object> data = new HashMap<>();
        data.put("urls", new ArrayList<>(uploadedUrls));
        data.put("lastUpdated", FieldValue.serverTimestamp());
        b.set(
                db.collection("hi_tech_controls_dataset_JUNE")
                        .document(clientId)
                        .collection("pages")
                        .document("storage"),
                data);
        b.commit().addOnCompleteListener(x -> {
            Log.d(TAG, "saveUrlsToFirestoreBatched commit complete");
            if (done != null)
                done.run();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "saveUrlsToFirestoreBatched failed: " + e.getMessage(), e);
            if (done != null)
                done.run();
        });
    }

    private void openMedia(String url) {
        Log.d(TAG, "openMedia() url=" + url);
        Intent i = new Intent(Intent.ACTION_VIEW);

        if (url.endsWith(".ivf")) {
            Log.d(TAG, "openMedia() IVF chosen — opening in browser");
            Toast.makeText(this, "IVF will open in supported player/browser", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open IVF: " + e.getMessage(), e);
                Toast.makeText(this, "No viewer available", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        i.setDataAndType(Uri.parse(url), url.endsWith(".mp4") ? "video/mp4" : "image/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "openMedia startActivity failed: " + e.getMessage(), e);
            Toast.makeText(this, "No viewer", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPageProgress(boolean s) {
        pageProgress.setVisibility(s ? View.VISIBLE : View.GONE);
    }

    private boolean checkPerm() {
        Log.d(TAG, "checkPerm()");
        String[] p = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        for (String x : p)
            if (ContextCompat.checkSelfPermission(this, x) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions missing, requesting");
                ActivityCompat.requestPermissions(this, p, PERMISSION_REQUEST);
                return false;
            }
        Log.d(TAG, "All permissions granted");
        return true;
    }

    private File createFile(int idx) {
        String n = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File f = new File(getExternalFilesDir(null), n + "_" + idx + ".jpg");
        Log.d(TAG, "createFile idx=" + idx + " path=" + f.getAbsolutePath());
        return f;
    }

    private void resetBox(int idx) {
        Log.d(TAG, "resetBox idx=" + idx);
        allImageViews.get(idx).setImageResource(R.drawable.imageview);
        allCheckViews.get(idx).setVisibility(View.GONE);
        allPlayIcons.get(idx).setVisibility(View.GONE);
        allDeleteButtons.get(idx).setVisibility(View.GONE);
        CircularProgressIndicator p = allProgressCircles.get(idx);
        p.setVisibility(View.GONE);
        p.setProgress(0);
    }

    private void queueDelete(String x) {
        Log.d(TAG, "queueDelete url=" + x);
        pendingDeletes.add(x);
        persistPendingDeletes();
    }

    private void persistPendingDeletes() {
        Log.d(TAG, "persistPendingDeletes() pending count=" + pendingDeletes.size());
        SharedPreferences sp = getSharedPreferences("media_ops", MODE_PRIVATE);
        sp.edit().putString("del_" + clientId, TextUtils.join("||", pendingDeletes)).apply();
    }

    private void flushPendingDeletesIfAny() {
        Log.d(TAG, "flushPendingDeletesIfAny()");
        SharedPreferences sp = getSharedPreferences("media_ops", MODE_PRIVATE);
        String raw = sp.getString("del_" + clientId, "");
        if (raw.isEmpty()) {
            Log.d(TAG, "No pending deletes found");
            return;
        }

        List<String> list = Arrays.asList(raw.split("\\|\\|"));
        Log.d(TAG, "Found pending deletes count=" + list.size());
        ioPool.execute(() -> {
            Deque<String> still = new ArrayDeque<>();
            for (String u : list) {
                final boolean[] done = { false };
                Log.d(TAG, "Attempting pending delete for url=" + u);
                supabase.deleteMedia(u, clientId, ok -> {
                    done[0] = true;
                    if (!ok)
                        still.add(u);
                });
                long t = System.currentTimeMillis();
                while (!done[0] && System.currentTimeMillis() - t < 8000)
                    try {
                        Thread.sleep(120);
                    } catch (Exception ignored) {
                    }
            }
            pendingDeletes.clear();
            pendingDeletes.addAll(still);
            persistPendingDeletes();
            Log.d(TAG, "Pending deletes flush completed. remaining=" + pendingDeletes.size());
        });
    }
}
