// ============================================
// fill_four_fragment.java
// ============================================
package com.example.hi_tech_controls.ui.fragments;

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

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.LoadingDialog;
import com.example.hi_tech_controls.ui.activity.AddDetailsActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class fill_four_fragment extends Fragment {

    // ----------------------------------------------------
    // Constants
    // ----------------------------------------------------
    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";

    // ----------------------------------------------------
    // Firestore
    // ----------------------------------------------------
    private FirebaseFirestore db;
    private String clientId;
    private DocumentReference fillFourRef;

    // ----------------------------------------------------
    // UI Elements
    // ----------------------------------------------------
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

    // ----------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_fill_four, container, false);

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

        initializeUIElements(root);
        setupSpinner();

        if (isRealClientId()) {
            loadExistingData();
        }

        return root;
    }

    // ----------------------------------------------------
    // Helpers
    // ----------------------------------------------------
    private boolean isRealClientId() {
        return clientId != null
                && !clientId.isEmpty()
                && !clientId.startsWith("temp");
    }

    // ----------------------------------------------------
    // UI Initialization
    // ----------------------------------------------------
    private void initializeUIElements(View root) {

        selectEmply = root.findViewById(R.id.fill_four_selectEmply);

        checkbox1HP = root.findViewById(R.id.fill_four_checkbox1HP);
        checkbox10HP = root.findViewById(R.id.fill_four_checkbox10HP);
        checkbox30HP = root.findViewById(R.id.fill_four_checkbox30HP);

        OnDisplay_text = root.findViewById(R.id.fill_four_OnDisplay_text);
        OnClamp_text = root.findViewById(R.id.fill_four_OnClamp_text);

        checkboxAMP_U = root.findViewById(R.id.fill_four_checkboxAMP_U);
        checkboxAMP_V = root.findViewById(R.id.fill_four_checkboxAMP_V);
        checkboxAMP_W = root.findViewById(R.id.fill_four_checkboxAMP_W);

        DC_DISP_text = root.findViewById(R.id.fill_four_DC_DISP_text);
        DC_MET_text = root.findViewById(R.id.fill_four_DC_MET_text);

        OUTPUT_DISP_text = root.findViewById(R.id.fill_four_OUTPUT_DISP_text);
        OUTPUT_MET_text = root.findViewById(R.id.fill_four_OUTPUT_MET_text);

        enterRH_text = root.findViewById(R.id.fill_four_enterRH_text);
        enterReplayOP_text = root.findViewById(R.id.fill_four_enterReplayOP_text);
        enterFANOpr_text = root.findViewById(R.id.fill_four_enterFANOpr_text);

        enterBODYCondition_text = root.findViewById(R.id.fill_four_enterBODYCondition_text);
        enterIOcheck_text = root.findViewById(R.id.fill_four_enterIOcheck_text);

        enterClean_Text = root.findViewById(R.id.fill_four_enterClean_Text);
        enterPramCopy_Text = root.findViewById(R.id.fill_four_enterPramCopy_Text);
    }

    // ----------------------------------------------------
    // Spinner Setup
    // ----------------------------------------------------
    private void setupSpinner() {

        String[] employees = {
                "Select Employee",
                "Arshad",
                "Samir",
                "Akhil",
                "Vishal"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item,
                employees
        );

        adapter.setDropDownViewResource(
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item
        );

        selectEmply.setAdapter(adapter);

        selectEmply.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int i, long id) {
                        if (i > 0 && isAdded()) {
                            showToastSafe("Selected: " + parent.getItemAtPosition(i));
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    // ----------------------------------------------------
    // Load Existing Data
    // ----------------------------------------------------
    private void loadExistingData() {
        if (fillFourRef == null) return;

        LoadingDialog.getInstance().show(requireContext());

        fillFourRef.get().addOnSuccessListener(doc -> {

            LoadingDialog.getInstance().hide();

            if (!isAdded() || !doc.exists()) return;

            // Spinner
            String emp = doc.getString("select_emp");
            if (emp != null) {
                selectEmply.setSelection(getSpinnerIndex(selectEmply, emp));
            }

            // Checkboxes
            checkbox1HP.setChecked(getBoolean(doc, "checkbox_1HP"));
            checkbox10HP.setChecked(getBoolean(doc, "checkbox_10HP"));
            checkbox30HP.setChecked(getBoolean(doc, "checkbox_30HP"));

            checkboxAMP_U.setChecked(getBoolean(doc, "checkbox_u"));
            checkboxAMP_V.setChecked(getBoolean(doc, "checkbox_v"));
            checkboxAMP_W.setChecked(getBoolean(doc, "checkbox_w"));

            // EditTexts
            OnDisplay_text.setText(doc.getString("On_Display"));
            OnClamp_text.setText(doc.getString("On_Clamp"));

            DC_DISP_text.setText(doc.getString("dc_dsp"));
            DC_MET_text.setText(doc.getString("dc_met"));

            OUTPUT_DISP_text.setText(doc.getString("output_dsp"));
            OUTPUT_MET_text.setText(doc.getString("output_met"));

            enterRH_text.setText(doc.getString("enter_RH"));
            enterReplayOP_text.setText(doc.getString("enterReplayOP"));
            enterFANOpr_text.setText(doc.getString("enter_FANOpr"));

            enterBODYCondition_text.setText(doc.getString("enter_BODY_Condition"));
            enterIOcheck_text.setText(doc.getString("enter_io_check"));

            enterClean_Text.setText(doc.getString("enterClean"));
            enterPramCopy_Text.setText(doc.getString("enterPramCopy"));

            showToastSafe("Data loaded 4");

        }).addOnFailureListener(e -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Load failed");
        });
    }

    private boolean getBoolean(DocumentSnapshot doc, String key) {
        Boolean b = doc.getBoolean(key);
        return b != null && b;
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    // ----------------------------------------------------
    // Save to Firestore
    // ----------------------------------------------------
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

        Map<String, Object> data = buildFirestoreData();

        WriteBatch batch = db.batch();
        batch.set(pageRef, data);

        DocumentReference rootRef = db.collection(COLLECTION_NAME).document(clientId);
        batch.update(rootRef, "progress", 100, "lastUpdated", System.currentTimeMillis());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    LoadingDialog.getInstance().hide();
                    showToastSafe("All steps completed!");
                    callback.onSaveComplete(true);
                })
                .addOnFailureListener(e -> {
                    LoadingDialog.getInstance().hide();
                    showToastSafe("Save failed");
                    callback.onSaveComplete(false);
                });
    }

    private Map<String, Object> buildFirestoreData() {

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

        return data;
    }

    // ----------------------------------------------------
    // Safe Helpers
    // ----------------------------------------------------
    private String getText(EditText et) {
        return et.getText().toString().trim();
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

    private void showToastSafe(String msg) {
        if (!isAdded() || getContext() == null) return;

        if (activeToast != null) activeToast.cancel();

        activeToast = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT);
        activeToast.show();
    }
}
