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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.hi_tech_controls.helper.FirestoreUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ViewDetailsActivity extends BaseActivity {

    private static final String TAG = "ViewDetailsActivity";
    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";

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

        com.example.hi_tech_controls.helper.AnalyticsManager.logEvent(this, "view_clients_list");

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
                    com.example.hi_tech_controls.helper.AnalyticsManager.logEvent(ViewDetailsActivity.this, "client_search");
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
            if (searchField.getText().toString().isEmpty()) {
                resetPagination();
                loadRecentClients();
            } else {
                searchField.setText(""); // This will trigger the listener to load recents
            }
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

        Query query = db.collection(COLLECTION_NAME)
                .whereEqualTo("progress", 100)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(5);

        query.get()
                .addOnSuccessListener(snapshot -> handleClientBatch(snapshot, true))
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

        Query query = db.collection(COLLECTION_NAME)
                .whereEqualTo("progress", 100)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(10);

        query.get()
                .addOnSuccessListener(snapshot -> handleClientBatch(snapshot, false))
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

        List<ClientModel> immediateList = new ArrayList<>();
        List<DocumentSnapshot> missingDetailsDocs = new ArrayList<>();
        List<DocumentSnapshot> documents = querySnapshot.getDocuments();

        // mark lastVisible for pagination
        lastVisible = documents.get(documents.size() - 1);
        Log.d(TAG, "lastVisible updated. docCount=" + documents.size() + " lastId=" + lastVisible.getId());

        for (DocumentSnapshot doc : documents) {
            String clientId = doc.getId();
            String name = FirestoreUtils.getStringSafe(doc, "name");
            String gpDate = FirestoreUtils.getStringSafe(doc, "gp_date");
            String makeName = FirestoreUtils.getStringSafe(doc, "make_name");

            // If we have all preview fields in root, add immediately
            if (!name.isEmpty() && !gpDate.isEmpty() && !makeName.isEmpty()) {
                immediateList.add(new ClientModel(name, clientId, formatDate(gpDate), makeName));
            } else {
                // Legacy document: missing denormalized fields
                missingDetailsDocs.add(doc);
            }
        }

        if (missingDetailsDocs.isEmpty()) {
            // BEST CASE: All data was in root documents. No extra calls!
            updateAdapterWithBatch(immediateList, clearOld);
        } else {
            // WORST CASE: Some documents are old and need extra fetches
            Log.d(TAG, "Some docs missing root details: " + missingDetailsDocs.size());
            fetchMissingDetails(missingDetailsDocs, immediateList, clearOld);
        }
    }

    private void updateAdapterWithBatch(List<ClientModel> batch, boolean clearOld) {
        runOnUiThread(() -> {
            batch.sort((a, b) -> b.gpDate.compareTo(a.gpDate));
            if (clearOld) {
                adapter.hideShimmer(batch);
            } else {
                adapter.addMore(batch);
            }
            swipeRefreshLayout.setRefreshing(false);
            footerProgress.setVisibility(View.GONE);
            isLoadingMore = false;

            if (adapter.getItemCount() == 0) {
                emptyStateText.setVisibility(View.VISIBLE);
            } else {
                emptyStateText.setVisibility(View.GONE);
            }
        });
    }

    private void fetchMissingDetails(List<DocumentSnapshot> missingDocs, List<ClientModel> alreadyFetched, boolean clearOld) {
        int total = missingDocs.size();
        AtomicInteger completedCount = new AtomicInteger(0);
        List<ClientModel> fetchedList = new ArrayList<>(alreadyFetched);

        for (DocumentSnapshot doc : missingDocs) {
            final String clientId = doc.getId();
            fetchClientDetails(clientId, new OnClientDetailsFetched() {
                @Override
                public void onFetched(ClientModel model) {
                    synchronized (fetchedList) {
                        fetchedList.add(model);
                        if (completedCount.incrementAndGet() == total) {
                            updateAdapterWithBatch(fetchedList, clearOld);
                        }
                    }
                }

                @Override
                public void onFailed() {
                    if (completedCount.incrementAndGet() == total) {
                        updateAdapterWithBatch(fetchedList, clearOld);
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

        db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .get()
                .addOnSuccessListener(fillDoc -> {
                    try {
                        String name = FirestoreUtils.getStringSafe(fillDoc, "name");
                        String gpDate = FirestoreUtils.getStringSafe(fillDoc, "gp_date");
                        String makeName = FirestoreUtils.getStringSafe(fillDoc, "make_name");

                        if (name == null || name.isEmpty()) {
                            // fallback to root doc
                            db.collection(COLLECTION_NAME)
                                    .document(clientId)
                                    .get()
                                    .addOnSuccessListener(mainDoc -> {
                                        String fallbackName = FirestoreUtils.getStringSafe(mainDoc, "name");
                                        ClientModel model = new ClientModel(
                                                fallbackName.isEmpty() ? "Unknown" : fallbackName,
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
                                    gpDate.isEmpty() ? "N/A" : formatDate(gpDate),
                                    makeName.isEmpty() ? "N/A" : makeName
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

        db.collection(COLLECTION_NAME)
                .whereEqualTo("progress", 100)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClientModel> searchResults = new ArrayList<>();
                    List<DocumentSnapshot> missingDetailsDocs = new ArrayList<>();
                    List<DocumentSnapshot> documents = querySnapshot.getDocuments();

                    if (documents.isEmpty()) {
                        adapter.hideShimmer(new ArrayList<>());
                        emptyStateText.setVisibility(View.VISIBLE);
                        emptyStateText.setText("No match found");
                        return;
                    }

                    for (DocumentSnapshot doc : documents) {
                        String clientId = doc.getId();
                        String name = FirestoreUtils.getStringSafe(doc, "name");
                        String gpDate = FirestoreUtils.getStringSafe(doc, "gp_date");
                        String makeName = FirestoreUtils.getStringSafe(doc, "make_name");

                        // Check if it matches locally (from root data)
                        if (name.toLowerCase().contains(q) || clientId.toLowerCase().contains(q) || gpDate.toLowerCase().contains(q)) {
                            if (!name.isEmpty() && !gpDate.isEmpty() && !makeName.isEmpty()) {
                                searchResults.add(new ClientModel(name, clientId, formatDate(gpDate), makeName));
                            } else {
                                // Match found but details missing in root
                                missingDetailsDocs.add(doc);
                            }
                        }
                    }

                    if (missingDetailsDocs.isEmpty()) {
                        updateSearchAdapter(searchResults);
                    } else {
                        fetchMissingSearchDetails(missingDetailsDocs, searchResults);
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

    private void updateSearchAdapter(List<ClientModel> results) {
        runOnUiThread(() -> {
            results.sort((a, b) -> b.gpDate.compareTo(a.gpDate));
            adapter.hideShimmer(results);

            if (results.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("No match found");
            } else {
                emptyStateText.setVisibility(View.GONE);
            }
        });
    }

    private void fetchMissingSearchDetails(List<DocumentSnapshot> missingDocs, List<ClientModel> alreadyMatched) {
        int total = missingDocs.size();
        AtomicInteger completedCount = new AtomicInteger(0);
        List<ClientModel> finalResults = new ArrayList<>(alreadyMatched);

        for (DocumentSnapshot doc : missingDocs) {
            fetchClientDetails(doc.getId(), new OnClientDetailsFetched() {
                @Override
                public void onFetched(ClientModel model) {
                    synchronized (finalResults) {
                        finalResults.add(model);
                        if (completedCount.incrementAndGet() == total) {
                            updateSearchAdapter(finalResults);
                        }
                    }
                }

                @Override
                public void onFailed() {
                    if (completedCount.incrementAndGet() == total) {
                        updateSearchAdapter(finalResults);
                    }
                }
            });
        }
    }

    // ---------------------------
    // Utility fetch for fill_one (kept for compatibility)
    // ---------------------------
    private void fetchFillOneData(String clientId, OnFillOneFetched callback) {
        if (db == null) {
            callback.onFetched(new ClientModel("Unknown", clientId, "N/A", "N/A"));
            return;
        }

        db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = FirestoreUtils.getStringSafe(doc, "name");
                        String gpDateRaw = FirestoreUtils.getStringSafe(doc, "gp_date");
                        String makeName = FirestoreUtils.getStringSafe(doc, "make_name");
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
