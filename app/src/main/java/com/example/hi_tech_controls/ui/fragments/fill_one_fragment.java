// ============================================
// fill_one_fragment.java
// ============================================
package com.example.hi_tech_controls.ui.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.LoadingDialog;
import com.example.hi_tech_controls.helper.OfflineSyncManager;
import com.example.hi_tech_controls.helper.FirestoreUtils;
import com.example.hi_tech_controls.ui.activity.AddDetailsActivity;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class fill_one_fragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";
    private static final String TAG = "fill_one_fragment";

    private FirebaseFirestore db;
    private String clientId;

    private TextView clientIdTv;
    private EditText enterName, enterNumber, enterGPNumber, enterDate,
            enterMakeName, enterModelName, enterHPrate, enterSerialNumber;

    private DatePickerDialog datePickerDialog;
    private Toast activeToast;
    private ShimmerFrameLayout shimmerLayout;
    private View contentContainer;
    private Map<String, Object> lastSavedData = new HashMap<>();

    // -------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_one, container, false);

        extractArguments();
        initializeFirestore();
        initializeViews(rootView);
        setupAnimations(rootView);
        initDatePicker();
        setupFocusFlow();
        populateClientId();
        loadExistingIfRequired();

        return rootView;
    }

    // -------------------------------------------------------------------
    // INITIAL SETUP METHODS
    // -------------------------------------------------------------------
    private void extractArguments() {
        clientId = getArguments() != null ? getArguments().getString("clientId") : null;
    }

    private void initializeFirestore() {
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews(View root) {
        clientIdTv = root.findViewById(R.id.clientId);
        enterName = root.findViewById(R.id.fill_one_enterName);
        enterNumber = root.findViewById(R.id.fill_one_enterNumber);
        enterGPNumber = root.findViewById(R.id.fill_one_enterGPNumber);
        enterDate = root.findViewById(R.id.fill_one_enterDate);
        enterMakeName = root.findViewById(R.id.fill_one_enterMakeName);
        enterModelName = root.findViewById(R.id.fill_one_enterModelName);
        enterHPrate = root.findViewById(R.id.fill_one_enterHPrate);
        enterSerialNumber = root.findViewById(R.id.fill_one_enterSerialNumber);
        shimmerLayout = root.findViewById(R.id.shimmer_layout);
        contentContainer = root.findViewById(R.id.content_container);
    }

    private void setupAnimations(View rootView) {
        rootView.setAlpha(0f);
        rootView.animate().alpha(1f).setDuration(300).start();
    }

    private void populateClientId() {
        if (clientId != null)
            clientIdTv.setText(clientId.replace("temp", ""));
    }

    public void updateClientId(String newId) {
        this.clientId = newId;
        if (clientIdTv != null && isAdded()) {
            clientIdTv.setText(newId.replace("temp", ""));
        }
    }

    private void loadExistingIfRequired() {
        if (isValidClientId())
            loadExistingData();
    }

    private boolean isValidClientId() {
        return clientId != null
                && !clientId.isEmpty()
                && !clientId.startsWith("temp");
    }

    // -------------------------------------------------------------------
    // DATE PICKER
    // -------------------------------------------------------------------
    private void initDatePicker() {
        Calendar c = Calendar.getInstance();
        datePickerDialog = new DatePickerDialog(
                requireContext(), this,
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));
        enterDate.setOnClickListener(v -> datePickerDialog.show());
    }

    @Override
    public void onDateSet(android.widget.DatePicker view, int y, int m, int d) {
        enterDate.setText(d + "/" + (m + 1) + "/" + y);
    }

    // -------------------------------------------------------------------
    // INPUT NAVIGATION FLOW
    // -------------------------------------------------------------------
    private void setupFocusFlow() {
        enterName.setOnEditorActionListener((v, a, e) -> {
            enterNumber.requestFocus();
            return true;
        });
        enterNumber.setOnEditorActionListener((v, a, e) -> {
            enterGPNumber.requestFocus();
            return true;
        });
        enterGPNumber.setOnEditorActionListener((v, a, e) -> {
            enterDate.performClick();
            return true;
        });
    }

    // -------------------------------------------------------------------
    // LOAD EXISTING FIRESTORE DATA
    // -------------------------------------------------------------------
    private void loadExistingData() {
        showShimmer();
        safeSetHint(enterName, "Loading...");

        db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .get()
                .addOnSuccessListener(doc -> {
                    hideShimmer();
                    safeSetHint(enterName, "Enter name");
                    if (doc.exists()) {
                        populateFields(doc);
                        lastSavedData = preparePageData(); // Cache initial state
//                        showToastSafe("Data loaded!");
                    }
                })
                .addOnFailureListener(e -> {
                    hideShimmer();
                    showToastSafe("Load failed: " + e.getMessage());
                    Log.e(TAG, "Firestore load error", e);
                });
    }

    private void populateFields(com.google.firebase.firestore.DocumentSnapshot doc) {
        safeSetText(enterName, FirestoreUtils.getStringSafe(doc, "name"));
        safeSetText(enterNumber, FirestoreUtils.getStringSafe(doc, "client_number"));
        safeSetText(enterGPNumber, FirestoreUtils.getStringSafe(doc, "gp_number"));
        safeSetText(enterDate, FirestoreUtils.getStringSafe(doc, "gp_date"));
        safeSetText(enterMakeName, FirestoreUtils.getStringSafe(doc, "make_name"));
        safeSetText(enterModelName, FirestoreUtils.getStringSafe(doc, "model_name"));
        safeSetText(enterHPrate, FirestoreUtils.getStringSafe(doc, "hp_rate"));
        safeSetText(enterSerialNumber, FirestoreUtils.getStringSafe(doc, "serial_number"));
    }

    // -------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------
    private boolean validateFields() {
        if (isEmpty(enterName)) {
            showToastSafe("Enter client name");
            return false;
        }
        if (isEmpty(enterGPNumber)) {
            showToastSafe("Enter GP Number");
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------
    // SAVE FIRESTORE DATA
    // -------------------------------------------------------------------
    public void saveToFirestore(String clientId, AddDetailsActivity.SaveCallback callback) {

        if (!validatePreSave(clientId)) {
            callback.onSaveComplete(false);
            return;
        }

        Map<String, Object> pageData = preparePageData();

        if (lastSavedData != null && !lastSavedData.isEmpty() && !isDataChanged(pageData)) {
            Log.d(TAG, "No changes in fill_one, skipping save");
            callback.onSaveComplete(true);
            return;
        }

        showLoading();
        showToastSafe("Saving...");

        // Strip temp prefix for real document name
        String realId = clientId.replace("temp", "");
        savePageData(realId, pageData, success -> {
            if (success) {
                lastSavedData = new HashMap<>(pageData); // Update cache on success
            }
            callback.onSaveComplete(success);
        });
    }

    private boolean isDataChanged(Map<String, Object> newData) {
        if (lastSavedData == null || lastSavedData.isEmpty())
            return true;

        for (Map.Entry<String, Object> entry : newData.entrySet()) {
            String key = entry.getKey();
            if (key.equals("timestamp") || key.equals("lastUpdated"))
                continue;

            Object oldVal = lastSavedData.get(key);
            Object newVal = entry.getValue();

            if (oldVal == null && newVal == null)
                continue;
            if (oldVal == null || !oldVal.equals(newVal)) {
                Log.d(TAG, "Change detected in key: " + key + " (old=" + oldVal + " new=" + newVal + ")");
                return true;
            }
        }
        return false;
    }

    private boolean validatePreSave(String clientId) {
        return clientId != null
                && !clientId.trim().isEmpty()
                && validateFields();
    }

    private Map<String, Object> preparePageData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", getText(enterName));
        data.put("client_number", getText(enterNumber));
        data.put("gp_number", getText(enterGPNumber));
        data.put("gp_date", getText(enterDate));
        data.put("make_name", getText(enterMakeName));
        data.put("model_name", getText(enterModelName));
        data.put("hp_rate", getText(enterHPrate));
        data.put("serial_number", getText(enterSerialNumber));
        data.put("completed", true);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    private void savePageData(String clientId, Map<String, Object> pageData, AddDetailsActivity.SaveCallback callback) {

        db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .set(pageData)
                .addOnSuccessListener(a -> saveRootData(clientId, pageData, callback))
                .addOnFailureListener(e -> handlePageSaveFailure(clientId, pageData, callback));
    }

    private void saveRootData(String clientId, Map<String, Object> pageData, AddDetailsActivity.SaveCallback callback) {
        Map<String, Object> rootData = new HashMap<>();
        rootData.put("name", getText(enterName));
        rootData.put("gp_date", getText(enterDate));
        rootData.put("make_name", getText(enterMakeName));
        rootData.put("progress", 25);
        rootData.put("lastUpdated", System.currentTimeMillis());

        db.collection(COLLECTION_NAME)
                .document(clientId)
                .set(rootData, SetOptions.merge())
                .addOnSuccessListener(u -> handleSaveSuccess(callback))
                .addOnFailureListener(e -> handleRootSaveFailure(clientId, rootData, callback));
    }

    private void handlePageSaveFailure(String clientId, Map<String, Object> pageData,
            AddDetailsActivity.SaveCallback callback) {

        hideLoading();
        queueOffline(pageData, "pages", "fill_one");
        showToastSafe("⚠️ Saved offline");
        callback.onSaveComplete(true);
    }

    private void handleRootSaveFailure(String clientId, Map<String, Object> rootData,
            AddDetailsActivity.SaveCallback callback) {

        hideLoading();
        queueOffline(rootData);
        showToastSafe("⚠️ Saved offline");
        callback.onSaveComplete(true);
    }

    private void handleSaveSuccess(AddDetailsActivity.SaveCallback callback) {
        hideLoading();
        animateSuccess();
        showToastSafe("✓ Saved!");
        callback.onSaveComplete(true);
    }

    // -------------------------------------------------------------------
    // OFFLINE SAVE QUEUE
    // -------------------------------------------------------------------
    private void queueOffline(Map<String, Object> data) {
        OfflineSyncManager.getInstance().queuePendingUpdate(
                COLLECTION_NAME,
                clientId,
                data);
    }

    private void queueOffline(Map<String, Object> data, String subCollection, String docId) {
        OfflineSyncManager.getInstance().queuePendingUpdate(
                COLLECTION_NAME + "/" + clientId + "/" + subCollection,
                docId,
                data);
    }

    // -------------------------------------------------------------------
    // SMALL UTILITIES
    // -------------------------------------------------------------------
    private void animateSuccess() {
        View v = getView();
        if (v != null) {
            v.animate()
                    .scaleX(1.02f).scaleY(1.02f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100))
                    .start();
        }
    }

    private void safeSetText(EditText et, String text) {
        if (et != null && isAdded())
            et.setText(text != null ? text : "");
    }

    private void safeSetHint(EditText et, String hint) {
        if (et != null && isAdded())
            et.setHint(hint);
    }

    private String getText(EditText et) {
        return et != null ? et.getText().toString().trim() : "";
    }

    private boolean isEmpty(EditText et) {
        return et == null || et.getText().toString().trim().isEmpty();
    }

    private void showLoading() {
        if (isAdded())
            LoadingDialog.getInstance().show(requireContext());
    }

    private void hideLoading() {
        if (isAdded())
            LoadingDialog.getInstance().hide();
    }

    private void showShimmer() {
        if (shimmerLayout != null && isAdded()) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }
        if (contentContainer != null && isAdded()) {
            contentContainer.setVisibility(View.GONE);
        }
    }

    private void hideShimmer() {
        if (shimmerLayout != null && isAdded()) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (contentContainer != null && isAdded()) {
            contentContainer.setVisibility(View.VISIBLE);
        }
    }

    private void showToastSafe(String msg) {
        if (!isAdded() || getContext() == null)
            return;
        if (activeToast != null)
            activeToast.cancel();
        activeToast = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT);
        activeToast.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
        }
        if (activeToast != null)
            activeToast.cancel();
        try {
            LoadingDialog.getInstance().dismiss();
        } catch (Exception ignored) {
        }
    }
}
