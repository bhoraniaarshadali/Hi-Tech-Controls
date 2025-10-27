// ============================================
// fill_four_fragment.java
// ============================================
package com.example.hi_tech_controls.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.AddDetailsActivity;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.LoadingDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class fill_four_fragment extends Fragment {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";

    private FirebaseFirestore db;
    private String clientId;
    private DocumentReference fillFourRef;

    // UI Elements
    private Spinner selectEmply;
    private CheckBox checkbox1HP, checkbox10HP, checkbox30HP;
    private EditText OnDisplay_text, OnClamp_text;
    private CheckBox checkboxAMP_U, checkboxAMP_V, checkboxAMP_W;
    private EditText DC_DISP_text, DC_MET_text;
    private EditText OUTPUT_DISP_text, OUTPUT_MET_text;
    private EditText enterRH_text, enterReplayOP_text, enterFANOpr_text;
    private EditText enterBODYCondition_text, enterIOcheck_text;
    private EditText enterClean_Text, enterPramCopy_Text;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_four, container, false);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            clientId = getArguments().getString("clientId");
        }

        // Only create ref if real ID
        if (clientId != null && isRealClientId()) {
            fillFourRef = db.collection(COLLECTION_NAME)
                    .document(clientId)
                    .collection("pages")
                    .document("fill_four");
        }

        initializeUIElements(rootView);
        setUpSpinner();

        // LOAD DATA ONLY IF REAL CLIENT
        if (isRealClientId()) {
            loadExistingData();
        }

        return rootView;
    }

    // CHECK IF ID IS REAL (NOT TEMP)
    private boolean isRealClientId() {
        return clientId != null &&
                !clientId.isEmpty() &&
                !clientId.equals("temp") &&
                !clientId.startsWith("temp") &&
                !clientId.contains("temp");
    }

    private void initializeUIElements(View rootView) {
        selectEmply = rootView.findViewById(R.id.fill_four_selectEmply);
        checkbox1HP = rootView.findViewById(R.id.fill_four_checkbox1HP);
        checkbox10HP = rootView.findViewById(R.id.fill_four_checkbox10HP);
        checkbox30HP = rootView.findViewById(R.id.fill_four_checkbox30HP);
        OnDisplay_text = rootView.findViewById(R.id.fill_four_OnDisplay_text);
        OnClamp_text = rootView.findViewById(R.id.fill_four_OnClamp_text);
        checkboxAMP_U = rootView.findViewById(R.id.fill_four_checkboxAMP_U);
        checkboxAMP_V = rootView.findViewById(R.id.fill_four_checkboxAMP_V);
        checkboxAMP_W = rootView.findViewById(R.id.fill_four_checkboxAMP_W);
        DC_DISP_text = rootView.findViewById(R.id.fill_four_DC_DISP_text);
        DC_MET_text = rootView.findViewById(R.id.fill_four_DC_MET_text);
        OUTPUT_DISP_text = rootView.findViewById(R.id.fill_four_OUTPUT_DISP_text);
        OUTPUT_MET_text = rootView.findViewById(R.id.fill_four_OUTPUT_MET_text);
        enterRH_text = rootView.findViewById(R.id.fill_four_enterRH_text);
        enterReplayOP_text = rootView.findViewById(R.id.fill_four_enterReplayOP_text);
        enterFANOpr_text = rootView.findViewById(R.id.fill_four_enterFANOpr_text);
        enterBODYCondition_text = rootView.findViewById(R.id.fill_four_enterBODYCondition_text);
        enterIOcheck_text = rootView.findViewById(R.id.fill_four_enterIOcheck_text);
        enterClean_Text = rootView.findViewById(R.id.fill_four_enterClean_Text);
        enterPramCopy_Text = rootView.findViewById(R.id.fill_four_enterPramCopy_Text);
    }

    private void setUpSpinner() {
        String[] employees = {"Select Employee", "Arshad", "Samir", "Akhil", "Vishal"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item,
                employees
        );
        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        selectEmply.setAdapter(adapter);

        selectEmply.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && isAdded()) {
                    String selected = parent.getItemAtPosition(position).toString();
                    showToastSafe("Selected: " + selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // LOAD DATA ONLY FOR REAL CLIENTS
    private void loadExistingData() {
        if (fillFourRef == null) return;

        LoadingDialog.getInstance().show(requireContext());

        fillFourRef.get().addOnSuccessListener(document -> {
            LoadingDialog.getInstance().hide();
            if (!isAdded()) return;

            if (document.exists()) {
                // Spinner
                String emp = document.getString("select_emp");
                if (emp != null && !emp.equals("Select Employee")) {
                    selectEmply.setSelection(getSpinnerIndex(selectEmply, emp));
                }

                // Checkboxes (Boolean)
                checkbox1HP.setChecked(Boolean.TRUE.equals(document.getBoolean("checkbox_1HP")));
                checkbox10HP.setChecked(Boolean.TRUE.equals(document.getBoolean("checkbox_10HP")));
                checkbox30HP.setChecked(Boolean.TRUE.equals(document.getBoolean("checkbox_30HP")));
                checkboxAMP_U.setChecked(Boolean.TRUE.equals(document.getBoolean("checkbox_u")));
                checkboxAMP_V.setChecked(Boolean.TRUE.equals(document.getBoolean("checkbox_v")));
                checkboxAMP_W.setChecked(Boolean.TRUE.equals(document.getBoolean("checkbox_w")));

                // EditTexts
                OnDisplay_text.setText(document.getString("On_Display"));
                OnClamp_text.setText(document.getString("On_Clamp"));
                DC_DISP_text.setText(document.getString("dc_dsp"));
                DC_MET_text.setText(document.getString("dc_met"));
                OUTPUT_DISP_text.setText(document.getString("output_dsp"));
                OUTPUT_MET_text.setText(document.getString("output_met"));
                enterRH_text.setText(document.getString("enter_RH"));
                enterReplayOP_text.setText(document.getString("enterReplayOP"));
                enterFANOpr_text.setText(document.getString("enter_FANOpr"));
                enterBODYCondition_text.setText(document.getString("enter_BODY_Condition"));
                enterIOcheck_text.setText(document.getString("enter_io_check"));
                enterClean_Text.setText(document.getString("enterClean"));
                enterPramCopy_Text.setText(document.getString("enterPramCopy"));

                showToastSafe("Data loaded");
            }
        }).addOnFailureListener(e -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Load failed");
        });
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    // SAVE DATA (WORKS WITH TEMP OR REAL ID)
    public void saveToFirestore(String clientId, AddDetailsActivity.SaveCallback callback) {
        if (clientId == null || clientId.isEmpty()) {
            callback.onSaveComplete(false);
            return;
        }

        DocumentReference ref = db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_four");

        LoadingDialog.getInstance().show(requireContext());

        Map<String, Object> data = new HashMap<>();
        data.put("select_emp", selectEmply.getSelectedItem() != null ? selectEmply.getSelectedItem().toString() : "");
        data.put("checkbox_1HP", checkbox1HP.isChecked());
        data.put("checkbox_10HP", checkbox10HP.isChecked());
        data.put("checkbox_30HP", checkbox30HP.isChecked());
        data.put("On_Display", OnDisplay_text.getText().toString().trim());
        data.put("On_Clamp", OnClamp_text.getText().toString().trim());
        data.put("checkbox_u", checkboxAMP_U.isChecked());
        data.put("checkbox_v", checkboxAMP_V.isChecked());
        data.put("checkbox_w", checkboxAMP_W.isChecked());
        data.put("dc_dsp", DC_DISP_text.getText().toString().trim());
        data.put("dc_met", DC_MET_text.getText().toString().trim());
        data.put("output_dsp", OUTPUT_DISP_text.getText().toString().trim());
        data.put("output_met", OUTPUT_MET_text.getText().toString().trim());
        data.put("enter_RH", enterRH_text.getText().toString().trim());
        data.put("enterReplayOP", enterReplayOP_text.getText().toString().trim());
        data.put("enter_FANOpr", enterFANOpr_text.getText().toString().trim());
        data.put("enter_BODY_Condition", enterBODYCondition_text.getText().toString().trim());
        data.put("enter_io_check", enterIOcheck_text.getText().toString().trim());
        data.put("enterClean", enterClean_Text.getText().toString().trim());
        data.put("enterPramCopy", enterPramCopy_Text.getText().toString().trim());

        data.put("completed", true);
        data.put("timestamp", System.currentTimeMillis());

        ref.set(data).addOnSuccessListener(aVoid -> {
            db.collection(COLLECTION_NAME).document(clientId)
                    .update("progress", 100, "lastUpdated", System.currentTimeMillis())
                    .addOnSuccessListener(unused -> {
                        LoadingDialog.getInstance().hide();
                        showToastSafe("All steps completed!");
                        callback.onSaveComplete(true);
                    })
                    .addOnFailureListener(e -> {
                        LoadingDialog.getInstance().hide();
                        callback.onSaveComplete(false);
                    });
        }).addOnFailureListener(e -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Save failed");
            callback.onSaveComplete(false);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LoadingDialog.getInstance().dismiss(); // Prevent leak
    }

    private void showToastSafe(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}