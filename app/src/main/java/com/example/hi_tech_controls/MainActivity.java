package com.example.hi_tech_controls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.adapter.AddDetailsAdp;
import com.example.hi_tech_controls.adapter.DetailsModel;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewDiscovery1;
    private AddDetailsAdp addDetailsAdapter;
    private ArrayList<DetailsModel> detailsDataList = new ArrayList<>();
    private FirebaseFirestore db;
    private CollectionReference collectionRef;
    private ListenerRegistration realtimeListener;

    private Button addClientBtn1;
    private Button viewClientBtn1;
    private ImageView logout_btn_layout;
    private ImageView refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);
        logout_btn_layout = findViewById(R.id.logout_btn);
        refreshButton = findViewById(R.id.refreshButton);

        recyclerViewDiscovery1 = findViewById(R.id.recyclerViewDiscovery);
        recyclerViewDiscovery1.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter
        addDetailsAdapter = new AddDetailsAdp(this, detailsDataList);
        recyclerViewDiscovery1.setAdapter(addDetailsAdapter);

        // Firebase
        db = FirebaseFirestore.getInstance();
        collectionRef = db.collection("hi_tech_controls_dataset_JUNE");

        // Buttons
        addClientBtn1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddDetailsActivity.class);
            // Don't pass clientId for new client
            startActivity(intent);
        });

        viewClientBtn1.setOnClickListener(v -> {
//            Toast.makeText(MainActivity.this, "You are already on dashboard",
//                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, ViewDetailsActivity.class);
            // Don't pass clientId for new client
            startActivity(intent);

        });

        refreshButton.setOnClickListener(v -> {
            refreshData();
        });

        logout_btn_layout.setOnClickListener(v -> showExitConfirmationDialog());

        // Start real-time listener
        startRealtimeListener();
    }

    private void startRealtimeListener() {
        realtimeListener = collectionRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(MainActivity.this, "Listen failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshots == null) return;

                // Rebuild list from snapshot
                ArrayList<DetailsModel> newList = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String docId = doc.getId();

                    // Skip control documents
                    if (docId.equals("last_id") || docId.equals("initialDoc")) continue;

                    DetailsModel model = new DetailsModel();

                    // Parse client ID
                    try {
                        model.setUId(Integer.parseInt(docId));
                    } catch (Exception ex) {
                        Long maybeId = doc.getLong("clientId");
                        if (maybeId != null) model.setUId(maybeId.intValue());
                        else continue; // Skip invalid entries
                    }

                    // Get name
                    String name = doc.getString("name");
                    model.setuName(name != null ? name : "No Name");

                    // Get progress
                    Long p = doc.getLong("progress");
                    model.setProgress(p != null ? p.intValue() : 0);

                    newList.add(model);
                }

                // Sort by ID descending (newest first)
                Collections.sort(newList, new Comparator<DetailsModel>() {
                    @Override
                    public int compare(DetailsModel o1, DetailsModel o2) {
                        return Integer.compare(o2.getUId(), o1.getUId());
                    }
                });

                // Update adapter
                detailsDataList.clear();
                detailsDataList.addAll(newList);
                addDetailsAdapter.notifyDataSetChanged();
            }
        });
    }

    // ... (same as you sent, but with one small fix in refreshData)
    private void refreshData() {
        Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
        collectionRef.get().addOnSuccessListener(query -> {
            ArrayList<DetailsModel> newList = new ArrayList<>();
            for (DocumentSnapshot doc : query.getDocuments()) {
                String id = doc.getId();
                if (id.equals("last_id") || id.equals("initialDoc")) continue;

                DetailsModel m = new DetailsModel();
                try { m.setUId(Integer.parseInt(id)); }
                catch (Exception e) { continue; }

                m.setuName(doc.getString("name"));
                Long p = doc.getLong("progress");
                m.setProgress(p != null ? p.intValue() : 0);
                newList.add(m);
            }

            Collections.sort(newList, (a, b) -> Integer.compare(b.getUId(), a.getUId()));
            detailsDataList.clear();
            detailsDataList.addAll(newList);
            addDetailsAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Refreshed!", Toast.LENGTH_SHORT).show();
        });
    }

    private void showExitConfirmationDialog() {
        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        dialog.setTitleText("Logout");
        dialog.setContentText("Are you sure you want to logout?");
        dialog.setConfirmButtonBackgroundColor(Color.parseColor("#FF0000"));
        dialog.setConfirmText("Logout");
        dialog.setCancelButtonBackgroundColor(Color.parseColor("#7C7C7C"));
        dialog.setCancelText("Cancel");
        dialog.showCancelButton(true);

        dialog.setCancelClickListener(SweetAlertDialog::dismissWithAnimation);

        dialog.setConfirmClickListener(sDialog -> {
            logout();
            sDialog.dismissWithAnimation();
        });

        dialog.show();
    }

    private void logout() {
        SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("flag", false);
        editor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realtimeListener != null) {
            realtimeListener.remove();
        }
    }

    @Override
    public void onBackPressed() {
        SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("flag", false);
        if (isLoggedIn) {
            showExitConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }
}