package com.example.hi_tech_controls;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hi_tech_controls.adapter.ClientAdapter;
import com.example.hi_tech_controls.adapter.ClientModel;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ViewDetailsActivity extends AppCompatActivity {

    ImageView backBtn;
    EditText searchField;
    //Button searchBtn;
    RecyclerView recyclerView;
    ProgressBar loadingProgress;
    ClientAdapter adapter;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_details);

        backBtn = findViewById(R.id.viewClientDtls_Back);
        searchField = findViewById(R.id.searchField);
        //searchBtn = findViewById(R.id.searchButton);
        recyclerView = findViewById(R.id.recentClientsRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);

        db = FirebaseFirestore.getInstance();
        adapter = new ClientAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backBtn.setOnClickListener(v -> finish());

        loadRecentClients();

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 2) searchClients(s.toString());
                else loadRecentClients();
            }
        });

//        searchBtn.setOnClickListener(v -> {
            String q = searchField.getText().toString().trim();
            if (!q.isEmpty()) searchClients(q);
//        });
    }

    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingProgress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void loadRecentClients() {
        showLoading();
        db.collection("hi_tech_controls_dataset_JUNE")
                .orderBy("gp_date", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClientModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String clientId = doc.getId();
                        fetchFillOneData(clientId, model -> {
                            list.add(model);
                            if (list.size() == querySnapshot.size()) {
                                list.sort((a, b) -> b.gpDate.compareTo(a.gpDate));
                                adapter.update(list);
                                hideLoading();
                            }
                        });
                    }
                    if (querySnapshot.isEmpty()) hideLoading();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
    }

    private void searchClients(String query) {
        showLoading();
        String q = query.toLowerCase();
        db.collection("hi_tech_controls_dataset_JUNE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClientModel> list = new ArrayList<>();
                    int total = querySnapshot.size();
                    int[] count = {0};

                    if (total == 0) {
                        hideLoading();
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot) {
                        String clientId = doc.getId();
                        fetchFillOneData(clientId, model -> {
                            if (model.name.toLowerCase().contains(q) ||
                                    clientId.toLowerCase().contains(q) ||
                                    model.gpDate.toLowerCase().contains(q)) {
                                list.add(model);
                            }
                            count[0]++;
                            if (count[0] == total) {
                                adapter.update(list);
                                hideLoading();
                            }
                        });
                    }
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
                .addOnFailureListener(e -> callback.onFetched(new ClientModel("Error", clientId, "N/A", "N/A")));
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

    interface OnFillOneFetched {
        void onFetched(ClientModel model);
    }
}