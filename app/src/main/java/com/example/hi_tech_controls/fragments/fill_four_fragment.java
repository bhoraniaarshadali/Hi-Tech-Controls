// ============================================
// fill_four_fragment.java (Safe + Optimized Version)
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
import com.google.firebase.firestore.WriteBatch;

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
    private Toast activeToast;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_four, container, false);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            clientId = getArguments().getString("clientId");
        }

        if (isRealClientId()) {
            fillFourRef = db.collection(COLLECTION_NAME)
                    .document(clientId)
                    .collection("pages")
                    .document("fill_four");
        }

        initializeUIElements(rootView);
        setUpSpinner();

        if (isRealClientId()) {
            loadExistingData();
        }

        return rootView;
    }

    private boolean isRealClientId() {
        return clientId != null && !clientId.isEmpty() &&
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadExistingData() {
        if (fillFourRef == null) return;

        LoadingDialog.getInstance().show(requireContext());

        fillFourRef.get().addOnSuccessListener(document -> {
            LoadingDialog.getInstance().hide();
            if (!isAdded() || !document.exists()) return;

            String emp = document.getString("select_emp");
            if (emp != null && !emp.equals("Select Employee")) {
                selectEmply.setSelection(getSpinnerIndex(selectEmply, emp));
            }

            checkbox1HP.setChecked(getBoolean(document, "checkbox_1HP"));
            checkbox10HP.setChecked(getBoolean(document, "checkbox_10HP"));
            checkbox30HP.setChecked(getBoolean(document, "checkbox_30HP"));
            checkboxAMP_U.setChecked(getBoolean(document, "checkbox_u"));
            checkboxAMP_V.setChecked(getBoolean(document, "checkbox_v"));
            checkboxAMP_W.setChecked(getBoolean(document, "checkbox_w"));

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

            showToastSafe("Data loaded 4");
        }).addOnFailureListener(e -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Load failed");
        });
    }

    private boolean getBoolean(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        Boolean value = doc.getBoolean(key);
        return value != null && value;
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    public void saveToFirestore(String clientId, AddDetailsActivity.SaveCallback callback) {
        if (clientId == null || clientId.isEmpty()) {
            callback.onSaveComplete(false);
            return;
        }

        DocumentReference pageRef = db.collection(COLLECTION_NAME)
                .document(clientId)
                .collection("pages")
                .document("fill_four");

        LoadingDialog.getInstance().show(requireContext());

        Map<String, Object> data = new HashMap<>();
        data.put("select_emp", getSpinnerValue(selectEmply));
        data.put("checkbox_1HP", checkbox1HP.isChecked());
        data.put("checkbox_10HP", checkbox10HP.isChecked());
        data.put("checkbox_30HP", checkbox30HP.isChecked());
        data.put("On_Display", getText(OnDisplay_text));
        data.put("On_Clamp", getText(OnClamp_text));
        data.put("checkbox_u", checkboxAMP_U.isChecked());
        data.put("checkbox_v", checkboxAMP_V.isChecked());
        data.put("checkbox_w", checkboxAMP_W.isChecked());
        data.put("dc_dsp", getText(DC_DISP_text));
        data.put("dc_met", getText(DC_MET_text));
        data.put("output_dsp", getText(OUTPUT_DISP_text));
        data.put("output_met", getText(OUTPUT_MET_text));
        data.put("enter_RH", getText(enterRH_text));
        data.put("enterReplayOP", getText(enterReplayOP_text));
        data.put("enter_FANOpr", getText(enterFANOpr_text));
        data.put("enter_BODY_Condition", getText(enterBODYCondition_text));
        data.put("enter_io_check", getText(enterIOcheck_text));
        data.put("enterClean", getText(enterClean_Text));
        data.put("enterPramCopy", getText(enterPramCopy_Text));
        data.put("completed", true);
        data.put("timestamp", System.currentTimeMillis());

        // ✅ Use batch for atomic commit
        WriteBatch batch = db.batch();
        batch.set(pageRef, data);

        DocumentReference rootRef = db.collection(COLLECTION_NAME).document(clientId);
        batch.update(rootRef, "progress", 100, "lastUpdated", System.currentTimeMillis());

        batch.commit().addOnSuccessListener(aVoid -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("All steps completed!");
            callback.onSaveComplete(true);
        }).addOnFailureListener(e -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Save failed");
            callback.onSaveComplete(false);
        });
    }

    private String getText(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String getSpinnerValue(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        return item != null ? item.toString() : "";
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

        // Cancel any currently showing toast
        if (activeToast != null) {
            activeToast.cancel();
        }

        // Create a new one and show
        activeToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT);
        activeToast.show();
    }
}
