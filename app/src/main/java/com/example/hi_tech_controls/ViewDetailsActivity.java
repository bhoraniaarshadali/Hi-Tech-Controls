package com.example.hi_tech_controls;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hi_tech_controls.adapter.ClientAdapter;
import com.example.hi_tech_controls.adapter.ClientModel;
import com.example.hi_tech_controls.adapter.OfflineSyncManager;
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

        backBtn = findViewById(R.id.viewClientDtls_Back);
        searchField = findViewById(R.id.searchField);
        recyclerView = findViewById(R.id.recentClientsRecyclerView);
        footerProgress = findViewById(R.id.footerProgress);
        emptyStateText = findViewById(R.id.emptyStateText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        db = FirebaseFirestore.getInstance();
        adapter = new ClientAdapter();
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        backBtn.setOnClickListener(v -> finish());

        loadRecentClients();

        // 🔹 Search listener
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
                    searchClients(text);
                } else {
                    resetPagination();
                    loadRecentClients();
                }
            }
        });

        // 🔹 Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            resetPagination();
            searchField.setText("");
            loadRecentClients();
        });

        // 🔹 Scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                if (!isLoadingMore && !isLastPage &&
                        (visibleItemCount + firstVisibleItem >= totalItemCount - 2) &&
                        firstVisibleItem >= 0) {
                    loadMoreClients();
                }
            }
        });
    }

    @Override
    protected void onNetworkStateChanged(boolean isOnline) {
        if (isOnline) {
            OfflineSyncManager.getInstance().syncNow(this);
        }
    }

    private void resetPagination() {
        lastVisible = null;
        isLastPage = false;
        isLoadingMore = false;
        adapter.showShimmer();
    }

    private void loadRecentClients() {
        adapter.showShimmer();
        emptyStateText.setVisibility(View.GONE);
        isLoadingMore = false;

        Query query = db.collection("hi_tech_controls_dataset_JUNE")
                .whereEqualTo("progress", 100)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(5);

        query.get().addOnSuccessListener(querySnapshot -> {
            handleClientBatch(querySnapshot, true);
        }).addOnFailureListener(e -> {
            adapter.hideShimmer(new ArrayList<>());
            emptyStateText.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Failed to load recents", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadMoreClients() {
        if (lastVisible == null || isLastPage || isLoadingMore) return;

        isLoadingMore = true;
        footerProgress.setVisibility(View.VISIBLE);

        Query query = db.collection("hi_tech_controls_dataset_JUNE")
                .whereEqualTo("progress", 100)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(5);

        query.get().addOnSuccessListener(querySnapshot -> {
            handleClientBatch(querySnapshot, false);
        }).addOnFailureListener(e -> {
            footerProgress.setVisibility(View.GONE);
            isLoadingMore = false;
            Toast.makeText(this, "Failed to load more", Toast.LENGTH_SHORT).show();
        });
    }


    // 🔹 FIXED: Completely rewritten handleClientBatch method
    private void handleClientBatch(QuerySnapshot querySnapshot, boolean clearOld) {
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

        // Store lastVisible before async operations
        lastVisible = documents.get(documents.size() - 1);

        for (DocumentSnapshot doc : documents) {
            String clientId = doc.getId();
            fetchClientDetails(clientId, new OnClientDetailsFetched() {
                @Override
                public void onFetched(ClientModel model) {
                    synchronized (tempList) {
                        tempList.add(model);

                        if (completedCount.incrementAndGet() == total) {
                            // Sort on UI thread
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

    // 🔹 New method to fetch client details
    private void fetchClientDetails(String clientId, OnClientDetailsFetched callback) {
        db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .get()
                .addOnSuccessListener(fillDoc -> {
                    String name = fillDoc.getString("name");
                    String gpDate = fillDoc.getString("gp_date");
                    String makeName = fillDoc.getString("make_name");

                    // Fallback to main document if fill_one data is missing
                    if (name == null || name.isEmpty()) {
                        db.collection("hi_tech_controls_dataset_JUNE")
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
                })
                .addOnFailureListener(e -> {
                    callback.onFailed();
                });
    }


    // 🔹 Fixed search method
    private void searchClients(String query) {
        adapter.showShimmer();
        emptyStateText.setVisibility(View.GONE);
        String q = query.toLowerCase().trim();

        db.collection("hi_tech_controls_dataset_JUNE")
                .whereEqualTo("progress", 100)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClientModel> searchResults = new ArrayList<>();
                    List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                    AtomicInteger completedCount = new AtomicInteger(0);

                    if (documents.isEmpty()) {
                        adapter.hideShimmer(new ArrayList<>());
                        emptyStateText.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (DocumentSnapshot doc : documents) {
                        String clientId = doc.getId();
                        fetchClientDetails(clientId, new OnClientDetailsFetched() {
                            @Override
                            public void onFetched(ClientModel model) {
                                synchronized (searchResults) {
                                    if (model.name.toLowerCase().contains(q) ||
                                            model.clientId.toLowerCase().contains(q) ||
                                            model.gpDate.toLowerCase().contains(q)) {
                                        searchResults.add(model);
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
                                    runOnUiThread(() -> {
                                        adapter.hideShimmer(searchResults);
                                    });
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        adapter.hideShimmer(new ArrayList<>());
                        emptyStateText.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void fetchFillOneData(String clientId, OnFillOneFetched callback) {
        db.collection("hi_tech_controls_dataset_JUNE")
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
                .addOnFailureListener(e ->
                        callback.onFetched(new ClientModel("Error", clientId, "N/A", "N/A")));
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "N/A";
        try {
            SimpleDateFormat input = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return output.format(Objects.requireNonNull(input.parse(raw)));
        } catch (Exception e) {
            return raw;
        }
    }

    interface OnClientDetailsFetched {
        void onFetched(ClientModel model);

        void onFailed();
    }

    interface OnFillOneFetched {
        void onFetched(ClientModel model);
    }
}
