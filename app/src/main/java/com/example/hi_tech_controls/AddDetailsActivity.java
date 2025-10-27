// ============================================
// AddDetailsActivity.java
// ============================================
package com.example.hi_tech_controls;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.hi_tech_controls.fragments.View_data_fragment;
import com.example.hi_tech_controls.fragments.fill_four_fragment;
import com.example.hi_tech_controls.fragments.fill_one_fragment;
import com.example.hi_tech_controls.fragments.fill_three_fragment;
import com.example.hi_tech_controls.fragments.fill_two_fragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddDetailsActivity extends AppCompatActivity {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";
    private final String[] switcherValues = {
            "Inward Details",
            "Initial Observation",
            "Repairs Details",
            "Final Trial Check"
    };

    private FirebaseFirestore db;
    private String clientId;
    private String tempClientId; // Only for display
    private int currentProgress = 0;
    private int currentFragmentIndex = 0;
    private boolean isExistingClient = false;
    private boolean isIdCommitted = false;

    private ProgressBar progressBar;
    private TextSwitcher textSwitcher;
    private ImageView backBtn, nextBtn;
    private Button cameraButton;
    private ListenerRegistration progressListener; // Only for existing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        clientId = intent.getStringExtra("clientId");

        initializeUIElements();

        if (clientId != null && !clientId.isEmpty()) {
            // EXISTING CLIENT → Attach listener
            isExistingClient = true;
            isIdCommitted = true;
            loadClientProgressAndResume();
        } else {
            // NEW CLIENT → No listener, just show temp ID
            generateTempClientIdAndShow();
        }

        setupListeners();
    }

    private void initializeUIElements() {
        progressBar = findViewById(R.id.progressBar);
        backBtn = findViewById(R.id.addClientDtls_Back);
        nextBtn = findViewById(R.id.addClientDtls_Next);
        cameraButton = findViewById(R.id.cameraButton);

        textSwitcher = findViewById(R.id.textSwitcher);
        textSwitcher.setFactory(() -> {
            TextView textView = new TextView(this);
            textView.setTextSize(17);
            textView.setTextColor(Color.BLACK);
            textView.setGravity(android.view.Gravity.CENTER);
            return textView;
        });
    }

    private void setupListeners() {
        backBtn.setOnClickListener(v -> goBack());
        nextBtn.setOnClickListener(v -> loadNextFragment());
        cameraButton.setOnClickListener(v -> {
            if (clientId != null) {
                Intent intent = new Intent(this, MediaUpload.class);
                intent.putExtra("clientId", clientId);
                startActivity(intent);
            }
        });
    }

    // NEW CLIENT: Only show temp ID (NO LISTENER)
    private void generateTempClientIdAndShow() {
        DocumentReference lastIdRef = db.collection(COLLECTION_NAME).document("last_id");

        lastIdRef.get().addOnSuccessListener(snapshot -> {
            long lastId = 2000;
            if (snapshot.exists() && snapshot.getLong("lastId") != null) {
                lastId = snapshot.getLong("lastId");
            }
            tempClientId = String.valueOf(lastId + 1);

            currentFragmentIndex = 0;
            currentProgress = 0;
            updateUI();
            loadFragment(new fill_one_fragment(), tempClientId);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load ID", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // EXISTING CLIENT: Attach listener
    private void loadClientProgressAndResume() {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(clientId);

        progressListener = docRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            Long progressLong = snapshot.getLong("progress");
            currentProgress = progressLong != null ? progressLong.intValue() : 0;
            currentFragmentIndex = calculateFragmentIndex(currentProgress);
            updateUI();
            loadCurrentFragment();
        });
    }

    private int calculateFragmentIndex(int progress) {
        if (progress >= 100) return 3;
        else if (progress >= 75) return 3;
        else if (progress >= 50) return 2;
        else if (progress >= 25) return 1;
        else return 0;
    }

    private void updateUI() {
        progressBar.setProgress(currentProgress);
        if (currentFragmentIndex < switcherValues.length) {
            textSwitcher.setText(switcherValues[currentFragmentIndex]);
        }
    }

    private void loadCurrentFragment() {
        Fragment fragment;
        switch (currentFragmentIndex) {
            case 0: fragment = new fill_one_fragment(); break;
            case 1: fragment = new fill_two_fragment(); break;
            case 2: fragment = new fill_three_fragment(); break;
            case 3: fragment = new fill_four_fragment(); break;
            default: fragment = new fill_one_fragment();
        }
        loadFragment(fragment, clientId);
    }

    private void loadNextFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        if (currentFragment == null) return;

        String saveId = isIdCommitted ? clientId : tempClientId;

        if (currentFragment instanceof fill_one_fragment) {
            ((fill_one_fragment) currentFragment).saveToFirestore(saveId, success -> {
                if (success) commitClientIdAndProceed();
            });
        } else if (currentFragment instanceof fill_two_fragment) {
            ((fill_two_fragment) currentFragment).saveToFirestore(saveId, success -> {
                if (success) updateProgressAndNavigate(50, 2);
            });
        } else if (currentFragment instanceof fill_three_fragment) {
            ((fill_three_fragment) currentFragment).saveToFirestore(saveId, success -> {
                if (success) updateProgressAndNavigate(75, 3);
            });
        } else if (currentFragment instanceof fill_four_fragment) {
            ((fill_four_fragment) currentFragment).saveToFirestore(saveId, success -> {
                if (success) {
                    updateProgressAndNavigate(100, 4);
                    showCompletionPopup();
                }
            });
        }
    }

    // COMMIT ID ONLY ON SAVE
    private void commitClientIdAndProceed() {
        if (isIdCommitted) {
            updateProgressAndNavigate(25, 1);
            return;
        }

        DocumentReference lastIdRef = db.collection(COLLECTION_NAME).document("last_id");
        DocumentReference clientRef = db.collection(COLLECTION_NAME).document(tempClientId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(lastIdRef);
            long currentLastId = snap.exists() && snap.getLong("lastId") != null ? snap.getLong("lastId") : 2000;
            if (currentLastId + 1 != Long.parseLong(tempClientId)) {
                try {
                    throw new Exception("ID conflict");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            transaction.set(lastIdRef, new HashMap<String, Object>() {{ put("lastId", Long.parseLong(tempClientId)); }}, SetOptions.merge());
            return null;
        }).addOnSuccessListener(aVoid -> {
            clientId = tempClientId;
            isIdCommitted = true;

            Map<String, Object> rootData = new HashMap<>();
            rootData.put("progress", 25);
            rootData.put("lastUpdated", System.currentTimeMillis());
            clientRef.set(rootData, SetOptions.merge());

            updateProgressAndNavigate(25, 1);
        });
    }

    private void updateProgressAndNavigate(int newProgress, int nextIndex) {
        currentProgress = newProgress;
        DocumentReference clientRef = db.collection(COLLECTION_NAME).document(clientId);
        clientRef.update("progress", newProgress, "lastUpdated", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if (nextIndex < 4) {
                        currentFragmentIndex = nextIndex;
                        updateUI();
                        loadCurrentFragment();
                    }
                });
    }

    private void goBack() {
        if (currentFragmentIndex > 0) {
            currentFragmentIndex--;
            updateUI();
            loadCurrentFragment();
        } else {
            showExitDialog();
        }
    }

    private void showExitDialog() {
        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        dialog.setTitleText("Exit?");
        dialog.setContentText(isIdCommitted ? "Your progress is saved." : "No data will be saved.");
        dialog.setConfirmText("Exit");
        dialog.setCancelText("Stay");
        dialog.showCancelButton(true);

        dialog.setConfirmClickListener(sDialog -> {
            sDialog.dismissWithAnimation();
            finish();
        });

        dialog.setCancelClickListener(SweetAlertDialog::dismissWithAnimation);
        dialog.show();
    }

    private void showCompletionPopup() {
        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        dialog.setTitleText("Success!");
        dialog.setContentText("Client ID " + clientId + " completed!");
        dialog.setConfirmText("View Report");
        dialog.setCancelText("Dashboard");
        dialog.showCancelButton(true);

        dialog.setConfirmClickListener(sDialog -> {
            sDialog.dismissWithAnimation();
            loadFragment(new View_data_fragment(), clientId);
        });

        dialog.setCancelClickListener(sDialog -> {
            sDialog.dismissWithAnimation();
            finish();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void loadFragment(Fragment fragment, String id) {
        Bundle bundle = new Bundle();
        if (id != null) bundle.putString("clientId", id);
        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressListener != null) {
            progressListener.remove();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showExitDialog();
    }

    public interface SaveCallback {
        void onSaveComplete(boolean success);
    }
}