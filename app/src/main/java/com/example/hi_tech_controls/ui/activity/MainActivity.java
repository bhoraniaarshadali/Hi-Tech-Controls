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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final int PAGE_LIMIT = 150;
    private final Handler slowNetHandler = new Handler();
    private RecyclerView recyclerViewDiscovery1;
    private AddDetailsAdp addDetailsAdapter;
    private CollectionReference collectionRef;
    private ListenerRegistration realtimeListener;
    private ShimmerFrameLayout shimmerLayout;
    private TextView emptyView;
    private Button addClientBtn1, viewClientBtn1;
    private ImageView logoutBtn, refreshButton;
    private boolean isListenerActive = false;
    private boolean slowToastShown = false;
    private DocumentSnapshot lastDoc = null;
    private boolean isLoadingMore = false;
    private Toast currentToast;

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
        bindViews();
        setupRecyclerView();
        setupFirestoreReference();
        setupClickListeners();

        showShimmer();
        Log.d(TAG, "Shimmer started, loading initial data");
        loadInitialData();

        setupScrollPagination();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume → starting realtime listener");
        startRealtimeListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause → stopping realtime listener");
        stopRealtimeListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy → cleaning up listeners");
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

    private void bindViews() {
        Log.d(TAG, "Binding views");
        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);
        logoutBtn = findViewById(R.id.logout_btn);
        refreshButton = findViewById(R.id.refreshButton);
        recyclerViewDiscovery1 = findViewById(R.id.recyclerViewDiscovery);
        shimmerLayout = findViewById(R.id.shimmerLayout);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");
        recyclerViewDiscovery1.setLayoutManager(new LinearLayoutManager(this));
        addDetailsAdapter = new AddDetailsAdp(this, new ArrayList<>());
        recyclerViewDiscovery1.setAdapter(addDetailsAdapter);
    }

    private void setupFirestoreReference() {
        Log.d(TAG, "Initializing Firestore reference");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        collectionRef = db.collection("hi_tech_controls_dataset_JUNE");
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting click listeners");

        addClientBtn1.setOnClickListener(v -> {
            Log.d(TAG, "Add Client clicked");
            navigateTo(AddDetailsActivity.class);
        });

        viewClientBtn1.setOnClickListener(v -> {
            Log.d(TAG, "View Client clicked");
            navigateTo(ViewDetailsActivity.class);
        });

        refreshButton.setOnClickListener(v -> {
            Log.d(TAG, "Refresh clicked");
            refreshData();
        });

        logoutBtn.setOnClickListener(v -> {
            Log.d(TAG, "Logout clicked");
            showExitConfirmationDialog();
        });
    }

    private void navigateTo(Class<?> cls) {
        Log.d(TAG, "Navigating to " + cls.getSimpleName());
        startActivity(new Intent(this, cls));
    }

    // ---------------------------------------------------------------------
    // LOADING FLOW (cache → server → realtime)
    // ---------------------------------------------------------------------
    private void loadInitialData() {
        Log.d(TAG, "loadInitialData() started");

        Query q = collectionRef.orderBy("clientId", Query.Direction.DESCENDING)
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

        Log.d(TAG, "Starting realtime listener");
        isListenerActive = true;

        handleSlowNetworkNotice();

        realtimeListener = collectionRef.addSnapshotListener((snapshots, e) -> {
            if (!isListenerActive) return;

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
        ArrayList<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (DocumentSnapshot doc : docs) {
            if (!isValidDoc(doc)) {
                Log.d(TAG, "Skipping invalid doc: " + doc.getId());
                continue;
            }

            DetailsModel model = parseDocument(doc);
            temp.add(model);

            tasks.add(fetchNameAsync(doc, model));
        }

        if (tasks.isEmpty()) {
            updateList(temp);
            return;
        }

        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(done -> updateList(temp));
    }

    private boolean isValidDoc(DocumentSnapshot doc) {
        String id = doc.getId();
        boolean valid = !id.equals("last_id") && !id.equals("initialDoc");

        Log.d(TAG, "Doc " + id + " valid=" + valid);
        return valid;
    }

    private DetailsModel parseDocument(DocumentSnapshot doc) {
        Integer uid = safeParse(doc.getId(), doc.getLong("clientId"));

        DetailsModel m = new DetailsModel();
        m.setUId(uid);
        Long p = doc.getLong("progress");
        m.setProgress(p != null ? p.intValue() : 0);
        m.setuName("…");

        Log.d(TAG, "Parsed model → ID=" + uid + " progress=" + m.getProgress());
        return m;
    }

    private Task<DocumentSnapshot> fetchNameAsync(DocumentSnapshot doc, DetailsModel model) {
        Log.d(TAG, "Fetching name for doc " + doc.getId());

        return collectionRef.document(doc.getId())
                .collection("pages").document("fill_one")
                .get()
                .addOnSuccessListener(d -> {
                    String name = d.getString("name");
                    model.setuName(name != null ? name : "Unknown");
                    Log.d(TAG, "Name set for " + doc.getId() + " = " + model.getuName());
                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Name fetch failed for " + doc.getId());
                    model.setuName("Unknown");
                });
    }

    // ---------------------------------------------------------------------
    // LIST UPDATE + SORT
    // ---------------------------------------------------------------------
    private void updateList(ArrayList<DetailsModel> temp) {
        Log.d(TAG, "Updating list, count=" + temp.size());
        sortList(temp);
        addDetailsAdapter.submitList(temp);
        toggleEmptyState(temp.isEmpty());
    }

    private void sortList(ArrayList<DetailsModel> list) {
        Log.d(TAG, "Sorting list");
        Collections.sort(list, (a, b) -> {
            boolean aDone = a.getProgress() == 100;
            boolean bDone = b.getProgress() == 100;

            if (aDone && !bDone) return 1;
            if (!aDone && bDone) return -1;
            return Integer.compare(b.getUId(), a.getUId());
        });
    }

    private void toggleEmptyState(boolean empty) {
        Log.d(TAG, "toggleEmptyState empty=" + empty);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewDiscovery1.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ---------------------------------------------------------------------
    // PAGINATION
    // ---------------------------------------------------------------------
    private void setupScrollPagination() {
        Log.d(TAG, "Setting up pagination listener");

        recyclerViewDiscovery1.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!rv.canScrollVertically(1)) {
                    Log.d(TAG, "Reached bottom → loading more");
                    loadMore();
                }
            }
        });
    }

    private void loadMore() {
        if (isLoadingMore || lastDoc == null) {
            Log.d(TAG, "loadMore() blocked isLoadingMore=" + isLoadingMore + " lastDoc=" + (lastDoc != null));
            return;
        }

        Log.d(TAG, "Loading more items…");
        isLoadingMore = true;

        collectionRef.orderBy("clientId", Query.Direction.DESCENDING)
                .startAfter(lastDoc)
                .limit(PAGE_LIMIT)
                .get()
                .addOnSuccessListener(this::appendMoreData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Load more failed: " + e.getMessage());
                    isLoadingMore = false;
                });
    }

    private void appendMoreData(QuerySnapshot snap) {
        Log.d(TAG, "appendMoreData() count=" + snap.size());

        if (snap.isEmpty()) {
            isLoadingMore = false;
            return;
        }

        lastDoc = snap.getDocuments().get(snap.size() - 1);
        Log.d(TAG, "New lastDoc=" + lastDoc.getId());

        ArrayList<DetailsModel> base = new ArrayList<>(addDetailsAdapter.getCurrentItems());
        applySnapshotForAppend(snap.getDocuments(), base);
    }

    private void applySnapshotForAppend(List<DocumentSnapshot> docs, ArrayList<DetailsModel> base) {
        Log.d(TAG, "applySnapshotForAppend() new count=" + docs.size());

        ArrayList<DetailsModel> tempNew = new ArrayList<>();
        ArrayList<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (DocumentSnapshot doc : docs) {
            if (!isValidDoc(doc)) continue;

            DetailsModel model = parseDocument(doc);
            tempNew.add(model);

            tasks.add(fetchNameAsync(doc, model));
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            base.addAll(tempNew);
            updateList(base);
            isLoadingMore = false;
        });
    }

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
        Log.d(TAG, "Refreshing data…");

        stopRealtimeListener();
        lastDoc = null;

        addDetailsAdapter.submitList(new ArrayList<>());
        showShimmer();
        loadInitialData();
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
        if (isOnline) OfflineSyncManager.getInstance().syncNow(this);
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
        Log.d(TAG, "Logging out…");

        SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
        preferences.edit().putBoolean("flag", false).apply();

        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);

        finish();
    }

    private void showCleanToast(String msg) {
        Log.d(TAG, "Toast: " + msg);

        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    @Override
    public void onBackPressed() {
        boolean flag = getSharedPreferences("Login", MODE_PRIVATE).getBoolean("flag", false);
        Log.d(TAG, "onBackPressed flag=" + flag);

        if (flag) {
            showExitConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }
}
