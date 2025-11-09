package com.example.hi_tech_controls;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.hi_tech_controls.adapter.OfflineSyncManager;
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

/**
 * AddDetailsActivity — Refactored (Commented / Dev Insight Mode)
 * <p>
 * Purpose:
 * - Manages multi-step client details workflow (4 fragments)
 * - Uses tempClientId for new clients and commits to Firestore on first save
 * - Attaches a realtime listener when editing existing clients
 * <p>
 * Important:
 * - This is intentionally behavior-preserving. No logic changes — only safety, comments and tiny defensive checks.
 */
public class AddDetailsActivity extends BaseActivity {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";

    // UI labels for the text switcher (keeps UX consistent)
    private final String[] switcherValues = {
            "Inward Details",
            "Initial Observation",
            "Repairs Details",
            "Final Trial Check"
    };

    // --- State & Firestore ---
    private FirebaseFirestore db;
    private String clientId;          // committed client id (document id)
    private String tempClientId;      // temporary id shown to user until commit
    private int currentProgress = 0;  // 0..100
    private int currentFragmentIndex = 0;
    private boolean isExistingClient = false; // true when clientId is provided
    private boolean isIdCommitted = false;    // true once tempClientId is committed to last_id

    // --- UI ---
    private ProgressBar progressBar;
    private TextSwitcher textSwitcher;
    private ImageView backBtn, nextBtn;
    private Button cameraButton;
    private TextView dash_tv;
    private CardView progressContainer;

    // Firestore snapshot listener handle — must remove in onDestroy/onPause
    private ListenerRegistration progressListener;

    // Re-usable dialog ref — dismiss on lifecycle end to avoid leaks
    private SweetAlertDialog exitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

        // Firestore instance
        db = FirebaseFirestore.getInstance();

        // Read intent — if clientId passed, we are editing an existing client
        Intent intent = getIntent();
        clientId = intent.getStringExtra("clientId");

        // Initialize UI references & UI components
        initializeUIElements();

        // If editing existing client -> attach listener (real-time sync)
        if (clientId != null && !clientId.isEmpty()) {
            isExistingClient = true;
            isIdCommitted = true;

            // First check current progress without listener to decide initial fragment
            checkInitialProgressAndLoad();
        } else {
            // New client -> generate a temp ID (no listener yet)
            generateTempClientIdAndShow();
        }

        // Wire UI listeners (back/next/camera)
        setupListeners();

        // Override hardware back to show exit dialog (consistent UX)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);

                // If current fragment is View_data_fragment, go back to MainActivity
                if (currentFragment instanceof View_data_fragment) {
                    navigateToMainActivity();
                } else {
                    showExitDialog();
                }
            }
        });
    }

    /**
     * Check initial progress to decide whether to load form or directly show report
     */
    private void checkInitialProgressAndLoad() {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(clientId);

        docRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Long progressLong = snapshot.getLong("progress");
                int initialProgress = progressLong != null ? progressLong.intValue() : 0;

                if (initialProgress >= 100) {
                    // Directly load View_data_fragment for completed clients
                    setViewModeUI();
                    loadFragment(new View_data_fragment(), clientId);
                } else {
                    // Normal flow - attach listener and load appropriate form fragment
                    loadClientProgressAndResume();
                }
            } else {
                // Document doesn't exist, proceed with normal flow
                loadClientProgressAndResume();
            }
        }).addOnFailureListener(e -> {
            // On failure, proceed with normal flow
            loadClientProgressAndResume();
        });
    }

    /**
     * Called by BaseActivity when network state changes.
     * Keep existing behavior: trigger offline queue sync when network returns.
     */
    @Override
    protected void onNetworkStateChanged(boolean isOnline) {
        if (isOnline) {
            OfflineSyncManager.getInstance().syncNow(this);
        }
    }

    /**
     * Initialize UI elements and the text-switcher factory.
     * Keep rendering simple and consistent.
     */
    private void initializeUIElements() {
        dash_tv = findViewById(R.id.dash_tv);
        progressContainer = findViewById(R.id.progressContainer);

        progressBar = findViewById(R.id.progressBar);
        backBtn = findViewById(R.id.addClientDtls_Back);
        nextBtn = findViewById(R.id.addClientDtls_Next);
        cameraButton = findViewById(R.id.cameraButton);

        textSwitcher = findViewById(R.id.textSwitcher);
        textSwitcher.setFactory(() -> {
            TextView textView = new TextView(this);
            textView.setTextSize(17);
            textView.setTextColor(Color.WHITE);
            textView.setGravity(android.view.Gravity.CENTER);
            return textView;
        });
    }

    /**
     * Wire button listeners.
     * - Back navigates through fragments or shows exit dialog
     * - Next attempts to save current fragment then proceeds
     * - Camera launches MediaUpload with active client id (temp or committed)
     */
    private void setupListeners() {
        backBtn.setOnClickListener(v -> goBack());
        nextBtn.setOnClickListener(v -> loadNextFragment());

        cameraButton.setOnClickListener(v -> {
            // Use whichever ID is available (clientId preferred)
            String activeId = (clientId != null && !clientId.isEmpty()) ? clientId : tempClientId;

            if (activeId != null && !activeId.isEmpty()) {
                Intent intent = new Intent(this, MediaUpload.class);
                intent.putExtra("clientId", activeId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Client ID not ready yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -----------------------------
    // NEW CLIENT Flow (temp ID)
    // -----------------------------
    private void generateTempClientIdAndShow() {
        DocumentReference lastIdRef = db.collection(COLLECTION_NAME).document("last_id");

        // Read last_id document once to generate next temp id
        lastIdRef.get().addOnSuccessListener(snapshot -> {
            long lastId = 2000; // default base if missing
            if (snapshot.exists() && snapshot.getLong("lastId") != null) {
                lastId = snapshot.getLong("lastId");
            }

            // Use next id as temp id for UX; commit later on first save
            tempClientId = String.valueOf(lastId + 1);

            // Update dash_tv with temp client ID
            if (dash_tv != null) {
                dash_tv.setText("Client ID: " + tempClientId);
            }

            // Reset UI state and load the first fragment with the temp id
            currentFragmentIndex = 0;
            currentProgress = 0;
            updateUI();
            loadFragment(new fill_one_fragment(), tempClientId);

        }).addOnFailureListener(e -> {
            // Show friendly error and exit — can't continue without an ID
            new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Oops!")
                    .setContentText("Unable to fetch client ID. Please try again.")
                    .show();
            finish();
        });
    }

    // -----------------------------
    // EXISTING CLIENT Flow (listener)
    // -----------------------------
    private void loadClientProgressAndResume() {
        // Defensively attach a listener for real-time updates to this client's root document
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(clientId);

        progressListener = docRef.addSnapshotListener((snapshot, error) -> {
            // Defensive guards — don't proceed on error or null snapshot
            if (error != null || snapshot == null) return;
            if (!snapshot.exists()) return;

            // Read the progress field (safe null handling)
            Long progressLong = snapshot.getLong("progress");
            currentProgress = progressLong != null ? progressLong.intValue() : 0;

            // Update dash_tv with client ID
            runOnUiThread(() -> {
                if (dash_tv != null) {
                    dash_tv.setText("Client ID: " + clientId);
                }
            });

            // If progress is 100%, directly load View_data_fragment
            if (currentProgress >= 100) {
                runOnUiThread(() -> {
                    setViewModeUI();
                    loadFragment(new View_data_fragment(), clientId);
                });
                return;
            }

            // Normal flow for incomplete clients
            currentFragmentIndex = calculateFragmentIndex(currentProgress);
            updateUI();
            loadCurrentFragment();
        });
    }

    /*
     * Set UI for view mode (when showing View_data_fragment)
     */

    /**
     * Set UI for view mode (when showing View_data_fragment)
     */
    private void setViewModeUI() {
        // Change dash_tv text to show Client ID
        if (dash_tv != null) {
            dash_tv.setText("Client ID: " + clientId);
        }

        // Hide progress container
        if (progressContainer != null) {
            progressContainer.setVisibility(View.GONE);
        }

        // Hide only next button, keep back button visible
        if (nextBtn != null) {
            nextBtn.setVisibility(View.GONE);
        }
        if (backBtn != null) {
            backBtn.setVisibility(View.VISIBLE);
        }

        // Update progress bar to 100%
        if (progressBar != null) {
            progressBar.setProgress(100);
        }

        // Update text switcher to show "View Report"
        if (textSwitcher != null) {
            textSwitcher.setText("View Report");
        }
    }

    /**
     * Set UI for form mode (when filling forms)
     */
    @SuppressLint("SetTextI18n")
    private void setFormModeUI() {
        // Change dash_tv text to show Client ID during form filling
        if (dash_tv != null) {
            if (clientId != null && !clientId.isEmpty()) {
                dash_tv.setText(String.format("Client ID: %s", clientId));
            } else if (tempClientId != null && !tempClientId.isEmpty()) {
                dash_tv.setText(String.format("Client ID: %s", tempClientId));
            } else {
                dash_tv.setText("Add Client Details");
            }
        }

        // Show progress container
        if (progressContainer != null) {
            progressContainer.setVisibility(View.VISIBLE);
        }

        // Show both navigation buttons
        if (nextBtn != null) {
            nextBtn.setVisibility(View.VISIBLE);
        }
        if (backBtn != null) {
            backBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Map progress numeric value to fragment index. This keeps behavior identical to original.
     */
    private int calculateFragmentIndex(int progress) {
        if (progress >= 100) return 3; // This case should be handled separately now
        else if (progress >= 75) return 3;
        else if (progress >= 50) return 2;
        else if (progress >= 25) return 1;
        else return 0;
    }

    /**
     * Update visual progress UI. Using the two-arg setProgress on some devices is smoother.
     * This keeps compatibility and removes a deprecation warning.
     */
    private void updateUI() {
        // Some devices/appcompat support the animated setProgress(progress, true)
        try {
            progressBar.setProgress(currentProgress, true);
        } catch (NoSuchMethodError | Exception ignored) {
            // Fallback if device API doesn't support the animated method
            progressBar.setProgress(currentProgress);
        }

        if (currentFragmentIndex < switcherValues.length) {
            textSwitcher.setText(switcherValues[currentFragmentIndex]);
        }
    }

    /**
     * Load the appropriate fragment given the currentFragmentIndex.
     * We keep fragment creation logic the same, only factor into a helper.
     */
    private void loadCurrentFragment() {
        Fragment fragment;
        switch (currentFragmentIndex) {
            case 1:
                fragment = new fill_two_fragment();
                break;
            case 2:
                fragment = new fill_three_fragment();
                break;
            case 3:
                fragment = new fill_four_fragment();
                break;
            default:
                fragment = new fill_one_fragment();
        }
        loadFragment(fragment, clientId);
    }

    private void loadNextFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        if (currentFragment == null) return;

        // Use committed id if present, else temporary id
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
                    //showCompletionPopup();
                }
            });
        }
    }

    // -----------------------------
    // ID Commit Logic (transactional)
    // -----------------------------

    /**
     * Commit tempClientId into the collection's last_id using a transaction.
     * This prevents duplicate IDs when multiple users create clients simultaneously.
     * <p>
     * Behavior unchanged: on success we create the client root doc with initial progress 25.
     */
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

            // If someone else moved lastId forward, throw -> abort and fail the transaction
            if (currentLastId + 1 != Long.parseLong(tempClientId)) {
                // Throwing here will cause the transaction to fail; we re-wrap to a runtime exception
                throw new RuntimeException("ID conflict during commit");
            }

            // Atomically update last_id to new value (merge to preserve fields)
            transaction.set(lastIdRef, new HashMap<String, Object>() {{
                put("lastId", Long.parseLong(tempClientId));
            }}, SetOptions.merge());

            return null;
        }).addOnSuccessListener(aVoid -> {
            // Transaction succeeded -> now commit the client root doc and proceed
            clientId = tempClientId;
            isIdCommitted = true;

            Map<String, Object> rootData = new HashMap<>();
            rootData.put("progress", 25);
            rootData.put("lastUpdated", System.currentTimeMillis());
            clientRef.set(rootData, SetOptions.merge());

            updateProgressAndNavigate(25, 1);
        }).addOnFailureListener(e -> {
            // If transaction failed, inform the user. In practice, this branch is rare.
            new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("ID Conflict")
                    .setContentText("Could not reserve client ID. Please try again.")
                    .show();
        });
    }

    /**
     * Update the local UI immediately (optimistic) and then push progress to Firestore.
     * On Firestore failure we show a warning and queue the update with OfflineSyncManager.
     * <p>
     * Note: Behavior preserved. We do not rollback UI immediately on failure — we inform user and queue.
     */
    private void updateProgressAndNavigate(int newProgress, int nextIndex) {
        // Immediate UI update for snappy experience
        currentProgress = newProgress;
        currentFragmentIndex = nextIndex;
        updateUI();
        loadCurrentFragment();

        // Firestore asynchronous update
        db.collection(COLLECTION_NAME)
                .document(clientId)
                .update("progress", newProgress, "lastUpdated", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    // nothing extra required — operation succeeded
                    Log.d("FirestoreSync", "Progress updated successfully for " + clientId);
                })
                .addOnFailureListener(e -> {
                    // Inform user and queue offline update
                    new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Sync Delayed")
                            .setContentText("Changes saved locally. Will sync once you're online.")
                            .setConfirmText("Okay")
                            .show();

                    OfflineSyncManager.getInstance().queuePendingUpdate(
                            COLLECTION_NAME, clientId,
                            Map.of("progress", newProgress, "lastUpdated", System.currentTimeMillis())
                    );
                });
    }

    // -----------------------------
    // Navigation helpers
    // -----------------------------
    private void goBack() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);

        // If current fragment is View_data_fragment, go back to MainActivity
        if (currentFragment instanceof View_data_fragment) {
            navigateToMainActivity();
            return;
        }

        // Normal navigation for other fragments
        if (currentFragmentIndex > 0) {
            currentFragmentIndex--;
            updateUI();
            loadCurrentFragment();
        } else {
            showExitDialog();
        }
    }

    /**
     * Navigate back to MainActivity when user clicks back button from View_data_fragment
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Standard exit confirmation using SweetAlertDialog.
     * We guard against activity finishing/destroyed to avoid window leaks.
     */
    private void showExitDialog() {
        if (isFinishing() || isDestroyed()) return;

        exitDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        exitDialog.setTitleText("Exit?");
        exitDialog.setContentText(isIdCommitted ? "Your progress is saved." : "No data will be saved.");
        exitDialog.setConfirmText("Exit");
        exitDialog.setCancelText("Stay");
        exitDialog.showCancelButton(true);

        exitDialog.setConfirmClickListener(sDialog -> {
            sDialog.dismissWithAnimation();
            finish();
        });
        exitDialog.setCancelClickListener(SweetAlertDialog::dismissWithAnimation);
        exitDialog.show();
    }

    // -----------------------------
    // Completion popup
    // -----------------------------
    private void showCompletionPopup() {
        if (isFinishing() || isDestroyed()) return;

        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        dialog.setTitleText("Success!");
        dialog.setContentText("Client ID " + clientId + " completed!");
        dialog.setConfirmText("View Report");
        dialog.setCancelText("Dashboard");
        dialog.showCancelButton(true);

        dialog.setConfirmClickListener(sDialog -> {
            sDialog.dismissWithAnimation();
            // Set view mode UI before loading View_data_fragment
            setViewModeUI();
            loadFragment(new View_data_fragment(), clientId);
        });

        dialog.setCancelClickListener(sDialog -> {
            sDialog.dismissWithAnimation();
            finish();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * Load a fragment safely — we check that the activity isn't finishing/destroyed to avoid IllegalStateException.
     * Using commit() (not commitAllowingStateLoss) but only after defensive check to preserve behavior.
     */
    private void loadFragment(Fragment fragment, String id) {
        Bundle bundle = new Bundle();
        if (id != null) bundle.putString("clientId", id);
        fragment.setArguments(bundle);

        // Set appropriate UI mode based on fragment type
        if (fragment instanceof View_data_fragment) {
            setViewModeUI();
        } else {
            setFormModeUI();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!isFinishing() && !isDestroyed()) {
            transaction.setReorderingAllowed(true);
            transaction.replace(R.id.frameLayout, fragment);
            transaction.commit();
        } else {
            Log.w("AddDetailsActivity", "Skipping fragment commit because activity is finishing/destroyed.");
        }
    }

    // -----------------------------
    // Lifecycle cleanup — remove listeners & dialogs to avoid leaks
    // -----------------------------
    @Override
    protected void onDestroy() {
        // Dismiss any showing dialog to avoid WindowLeaked exceptions
        if (exitDialog != null) {
            if (exitDialog.isShowing()) exitDialog.dismiss();
            exitDialog = null;
        }

        // Remove Firestore listener if attached
        if (progressListener != null) {
            try {
                progressListener.remove();
            } catch (Exception ignored) {
                // Defensive: ignore removal errors during destroy
            }
            progressListener = null;
        }

        super.onDestroy();
    }

    // Add this method in AddDetailsActivity class
    public String getCurrentClientId() {
        if (clientId != null && !clientId.isEmpty()) {
            return clientId;
        } else if (tempClientId != null && !tempClientId.isEmpty()) {
            return tempClientId;
        }
        return "";
    }

    public interface SaveCallback {
        void onSaveComplete(boolean success);
    }
}