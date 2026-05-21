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
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import android.app.Dialog;
import android.widget.VideoView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private final List<ProgressBar> allProgressCircles = new ArrayList<>();
    private final List<Uri> mediaUris = new ArrayList<>();
    private final List<String> uploadedUrls = new ArrayList<>();
    private final List<String> fullMediaUrls = new ArrayList<>();
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
        } else if (currentCaptureIndex != -1) {
            Uri uri = mediaUris.get(currentCaptureIndex);
            if (uri != null && uri.toString().startsWith("content://media/")) {
                try {
                    getContentResolver().delete(uri, null, null);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete cancelled MediaStore entry", e);
                }
            }
            mediaUris.set(currentCaptureIndex, null);
        }
        currentCaptureIndex = -1;
            }
    );

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && currentCaptureIndex != -1) {
                    handleMedia(currentCaptureIndex, uri);
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
        if (savedInstanceState != null) {
            currentCaptureIndex = savedInstanceState.getInt("currentCaptureIndex", -1);
            ArrayList<Uri> restoredUris = savedInstanceState.getParcelableArrayList("mediaUris");
            if (restoredUris != null) {
                for (int i = 0; i < restoredUris.size() && i < mediaUris.size(); i++) {
                    mediaUris.set(i, restoredUris.get(i));
                }
            }
        }
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
            ProgressBar p = box.findViewById(R.id.progressCircle);
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
        openMedia(url);
    }

    private void deleteMedia(int index) {
        String url = fullMediaUrls.get(index);
        if (url == null) return;
        safeRunOnUiThread(() -> {
            fullMediaUrls.set(index, null);
            syncUploadedUrlsWithFullMediaUrls();
            resetBox(index);
            ProgressBar p = allProgressCircles.get(index);
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

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_media_chooser);

        dialog.findViewById(R.id.btnCameraPhoto).setOnClickListener(v -> {
            dialog.dismiss();
            launchCameraPhoto(i);
        });

        dialog.findViewById(R.id.btnCameraVideo).setOnClickListener(v -> {
            dialog.dismiss();
            launchCameraVideo(i);
        });

        dialog.findViewById(R.id.btnGalleryPhoto).setOnClickListener(v -> {
            dialog.dismiss();
            currentCaptureIndex = i;
            galleryLauncher.launch("image/*");
        });

        dialog.findViewById(R.id.btnGalleryVideo).setOnClickListener(v -> {
            dialog.dismiss();
            currentCaptureIndex = i;
            galleryLauncher.launch("video/*");
        });

        dialog.show();
    }

    private void launchCameraPhoto(int i) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "IMG_" + System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + "_" + i + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HiTechControls");
            Uri u = null;
            try {
                u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } catch (Exception e) {
                Log.e(TAG, "Failed to insert image into MediaStore", e);
            }
            if (u != null) {
                mediaUris.set(i, u);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, u);
            } else {
                File f = createFile(i);
                Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
                mediaUris.set(i, fileUri);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            }
        } else {
            File f = createFile(i);
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
            mediaUris.set(i, u);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, u);
        }
        currentCaptureIndex = i;
        captureLauncher.launch(intent);
    }

    private void launchCameraVideo(int i) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(MediaStore.Video.Media.TITLE, "VID_" + System.currentTimeMillis());
            values.put(MediaStore.Video.Media.DISPLAY_NAME, "VID_" + System.currentTimeMillis() + "_" + i + ".mp4");
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HiTechControls");
            Uri u = null;
            try {
                u = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            } catch (Exception e) {
                Log.e(TAG, "Failed to insert video into MediaStore", e);
            }
            if (u != null) {
                mediaUris.set(i, u);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, u);
            } else {
                File f = createVideoFile(i);
                Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
                mediaUris.set(i, fileUri);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            }
        } else {
            File f = createVideoFile(i);
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
            mediaUris.set(i, u);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, u);
        }
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // 0 = Low quality (faster)
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 90); // 90 seconds limit
        currentCaptureIndex = i;
        captureLauncher.launch(intent);
    }

    private void handleMedia(int i, Uri uri) {
        ImageView iv = allImageViews.get(i);
        ProgressBar p = allProgressCircles.get(i);
        Glide.with(this).load(uri).thumbnail(0.2f).centerCrop().into(iv);
        p.setVisibility(View.VISIBLE); p.setProgress(5);
        if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire(10 * 60 * 1000L);
        activeTasks.incrementAndGet();

        ioPool.execute(() -> {
            try {
                boolean isVideo = isVideoUri(uri);
                if (isVideo) {
                    // Copy to local file to ensure reliable permission-free access for transcoder library in background
                    File localInputFile;
                    if (uri.getScheme() != null && uri.getScheme().equals("file")) {
                        localInputFile = new File(uri.getPath());
                    } else {
                        localInputFile = copyFile(uri, ".mp4", i);
                    }

                    File outFile = new File(getExternalFilesDir(null), "vid_c_" + System.currentTimeMillis() + "_" + i + ".mp4");
                    safeRunOnUiThread(() -> { p.setIndeterminate(true); Toast.makeText(this, "Compressing...", Toast.LENGTH_SHORT).show(); });
                    VideoCompressor compressor = new VideoCompressor();
                    synchronized (activeCompressors) { activeCompressors.add(compressor); }
                    compressor.compress(this, Uri.fromFile(localInputFile), outFile, new VideoCompressor.Callback() {
                        @Override public void onSuccess(File output) {
                            synchronized (activeCompressors) { activeCompressors.remove(compressor); }
                            if (localInputFile.exists() && (uri.getPath() == null || !localInputFile.getAbsolutePath().equals(uri.getPath()))) {
                                localInputFile.delete();
                            }
                            safeRunOnUiThread(() -> { p.setIndeterminate(false); p.setProgress(50); });
                            upload(output, i, p);
                        }
                        @Override public void onError(Exception e) {
                            synchronized (activeCompressors) { activeCompressors.remove(compressor); }
                            if (localInputFile.exists() && (uri.getPath() == null || !localInputFile.getAbsolutePath().equals(uri.getPath()))) {
                                localInputFile.delete();
                            }
                            try { upload(copyFile(uri, ".mp4", i), i, p); } catch (Exception ex) { handleUploadError(ex.getMessage(), p); }
                        }
                        @Override public void onCancelled() {
                            synchronized (activeCompressors) { activeCompressors.remove(compressor); }
                            if (localInputFile.exists() && (uri.getPath() == null || !localInputFile.getAbsolutePath().equals(uri.getPath()))) {
                                localInputFile.delete();
                            }
                            handleUploadError("Cancelled", p);
                        }
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

    private void upload(File file, int i, ProgressBar p) {
        boolean isVideo = file.getName().toLowerCase().endsWith(".mp4");
        if (isVideo) {
            Bitmap thumbBmp = extractFrame(file);
            if (thumbBmp != null) {
                try {
                    File thumbFile = WebPCompressor.compressBitmapToWebP(this, thumbBmp, 70, "THUMB");
                    supabase.uploadMedia(thumbFile, clientId, new SupabaseClient.UploadCallback() {
                        @Override public void onSuccess(String tUrl) {
                            if (thumbFile.exists()) thumbFile.delete();
                            supabase.uploadMedia(file, clientId, new SupabaseClient.UploadCallback() {
                                @Override public void onSuccess(String vUrl) {
                                    if (file.exists()) file.delete();
                                    handleUploadSuccess(vUrl + "|" + tUrl, i, p);
                                }
                                @Override public void onError(String e) {
                                    if (file.exists()) file.delete();
                                    handleUploadError(e, p);
                                }
                            });
                        }
                        @Override public void onError(String e) {
                            if (thumbFile.exists()) thumbFile.delete();
                            supabase.uploadMedia(file, clientId, new SupabaseClient.UploadCallback() {
                                @Override public void onSuccess(String vUrl) {
                                    if (file.exists()) file.delete();
                                    handleUploadSuccess(vUrl, i, p);
                                }
                                @Override public void onError(String e) {
                                    if (file.exists()) file.delete();
                                    handleUploadError(e, p);
                                }
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

    private void handleUploadSuccess(String combined, int i, ProgressBar p) {
        safeRunOnUiThread(() -> {
            p.setProgress(100); p.setVisibility(View.GONE);
            fullMediaUrls.set(i, combined);
            syncUploadedUrlsWithFullMediaUrls();
            displayThumb(i, combined);
            saveUrlsToFirestoreBatched(() -> { checkReleaseWakeLock(); activeTasks.decrementAndGet(); });
            Toast.makeText(this, "Uploaded", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleUploadError(String err, ProgressBar p) {
        safeRunOnUiThread(() -> { p.setVisibility(View.GONE); checkReleaseWakeLock(); activeTasks.decrementAndGet(); Toast.makeText(this, "Fail: " + err, Toast.LENGTH_SHORT).show(); });
    }

    private void checkReleaseWakeLock() {
        boolean any = false;
        for (ProgressBar cp : allProgressCircles) if (cp.getVisibility() == View.VISIBLE) { any = true; break; }
        if (!any && wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    private void syncUploadedUrlsWithFullMediaUrls() {
        uploadedUrls.clear();
        for (String u : fullMediaUrls) if (u != null) uploadedUrls.add(u);
    }

    private void loadExistingMedia() {
        showPageProgress(true);
        db.collection("hi_tech_controls_dataset_JUNE").document(clientId).collection("pages").document("storage").get()
            .addOnSuccessListener(d -> {
                if (!isDestroyed && !isFinishing()) {
                    showPageProgress(false);
                    processMediaResults(d);
                }
            })
            .addOnFailureListener(e -> {
                if (!isDestroyed && !isFinishing()) {
                    showPageProgress(false);
                }
            });
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
        } else { for (int i = 0; i < MAX_BOXES; i++) { fullMediaUrls.set(i, null); resetBox(i); } }
    }

    private void displayThumb(int i, String url) {
        if (url == null || url.isEmpty()) { resetBox(i); return; }

        ImageView iv = allImageViews.get(i);
        ImageView play = allPlayIcons.get(i);
        ProgressBar progress = allProgressCircles.get(i);

        // Clear any previous glide loads and reset default state
        try { Glide.with(this).clear(iv); } catch (Exception ignored) {}
        iv.setImageDrawable(null);
        play.setVisibility(View.GONE);
        allCheckViews.get(i).setVisibility(View.GONE);
        allDeleteButtons.get(i).setVisibility(View.GONE);

        // Show the loader spinner
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);

        // ✅ Check base URL only (before query params / pipe separator)
        String baseUrl = url.contains("|") ? url.split("\\|")[0] : url;
        String cleanBase = baseUrl.contains("?") ? baseUrl.substring(0, baseUrl.indexOf("?")) : baseUrl;
        boolean isVideo = cleanBase.toLowerCase().endsWith(".mp4")
                || cleanBase.toLowerCase().contains("/video/");

        String display = url.contains("|") ? url.split("\\|")[1] : url;

        RequestOptions o = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(220, 220)
                .centerCrop();

        try {
            if (isVideo && !url.contains("|")) {
                Glide.with(this)
                     .asBitmap()
                     .load(url)
                     .apply(o)
                     .frame(1_000_000)
                     .listener(new com.bumptech.glide.request.RequestListener<Bitmap>() {
                         @Override
                         public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<Bitmap> target, boolean isFirstResource) {
                             safeRunOnUiThread(() -> {
                                 progress.setVisibility(View.GONE);
                                 iv.setImageResource(R.drawable.imageview);
                             });
                             return false;
                         }

                         @Override
                         public boolean onResourceReady(Bitmap resource, Object model, com.bumptech.glide.request.target.Target<Bitmap> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                             safeRunOnUiThread(() -> {
                                 progress.setVisibility(View.GONE);
                                 play.setVisibility(View.VISIBLE);
                             });
                             return false;
                         }
                     })
                     .into(iv);
            } else {
                Glide.with(this)
                     .load(display)
                     .apply(o)
                     .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                         @Override
                         public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                             safeRunOnUiThread(() -> {
                                 progress.setVisibility(View.GONE);
                                 iv.setImageResource(R.drawable.imageview);
                             });
                             return false;
                         }

                         @Override
                         public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                             safeRunOnUiThread(() -> {
                                 progress.setVisibility(View.GONE);
                                 if (isVideo) {
                                     play.setVisibility(View.VISIBLE);
                                 }
                             });
                             return false;
                         }
                     })
                     .into(iv);
            }
        } catch (Exception e) { resetBox(i); }
    }

    private void saveUrlsToFirestoreBatched(Runnable done) {
        Map<String, Object> data = new HashMap<>();
        data.put("urls", new ArrayList<>(uploadedUrls));
        data.put("lastUpdated", FieldValue.serverTimestamp());
        db.collection("hi_tech_controls_dataset_JUNE").document(clientId).collection("pages").document("storage").set(data, SetOptions.merge())
            .addOnCompleteListener(t -> { if (done != null) done.run(); });
    }

    private void openMedia(String url) {
        if (url == null || url.isEmpty()) return;

        // Clean the URL if it contains pipe separator
        String actualUrl = url.contains("|") ? url.split("\\|")[0] : url;
        
        // Check if video
        String cleanUrl = actualUrl.contains("?") ? actualUrl.substring(0, actualUrl.indexOf("?")) : actualUrl;
        boolean isVideo = cleanUrl.toLowerCase().endsWith(".mp4") || cleanUrl.toLowerCase().contains("/video/");

        // Show our beautiful custom dialog
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_media_viewer);

        ImageView iv = dialog.findViewById(R.id.viewerImageView);
        VideoView vv = dialog.findViewById(R.id.viewerVideoView);
        ProgressBar pb = dialog.findViewById(R.id.viewerProgressBar);
        ImageView close = dialog.findViewById(R.id.viewerClose);
        TextView subtitle = dialog.findViewById(R.id.viewerSubtitle);
        TextView share = dialog.findViewById(R.id.viewerShare);
        TextView download = dialog.findViewById(R.id.viewerDownload);

        subtitle.setText("Client ID: " + clientId);
        subtitle.setPaintFlags(subtitle.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        close.setOnClickListener(v -> dialog.dismiss());

        // Setup share click
        share.setOnClickListener(v -> shareMedia(actualUrl, isVideo));

        // Setup download click
        String fileName = (isVideo ? "VID_" : "IMG_") + clientId + "_" + System.currentTimeMillis() + (isVideo ? ".mp4" : ".jpg");
        download.setOnClickListener(v -> downloadFile(actualUrl, fileName));

        pb.setVisibility(View.VISIBLE);

        Handler handler = new Handler(Looper.getMainLooper());
        RelativeLayout videoControlsContainer = dialog.findViewById(R.id.videoControlsContainer);
        ImageView btnPlayPause = dialog.findViewById(R.id.btnPlayPause);
        SeekBar videoSeekBar = dialog.findViewById(R.id.videoSeekBar);
        TextView tvCurrentTime = dialog.findViewById(R.id.tvCurrentTime);
        TextView tvTotalDuration = dialog.findViewById(R.id.tvTotalDuration);

        Runnable updateSeekBar = new Runnable() {
            @Override
            public void run() {
                try {
                    if (vv.isPlaying()) {
                        int current = vv.getCurrentPosition();
                        videoSeekBar.setProgress(current);
                        tvCurrentTime.setText(formatTime(current));
                        handler.postDelayed(this, 200);
                    } else {
                        handler.postDelayed(this, 500);
                    }
                } catch (Exception ignored) {}
            }
        };

        if (isVideo) {
            vv.setVisibility(View.VISIBLE);
            iv.setVisibility(View.GONE);
            
            vv.setVideoPath(actualUrl);

            vv.setOnPreparedListener(mp -> {
                pb.setVisibility(View.GONE);
                videoControlsContainer.setVisibility(View.VISIBLE);

                int duration = vv.getDuration();
                videoSeekBar.setMax(duration);
                tvTotalDuration.setText(formatTime(duration));

                vv.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                handler.post(updateSeekBar);
            });

            btnPlayPause.setOnClickListener(v -> {
                if (vv.isPlaying()) {
                    vv.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                } else {
                    vv.start();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    handler.post(updateSeekBar);
                }
            });

            videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        vv.seekTo(progress);
                        tvCurrentTime.setText(formatTime(progress));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            vv.setOnErrorListener((mp, what, extra) -> {
                pb.setVisibility(View.GONE);
                Toast.makeText(MediaUploadActivity.this, "Cannot play video in-app, launching external player...", Toast.LENGTH_SHORT).show();
                // Fallback to external player
                launchExternalViewer(actualUrl, true);
                dialog.dismiss();
                return true;
            });
        } else {
            iv.setVisibility(View.VISIBLE);
            vv.setVisibility(View.GONE);
            videoControlsContainer.setVisibility(View.GONE);

            Glide.with(this)
                    .load(actualUrl)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            pb.setVisibility(View.GONE);
                            Toast.makeText(MediaUploadActivity.this, "Failed to load image in-app", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            pb.setVisibility(View.GONE);
                            iv.post(() -> {
                                iv.setOnTouchListener(new ZoomableTouchListener(iv));
                            });
                            return false;
                        }
                    })
                    .into(iv);
        }

        dialog.setOnDismissListener(d -> {
            handler.removeCallbacks(updateSeekBar);
            try {
                if (vv.isPlaying()) {
                    vv.stopPlayback();
                }
            } catch (Exception ignored) {}
        });

        dialog.show();
    }

    private void shareMedia(String url, boolean isVideo) {
        Toast.makeText(this, "Preparing share...", Toast.LENGTH_SHORT).show();
        ioPool.execute(() -> {
            try {
                // Download file to temp cache directory
                java.net.URL urlObj = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
                conn.connect();
                File tempFile = new File(getCacheDir(), "SHARE_" + clientId + "_" + System.currentTimeMillis() + (isVideo ? ".mp4" : ".jpg"));
                try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
                
                // Get Uri from FileProvider
                Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        tempFile
                );
                
                safeRunOnUiThread(() -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(isVideo ? "video/*" : "image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Client ID: " + clientId);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share Client Media"));
                });
            } catch (Exception e) {
                Log.e(TAG, "Share error", e);
                safeRunOnUiThread(() -> Toast.makeText(this, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String formatTime(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / (1000 * 60)) % 60;
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void downloadFile(String url, String fileName) {
        try {
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));
            request.setDescription("Downloading client media file...");
            request.setTitle(fileName);
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);

            android.app.DownloadManager manager = (android.app.DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Download started. Check notifications.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Download manager not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "DownloadManager error", e);
            Toast.makeText(this, "Failed to start download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchExternalViewer(String url, boolean isVideo) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), isVideo ? "video/*" : "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(intent); } catch (Exception e) { Toast.makeText(this, "No viewer available", Toast.LENGTH_SHORT).show(); }
    }

    private boolean isVideoUri(Uri uri) {
        if (uri == null) return false;
        String type = getContentResolver().getType(uri);
        if (type != null && type.startsWith("video/")) return true;
        
        String path = uri.getPath();
        if (path != null) {
            String lower = path.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".3gp") || lower.endsWith(".mov")) {
                return true;
            }
        }
        
        String uriStr = uri.toString().toLowerCase();
        return uriStr.contains("video");
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
    private File createVideoFile(int idx) { return new File(getExternalFilesDir(null), "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_" + idx + ".mp4"); }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentCaptureIndex", currentCaptureIndex);
        outState.putParcelableArrayList("mediaUris", new ArrayList<>(mediaUris));
    }

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
