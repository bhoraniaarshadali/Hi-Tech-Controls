package com.example.hi_tech_controls.ui.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.ClientAdapter;
import com.example.hi_tech_controls.helper.OfflineSyncManager;
import com.example.hi_tech_controls.model.ClientModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ViewDetailsActivity extends BaseActivity {

    private static final String TAG = "ViewDetailsActivity";
    private static final String COLLECTION = "hi_tech_controls_dataset_JUNE";

    ImageView backBtn;
    EditText searchField;
    RecyclerView recyclerView;
    ProgressBar footerProgress;
    TextView emptyStateText;
    SwipeRefreshLayout swipeRefreshLayout;

    ClientAdapter adapter;
    FirebaseFirestore db;
    LinearLayoutManager layoutManager;

    private DocumentSnapshot lastVisible = null;
    private boolean isLoadingMore = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_details);

        Log.d(TAG, "onCreate - start");

        initViews();
        initFirestore();
        initRecycler();
        initListeners();
        resetPagination();
        loadRecentClients();

        Log.d(TAG, "onCreate - end");
    }

    // ---------------------------
    // Initialization
    // ---------------------------
    private void initViews() {
        backBtn = findViewById(R.id.viewClientDtls_Back);
        searchField = findViewById(R.id.searchField);
        recyclerView = findViewById(R.id.recentClientsRecyclerView);
        footerProgress = findViewById(R.id.footerProgress);
        emptyStateText = findViewById(R.id.emptyStateText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        footerProgress.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
    }

    private void initFirestore() {
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore initialized");
        } catch (Exception e) {
            db = null;
            Log.e(TAG, "Firestore init failed", e);
            showToast("Firestore unavailable");
        }
    }

    private void initRecycler() {
        adapter = new ClientAdapter();
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // pagination on scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                try {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoadingMore && !isLastPage &&
                            (visibleItemCount + firstVisibleItem >= totalItemCount - 2) &&
                            firstVisibleItem >= 0) {
                        Log.d(TAG, "Scrolled to threshold, loading more");
                        loadMoreClients();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onScrolled error", e);
                }
            }
        });
    }

    private void initListeners() {
        backBtn.setOnClickListener(v -> {
            Log.d(TAG, "Back pressed");
            finish();
        });

        // search watcher
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                if (text.length() > 2) {
                    Log.d(TAG, "Searching clients for: " + text);
                    searchClients(text);
                } else {
                    Log.d(TAG, "Search cleared or too short, reloading recents");
                    resetPagination();
                    loadRecentClients();
                }
            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Pull-to-refresh triggered");
            resetPagination();
            searchField.setText("");
            loadRecentClients();
        });
    }

    @Override
    protected void onNetworkStateChanged(boolean isOnline) {
        super.onNetworkStateChanged(isOnline);
        if (isOnline) {
            Log.d(TAG, "Network back online - triggering offline sync");
            OfflineSyncManager.getInstance().syncNow(this);
        } else {
            Log.d(TAG, "Network offline");
        }
    }

    // ---------------------------
    // Pagination state helpers
    // ---------------------------
    private void resetPagination() {
        Log.d(TAG, "resetPagination");
        lastVisible = null;
        isLastPage = false;
        isLoadingMore = false;
        adapter.showShimmer();
        footerProgress.setVisibility(View.GONE);
    }

    // ---------------------------
    // Load initial recents
    // ---------------------------
    private void loadRecentClients() {
        Log.d(TAG, "loadRecentClients start");
        if (db == null) {
            adapter.hideShimmer(new ArrayList<>());
            emptyStateText.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
            showToast("Firestore unavailable");
            return;
        }

        adapter.showShimmer();
        emptyStateText.setVisibility(View.GONE);
        isLoadingMore = false;

        Query query = db.collection(COLLECTION)
                .whereEqualTo("progress", 100)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(5);

        query.get()
                .addOnSuccessListener(this::handleClientBatch)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadRecentClients failed", e);
                    adapter.hideShimmer(new ArrayList<>());
                    emptyStateText.setVisibility(View.VISIBLE);
                    swipeRefreshLayout.setRefreshing(false);
                    footerProgress.setVisibility(View.GONE);
                    showToast("Failed to load recents");
                });
    }

    // ---------------------------
    // Load more (pagination)
    // ---------------------------
    private void loadMoreClients() {
        if (lastVisible == null || isLastPage || isLoadingMore) {
            Log.d(TAG, "loadMoreClients skipped: lastVisible=" + (lastVisible != null) + " isLastPage=" + isLastPage + " isLoadingMore=" + isLoadingMore);
            return;
        }

        Log.d(TAG, "loadMoreClients start");
        isLoadingMore = true;
        footerProgress.setVisibility(View.VISIBLE);

        Query query = db.collection(COLLECTION)
                .whereEqualTo("progress", 100)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(5);

        query.get()
                .addOnSuccessListener(this::handleClientBatch)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadMoreClients failed", e);
                    footerProgress.setVisibility(View.GONE);
                    isLoadingMore = false;
                    showToast("Failed to load more");
                });
    }

    // ---------------------------
    // Batch handler (common for initial and load more)
    // ---------------------------
    private void handleClientBatch(QuerySnapshot querySnapshot) {
        handleClientBatch(querySnapshot, lastVisible == null);
    }

    private void handleClientBatch(QuerySnapshot querySnapshot, boolean clearOld) {
        Log.d(TAG, "handleClientBatch called. clearOld=" + clearOld + " snapshotEmpty=" + (querySnapshot == null || querySnapshot.isEmpty()));

        if (querySnapshot == null || querySnapshot.isEmpty()) {
            if (clearOld) {
                adapter.hideShimmer(new ArrayList<>());
                emptyStateText.setVisibility(View.VISIBLE);
            }
            isLastPage = true;
            swipeRefreshLayout.setRefreshing(false);
            footerProgress.setVisibility(View.GONE);
            isLoadingMore = false;
            return;
        }

        List<ClientModel> tempList = new ArrayList<>();
        List<DocumentSnapshot> documents = querySnapshot.getDocuments();
        int total = documents.size();
        AtomicInteger completedCount = new AtomicInteger(0);

        // mark lastVisible for pagination BEFORE async fetches
        lastVisible = documents.get(documents.size() - 1);
        Log.d(TAG, "lastVisible updated. docCount=" + total + " lastId=" + lastVisible.getId());

        for (DocumentSnapshot doc : documents) {
            final String clientId = doc.getId();
            fetchClientDetails(clientId, new OnClientDetailsFetched() {
                @Override
                public void onFetched(ClientModel model) {
                    synchronized (tempList) {
                        tempList.add(model);
                        if (completedCount.incrementAndGet() == total) {
                            Log.d(TAG, "All details fetched for batch. total=" + total);
                            runOnUiThread(() -> {
                                tempList.sort((a, b) -> b.gpDate.compareTo(a.gpDate));
                                if (clearOld) {
                                    adapter.hideShimmer(tempList);
                                } else {
                                    adapter.addMore(tempList);
                                }
                                swipeRefreshLayout.setRefreshing(false);
                                footerProgress.setVisibility(View.GONE);
                                isLoadingMore = false;

                                if (tempList.isEmpty()) {
                                    emptyStateText.setVisibility(View.VISIBLE);
                                } else {
                                    emptyStateText.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailed() {
                    Log.w(TAG, "fetchClientDetails failed for id=" + clientId);
                    if (completedCount.incrementAndGet() == total) {
                        runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            footerProgress.setVisibility(View.GONE);
                            isLoadingMore = false;
                        });
                    }
                }
            });
        }
    }

    // ---------------------------
    // Individual client details fetch
    // ---------------------------
    private void fetchClientDetails(String clientId, OnClientDetailsFetched callback) {
        if (db == null) {
            callback.onFailed();
            return;
        }

        db.collection(COLLECTION)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .get()
                .addOnSuccessListener(fillDoc -> {
                    try {
                        String name = fillDoc.getString("name");
                        String gpDate = fillDoc.getString("gp_date");
                        String makeName = fillDoc.getString("make_name");

                        if (name == null || name.isEmpty()) {
                            // fallback to root doc
                            db.collection(COLLECTION)
                                    .document(clientId)
                                    .get()
                                    .addOnSuccessListener(mainDoc -> {
                                        String fallbackName = mainDoc.getString("name");
                                        ClientModel model = new ClientModel(
                                                fallbackName != null ? fallbackName : "Unknown",
                                                clientId,
                                                gpDate != null ? formatDate(gpDate) : "N/A",
                                                makeName != null ? makeName : "N/A"
                                        );
                                        callback.onFetched(model);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "fallback main doc fetch failed for " + clientId, e);
                                        callback.onFailed();
                                    });
                        } else {
                            ClientModel model = new ClientModel(
                                    name,
                                    clientId,
                                    gpDate != null ? formatDate(gpDate) : "N/A",
                                    makeName != null ? makeName : "N/A"
                            );
                            callback.onFetched(model);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error mapping fill_one for " + clientId, e);
                        callback.onFailed();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "fill_one fetch failed for " + clientId, e);
                    callback.onFailed();
                });
    }

    // ---------------------------
    // Search implementation
    // ---------------------------
    private void searchClients(String query) {
        Log.d(TAG, "searchClients: " + query);
        if (db == null) {
            showToast("Firestore unavailable");
            return;
        }

        adapter.showShimmer();
        emptyStateText.setVisibility(View.GONE);
        String q = query.toLowerCase().trim();

        db.collection(COLLECTION)
                .whereEqualTo("progress", 100)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClientModel> searchResults = new ArrayList<>();
                    List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                    AtomicInteger completedCount = new AtomicInteger(0);

                    if (documents.isEmpty()) {
                        adapter.hideShimmer(new ArrayList<>());
                        emptyStateText.setVisibility(View.VISIBLE);
                        emptyStateText.setText("No match found");
                        return;
                    }

                    for (DocumentSnapshot doc : documents) {
                        String clientId = doc.getId();
                        fetchClientDetails(clientId, new OnClientDetailsFetched() {
                            @Override
                            public void onFetched(ClientModel model) {
                                synchronized (searchResults) {
                                    try {
                                        if ((model.name != null && model.name.toLowerCase().contains(q)) ||
                                                (model.clientId != null && model.clientId.toLowerCase().contains(q)) ||
                                                (model.gpDate != null && model.gpDate.toLowerCase().contains(q))) {
                                            searchResults.add(model);
                                        }
                                    } catch (Exception ex) {
                                        Log.w(TAG, "search matching error", ex);
                                    }

                                    if (completedCount.incrementAndGet() == documents.size()) {
                                        runOnUiThread(() -> {
                                            searchResults.sort((a, b) -> b.gpDate.compareTo(a.gpDate));
                                            adapter.hideShimmer(searchResults);

                                            if (searchResults.isEmpty()) {
                                                emptyStateText.setVisibility(View.VISIBLE);
                                                emptyStateText.setText("No match found");
                                            } else {
                                                emptyStateText.setVisibility(View.GONE);
                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onFailed() {
                                if (completedCount.incrementAndGet() == documents.size()) {
                                    runOnUiThread(() -> adapter.hideShimmer(searchResults));
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "searchClients failed", e);
                    runOnUiThread(() -> {
                        adapter.hideShimmer(new ArrayList<>());
                        emptyStateText.setVisibility(View.VISIBLE);
                        swipeRefreshLayout.setRefreshing(false);
                        showToast("Search failed");
                    });
                });
    }

    // ---------------------------
    // Utility fetch for fill_one (kept for compatibility)
    // ---------------------------
    private void fetchFillOneData(String clientId, OnFillOneFetched callback) {
        if (db == null) {
            callback.onFetched(new ClientModel("Unknown", clientId, "N/A", "N/A"));
            return;
        }

        db.collection(COLLECTION)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String gpDateRaw = doc.getString("gp_date");
                        String makeName = doc.getString("make_name");
                        String formattedDate = formatDate(gpDateRaw);
                        callback.onFetched(new ClientModel(name, clientId, formattedDate, makeName));
                    } else {
                        callback.onFetched(new ClientModel("Unknown", clientId, "N/A", "N/A"));
                    }
                })
                .addOnFailureListener(e -> callback.onFetched(new ClientModel("Error", clientId, "N/A", "N/A")));
    }

    // ---------------------------
    // Helpers
    // ---------------------------
    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "N/A";
        try {
            SimpleDateFormat input = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return output.format(Objects.requireNonNull(input.parse(raw)));
        } catch (Exception e) {
            Log.w(TAG, "formatDate fallback to raw", e);
            return raw;
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------
    // Callbacks
    // ---------------------------
    interface OnClientDetailsFetched {
        void onFetched(ClientModel model);

        void onFailed();
    }

    interface OnFillOneFetched {
        void onFetched(ClientModel model);
    }
}
