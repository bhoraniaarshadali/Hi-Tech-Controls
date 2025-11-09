// ============================================
// fill_two_fragment.java (final upgraded & stable)
// ============================================
package com.example.hi_tech_controls.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.AddDetailsActivity;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.LoadingDialog;
import com.example.hi_tech_controls.adapter.OfflineSyncManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class fill_two_fragment extends Fragment {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_EMP = "last_emp";

    private FirebaseFirestore db;
    private String clientId;
    private DocumentReference fillTwoRef;

    // UI
    private Spinner selectEmply;
    private EditText enterFRrate, localEditText;
    private RadioButton radioButtonLocal, radioButtonRemote, radioButtonComm;
    private RadioButton radioButtonDIODE, radioButtonSCR;
    private CheckBox input_POS_checkbox_U, input_POS_checkbox_V, input_POS_checkbox_W;
    private CheckBox input_NEG_checkbox_U, input_NEG_checkbox_V, input_NEG_checkbox_W;
    private CheckBox output_POS_checkbox_U, output_POS_checkbox_V, output_POS_checkbox_W;
    private CheckBox output_NEG_checkbox_U, output_NEG_checkbox_V, output_NEG_checkbox_W;
    private EditText clientObsText, ourObsText, lastFaultText;
    private Toast activeToast;
    private ScrollView scrollView;
    private Button saveButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_two, container, false);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            clientId = getArguments().getString("clientId");
        }

        if (clientId != null && isRealClientId()) {
            fillTwoRef = db.collection(COLLECTION_NAME)
                    .document(clientId)
                    .collection("pages")
                    .document("fill_two");
        }

        initializeUIElements(rootView);
        setUpSpinner();
        setUpRadioGroupListener();

        if (isRealClientId()) {
            loadExistingData(); // ✅ added back
        }

        if (scrollView != null) scrollView.scrollTo(0, 0);
        return rootView;
    }

    private boolean isRealClientId() {
        return clientId != null && !clientId.isEmpty()
                && !clientId.equals("temp")
                && !clientId.startsWith("temp")
                && !clientId.contains("temp");
    }

    private void initializeUIElements(View rootView) {
        scrollView = rootView.findViewById(R.id.scrollView);

        selectEmply = rootView.findViewById(R.id.fill_two_selectEmply);
        enterFRrate = rootView.findViewById(R.id.fill_two_enterFRrate);
        radioButtonLocal = rootView.findViewById(R.id.fill_two_radioButtonLocal);
        radioButtonRemote = rootView.findViewById(R.id.fill_two_radioButtonRemote);
        radioButtonComm = rootView.findViewById(R.id.fill_two_radioButtonComm);
        localEditText = rootView.findViewById(R.id.fill_two_localEditText);
        radioButtonDIODE = rootView.findViewById(R.id.fill_two_radioButtonDIODE);
        radioButtonSCR = rootView.findViewById(R.id.fill_two_radioButtonSCR);

        input_POS_checkbox_U = rootView.findViewById(R.id.fill_two_input_POS_checkbox_U);
        input_POS_checkbox_V = rootView.findViewById(R.id.fill_two_input_POS_checkbox_V);
        input_POS_checkbox_W = rootView.findViewById(R.id.fill_two_input_POS_checkbox_W);
        input_NEG_checkbox_U = rootView.findViewById(R.id.fill_two_input_NEG_checkbox_U);
        input_NEG_checkbox_V = rootView.findViewById(R.id.fill_two_input_NEG_checkbox_V);
        input_NEG_checkbox_W = rootView.findViewById(R.id.fill_two_input_NEG_checkbox_W);
        output_POS_checkbox_U = rootView.findViewById(R.id.fill_two_output_POS_checkbox_U);
        output_POS_checkbox_V = rootView.findViewById(R.id.fill_two_output_POS_checkbox_V);
        output_POS_checkbox_W = rootView.findViewById(R.id.fill_two_output_POS_checkbox_W);
        output_NEG_checkbox_U = rootView.findViewById(R.id.fill_two_output_NEG_checkbox_U);
        output_NEG_checkbox_V = rootView.findViewById(R.id.fill_two_output_NEG_checkbox_V);
        output_NEG_checkbox_W = rootView.findViewById(R.id.fill_two_output_NEG_checkbox_W);

        clientObsText = rootView.findViewById(R.id.fill_two_clientObs_text);
        ourObsText = rootView.findViewById(R.id.fill_two_ourObs_text);
        lastFaultText = rootView.findViewById(R.id.fill_two_lastFault_text);
    }

    private void setUpSpinner() {
        String[] employees = {"Select Employee", "Arshad", "Samir", "Akhil", "Vishal"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, employees);
        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        selectEmply.setAdapter(adapter);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastEmp = prefs.getString(KEY_LAST_EMP, "Select Employee");
        int pos = adapter.getPosition(lastEmp);
        if (pos >= 0) selectEmply.setSelection(pos);

        selectEmply.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && isAdded()) {
                    String selected = parent.getItemAtPosition(position).toString();
                    prefs.edit().putString(KEY_LAST_EMP, selected).apply();
                    showToastSafe("Selected: " + selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setUpRadioGroupListener() {
        localEditText.setVisibility(radioButtonLocal.isChecked() ? View.VISIBLE : View.GONE);
        radioButtonLocal.setOnCheckedChangeListener((b, isChecked) -> {
            localEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) localEditText.requestFocus();
        });
        radioButtonRemote.setOnCheckedChangeListener((b, isChecked) -> {
            if (isChecked) localEditText.setVisibility(View.GONE);
        });
        radioButtonComm.setOnCheckedChangeListener((b, isChecked) -> {
            if (isChecked) localEditText.setVisibility(View.GONE);
        });
    }

    // ✅ loadExistingData restored and upgraded safely
    private void loadExistingData() {
        if (fillTwoRef == null) return;
        if (isAdded()) LoadingDialog.getInstance().show(requireContext());

        fillTwoRef.get()
                .addOnSuccessListener(document -> {
                    if (isAdded()) LoadingDialog.getInstance().hide();
                    if (!document.exists()) return;

                    String emp = document.getString("select_emp");
                    if (emp != null) {
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) selectEmply.getAdapter();
                        int pos = adapter.getPosition(emp);
                        if (pos >= 0) selectEmply.setSelection(pos);
                    }

                    enterFRrate.setText(document.getString("fr_rate"));
                    localEditText.setText(document.getString("localEditText"));
                    clientObsText.setText(document.getString("client_obs"));
                    ourObsText.setText(document.getString("our_obs"));
                    lastFaultText.setText(document.getString("last_fault"));

                    radioButtonLocal.setChecked(getBoolean(document, "local_radio_checked"));
                    radioButtonRemote.setChecked(getBoolean(document, "remote_radio_checked"));
                    radioButtonComm.setChecked(getBoolean(document, "comm_radio_checked"));
                    radioButtonDIODE.setChecked(getBoolean(document, "diode_radio_checked"));
                    radioButtonSCR.setChecked(getBoolean(document, "scr_radio_checked"));
                    localEditText.setVisibility(radioButtonLocal.isChecked() ? View.VISIBLE : View.GONE);

                    input_POS_checkbox_U.setChecked(getBoolean(document, "input_pos_checkbox_U"));
                    input_POS_checkbox_V.setChecked(getBoolean(document, "input_pos_checkbox_V"));
                    input_POS_checkbox_W.setChecked(getBoolean(document, "input_pos_checkbox_W"));
                    input_NEG_checkbox_U.setChecked(getBoolean(document, "input_neg_checkbox_U"));
                    input_NEG_checkbox_V.setChecked(getBoolean(document, "input_neg_checkbox_V"));
                    input_NEG_checkbox_W.setChecked(getBoolean(document, "input_neg_checkbox_W"));
                    output_POS_checkbox_U.setChecked(getBoolean(document, "output_pos_checkbox_U"));
                    output_POS_checkbox_V.setChecked(getBoolean(document, "output_pos_checkbox_V"));
                    output_POS_checkbox_W.setChecked(getBoolean(document, "output_pos_checkbox_W"));
                    output_NEG_checkbox_U.setChecked(getBoolean(document, "output_neg_checkbox_U"));
                    output_NEG_checkbox_V.setChecked(getBoolean(document, "output_neg_checkbox_V"));
                    output_NEG_checkbox_W.setChecked(getBoolean(document, "output_neg_checkbox_W"));

                    showToastSafe("Data loaded");
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) LoadingDialog.getInstance().hide();
                    showToastSafe("Failed to load data");
                });
    }

    private boolean getBoolean(com.google.firebase.firestore.DocumentSnapshot doc, String field) {
        Boolean b = doc.getBoolean(field);
        if (b != null) return b;
        String val = doc.getString(field);
        return val != null && val.equalsIgnoreCase("true");
    }

    public void saveToFirestore(String clientId, AddDetailsActivity.SaveCallback callback) {
        if (clientId == null || clientId.isEmpty()) {
            callback.onSaveComplete(false);
            return;
        }

        String selectedEmp = selectEmply.getSelectedItem().toString();
        if (selectedEmp.equals("Select Employee")) {
            showToastSafe("Please select employee");
            callback.onSaveComplete(false);
            return;
        }

        if (enterFRrate.getText().toString().trim().isEmpty()) {
            showToastSafe("Please enter FR rate");
            callback.onSaveComplete(false);
            return;
        }

        if (isAdded()) LoadingDialog.getInstance().show(requireContext());
        if (saveButton != null) saveButton.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("select_emp", selectedEmp);
        data.put("fr_rate", enterFRrate.getText().toString().trim());
        data.put("localEditText", localEditText.getText().toString().trim());
        data.put("client_obs", clientObsText.getText().toString().trim());
        data.put("our_obs", ourObsText.getText().toString().trim());
        data.put("last_fault", lastFaultText.getText().toString().trim());

        data.put("local_radio_checked", radioButtonLocal.isChecked());
        data.put("remote_radio_checked", radioButtonRemote.isChecked());
        data.put("comm_radio_checked", radioButtonComm.isChecked());
        data.put("diode_radio_checked", radioButtonDIODE.isChecked());
        data.put("scr_radio_checked", radioButtonSCR.isChecked());

        data.put("input_pos_checkbox_U", input_POS_checkbox_U.isChecked());
        data.put("input_pos_checkbox_V", input_POS_checkbox_V.isChecked());
        data.put("input_pos_checkbox_W", input_POS_checkbox_W.isChecked());
        data.put("input_neg_checkbox_U", input_NEG_checkbox_U.isChecked());
        data.put("input_neg_checkbox_V", input_NEG_checkbox_V.isChecked());
        data.put("input_neg_checkbox_W", input_NEG_checkbox_W.isChecked());
        data.put("output_pos_checkbox_U", output_POS_checkbox_U.isChecked());
        data.put("output_pos_checkbox_V", output_POS_checkbox_V.isChecked());
        data.put("output_pos_checkbox_W", output_POS_checkbox_W.isChecked());
        data.put("output_neg_checkbox_U", output_NEG_checkbox_U.isChecked());
        data.put("output_neg_checkbox_V", output_NEG_checkbox_V.isChecked());
        data.put("output_neg_checkbox_W", output_NEG_checkbox_W.isChecked());

        data.put("completed", true);
        data.put("timestamp", System.currentTimeMillis());
        data.put("timestamp_human", new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date()));

        DocumentReference ref = db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_two");

        WriteBatch batch = db.batch();
        batch.set(ref, data);
        batch.update(db.collection(COLLECTION_NAME).document(clientId),
                "progress", 50,
                "lastUpdated", System.currentTimeMillis());

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (isAdded()) LoadingDialog.getInstance().hide();
                    if (saveButton != null) saveButton.setEnabled(true);
                    showToastSafe("Step 2 saved successfully ✅");
                    callback.onSaveComplete(true);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) LoadingDialog.getInstance().hide();
                    if (saveButton != null) saveButton.setEnabled(true);
                    showToastSafe("Save failed (offline mode active)");
                    try {
                        OfflineSyncManager.getInstance().queuePendingUpdate(COLLECTION_NAME, clientId, data);
                    } catch (Exception ignored) {
                    }
                    callback.onSaveComplete(true);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeToast != null) {
            activeToast.cancel();
            activeToast = null;
        }
        LoadingDialog.getInstance().dismiss();
    }

    private void showToastSafe(String message) {
        if (!isAdded() || getContext() == null) return;
        if (activeToast != null) activeToast.cancel();
        activeToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT);
        activeToast.show();
    }
}
