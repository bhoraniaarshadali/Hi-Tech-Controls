package com.example.hi_tech_controls.fragments;

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

import com.example.hi_tech_controls.AddDetailsActivity;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.LoadingDialog;
import com.example.hi_tech_controls.adapter.OfflineSyncManager;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_one, container, false);

        db = FirebaseFirestore.getInstance();
        clientId = getArguments() != null ? getArguments().getString("clientId") : null;

        initializeViews(rootView);
        initDatePicker();
        setupFocusFlow();

        // Fade-in
        rootView.setAlpha(0f);
        rootView.animate().alpha(1f).setDuration(300).start();

        if (clientId != null) {
            clientIdTv.setText(clientId);
        }

        if (isRealClientId()) {
            loadExistingData();
        }

        return rootView;
    }

    private void initializeViews(View rootView) {
        clientIdTv = rootView.findViewById(R.id.clientId);
        enterName = rootView.findViewById(R.id.fill_one_enterName);
        enterNumber = rootView.findViewById(R.id.fill_one_enterNumber);
        enterGPNumber = rootView.findViewById(R.id.fill_one_enterGPNumber);
        enterDate = rootView.findViewById(R.id.fill_one_enterDate);
        enterMakeName = rootView.findViewById(R.id.fill_one_enterMakeName);
        enterModelName = rootView.findViewById(R.id.fill_one_enterModelName);
        enterHPrate = rootView.findViewById(R.id.fill_one_enterHPrate);
        enterSerialNumber = rootView.findViewById(R.id.fill_one_enterSerialNumber);
    }

    private void setupFocusFlow() {
        enterName.setOnEditorActionListener((v, actionId, event) -> {
            enterNumber.requestFocus();
            return true;
        });
        enterNumber.setOnEditorActionListener((v, actionId, event) -> {
            enterGPNumber.requestFocus();
            return true;
        });
        enterGPNumber.setOnEditorActionListener((v, actionId, event) -> {
            enterDate.performClick();
            return true;
        });
    }

    private void initDatePicker() {
        Calendar c = Calendar.getInstance();
        datePickerDialog = new DatePickerDialog(requireContext(), this,
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        enterDate.setOnClickListener(v -> datePickerDialog.show());
    }

    @Override
    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
        month += 1;
        enterDate.setText(dayOfMonth + "/" + month + "/" + year);
    }

    private boolean isRealClientId() {
        return clientId != null && !clientId.isEmpty() &&
                !clientId.equals("temp") && !clientId.startsWith("temp");
    }

    private void loadExistingData() {
        showLoading();
        safeSetHint(enterName, "Loading...");

        db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .get()
                .addOnSuccessListener(document -> {
                    hideLoading();
                    if (document.exists()) {
                        safeSetText(enterName, document.getString("name"));
                        safeSetText(enterNumber, document.getString("client_number"));
                        safeSetText(enterGPNumber, document.getString("gp_number"));
                        safeSetText(enterDate, document.getString("gp_date"));
                        safeSetText(enterMakeName, document.getString("make_name"));
                        safeSetText(enterModelName, document.getString("model_name"));
                        safeSetText(enterHPrate, document.getString("hp_rate"));
                        safeSetText(enterSerialNumber, document.getString("serial_number"));

                        showToastSafe("Data loaded!");
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showToastSafe("Load failed: " + e.getMessage());
                    Log.e(TAG, "Firestore load error", e);
                });
    }

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

    public void saveToFirestore(String clientId, AddDetailsActivity.SaveCallback callback) {
        if (clientId == null || clientId.trim().isEmpty()) {
            callback.onSaveComplete(false);
            return;
        }

        if (!validateFields()) {
            callback.onSaveComplete(false);
            return;
        }

        showLoading();
        showToastSafe("Saving...");

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("name", getText(enterName));
        pageData.put("client_number", getText(enterNumber));
        pageData.put("gp_number", getText(enterGPNumber));
        pageData.put("gp_date", getText(enterDate));
        pageData.put("make_name", getText(enterMakeName));
        pageData.put("model_name", getText(enterModelName));
        pageData.put("hp_rate", getText(enterHPrate));
        pageData.put("serial_number", getText(enterSerialNumber));
        pageData.put("completed", true);
        pageData.put("timestamp", System.currentTimeMillis());

        // ✅ Save PAGE data
        db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .set(pageData)
                .addOnSuccessListener(aVoid -> {
                    // ✅ Save ROOT data
                    Map<String, Object> rootData = new HashMap<>();
                    rootData.put("name", getText(enterName));
                    rootData.put("progress", 25);
                    rootData.put("lastUpdated", System.currentTimeMillis());

                    db.collection(COLLECTION_NAME)
                            .document(clientId)
                            .set(rootData, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                hideLoading();
                                animateSuccess();
                                showToastSafe("✅ Saved!");
                                callback.onSaveComplete(true);
                            })
                            .addOnFailureListener(e -> {
                                hideLoading();
                                queueOffline(rootData);  // ✅ FIXED: 3 params
                                showToastSafe("⚠️ Saved offline");
                                callback.onSaveComplete(true);  // Still success for UX
                            });
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    queueOffline(pageData, "pages", "fill_one");  // ✅ FIXED: 3 params
                    showToastSafe("⚠️ Saved offline");
                    callback.onSaveComplete(true);  // Still success for UX
                });
    }

    private void animateSuccess() {
        View v = getView();
        if (v != null && v.isAttachedToWindow()) {
            v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
        }
    }

    private void queueOffline(Map<String, Object> data) {
        OfflineSyncManager.getInstance().queuePendingUpdate(
                COLLECTION_NAME,           // collectionPath
                clientId,                  // documentId
                data                       // data
        );
    }

    private void queueOffline(Map<String, Object> data, String subCollection, String docId) {
        OfflineSyncManager.getInstance().queuePendingUpdate(
                COLLECTION_NAME + "/" + clientId + "/" + subCollection, // collectionPath
                docId,                                                           // documentId
                data                                                             // data
        );
    }

    // === SAFE HELPERS ===
    private void safeSetText(EditText et, String text) {
        if (et != null && isAdded()) {
            et.setText(text != null ? text : "");
        }
    }

    private void safeSetHint(EditText et, String hint) {
        if (et != null && isAdded()) {
            et.setHint(hint);
        }
    }

    private String getText(EditText et) {
        return et != null ? et.getText().toString().trim() : "";
    }

    private boolean isEmpty(EditText et) {
        return et == null || et.getText().toString().trim().isEmpty();
    }

    private void showLoading() {
        if (isAdded()) LoadingDialog.getInstance().show(requireContext());
    }

    private void hideLoading() {
        if (isAdded()) LoadingDialog.getInstance().hide();
    }

    private void showToastSafe(String msg) {
        if (!isAdded() || getContext() == null) return;
        if (activeToast != null) activeToast.cancel();
        activeToast = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT);
        activeToast.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeToast != null) activeToast.cancel();
        try {
            LoadingDialog.getInstance().dismiss();
        } catch (Exception ignored) {
        }
    }
}