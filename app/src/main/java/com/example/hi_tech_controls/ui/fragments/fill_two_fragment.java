// ============================================
// fill_two_fragment.java
// ============================================
package com.example.hi_tech_controls.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.LoadingDialog;
import com.example.hi_tech_controls.helper.OfflineSyncManager;
import com.example.hi_tech_controls.ui.activity.AddDetailsActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class fill_two_fragment extends Fragment {

    // ----------------------------------------------------
    // Constants
    // ----------------------------------------------------
    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_EMP = "last_emp";

    // ----------------------------------------------------
    // Firestore / IDs
    // ----------------------------------------------------
    private FirebaseFirestore db;
    private String clientId;
    private DocumentReference fillTwoRef;

    // ----------------------------------------------------
    // UI References
    // ----------------------------------------------------
    private Spinner selectEmply;
    private EditText enterFRrate, localEditText;
    private EditText clientObsText, ourObsText, lastFaultText;

    private RadioButton radioButtonLocal, radioButtonRemote, radioButtonComm;
    private RadioButton radioButtonDIODE, radioButtonSCR;

    private CheckBox input_POS_checkbox_U, input_POS_checkbox_V, input_POS_checkbox_W;
    private CheckBox input_NEG_checkbox_U, input_NEG_checkbox_V, input_NEG_checkbox_W;
    private CheckBox output_POS_checkbox_U, output_POS_checkbox_V, output_POS_checkbox_W;
    private CheckBox output_NEG_checkbox_U, output_NEG_checkbox_V, output_NEG_checkbox_W;

    private ScrollView scrollView;
    private Toast activeToast;

    // ----------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_fill_two, container, false);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            clientId = getArguments().getString("clientId");
        }

        if (isRealClientId()) {
            fillTwoRef = db.collection(COLLECTION_NAME)
                    .document(clientId)
                    .collection("pages")
                    .document("fill_two");
        }

        initializeUI(root);
        setUpSpinner();
        setUpRadioButtons();

        if (isRealClientId()) {
            loadExistingData();
        }

        if (scrollView != null) scrollView.scrollTo(0, 0);
        return root;
    }

    // ----------------------------------------------------
    // UI Initialization
    // ----------------------------------------------------
    private void initializeUI(View root) {
        scrollView = root.findViewById(R.id.scrollView);

        selectEmply = root.findViewById(R.id.fill_two_selectEmply);
        enterFRrate = root.findViewById(R.id.fill_two_enterFRrate);
        localEditText = root.findViewById(R.id.fill_two_localEditText);

        radioButtonLocal = root.findViewById(R.id.fill_two_radioButtonLocal);
        radioButtonRemote = root.findViewById(R.id.fill_two_radioButtonRemote);
        radioButtonComm = root.findViewById(R.id.fill_two_radioButtonComm);

        radioButtonDIODE = root.findViewById(R.id.fill_two_radioButtonDIODE);
        radioButtonSCR = root.findViewById(R.id.fill_two_radioButtonSCR);

        input_POS_checkbox_U = root.findViewById(R.id.fill_two_input_POS_checkbox_U);
        input_POS_checkbox_V = root.findViewById(R.id.fill_two_input_POS_checkbox_V);
        input_POS_checkbox_W = root.findViewById(R.id.fill_two_input_POS_checkbox_W);

        input_NEG_checkbox_U = root.findViewById(R.id.fill_two_input_NEG_checkbox_U);
        input_NEG_checkbox_V = root.findViewById(R.id.fill_two_input_NEG_checkbox_V);
        input_NEG_checkbox_W = root.findViewById(R.id.fill_two_input_NEG_checkbox_W);

        output_POS_checkbox_U = root.findViewById(R.id.fill_two_output_POS_checkbox_U);
        output_POS_checkbox_V = root.findViewById(R.id.fill_two_output_POS_checkbox_V);
        output_POS_checkbox_W = root.findViewById(R.id.fill_two_output_POS_checkbox_W);

        output_NEG_checkbox_U = root.findViewById(R.id.fill_two_output_NEG_checkbox_U);
        output_NEG_checkbox_V = root.findViewById(R.id.fill_two_output_NEG_checkbox_V);
        output_NEG_checkbox_W = root.findViewById(R.id.fill_two_output_NEG_checkbox_W);

        clientObsText = root.findViewById(R.id.fill_two_clientObs_text);
        ourObsText = root.findViewById(R.id.fill_two_ourObs_text);
        lastFaultText = root.findViewById(R.id.fill_two_lastFault_text);
    }

    // ----------------------------------------------------
    // Load Existing Data
    // ----------------------------------------------------
    private void loadExistingData() {
        if (fillTwoRef == null) return;

        LoadingDialog.getInstance().show(requireContext());

        fillTwoRef.get().addOnSuccessListener(doc -> {
            LoadingDialog.getInstance().hide();
            if (!doc.exists()) return;

            // Restore Spinner
            String emp = doc.getString("select_emp");
            if (emp != null) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) selectEmply.getAdapter();
                int pos = adapter.getPosition(emp);
                if (pos >= 0) selectEmply.setSelection(pos);
            }

            // Restore text fields
            enterFRrate.setText(doc.getString("fr_rate"));
            localEditText.setText(doc.getString("localEditText"));
            clientObsText.setText(doc.getString("client_obs"));
            ourObsText.setText(doc.getString("our_obs"));
            lastFaultText.setText(doc.getString("last_fault"));

            // Restore radio buttons
            radioButtonLocal.setChecked(getBoolean(doc, "local_radio_checked"));
            radioButtonRemote.setChecked(getBoolean(doc, "remote_radio_checked"));
            radioButtonComm.setChecked(getBoolean(doc, "comm_radio_checked"));
            radioButtonDIODE.setChecked(getBoolean(doc, "diode_radio_checked"));
            radioButtonSCR.setChecked(getBoolean(doc, "scr_radio_checked"));

            localEditText.setVisibility(radioButtonLocal.isChecked() ? View.VISIBLE : View.GONE);

            // Restore checkboxes
            input_POS_checkbox_U.setChecked(getBoolean(doc, "input_pos_checkbox_U"));
            input_POS_checkbox_V.setChecked(getBoolean(doc, "input_pos_checkbox_V"));
            input_POS_checkbox_W.setChecked(getBoolean(doc, "input_pos_checkbox_W"));
            input_NEG_checkbox_U.setChecked(getBoolean(doc, "input_neg_checkbox_U"));
            input_NEG_checkbox_V.setChecked(getBoolean(doc, "input_neg_checkbox_V"));
            input_NEG_checkbox_W.setChecked(getBoolean(doc, "input_neg_checkbox_W"));
            output_POS_checkbox_U.setChecked(getBoolean(doc, "output_pos_checkbox_U"));
            output_POS_checkbox_V.setChecked(getBoolean(doc, "output_pos_checkbox_V"));
            output_POS_checkbox_W.setChecked(getBoolean(doc, "output_pos_checkbox_W"));
            output_NEG_checkbox_U.setChecked(getBoolean(doc, "output_neg_checkbox_U"));
            output_NEG_checkbox_V.setChecked(getBoolean(doc, "output_neg_checkbox_V"));
            output_NEG_checkbox_W.setChecked(getBoolean(doc, "output_neg_checkbox_W"));

            showToastSafe("Data loaded");

        }).addOnFailureListener(e -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Failed to load data");
        });
    }

    // ----------------------------------------------------
    // Radio Buttons Logic
    // ----------------------------------------------------
    private void setUpRadioButtons() {

        localEditText.setVisibility(radioButtonLocal.isChecked() ? View.VISIBLE : View.GONE);

        radioButtonLocal.setOnCheckedChangeListener((b, checked) -> {
            localEditText.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (checked) localEditText.requestFocus();
        });

        radioButtonRemote.setOnCheckedChangeListener((b, checked) -> {
            if (checked) localEditText.setVisibility(View.GONE);
        });

        radioButtonComm.setOnCheckedChangeListener((b, checked) -> {
            if (checked) localEditText.setVisibility(View.GONE);
        });
    }

    // ----------------------------------------------------
    // Spinner Logic
    // ----------------------------------------------------
    private void setUpSpinner() {

        String[] employees = {"Select Employee", "Arshad", "Samir", "Akhil", "Vishal"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item, employees);

        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        selectEmply.setAdapter(adapter);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastEmp = prefs.getString(KEY_LAST_EMP, "Select Employee");

        int pos = adapter.getPosition(lastEmp);
        if (pos >= 0) selectEmply.setSelection(pos);

        selectEmply.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long id) {
                if (i == 0) return; // ignore "Select Employee"

                String selected = parent.getItemAtPosition(i).toString();
                prefs.edit().putString(KEY_LAST_EMP, selected).apply();
                showToastSafe("Selected: " + selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // ----------------------------------------------------
    // Save to Firestore
    // ----------------------------------------------------
    public void saveToFirestore(String clientId, AddDetailsActivity.SaveCallback callback) {

        if (clientId == null || clientId.isEmpty()) {
            callback.onSaveComplete(false);
            return;
        }

        if (selectEmply.getSelectedItem().toString().equals("Select Employee")) {
            showToastSafe("Please select employee");
            callback.onSaveComplete(false);
            return;
        }

        if (enterFRrate.getText().toString().trim().isEmpty()) {
            showToastSafe("Please enter FR rate");
            callback.onSaveComplete(false);
            return;
        }

        LoadingDialog.getInstance().show(requireContext());

        Map<String, Object> data = buildFirestoreData();

        DocumentReference pageRef = db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_two");

        WriteBatch batch = db.batch();
        batch.set(pageRef, data);
        batch.update(db.collection(COLLECTION_NAME).document(clientId),
                "progress", 50,
                "lastUpdated", System.currentTimeMillis());

        batch.commit().addOnSuccessListener(unused -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Step 2 saved successfully");
            callback.onSaveComplete(true);

        }).addOnFailureListener(e -> {
            LoadingDialog.getInstance().hide();
            OfflineSyncManager.getInstance().queuePendingUpdate(COLLECTION_NAME, clientId, data);
            showToastSafe("Saved offline - will sync later");
            callback.onSaveComplete(true);
        });
    }

    private Map<String, Object> buildFirestoreData() {

        Map<String, Object> data = new HashMap<>();

        data.put("select_emp", selectEmply.getSelectedItem().toString());
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

        // Checkboxes
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

        // Metadata
        data.put("completed", true);
        data.put("timestamp", System.currentTimeMillis());
        data.put("timestamp_human",
                new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date()));

        return data;
    }

    // ----------------------------------------------------
    // Helpers
    // ----------------------------------------------------
    private boolean isRealClientId() {
        return clientId != null && !clientId.isEmpty()
                && !clientId.startsWith("temp");
    }

    private boolean getBoolean(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        Boolean b = doc.getBoolean(key);
        if (b != null) return b;
        String s = doc.getString(key);
        return s != null && s.equalsIgnoreCase("true");
    }

    private void showToastSafe(String msg) {
        if (!isAdded()) return;
        if (activeToast != null) activeToast.cancel();
        activeToast = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT);
        activeToast.show();
    }

    // ----------------------------------------------------
    // Lifecycle cleanup
    // ----------------------------------------------------
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeToast != null) {
            activeToast.cancel();
            activeToast = null;
        }
        LoadingDialog.getInstance().dismiss();
    }
}
