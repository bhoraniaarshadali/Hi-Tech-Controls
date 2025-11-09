package com.example.hi_tech_controls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.adapter.AddDetailsAdp;
import com.example.hi_tech_controls.adapter.OfflineSyncManager;
import com.example.hi_tech_controls.adapter.PermissionUtils;
import com.example.hi_tech_controls.model.DetailsModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends BaseActivity {

    private static final int PAGE_LIMIT = 150;
    private final ArrayList<DetailsModel> detailsDataList = new ArrayList<>();
    private final Handler slowNetHandler = new Handler();
    private RecyclerView recyclerViewDiscovery1;
    private AddDetailsAdp addDetailsAdapter;
    private FirebaseFirestore db;
    private CollectionReference collectionRef;
    private ListenerRegistration realtimeListener;
    private Button addClientBtn1, viewClientBtn1;
    private ImageView logout_btn_layout, refreshButton;
    private ShimmerFrameLayout shimmerLayout;
    private TextView emptyView;
    private boolean isListenerActive = false;
    private boolean slowToastShown = false;
    private DocumentSnapshot lastDoc = null;
    private boolean isLoadingMore = false;
    private Toast currentToast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                .getBoolean("flag", false);

        if (!isLoggedIn) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        if (!PermissionUtils.hasStoragePermissions(this)) {
            PermissionUtils.requestStoragePermissions(this, 1001);
        }


        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);
        logout_btn_layout = findViewById(R.id.logout_btn);
        refreshButton = findViewById(R.id.refreshButton);
        recyclerViewDiscovery1 = findViewById(R.id.recyclerViewDiscovery);
        shimmerLayout = findViewById(R.id.shimmerLayout);
        emptyView = findViewById(R.id.emptyView);

        recyclerViewDiscovery1.setLayoutManager(new LinearLayoutManager(this));
        addDetailsAdapter = new AddDetailsAdp(this, new ArrayList<>());
        recyclerViewDiscovery1.setAdapter(addDetailsAdapter);

        db = FirebaseFirestore.getInstance();
        collectionRef = db.collection("hi_tech_controls_dataset_JUNE");

        addClientBtn1.setOnClickListener(v -> startActivity(new Intent(this, AddDetailsActivity.class)));
        viewClientBtn1.setOnClickListener(v -> startActivity(new Intent(this, ViewDetailsActivity.class)));
        refreshButton.setOnClickListener(v -> refreshData());
        logout_btn_layout.setOnClickListener(v -> showExitConfirmationDialog());

        showShimmer();
        // Cache first for instant UI, then network, then live updates
        readFromCacheThenRealtime();

        recyclerViewDiscovery1.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (!rv.canScrollVertically(1)) loadMore();
            }
        });
    }

    private void showShimmer() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();
        recyclerViewDiscovery1.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void hideShimmer() {
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
        recyclerViewDiscovery1.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onNetworkStateChanged(boolean isOnline) {
        if (isOnline) OfflineSyncManager.getInstance().syncNow(this);
    }

    private void readFromCacheThenRealtime() {
        Query q = collectionRef.orderBy("clientId", Query.Direction.DESCENDING).limit(PAGE_LIMIT);

        // 1) cache
        q.get(Source.CACHE).addOnSuccessListener(cached -> {
            if (!cached.isEmpty()) {
                applySnapshot(cached.getDocuments(), /*fromRealtime=*/false);
                hideShimmer();
            }
        });

        // 2) server page 1
        q.get().addOnSuccessListener(snap -> {
            lastDoc = snap.size() > 0 ? snap.getDocuments().get(snap.size() - 1) : null;
            applySnapshot(snap.getDocuments(), /*fromRealtime=*/false);
            hideShimmer();
            // 3) realtime after we have initial data
            startRealtimeListener();
        }).addOnFailureListener(e -> {
            hideShimmer();
            showCleanToast("Load failed: " + e.getMessage());
            //Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void startRealtimeListener() {
        if (isListenerActive) return;
        isListenerActive = true;

        slowToastShown = false;
        slowNetHandler.postDelayed(() -> {
            if (shimmerLayout.getVisibility() == View.VISIBLE && !slowToastShown) {
                slowToastShown = true;
                showCleanToast("Network slow, loading...");
                //Toast.makeText(this, "Network slow, loading...", Toast.LENGTH_SHORT).show();
            }
        }, 2500);

        realtimeListener = collectionRef.addSnapshotListener((snapshots, e) -> {
            if (!isListenerActive) return;
            slowNetHandler.removeCallbacksAndMessages(null);

            if (e != null) return;

            if (snapshots == null || snapshots.isEmpty()) {
                addDetailsAdapter.submitList(new ArrayList<>());
                emptyView.setVisibility(View.VISIBLE);
                recyclerViewDiscovery1.setVisibility(View.GONE);
                return;
            }

            // no shimmer during realtime; smooth update
            applySnapshot(snapshots.getDocuments(), /*fromRealtime=*/true);
        });
    }

    private void applySnapshot(List<DocumentSnapshot> docs, boolean fromRealtime) {
        ArrayList<DetailsModel> temp = new ArrayList<>();
        ArrayList<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (DocumentSnapshot doc : docs) {
            String id = doc.getId();
            if ("last_id".equals(id) || "initialDoc".equals(id)) continue;

            Integer uid = safeParse(id, doc.getLong("clientId"));
            if (uid == null) continue;

            DetailsModel m = new DetailsModel();
            m.setUId(uid);
            Long p = doc.getLong("progress");
            m.setProgress(p != null ? p.intValue() : 0);
            m.setuName("…"); // placeholder name, fill after subfetch
            temp.add(m);

            Task<DocumentSnapshot> t = collectionRef.document(id)
                    .collection("pages").document("fill_one")
                    .get()
                    .addOnSuccessListener(d -> {
                        String name = d.getString("name");
                        m.setuName(name != null ? name : "Unknown");
                    })
                    .addOnFailureListener(err -> m.setuName("Unknown"));

            tasks.add(t);
        }

        if (tasks.isEmpty()) {
            updateList(temp);
            return;
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> updateList(temp));
    }

    private void updateList(ArrayList<DetailsModel> temp) {
        Collections.sort(temp, (a, b) -> {
            boolean ad = a.getProgress() == 100, bd = b.getProgress() == 100;
            if (ad && !bd) return 1;     // completed bottom
            if (!ad && bd) return -1;    // incomplete top
            return Integer.compare(b.getUId(), a.getUId()); // newest first
        });

        addDetailsAdapter.submitList(temp);
        emptyView.setVisibility(temp.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerViewDiscovery1.setVisibility(temp.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadMore() {
        if (isLoadingMore || lastDoc == null) return;
        isLoadingMore = true;

        collectionRef.orderBy("clientId", Query.Direction.DESCENDING)
                .startAfter(lastDoc)
                .limit(PAGE_LIMIT)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        isLoadingMore = false;
                        return;
                    }
                    lastDoc = snap.getDocuments().get(snap.size() - 1);
                    // merge with current list
                    ArrayList<DetailsModel> current = new ArrayList<>(addDetailsAdapter.getCurrentItems());
                    applySnapshotForAppend(snap.getDocuments(), current);
                })
                .addOnFailureListener(e -> isLoadingMore = false);
    }

    private void applySnapshotForAppend(List<DocumentSnapshot> docs, ArrayList<DetailsModel> base) {
        ArrayList<DetailsModel> tempNew = new ArrayList<>();
        ArrayList<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (DocumentSnapshot doc : docs) {
            String id = doc.getId();
            if ("last_id".equals(id) || "initialDoc".equals(id)) continue;

            Integer uid = safeParse(id, doc.getLong("clientId"));
            if (uid == null) continue;

            DetailsModel m = new DetailsModel();
            m.setUId(uid);
            Long p = doc.getLong("progress");
            m.setProgress(p != null ? p.intValue() : 0);
            m.setuName("…");
            tempNew.add(m);

            Task<DocumentSnapshot> t = collectionRef.document(id)
                    .collection("pages").document("fill_one")
                    .get()
                    .addOnSuccessListener(d -> m.setuName(d.getString("name") != null ? d.getString("name") : "Unknown"))
                    .addOnFailureListener(err -> m.setuName("Unknown"));

            tasks.add(t);
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            base.addAll(tempNew);
            updateList(base);
            isLoadingMore = false;
        });
    }

    private Integer safeParse(String id, Long fb) {
        try {
            return Integer.parseInt(id);
        } catch (Exception e) {
            return fb == null ? null : fb.intValue();
        }
    }

    private void refreshData() {
        //Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
        //showCleanToast("Refreshing...");

        stopRealtimeListener();
        lastDoc = null;

        addDetailsAdapter.submitList(new ArrayList<>());
        showShimmer();
        readFromCacheThenRealtime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRealtimeListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRealtimeListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRealtimeListener();
    }

    private void stopRealtimeListener() {
        isListenerActive = false;
        slowNetHandler.removeCallbacksAndMessages(null);
        if (realtimeListener != null) {
            realtimeListener.remove();
            realtimeListener = null;
        }
    }

    private void showExitConfirmationDialog() {
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
        SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
        preferences.edit().putBoolean("flag", false).apply();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showCleanToast(String msg) {
        if (currentToast != null) currentToast.cancel(); // destroy old toast
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }


    @Override
    public void onBackPressed() {
        if (getSharedPreferences("Login", MODE_PRIVATE).getBoolean("flag", false))
            showExitConfirmationDialog();
        else
            super.onBackPressed();
    }
}
