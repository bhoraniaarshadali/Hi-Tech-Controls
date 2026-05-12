package com.example.hi_tech_controls.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.mediaControl.VideoCompressor;
import com.example.hi_tech_controls.mediaControl.WebPCompressor;
import com.example.hi_tech_controls.supabaseMedia.SupabaseClient;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaUploadActivity extends BaseActivity {

    private static final String TAG = "MediaUploadActivity";
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
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    private final ActivityResultLauncher<Intent> captureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
        if (result.getResultCode() == RESULT_OK && currentCaptureIndex != -1) {
            Intent data = result.getData();
            Uri uri = (data != null && data.getData() != null) ? data.getData() : mediaUris.get(currentCaptureIndex);
            if (uri != null) handleMedia(currentCaptureIndex, uri);
        }
        currentCaptureIndex = -1;
            }
    );

    private ProgressBar pageProgress;
    private String clientId;
    private SupabaseClient supabase;
    private FirebaseFirestore db;
    private boolean isDestroyed = false;
    private ShimmerFrameLayout shimmerLayout;
    private View contentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_upload);

        supabase = new SupabaseClient(this);
        db = FirebaseFirestore.getInstance();
        pageProgress = findViewById(R.id.uploadProgress);
        shimmerLayout = findViewById(R.id.shimmer_layout);
        contentContainer = findViewById(R.id.content_container);

        showShimmer();

        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "HiTech:MediaUpload");

        findViewById(R.id.mediaActivity_Back).setOnClickListener(v -> finish());
        clientId = getIntent().getStringExtra("clientId");
        if (clientId == null || clientId.isEmpty()) {
            Toast.makeText(this, "Invalid Client", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView dashTv = findViewById(R.id.dash_tv);
        TextView clientIdTv = findViewById(R.id.clientId_tv);
        if (dashTv != null) dashTv.setText("Images & Videos");
        if (clientIdTv != null) clientIdTv.setText("Client ID: " + clientId);

        setup9Boxes();
        loadExistingMedia();
        flushPendingDeletesIfAny();

        // Handle back press via dispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (activeTasks.get() > 0) {
                    new androidx.appcompat.app.AlertDialog.Builder(MediaUploadActivity.this)
                            .setTitle("Processing Media...")
                            .setMessage("Your images/videos are being compressed or uploaded. Please wait.")
                            .setPositiveButton("STAY", null)
                            .setNegativeButton("LEAVE", (dialog, which) -> {
                                setEnabled(false);
                                getOnBackPressedDispatcher().onBackPressed();
                            })
                            .show();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }


    private void safeRunOnUiThread(Runnable r) {
        if (!isDestroyed && !isFinishing()) runOnUiThread(r);
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        for (VideoCompressor c : activeCompressors) try { c.cancel(); } catch (Exception ignored) {}
        ioPool.shutdownNow();
        super.onDestroy();
    }

    private View selectionBar;
    private TextView selectionCountTv;
    private boolean isSelectionMode = false;
    private final List<Integer> selectedIndices = new ArrayList<>();

    private void setup9Boxes() {
        int[] ids = { R.id.box1, R.id.box2, R.id.box3, R.id.box4, R.id.box5, R.id.box6, R.id.box7, R.id.box8, R.id.box9 };
        selectionBar = findViewById(R.id.selectionBar);
        selectionCountTv = findViewById(R.id.selectionCountTv);
        findViewById(R.id.selectionCancel).setOnClickListener(v -> exitSelectionMode());
        findViewById(R.id.selectionDelete).setOnClickListener(v -> deleteSelectedMedia());
        findViewById(R.id.mediaActivity_Edit).setOnClickListener(v -> { if (isSelectionMode) exitSelectionMode(); else enterSelectionMode(); });

        for (int i = 0; i < MAX_BOXES; i++) {
            View box = findViewById(ids[i]);
            ImageView iv = box.findViewById(R.id.boxImage);
            ImageView check = box.findViewById(R.id.checkMark);
            CircularProgressIndicator p = box.findViewById(R.id.progressCircle);
            ImageView play = box.findViewById(R.id.playIcon);

            int idx = i;
            iv.setOnClickListener(v -> { if (isSelectionMode) toggleSelection(idx); else handleClick(idx); });
            iv.setOnLongClickListener(v -> { if (fullMediaUrls.get(idx) != null) { if (!isSelectionMode) enterSelectionMode(); toggleSelection(idx); return true; } return false; });

            allImageViews.add(iv);
            allCheckViews.add(check);
            allProgressCircles.add(p);
            allPlayIcons.add(play);
            fullMediaUrls.add(null);
            mediaUris.add(null);
            mediaLoaded.add(false);
            allDeleteButtons.add(box.findViewById(R.id.deleteBtn));
        }
    }

    private void enterSelectionMode() { isSelectionMode = true; selectionBar.setVisibility(View.VISIBLE); refreshSelectionUI(); }
    private void exitSelectionMode() { isSelectionMode = false; selectedIndices.clear(); selectionBar.setVisibility(View.GONE); refreshSelectionUI(); }
    private void toggleSelection(int idx) {
        if (fullMediaUrls.get(idx) == null) return;
        if (selectedIndices.contains(idx)) selectedIndices.remove(Integer.valueOf(idx));
        else selectedIndices.add(idx);
        refreshSelectionUI();
    }

    private void refreshSelectionUI() {
        for (int i = 0; i < MAX_BOXES; i++) {
            boolean sel = selectedIndices.contains(i);
            allCheckViews.get(i).setVisibility(sel ? View.VISIBLE : View.GONE);
            allImageViews.get(i).setAlpha(sel ? 0.5f : 1.0f);
        }
        selectionCountTv.setText(selectedIndices.size() + " items selected");
        updateEditIconVisibility();
    }

    private void updateEditIconVisibility() {
        boolean hasMedia = false;
        for (String u : fullMediaUrls) if (u != null) { hasMedia = true; break; }
        findViewById(R.id.mediaActivity_Edit).setVisibility(hasMedia ? View.VISIBLE : View.GONE);
    }

    private void deleteSelectedMedia() {
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Delete?").setMessage("Delete " + selectedIndices.size() + " items?").setPositiveButton("Delete", (d, w) -> {
            List<Integer> targets = new ArrayList<>(selectedIndices);
            exitSelectionMode();
            for (int idx : targets) deleteMedia(idx);
        }).setNegativeButton("Cancel", null).show();
    }

    private void handleClick(int i) {
        String url = fullMediaUrls.get(i);
        if (url == null) { openPicker(i); return; }
        if (mediaLoaded.get(i)) { openMedia(url); return; }
        CircularProgressIndicator p = allProgressCircles.get(i);
        p.setVisibility(View.VISIBLE);
        String actualUrl = url.contains("|") ? url.split("\\|")[0] : url;
        safeRunOnUiThread(() -> { p.setVisibility(View.GONE); mediaLoaded.set(i, true); openMedia(actualUrl); });
    }

    private void deleteMedia(int index) {
        String url = fullMediaUrls.get(index);
        if (url == null) return;
        safeRunOnUiThread(() -> {
            fullMediaUrls.set(index, null);
            syncUploadedUrlsWithFullMediaUrls();
            resetBox(index);
            CircularProgressIndicator p = allProgressCircles.get(index);
            p.setVisibility(View.VISIBLE); p.setProgress(50);
            supabase.deleteMedia(url, clientId, ok -> {
                if (!ok) queueDelete(url);
                safeRunOnUiThread(() -> saveUrlsToFirestoreBatched(() -> safeRunOnUiThread(() -> {
                    p.setVisibility(View.GONE); updateEditIconVisibility();
                    Toast.makeText(this, ok ? "Deleted" : "Queued", Toast.LENGTH_SHORT).show();
                })));
            });
        });
    }

    private void openPicker(int i) {
        if (!checkPerm()) return;
        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f = createFile(i);
        Uri u = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
        mediaUris.set(i, u);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, u);
        Intent vid = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        vid.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // 0 = Low quality (faster)
        vid.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 90); // 90 seconds limit
        Intent chooser = Intent.createChooser(cam, "Capture");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { vid });
        currentCaptureIndex = i;
        captureLauncher.launch(chooser);
    }

    private void handleMedia(int i, Uri uri) {
        ImageView iv = allImageViews.get(i);
        CircularProgressIndicator p = allProgressCircles.get(i);
        Glide.with(this).load(uri).thumbnail(0.2f).centerCrop().into(iv);
        p.setVisibility(View.VISIBLE); p.setProgress(5);
        if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire(10 * 60 * 1000L);
        activeTasks.incrementAndGet();

        ioPool.execute(() -> {
            try {
                String type = getContentResolver().getType(uri);
                boolean isVideo = (type != null && type.startsWith("video/")) || uri.toString().contains("video");
                if (isVideo) {
                    File outFile = new File(getExternalFilesDir(null), "vid_c_" + System.currentTimeMillis() + "_" + i + ".mp4");
                    safeRunOnUiThread(() -> { p.setIndeterminate(true); Toast.makeText(this, "Compressing...", Toast.LENGTH_SHORT).show(); });
                    VideoCompressor compressor = new VideoCompressor();
                    synchronized (activeCompressors) { activeCompressors.add(compressor); }
                    compressor.compress(this, uri, outFile, new VideoCompressor.Callback() {
                        @Override public void onSuccess(File output) {
                            synchronized (activeCompressors) { activeCompressors.remove(compressor); }
                            safeRunOnUiThread(() -> { p.setIndeterminate(false); p.setProgress(50); });
                            upload(output, i, p);
                        }
                        @Override public void onError(Exception e) {
                            synchronized (activeCompressors) { activeCompressors.remove(compressor); }
                            try { upload(copyFile(uri, ".mp4", i), i, p); } catch (Exception ex) { handleUploadError(ex.getMessage(), p); }
                        }
                        @Override public void onCancelled() { synchronized (activeCompressors) { activeCompressors.remove(compressor); } handleUploadError("Cancelled", p); }
                    });
                } else {
                    upload(WebPCompressor.compressToWebP(this, uri, 78), i, p);
                }
            } catch (Exception e) {
                handleUploadError(e.getMessage(), p);
            }
        });
    }

    private Bitmap extractFrame(File videoFile) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoFile.getAbsolutePath());
            return retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) { return null; }
        finally { try { retriever.release(); } catch (Exception ignored) {} }
    }

    private void upload(File file, int i, CircularProgressIndicator p) {
        boolean isVideo = file.getName().toLowerCase().endsWith(".mp4");
        if (isVideo) {
            Bitmap thumbBmp = extractFrame(file);
            if (thumbBmp != null) {
                try {
                    File thumbFile = WebPCompressor.compressBitmapToWebP(this, thumbBmp, 70, "THUMB");
                    supabase.uploadMedia(thumbFile, clientId, new SupabaseClient.UploadCallback() {
                        @Override public void onSuccess(String tUrl) {
                            supabase.uploadMedia(file, clientId, new SupabaseClient.UploadCallback() {
                                @Override public void onSuccess(String vUrl) { handleUploadSuccess(vUrl + "|" + tUrl, i, p); }
                                @Override public void onError(String e) { handleUploadError(e, p); }
                            });
                        }
                        @Override public void onError(String e) {
                            supabase.uploadMedia(file, clientId, new SupabaseClient.UploadCallback() {
                                @Override public void onSuccess(String vUrl) { handleUploadSuccess(vUrl, i, p); }
                                @Override public void onError(String e) { handleUploadError(e, p); }
                            });
                        }
                    });
                    return;
                } catch (Exception ignored) {}
            }
        }
        supabase.uploadMedia(file, clientId, new SupabaseClient.UploadCallback() {
            @Override public void onSuccess(String url) {
                if (file.exists()) file.delete(); // ✅ Cleanup local file
                handleUploadSuccess(url, i, p);
            }
            @Override public void onError(String e) {
                if (file.exists()) file.delete(); // ✅ Cleanup even on failure
                handleUploadError(e, p);
            }
        });
    }

    private void handleUploadSuccess(String combined, int i, CircularProgressIndicator p) {
        safeRunOnUiThread(() -> {
            p.setProgress(100); p.setVisibility(View.GONE);
            fullMediaUrls.set(i, combined);
            syncUploadedUrlsWithFullMediaUrls();
            displayThumb(i, combined);
            saveUrlsToFirestoreBatched(() -> { checkReleaseWakeLock(); activeTasks.decrementAndGet(); });
            Toast.makeText(this, "Uploaded", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleUploadError(String err, CircularProgressIndicator p) {
        safeRunOnUiThread(() -> { p.setVisibility(View.GONE); checkReleaseWakeLock(); activeTasks.decrementAndGet(); Toast.makeText(this, "Fail: " + err, Toast.LENGTH_SHORT).show(); });
    }

    private void checkReleaseWakeLock() {
        boolean any = false;
        for (CircularProgressIndicator cp : allProgressCircles) if (cp.getVisibility() == View.VISIBLE) { any = true; break; }
        if (!any && wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    private void syncUploadedUrlsWithFullMediaUrls() {
        uploadedUrls.clear();
        for (String u : fullMediaUrls) if (u != null) uploadedUrls.add(u);
    }

    private void loadExistingMedia() {
        showPageProgress(true);
        db.collection("hi_tech_controls_dataset_JUNE").document(clientId).collection("pages").document("storage").get()
            .addOnSuccessListener(d -> new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> { showPageProgress(false); processMediaResults(d); }, 800))
            .addOnFailureListener(e -> new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> showPageProgress(false), 800));
    }

    private void processMediaResults(com.google.firebase.firestore.DocumentSnapshot d) {
        // fullMediaUrls already has 9 nulls from setup9Boxes()
        // NEVER call .add() here — only .set()
        if (d.exists() && d.get("urls") instanceof List) {
            List<String> raw = (List<String>) d.get("urls");
            uploadedUrls.clear();
            for (int i = 0; i < MAX_BOXES; i++) {
                if (i < raw.size() && raw.get(i) != null && !raw.get(i).isEmpty()) {
                    fullMediaUrls.set(i, raw.get(i));
                    uploadedUrls.add(raw.get(i));
                    displayThumb(i, raw.get(i));
                } else {
                    fullMediaUrls.set(i, null);
                    resetBox(i);
                }
            }
            updateEditIconVisibility();
        } else { for (int i = 0; i < MAX_BOXES; i++) { fullMediaUrls.add(null); resetBox(i); } }
    }

    private void displayThumb(int i, String url) {
        if (url == null || url.isEmpty()) { resetBox(i); return; }

        ImageView iv = allImageViews.get(i);
        ImageView play = allPlayIcons.get(i);

        // ✅ Check base URL only (before query params / pipe separator)
        String baseUrl = url.contains("|") ? url.split("\\|")[0] : url;
        String cleanBase = baseUrl.contains("?") ? baseUrl.substring(0, baseUrl.indexOf("?")) : baseUrl;
        boolean isVideo = cleanBase.toLowerCase().endsWith(".mp4")
                || cleanBase.toLowerCase().contains("/video/");

        play.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        String display = url.contains("|") ? url.split("\\|")[1] : url;

        RequestOptions o = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.imageview)
                .override(220, 220)
                .centerCrop();

        try {
            if (isVideo && !url.contains("|")) {
                Glide.with(this).asBitmap().load(url).apply(o).frame(1_000_000).into(iv);
            } else {
                Glide.with(this).load(display).apply(o).into(iv);
            }
        } catch (Exception e) { resetBox(i); }

        allCheckViews.get(i).setVisibility(View.GONE);
    }

    private void saveUrlsToFirestoreBatched(Runnable done) {
        Map<String, Object> data = new HashMap<>();
        data.put("urls", new ArrayList<>(uploadedUrls));
        data.put("lastUpdated", FieldValue.serverTimestamp());
        db.collection("hi_tech_controls_dataset_JUNE").document(clientId).collection("pages").document("storage").set(data, SetOptions.merge())
            .addOnCompleteListener(t -> { if (done != null) done.run(); });
    }

    private void openMedia(String url) {
        if (url == null) return;
        boolean isVideo = url.toLowerCase().contains(".mp4");
        if (isVideo) Toast.makeText(this, "Opening Video...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), isVideo ? "video/*" : "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(intent); } catch (Exception e) { Toast.makeText(this, "No viewer", Toast.LENGTH_SHORT).show(); }
    }

    private void showPageProgress(boolean s) { pageProgress.setVisibility(s ? View.VISIBLE : View.GONE); if (s) showShimmer(); else hideShimmer(); }
    private void showShimmer() { if (shimmerLayout != null) { shimmerLayout.setVisibility(View.VISIBLE); shimmerLayout.startShimmer(); } if (contentContainer != null) contentContainer.setVisibility(View.GONE); }
    private void hideShimmer() { if (shimmerLayout != null) { shimmerLayout.stopShimmer(); shimmerLayout.setVisibility(View.GONE); } if (contentContainer != null) contentContainer.setVisibility(View.VISIBLE); }
    private boolean checkPerm() {
        String[] p = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        for (String x : p) if (ContextCompat.checkSelfPermission(this, x) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, p, PERMISSION_REQUEST); return false; }
        return true;
    }

    private File createFile(int idx) { return new File(getExternalFilesDir(null), "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_" + idx + ".jpg"); }
    private void resetBox(int idx) {
        ImageView iv = allImageViews.get(idx); try { Glide.with(this).clear(iv); } catch (Exception ignored) {}
        iv.setImageResource(R.drawable.imageview);
        allCheckViews.get(idx).setVisibility(View.GONE); allPlayIcons.get(idx).setVisibility(View.GONE);
        allDeleteButtons.get(idx).setVisibility(View.GONE); allProgressCircles.get(idx).setVisibility(View.GONE);
    }

    private void queueDelete(String x) { pendingDeletes.add(x); persistPendingDeletes(); }
    private void persistPendingDeletes() { getSharedPreferences("media_ops", MODE_PRIVATE).edit().putString("del_" + clientId, TextUtils.join("||", pendingDeletes)).apply(); }
    private void flushPendingDeletesIfAny() {
        SharedPreferences prefs = getSharedPreferences("media_ops", MODE_PRIVATE);
        String raw = prefs.getString("del_" + clientId, "");
        if (raw.isEmpty()) return;

        String[] parts = raw.split("\\|\\|");
        pendingDeletes.clear();
        for (String s : parts) {
            if (s != null && !s.trim().isEmpty()) {
                pendingDeletes.add(s);
            }
        }

        Log.d(TAG, "Flushing " + pendingDeletes.size() + " pending deletes");

        for (String u : new ArrayList<>(pendingDeletes)) {
            supabase.deleteMedia(u, clientId, ok -> {
                if (ok) {
                    pendingDeletes.remove(u);
                    Log.d(TAG, "Pending delete success: " + u);
                } else {
                    Log.w(TAG, "Pending delete failed again, will retry next launch: " + u);
                }
                // har callback ke baad persist karo — jo delete hua wo list se hata
                persistPendingDeletes();
            });
        }
    }

    private File copyFile(Uri uri, String ext, int idx) throws Exception {
        File out = new File(getExternalFilesDir(null), "vid_" + System.currentTimeMillis() + "_" + idx + ext);
        try (InputStream in = getContentResolver().openInputStream(uri); FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192]; int r;
            while ((r = in.read(buf)) != -1) fos.write(buf, 0, r);
        }
        return out;
    }
}
