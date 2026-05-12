package com.example.hi_tech_controls.ui.activity;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.animation.DecelerateInterpolator;
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
import androidx.annotation.NonNull;
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
import com.example.hi_tech_controls.helper.FirestoreUtils;

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
            "1. Inward Details",
            "2. Initial Observation",
            "3. Repairs Details",
            "4. Final Trial Check"
    };

    // --- State & Firestore ---
    private FirebaseFirestore db;
    private String clientId; // committed client id (document id)
    private String tempClientId; // temporary id shown to user until commit
    private int currentProgress = 0; // 0..100
    private int currentFragmentIndex = 0;
    private boolean isExistingClient = false; // true when clientId is provided
    private boolean isIdCommitted = false; // true once tempClientId is committed to last_id

    // --- UI ---
    private ProgressBar progressBar;
    private TextSwitcher textSwitcher;
    private ImageView backBtn, nextBtn;
    private Button cameraButton;
    private TextView dash_tv;
    private CardView progressContainer;

    // Firestore snapshot listener handle — must remove in onDestroy/onPause
    private ListenerRegistration progressListener;
    private ListenerRegistration lastIdListener;

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
        com.example.hi_tech_controls.helper.AnalyticsManager.logEvent(this, "intake_start");
        setupListeners(); // sets button listeners (back/next/camera)
        attachBackPressedHandler(); // hardware back behaviour

        // Determine flow (existing or new client)
        if (savedInstanceState != null) {
            clientId = savedInstanceState.getString("clientId");
            tempClientId = savedInstanceState.getString("tempClientId");
            currentProgress = savedInstanceState.getInt("progress");
            currentFragmentIndex = savedInstanceState.getInt("index");
            isIdCommitted = savedInstanceState.getBoolean("isIdCommitted");
            isExistingClient = savedInstanceState.getBoolean("isExistingClient");
            Log.d(TAG, "State restored: clientId=" + clientId + " progress=" + currentProgress);
            updateUI();
            loadCurrentFragment();
            if (isIdCommitted) loadClientProgressAndResume();
        } else {
            decideFlow();
        }

        Log.d(TAG, "onCreate() end");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("clientId", clientId);
        outState.putString("tempClientId", tempClientId);
        outState.putInt("progress", currentProgress);
        outState.putInt("index", currentFragmentIndex);
        outState.putBoolean("isIdCommitted", isIdCommitted);
        outState.putBoolean("isExistingClient", isExistingClient);
        Log.d(TAG, "onSaveInstanceState() called");
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
                Log.d(TAG, "Hardware back pressed. currentFragment="
                        + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
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
        Log.d(TAG, "Attaching realtime listener for last_id to generate tempClientId");

        lastIdListener = lastIdRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "last_id listener error", error);
                return;
            }
            if (snapshot == null || !snapshot.exists()) {
                Log.w(TAG, "last_id snapshot missing");
                return;
            }

            long lastId = FirestoreUtils.getLongSafe(snapshot, "lastId");
            if (lastId == 0)
                lastId = 2000; // fallback if missing
            tempClientId = String.valueOf(lastId + 1);
            Log.d(TAG, "Realtime predicted ID update: " + tempClientId);

            runOnUiThread(() -> {
                if (dash_tv != null && !isIdCommitted) {
                    dash_tv.setText(String.format("Client ID: %s", tempClientId));
                }

                // Switch to the predicted ID for real-time tracking
                if (clientId == null || clientId.isEmpty()) {
                    clientId = tempClientId;
                    updateUI();
                    loadCurrentFragment();
                } else if (!isIdCommitted) {
                    // Update current fragment's UI if it's already loaded
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
                    if (current instanceof fill_one_fragment) {
                        ((fill_one_fragment) current).updateClientId(tempClientId);
                    }
                }
            });

        });
    }

    // -------------------------------------------------------------------------
    // Existing client flow: check progress then attach listener/resume
    // -------------------------------------------------------------------------
    private void checkInitialProgressAndLoad() {
        if (db == null || clientId == null) {
            Log.e(TAG, "checkInitialProgressAndLoad: db or clientId null");
            return;
        }
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(clientId);
        Log.d(TAG, "Checking initial progress for clientId=" + clientId);

        docRef.get().addOnSuccessListener(snapshot -> {
            if (isFinishing() || isDestroyed()) return;

            if (snapshot.exists()) {
                Log.d(TAG, "Client document exists — resuming progress");
                isIdCommitted = true;
                loadClientProgressAndResume();
            } else {
                Log.d(TAG, "Client document does not exist yet — showing Step 1");
                isIdCommitted = false;
                currentProgress = 0;
                currentFragmentIndex = 0;
                updateUI();
                loadCurrentFragment();
                // Attach listener anyway to catch when someone else commits it
                loadClientProgressAndResume();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to read initial progress", e);
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
                // If it doesn't exist, it means it's a new client or not yet committed.
                // We keep the current fragment (likely fill_one) but don't force a jump.
                Log.d(TAG, "Realtime update: Document doesn't exist yet for " + clientId);
                return;
            }

            currentProgress = (int) FirestoreUtils.getLongSafe(snapshot, "progress");
            Log.d(TAG, "Realtime update: progress=" + currentProgress + " (committed=" + snapshot.exists() + ")");
            
            // If document exists, it's definitely committed
            isIdCommitted = true;

            runOnUiThread(() -> {
                if (dash_tv != null && clientId != null)
                    dash_tv.setText("Client ID: " + clientId.replace("temp", ""));
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
        if (dash_tv != null)
            dash_tv.setText("Client ID: " + clientId);
        if (progressContainer != null)
            progressContainer.setVisibility(View.GONE);
        if (nextBtn != null)
            nextBtn.setVisibility(View.GONE);
        if (backBtn != null)
            backBtn.setVisibility(View.VISIBLE);
        if (progressBar != null)
            progressBar.setProgress(100);
        if (textSwitcher != null)
            textSwitcher.setText("View Report");
    }

    @SuppressLint("SetTextI18n")
    private void setFormModeUI() {
        Log.d(TAG, "setFormModeUI()");
        if (dash_tv != null) {
            if (clientId != null && !clientId.isEmpty()) {
                dash_tv.setText(String.format("Client ID: %s", clientId.replace("temp", "")));
            } else if (tempClientId != null && !tempClientId.isEmpty()) {
                dash_tv.setText(String.format("Client ID: %s", tempClientId.replace("temp", "")));
            } else {
                dash_tv.setText("Add Client Details");
            }
        }
        if (progressContainer != null)
            progressContainer.setVisibility(View.VISIBLE);
        if (nextBtn != null)
            nextBtn.setVisibility(View.VISIBLE);
        if (backBtn != null)
            backBtn.setVisibility(View.VISIBLE);
    }

    private int calculateFragmentIndex(int progress) {
        if (progress >= 100)
            return 3;
        else if (progress >= 75)
            return 3;
        else if (progress >= 50)
            return 2;
        else if (progress >= 25)
            return 1;
        else
            return 0;
    }

    private void updateUI() {
        if (progressBar != null) {
            int targetProgress = currentProgress;
            int currentPos = progressBar.getProgress();

            if (currentPos != targetProgress) {
                ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", currentPos, targetProgress);
                animator.setDuration(1000); // 1 second for a smooth, premium feel
                animator.setInterpolator(new DecelerateInterpolator());
                animator.start();
            } else {
                progressBar.setProgress(targetProgress);
            }
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
        Log.d(TAG,
                "loadNextFragment saving to id=" + saveId + " fragment=" + currentFragment.getClass().getSimpleName());

        if (currentFragment instanceof fill_one_fragment) {
            ((fill_one_fragment) currentFragment).saveToFirestore(saveId, success -> {
                if (success)
                    commitClientIdAndProceed();
            });
        } else if (currentFragment instanceof fill_two_fragment) {
            ((fill_two_fragment) currentFragment).saveToFirestore(saveId, success -> {
                if (success)
                    updateProgressAndNavigate(50, 2);
            });
        } else if (currentFragment instanceof fill_three_fragment) {
            ((fill_three_fragment) currentFragment).saveToFirestore(saveId, success -> {
                if (success)
                    updateProgressAndNavigate(75, 3);
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
        String numericId = tempClientId.replace("temp", "");
        DocumentReference clientRef = db.collection(COLLECTION_NAME).document(numericId);
        Log.d(TAG, "Attempting transaction to commit numericId=" + numericId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(lastIdRef);
            long currentLastId = FirestoreUtils.getLongSafe(snap, "lastId");
            if (currentLastId == 0)
                currentLastId = 2000;

            if (currentLastId + 1 != Long.parseLong(numericId)) {
                Log.e(TAG, "ID conflict during commit. expected=" + (currentLastId + 1) + " got=" + numericId);
                throw new RuntimeException("ID conflict during commit");
            }

            transaction.set(lastIdRef, new HashMap<String, Object>() {
                {
                    put("lastId", Long.parseLong(numericId));
                }
            }, SetOptions.merge());

            return null;
        }).addOnSuccessListener(aVoid -> {
            clientId = numericId;
            isIdCommitted = true;
            Map<String, Object> rootData = new HashMap<>();
            rootData.put("progress", 25);
            rootData.put("lastUpdated", System.currentTimeMillis());
            clientRef.set(rootData, SetOptions.merge());
            Log.d(TAG, "ID commit successful. clientId=" + clientId);

            // Cleanup lastIdListener as ID is now fixed
            if (lastIdListener != null) {
                lastIdListener.remove();
                lastIdListener = null;
            }

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

        // Update local state immediately for UI responsiveness
        currentProgress = newProgress;
        currentFragmentIndex = nextIndex;
        updateUI();
        loadCurrentFragment();

        if (db == null || clientId == null) {
            Log.d(TAG, logPrefix + ": db or clientId null, local update only");
            return;
        }

        DocumentReference clientRef = db.collection(COLLECTION_NAME).document(clientId);

        // Use a transaction to ensure we only update progress if it's actually an advancement.
        // This prevents "rolling back" progress if one device is at Step 3 and another 
        // device just finished/viewed Step 1.
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(clientRef);
            if (snapshot.exists()) {
                long existingProgress = FirestoreUtils.getLongSafe(snapshot, "progress");
                if (newProgress > existingProgress) {
                    transaction.update(clientRef, "progress", newProgress, "lastUpdated", System.currentTimeMillis());
                    Log.d(TAG, "Transaction: Progress advanced to " + newProgress);
                } else {
                    // Just update the timestamp to show activity, but keep the higher progress
                    transaction.update(clientRef, "lastUpdated", System.currentTimeMillis());
                    Log.d(TAG, "Transaction: Keeping higher progress " + existingProgress + " (new was " + newProgress + ")");
                }
            } else {
                // Should not happen for existing committed clients
                Map<String, Object> data = new HashMap<>();
                data.put("progress", newProgress);
                data.put("lastUpdated", System.currentTimeMillis());
                transaction.set(clientRef, data, SetOptions.merge());
            }
            return null;
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Progress transaction failed, queuing offline", e);
            Map<String, Object> offlineData = new HashMap<>();
            offlineData.put("progress", newProgress);
            offlineData.put("lastUpdated", System.currentTimeMillis());
            OfflineSyncManager.getInstance().queuePendingUpdate(COLLECTION_NAME, clientId, offlineData);
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

    public String getActiveClientId() {
        return (clientId != null && !clientId.isEmpty()) ? clientId : tempClientId;
    }

    private void showCompletionPopup() {
        if (isFinishing() || isDestroyed())
            return;

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
        if (id != null) {
            // If it's a new client, ensure "temp" prefix is passed for logic but hidden in UI
            String passId = isIdCommitted ? id : (id.startsWith("temp") ? id : "temp" + id);
            bundle.putString("clientId", passId);
        }
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
                if (exitDialog.isShowing())
                    exitDialog.dismiss();
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
            Log.d(TAG, "Realtime progress listener removed");
        }

        if (lastIdListener != null) {
            try {
                lastIdListener.remove();
            } catch (Exception ignored) {
            }
            lastIdListener = null;
            Log.d(TAG, "Realtime lastId listener removed");
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
        if (currentToast != null)
            currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    public interface SaveCallback {
        void onSaveComplete(boolean success);
    }
}
