package com.example.hi_tech_controls.ui.activity;

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

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.OfflineSyncManager;
import com.example.hi_tech_controls.ui.fragments.View_data_fragment;
import com.example.hi_tech_controls.ui.fragments.fill_four_fragment;
import com.example.hi_tech_controls.ui.fragments.fill_one_fragment;
import com.example.hi_tech_controls.ui.fragments.fill_three_fragment;
import com.example.hi_tech_controls.ui.fragments.fill_two_fragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * AddDetailsActivity — refactored for readability + logging
 * - Behavior preserved exactly
 * - Code split into small methods called from onCreate()
 * - Added consistent Log.d/Log.e calls for debugging
 */
public class AddDetailsActivity extends BaseActivity {

    private static final String TAG = "AddDetailsActivity";
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

    // Toast holder
    private Toast currentToast;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() start");

        setContentView(R.layout.activity_add_details);

        initFirestore();
        readIntentExtras();
        initializeUIElements();
        setupListeners();                 // sets button listeners (back/next/camera)
        attachBackPressedHandler();       // hardware back behaviour

        // Determine flow (existing or new client)
        decideFlow();

        Log.d(TAG, "onCreate() end");
    }

    // -------------------------------------------------------------------------
    // Initialization helpers
    // -------------------------------------------------------------------------
    private void initFirestore() {
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore initialized");
        } catch (Exception e) {
            db = null;
            Log.e(TAG, "Firestore initialization failed", e);
        }
    }

    private void readIntentExtras() {
        Intent intent = getIntent();
        clientId = intent != null ? intent.getStringExtra("clientId") : null;
        Log.d(TAG, "Intent read. clientId=" + (clientId == null ? "null" : clientId));
    }

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

        Log.d(TAG, "UI elements initialized");
    }

    // -------------------------------------------------------------------------
    // Flow decision
    // -------------------------------------------------------------------------
    private void decideFlow() {
        if (clientId != null && !clientId.isEmpty()) {
            isExistingClient = true;
            isIdCommitted = true;
            Log.d(TAG, "Existing client detected: " + clientId);
            checkInitialProgressAndLoad();
        } else {
            Log.d(TAG, "New client flow (no clientId passed)");
            generateTempClientIdAndShow();
        }
    }

    // -------------------------------------------------------------------------
    // Listeners and handlers
    // -------------------------------------------------------------------------
    private void setupListeners() {
        backBtn.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            goBack();
        });
        nextBtn.setOnClickListener(v -> {
            Log.d(TAG, "Next button clicked");
            loadNextFragment();
        });

        cameraButton.setOnClickListener(v -> {
            Log.d(TAG, "Camera button clicked");
            String activeId = (clientId != null && !clientId.isEmpty()) ? clientId : tempClientId;
            if (activeId != null && !activeId.isEmpty()) {
                Intent intent = new Intent(this, MediaUploadActivity.class);
                intent.putExtra("clientId", activeId);
                startActivity(intent);
            } else {
                showToast("Client ID not ready yet");
                Log.w(TAG, "Camera click ignored — clientId not ready");
            }
        });

        Log.d(TAG, "UI listeners wired");
    }

    private void attachBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
                Log.d(TAG, "Hardware back pressed. currentFragment=" + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                if (currentFragment instanceof View_data_fragment) {
                    navigateToMainActivity();
                } else {
                    showExitDialog();
                }
            }
        });
        Log.d(TAG, "Back pressed handler attached");
    }

    // -------------------------------------------------------------------------
    // New client flow: generate tempID and load first fragment
    // -------------------------------------------------------------------------
    private void generateTempClientIdAndShow() {
        if (db == null) {
            showToast("Firestore unavailable. Can't generate client ID.");
            Log.e(TAG, "generateTempClientIdAndShow: db is null");
            finish();
            return;
        }

        DocumentReference lastIdRef = db.collection(COLLECTION_NAME).document("last_id");
        Log.d(TAG, "Reading last_id to generate tempClientId");

        lastIdRef.get().addOnSuccessListener(snapshot -> {
            long lastId = 2000; // default base if missing
            if (snapshot.exists() && snapshot.getLong("lastId") != null) {
                lastId = snapshot.getLong("lastId");
            }
            tempClientId = String.valueOf(lastId + 1);
            Log.d(TAG, "Generated tempClientId=" + tempClientId);

            if (dash_tv != null) dash_tv.setText("Client ID: " + tempClientId);

            currentFragmentIndex = 0;
            currentProgress = 0;
            updateUI();
            loadFragment(new fill_one_fragment(), tempClientId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to read last_id", e);
            new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Oops!")
                    .setContentText("Unable to fetch client ID. Please try again.")
                    .show();
            finish();
        });
    }

    // -------------------------------------------------------------------------
    // Existing client flow: check progress then attach listener/resume
    // -------------------------------------------------------------------------
    private void checkInitialProgressAndLoad() {
        if (db == null) {
            Log.e(TAG, "checkInitialProgressAndLoad: db is null");
            loadClientProgressAndResume(); // attempt to continue defensively
            return;
        }
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(clientId);
        Log.d(TAG, "Checking initial progress for clientId=" + clientId);

        docRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Long progressLong = snapshot.getLong("progress");
                int initialProgress = progressLong != null ? progressLong.intValue() : 0;
                Log.d(TAG, "Initial progress=" + initialProgress + " for clientId=" + clientId);

                if (initialProgress >= 100) {
                    Log.d(TAG, "Client already completed — loading view mode");
                    setViewModeUI();
                    loadFragment(new View_data_fragment(), clientId);
                } else {
                    loadClientProgressAndResume();
                }
            } else {
                Log.d(TAG, "Client document does not exist — continuing normal flow");
                loadClientProgressAndResume();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to read initial progress — continuing normal flow", e);
            loadClientProgressAndResume();
        });
    }

    private void loadClientProgressAndResume() {
        if (db == null) {
            Log.e(TAG, "loadClientProgressAndResume: db is null");
            return;
        }
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(clientId);
        Log.d(TAG, "Attaching realtime listener for clientId=" + clientId);

        progressListener = docRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Realtime listener error", error);
                return;
            }
            if (snapshot == null) {
                Log.w(TAG, "Realtime listener snapshot == null");
                return;
            }
            if (!snapshot.exists()) {
                Log.w(TAG, "Realtime snapshot does not exist for id=" + clientId);
                return;
            }

            Long progressLong = snapshot.getLong("progress");
            currentProgress = progressLong != null ? progressLong.intValue() : 0;
            Log.d(TAG, "Realtime update: progress=" + currentProgress);

            runOnUiThread(() -> {
                if (dash_tv != null) dash_tv.setText("Client ID: " + clientId);
            });

            if (currentProgress >= 100) {
                runOnUiThread(() -> {
                    setViewModeUI();
                    loadFragment(new View_data_fragment(), clientId);
                });
                return;
            }

            currentFragmentIndex = calculateFragmentIndex(currentProgress);
            updateUI();
            loadCurrentFragment();
        });
    }

    // -------------------------------------------------------------------------
    // UI mode helpers
    // -------------------------------------------------------------------------
    private void setViewModeUI() {
        Log.d(TAG, "setViewModeUI()");
        if (dash_tv != null) dash_tv.setText("Client ID: " + clientId);
        if (progressContainer != null) progressContainer.setVisibility(View.GONE);
        if (nextBtn != null) nextBtn.setVisibility(View.GONE);
        if (backBtn != null) backBtn.setVisibility(View.VISIBLE);
        if (progressBar != null) progressBar.setProgress(100);
        if (textSwitcher != null) textSwitcher.setText("View Report");
    }

    @SuppressLint("SetTextI18n")
    private void setFormModeUI() {
        Log.d(TAG, "setFormModeUI()");
        if (dash_tv != null) {
            if (clientId != null && !clientId.isEmpty()) {
                dash_tv.setText(String.format("Client ID: %s", clientId));
            } else if (tempClientId != null && !tempClientId.isEmpty()) {
                dash_tv.setText(String.format("Client ID: %s", tempClientId));
            } else {
                dash_tv.setText("Add Client Details");
            }
        }
        if (progressContainer != null) progressContainer.setVisibility(View.VISIBLE);
        if (nextBtn != null) nextBtn.setVisibility(View.VISIBLE);
        if (backBtn != null) backBtn.setVisibility(View.VISIBLE);
    }

    private int calculateFragmentIndex(int progress) {
        if (progress >= 100) return 3;
        else if (progress >= 75) return 3;
        else if (progress >= 50) return 2;
        else if (progress >= 25) return 1;
        else return 0;
    }

    private void updateUI() {
        try {
            if (progressBar != null) progressBar.setProgress(currentProgress, true);
        } catch (NoSuchMethodError | Exception ignored) {
            if (progressBar != null) progressBar.setProgress(currentProgress);
        }

        if (currentFragmentIndex < switcherValues.length && textSwitcher != null) {
            textSwitcher.setText(switcherValues[currentFragmentIndex]);
        }
        Log.d(TAG, "updateUI: progress=" + currentProgress + " index=" + currentFragmentIndex);
    }

    // -------------------------------------------------------------------------
    // Fragment navigation
    // -------------------------------------------------------------------------
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
        if (currentFragment == null) {
            Log.w(TAG, "loadNextFragment: currentFragment null");
            return;
        }

        String saveId = isIdCommitted ? clientId : tempClientId;
        Log.d(TAG, "loadNextFragment saving to id=" + saveId + " fragment=" + currentFragment.getClass().getSimpleName());

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
                    completeFormAndShowViewFragment();
                }
            });
        } else {
            Log.w(TAG, "loadNextFragment: unknown fragment type");
        }
    }

    private void completeFormAndShowViewFragment() {
        currentProgress = 100;

        if (db != null && clientId != null) {
            db.collection(COLLECTION_NAME)
                    .document(clientId)
                    .update("progress", 100, "lastUpdated", System.currentTimeMillis());
        }

        setViewModeUI();
        loadFragment(new View_data_fragment(), clientId);
    }


    // -------------------------------------------------------------------------
    // ID commit & progress update
    // -------------------------------------------------------------------------
    private void commitClientIdAndProceed() {
        if (isIdCommitted) {
            Log.d(TAG, "commitClientIdAndProceed: already committed");
            updateProgressAndNavigate(25, 1);
            return;
        }

        if (db == null) {
            Log.e(TAG, "commitClientIdAndProceed: db is null");
            showToast("Unable to commit ID (Firestore unavailable)");
            return;
        }

        DocumentReference lastIdRef = db.collection(COLLECTION_NAME).document("last_id");
        DocumentReference clientRef = db.collection(COLLECTION_NAME).document(tempClientId);
        Log.d(TAG, "Attempting transaction to commit tempClientId=" + tempClientId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(lastIdRef);
            long currentLastId = snap.exists() && snap.getLong("lastId") != null ? snap.getLong("lastId") : 2000;

            if (currentLastId + 1 != Long.parseLong(tempClientId)) {
                Log.e(TAG, "ID conflict during commit. expected=" + (currentLastId + 1) + " got=" + tempClientId);
                throw new RuntimeException("ID conflict during commit");
            }

            transaction.set(lastIdRef, new HashMap<String, Object>() {{
                put("lastId", Long.parseLong(tempClientId));
            }}, SetOptions.merge());

            return null;
        }).addOnSuccessListener(aVoid -> {
            clientId = tempClientId;
            isIdCommitted = true;
            Map<String, Object> rootData = new HashMap<>();
            rootData.put("progress", 25);
            rootData.put("lastUpdated", System.currentTimeMillis());
            clientRef.set(rootData, SetOptions.merge());
            Log.d(TAG, "ID commit successful. clientId=" + clientId);
            updateProgressAndNavigate(25, 1);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "ID commit failed", e);
            new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("ID Conflict")
                    .setContentText("Could not reserve client ID. Please try again.")
                    .show();
        });
    }

    private void updateProgressAndNavigate(int newProgress, int nextIndex) {
        String logPrefix = "updateProgressAndNavigate";
        Log.d(TAG, logPrefix + ": newProgress=" + newProgress + " nextIndex=" + nextIndex + " clientId=" + clientId);

        currentProgress = newProgress;
        currentFragmentIndex = nextIndex;
        updateUI();
        loadCurrentFragment();

        if (db == null) {
            Log.e(TAG, logPrefix + ": db null, queue offline");
            OfflineSyncManager.getInstance().queuePendingUpdate(
                    COLLECTION_NAME, clientId,
                    Map.of("progress", newProgress, "lastUpdated", System.currentTimeMillis())
            );
            return;
        }

        db.collection(COLLECTION_NAME)
                .document(clientId)
                .update("progress", newProgress, "lastUpdated", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> Log.d(TAG, logPrefix + ": progress updated successfully for " + clientId))
                .addOnFailureListener(e -> {
                    Log.e(TAG, logPrefix + ": failed to update progress, queuing", e);
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

    // -------------------------------------------------------------------------
    // Navigation helpers and dialogs
    // -------------------------------------------------------------------------
    private void goBack() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        if (currentFragment instanceof View_data_fragment) {
            navigateToMainActivity();
            return;
        }

        if (currentFragmentIndex > 0) {
            currentFragmentIndex--;
            updateUI();
            loadCurrentFragment();
        } else {
            showExitDialog();
        }
    }

    private void navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showExitDialog() {
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "showExitDialog: activity finishing/destroyed, skipping");
            return;
        }

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
        Log.d(TAG, "Exit dialog shown");
    }

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

    private void loadFragment(Fragment fragment, String id) {
        Bundle bundle = new Bundle();
        if (id != null) bundle.putString("clientId", id);
        fragment.setArguments(bundle);

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
            Log.d(TAG, "Fragment committed: " + fragment.getClass().getSimpleName() + " for id=" + id);
        } else {
            Log.w(TAG, "Skipping fragment commit because activity is finishing/destroyed.");
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle cleanup
    // -------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() cleaning up");
        if (exitDialog != null) {
            try {
                if (exitDialog.isShowing()) exitDialog.dismiss();
            } catch (Exception ignored) {
            }
            exitDialog = null;
        }

        if (progressListener != null) {
            try {
                progressListener.remove();
            } catch (Exception ignored) {
            }
            progressListener = null;
            Log.d(TAG, "Realtime listener removed");
        }

        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
        super.onDestroy();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------
    public String getCurrentClientId() {
        if (clientId != null && !clientId.isEmpty()) {
            return clientId;
        } else if (tempClientId != null && !tempClientId.isEmpty()) {
            return tempClientId;
        }
        return "";
    }

    private void showToast(String msg) {
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    public interface SaveCallback {
        void onSaveComplete(boolean success);
    }
}
