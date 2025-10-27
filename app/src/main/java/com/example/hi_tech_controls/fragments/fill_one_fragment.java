// ============================================
// fill_one_fragment.java
// ============================================
package com.example.hi_tech_controls.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.AddDetailsActivity;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.LoadingDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class fill_one_fragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";

    private FirebaseFirestore db;
    private String clientId;

    private TextView clientIdTv;
    private EditText enterName, enterNumber, enterGPNumber, enterDate,
            enterMakeName, enterModelName, enterHPrate, enterSerialNumber;

    private DatePickerDialog datePickerDialog;
    private AlertDialog loadingDialog;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_one, container, false);

        db = FirebaseFirestore.getInstance();

        // Get clientId from arguments
        if (getArguments() != null) {
            clientId = getArguments().getString("clientId");
        }

        initializeViews(rootView);
        initDatePicker();
        setupLoadingDialog();

        // Show client ID (temp or real)
        if (clientId != null) {
            clientIdTv.setText(clientId);
        }

        // ONLY LOAD IF REAL CLIENT ID (not temp)
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

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setCancelable(false);
        ProgressBar progressBar = new ProgressBar(requireContext());
        int padding = (int) getResources().getDimension(R.dimen.loading_padding);
        progressBar.setPadding(padding, padding, padding, padding);
        builder.setView(progressBar);
        loadingDialog = builder.create();
    }

    private void showLoading() {
        if (loadingDialog != null && !loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void initDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        datePickerDialog = new DatePickerDialog(requireActivity(), this, year, month, day);
        enterDate.setOnClickListener(v -> datePickerDialog.show());
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        month += 1;
        String date = dayOfMonth + "/" + month + "/" + year;
        enterDate.setText(date);
    }

    // CHECK IF ID IS REAL (not temp)
    private boolean isRealClientId() {
        return clientId != null && !clientId.isEmpty() &&
                !clientId.equals("temp") &&
                !clientId.startsWith("temp") &&
                !clientId.contains("temp");
    }

    /**
     * LOAD EXISTING DATA ONLY FOR REAL CLIENTS
     */
    private void loadExistingData() {
        if (!isRealClientId()) return;

        //showLoading();
        LoadingDialog.getInstance().show(requireContext());

        db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .get()
                .addOnSuccessListener(document -> {
                    //hideLoading();
                    LoadingDialog.getInstance().hide();

                    if (document.exists()) {
                        enterName.setText(document.getString("name"));
                        enterNumber.setText(document.getString("client_number"));
                        enterGPNumber.setText(document.getString("gp_number"));
                        enterDate.setText(document.getString("gp_date"));
                        enterMakeName.setText(document.getString("make_name"));
                        enterModelName.setText(document.getString("model_name"));
                        enterHPrate.setText(document.getString("hp_rate"));
                        enterSerialNumber.setText(document.getString("serial_number"));

                        Toast.makeText(getContext(), "Data loaded", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    //hideLoading();
                    LoadingDialog.getInstance().hide();
                    Toast.makeText(getContext(), "Load failed", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * SAVE DATA – CALLED FROM AddDetailsActivity
     */
    public void saveToFirestore(String clientId, AddDetailsActivity.SaveCallback callback) {
        String name = enterName.getText().toString().trim();
        String phone = enterNumber.getText().toString().trim();
        String gpNumber = enterGPNumber.getText().toString().trim();
        String gpDate = enterDate.getText().toString().trim();
        String makeName = enterMakeName.getText().toString().trim();
        String modelName = enterModelName.getText().toString().trim();
        String hpRate = enterHPrate.getText().toString().trim();
        String serialNumber = enterSerialNumber.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Enter client name", Toast.LENGTH_SHORT).show();
            callback.onSaveComplete(false);
            return;
        }
        if (gpNumber.isEmpty()) {
            Toast.makeText(getContext(), "Enter GP Number", Toast.LENGTH_SHORT).show();
            callback.onSaveComplete(false);
            return;
        }

        //showLoading();
        LoadingDialog.getInstance().show(requireContext());

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("name", name);
        pageData.put("client_number", phone);
        pageData.put("gp_number", gpNumber);
        pageData.put("gp_date", gpDate);
        pageData.put("make_name", makeName);
        pageData.put("model_name", modelName);
        pageData.put("hp_rate", hpRate);
        pageData.put("serial_number", serialNumber);
        pageData.put("completed", true);
        pageData.put("timestamp", System.currentTimeMillis());

        // Save to pages/fill_one
        db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_one")
                .set(pageData)
                .addOnSuccessListener(aVoid -> {
                    // Update root
                    Map<String, Object> rootData = new HashMap<>();
                    rootData.put("name", name);
                    rootData.put("progress", 25);
                    rootData.put("lastUpdated", System.currentTimeMillis());

                    db.collection(COLLECTION_NAME)
                            .document(clientId)
                            .set(rootData, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                //hideLoading();
                                LoadingDialog.getInstance().hide();
                                Toast.makeText(getContext(), "Saved!", Toast.LENGTH_SHORT).show();
                                callback.onSaveComplete(true);
                            })
                            .addOnFailureListener(e -> {
                                //hideLoading();
                                LoadingDialog.getInstance().hide();
                                callback.onSaveComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    //hideLoading();
                    LoadingDialog.getInstance().hide();
                    Toast.makeText(getContext(), "Save failed", Toast.LENGTH_SHORT).show();
                    callback.onSaveComplete(false);
                });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LoadingDialog.getInstance().dismiss(); // Prevent leak
    }
}