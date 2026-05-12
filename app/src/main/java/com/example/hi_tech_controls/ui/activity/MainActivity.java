package com.example.hi_tech_controls.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.AddDetailsAdp;
import com.example.hi_tech_controls.helper.OfflineSyncManager;
import com.example.hi_tech_controls.helper.PermissionUtils;
import com.example.hi_tech_controls.model.DetailsModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

import com.example.hi_tech_controls.helper.FirestoreUtils;
import com.example.hi_tech_controls.helper.AdminManager;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final int PAGE_LIMIT = 100;
    private final Handler slowNetHandler = new Handler();
    private RecyclerView recyclerViewDiscovery1;
    private AddDetailsAdp addDetailsAdapter;
    private CollectionReference collectionRef;
    private ListenerRegistration realtimeListener;
    private ShimmerFrameLayout shimmerLayout;
    private TextView emptyView;
    private Button addClientBtn1, viewClientBtn1;
    private ImageView logoutBtn;
    private boolean isListenerActive = false;
    private boolean slowToastShown = false;
    private DocumentSnapshot lastDoc = null;
    private boolean isLoadingMore = false;
    private Toast currentToast;
    private boolean doubleBackToExitPressedOnce = false;

    // ---------------------------------------------------------------------
    // LIFECYCLE
    // ---------------------------------------------------------------------

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() started");

        super.onCreate(savedInstanceState);

        if (!checkLoginStatus()) {
            Log.w(TAG, "User not logged in → redirecting to LoginActivity");
            return;
        }

        setContentView(R.layout.activity_main);
        Log.d(TAG, "Main layout loaded");

        requestPermissionsIfNeeded();
        initUI();
        initFirestore();
        loadInitialData();
        startRealtimeListener();

        // Handle back press via dispatcher (Double tap to exit)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finishAffinity(); // Close the entire app
                    return;
                }

                doubleBackToExitPressedOnce = true;
                Snackbar.make(findViewById(android.R.id.content),
                        "Press back again to exit", Snackbar.LENGTH_SHORT).show();

                new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false,
                        2000);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume → starting realtime data listener");
        startRealtimeListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause → stopping realtime data listener");
        stopRealtimeListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy → cleaning up");
        stopRealtimeListener();
    }

    // ---------------------------------------------------------------------
    // LOGIN CHECK
    // ---------------------------------------------------------------------
    private boolean checkLoginStatus() {
        Log.d(TAG, "Checking login status…");
        boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                .getBoolean("flag", false);

        Log.d(TAG, "Login flag = " + isLoggedIn);
        if (!isLoggedIn) {
            Log.w(TAG, "Not logged in → opening LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------------
    // INITIAL SETUP
    // ---------------------------------------------------------------------
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestPermissionsIfNeeded() {
        Log.d(TAG, "Checking storage permissions…");
        if (!PermissionUtils.hasStoragePermissions(this)) {
            Log.d(TAG, "Requesting storage permissions");
            PermissionUtils.requestStoragePermissions(this, 1001);
        }
    }

    private void initUI() {
        Log.d(TAG, "Initializing UI Components");
        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);
        logoutBtn = findViewById(R.id.logout_btn);
        recyclerViewDiscovery1 = findViewById(R.id.recyclerViewDiscovery);
        shimmerLayout = findViewById(R.id.shimmerLayout);
        emptyView = findViewById(R.id.emptyView);

        // Setup RecyclerView
        recyclerViewDiscovery1.setLayoutManager(new LinearLayoutManager(this));
        addDetailsAdapter = new AddDetailsAdp(this, new ArrayList<>());
        recyclerViewDiscovery1.setAdapter(addDetailsAdapter);

        // Setup Click Listeners
        addClientBtn1.setOnClickListener(v -> {
            Log.d(TAG, "Add Client clicked");
            navigateTo(AddDetailsActivity.class);
        });

        viewClientBtn1.setOnClickListener(v -> {
            Log.d(TAG, "View Client clicked");
            navigateTo(ViewDetailsActivity.class);
        });

        logoutBtn.setOnClickListener(v -> {
            Log.d(TAG, "Logout clicked");
            showExitConfirmationDialog();
        });
    }

    private void initFirestore() {
        Log.d(TAG, "Initializing Firestore Reference");
        collectionRef = FirebaseFirestore.getInstance().collection("hi_tech_controls_dataset_JUNE");
    }

    private void navigateTo(Class<?> cls) {
        Log.d(TAG, "Navigating to " + cls.getSimpleName());
        startActivity(new Intent(this, cls));
    }

    // ---------------------------------------------------------------------
    // LOADING FLOW (cache → server → realtime)
    // ---------------------------------------------------------------------
    private void loadInitialData() {
        Log.d(TAG, "loadInitialData() started with server-side filtering");

        // Server-side filter to optimize performance and reduce reads
        // Note: Removed 'clientId' orderBy as it might not be a field in all documents
        Query q = collectionRef.whereLessThan("progress", 100)
                .orderBy("progress")
                .limit(PAGE_LIMIT);

        loadCache(q);
        loadFromServer(q);
    }

    private void loadCache(Query q) {
        Log.d(TAG, "Loading cache…");

        q.get(Source.CACHE).addOnSuccessListener(cached -> {
            Log.d(TAG, "Cache snapshot size = " + cached.size());

            if (!cached.isEmpty()) {
                applySnapshot(cached.getDocuments(), false);
                hideShimmer();
            }
        });
    }

    private void loadFromServer(Query q) {
        Log.d(TAG, "Fetching fresh data from server…");

        q.get().addOnSuccessListener(snap -> {
            Log.d(TAG, "Server snapshot size = " + snap.size());
            updateLastDoc(snap);
            applySnapshot(snap.getDocuments(), false);
            hideShimmer();
            startRealtimeListener();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Server load failed: " + e.getMessage());
            hideShimmer();
            showCleanToast("Load failed: " + e.getMessage());
        });
    }

    private void updateLastDoc(QuerySnapshot snap) {
        if (snap.size() > 0) {
            lastDoc = snap.getDocuments().get(snap.size() - 1);
            Log.d(TAG, "Updated lastDoc = " + lastDoc.getId());
        }
    }

    // ---------------------------------------------------------------------
    // REALTIME LISTENER
    // ---------------------------------------------------------------------
    private void startRealtimeListener() {
        if (isListenerActive) {
            Log.d(TAG, "Realtime listener already active");
            return;
        }

        Log.d(TAG, "Starting realtime listener with filtered query");
        isListenerActive = true;

        handleSlowNetworkNotice();

        Query q = collectionRef.whereLessThan("progress", 100)
                .orderBy("progress")
                .limit(PAGE_LIMIT);

        realtimeListener = q.addSnapshotListener((snapshots, e) -> {
            if (!isListenerActive)
                return;

            if (e != null) {
                Log.e(TAG, "Realtime error: " + e.getMessage());
                return;
            }

            Log.d(TAG, "Realtime update received, size=" + (snapshots != null ? snapshots.size() : 0));
            slowNetHandler.removeCallbacksAndMessages(null);

            if (snapshots == null || snapshots.isEmpty()) {
                addDetailsAdapter.submitList(new ArrayList<>());
                showEmptyState();
                return;
            }

            applySnapshot(snapshots.getDocuments(), true);
        });
    }

    private void handleSlowNetworkNotice() {
        slowToastShown = false;

        slowNetHandler.postDelayed(() -> {
            if (shimmerLayout.getVisibility() == View.VISIBLE && !slowToastShown) {
                slowToastShown = true;
                Log.w(TAG, "Slow network detected");
                showCleanToast("Network slow, loading...");
            }
        }, 2500);
    }

    private void stopRealtimeListener() {
        Log.d(TAG, "Stopping realtime listener");

        isListenerActive = false;
        slowNetHandler.removeCallbacksAndMessages(null);

        if (realtimeListener != null) {
            realtimeListener.remove();
            realtimeListener = null;
        }
    }

    // ---------------------------------------------------------------------
    // SNAPSHOT PARSING
    // ---------------------------------------------------------------------
    private void applySnapshot(List<DocumentSnapshot> docs, boolean fromRealtime) {
        Log.d(TAG, "applySnapshot() count=" + docs.size() + " realtime=" + fromRealtime);

        ArrayList<DetailsModel> temp = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            if (!isValidDoc(doc))
                continue;
            temp.add(parseDocument(doc));
        }

        updateList(temp);
    }

    private boolean isValidDoc(DocumentSnapshot doc) {
        String id = doc.getId();
        boolean valid = !id.equals("last_id") && !id.equals("initialDoc");

        Log.d(TAG, "Doc " + id + " valid=" + valid);
        return valid;
    }

    private DetailsModel parseDocument(DocumentSnapshot doc) {
        Integer uid = safeParse(doc.getId(), FirestoreUtils.getLongSafe(doc, "clientId"));

        DetailsModel m = new DetailsModel();
        m.setUId(uid);
        m.setProgress((int) FirestoreUtils.getLongSafe(doc, "progress"));
        m.setLastUpdated(FirestoreUtils.getLongSafe(doc, "lastUpdated"));

        // DIRECT NAME: Read from main document (Optimization: No subcollection read)
        String name = FirestoreUtils.getStringSafe(doc, "name");
        m.setuName(name.isEmpty() ? "Unknown" : name);

        Log.d(TAG, "Parsed model — ID=" + uid + " name=" + m.getuName());
        return m;
    }

    // ---------------------------------------------------------------------
    // LIST UPDATE + SORT
    // ---------------------------------------------------------------------
    private void updateList(ArrayList<DetailsModel> temp) {
        Log.d(TAG, "Updating list, count=" + temp.size());

        // Sort by lastUpdated descending (Newest first)
        Collections.sort(temp, (a, b) -> Long.compare(b.getLastUpdated(), a.getLastUpdated()));

        addDetailsAdapter.submitList(temp);

        // Auto-scroll to top so the newest items are always visible
        if (!temp.isEmpty() && recyclerViewDiscovery1 != null) {
            recyclerViewDiscovery1.scrollToPosition(0);
        }

        toggleEmptyState(temp.isEmpty());
    }

    private void toggleEmptyState(boolean empty) {
        Log.d(TAG, "toggleEmptyState empty=" + empty);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewDiscovery1.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // Pagination removed as per user request (Realtime listener handles top items)

    // ---------------------------------------------------------------------
    // UTILITIES
    // ---------------------------------------------------------------------
    private Integer safeParse(String id, Long fb) {
        try {
            return Integer.parseInt(id);
        } catch (Exception e) {
            return fb == null ? null : fb.intValue();
        }
    }

    private void refreshData() {
        // Redundant as per user request (realtime should handle it)
        Log.d(TAG, "refreshData() called but skipped due to user request");
    }

    private void showShimmer() {
        Log.d(TAG, "Showing shimmer");
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();
        recyclerViewDiscovery1.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void hideShimmer() {
        Log.d(TAG, "Hiding shimmer");
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
        recyclerViewDiscovery1.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        Log.d(TAG, "Empty state shown");
        emptyView.setVisibility(View.VISIBLE);
        recyclerViewDiscovery1.setVisibility(View.GONE);
    }

    @Override
    protected void onNetworkStateChanged(boolean isOnline) {
        Log.d(TAG, "Network changed isOnline=" + isOnline);
        if (isOnline)
            OfflineSyncManager.getInstance().syncNow(this);
    }

    private void showExitConfirmationDialog() {
        Log.d(TAG, "Showing logout confirmation dialog");

        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Logout")
                .setContentText("Are you sure you want to logout?")
                .setConfirmButtonBackgroundColor(Color.parseColor("#FF0000"))
                .setCancelButtonBackgroundColor(Color.parseColor("#7C7C7C"))
                .setConfirmText("Logout")
                .setCancelText("Cancel")
                .setConfirmClickListener(s -> {
                    logout();
                    s.dismissWithAnimation();
                })
                .showCancelButton(true)
                .show();
    }

    private void logout() {
        baseLogout();
    }

    private void showCleanToast(String msg) {
        Log.d(TAG, "Toast: " + msg);

        if (currentToast != null)
            currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }

}
