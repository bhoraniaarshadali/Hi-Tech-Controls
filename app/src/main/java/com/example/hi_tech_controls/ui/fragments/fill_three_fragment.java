// ============================================
// fill_three_fragment.java
// ============================================
package com.example.hi_tech_controls.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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

public class fill_three_fragment extends Fragment {

    // ----------------------------------------------------
    // Constants
    // ----------------------------------------------------
    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";

    // ----------------------------------------------------
    // Firestore / IDs
    // ----------------------------------------------------
    private FirebaseFirestore db;
    private String clientId;
    private DocumentReference fillThreeRef;

    // ----------------------------------------------------
    // UI Elements
    // ----------------------------------------------------
    private Spinner selectEmply;

    private CheckBox checkboxCapacitor, checkboxDisplay, checkboxFAN, checkboxCC;
    private EditText firstRemarks;

    private CheckBox Repair_checkboxOne, Repair_checkboxTwo, Repair_checkboxThree;
    private CheckBox Repair_checkboxFour, Repair_checkboxFive, Repair_checkboxSix;

    private CheckBox Replace_checkboxOne, Replace_checkboxTwo, Replace_checkboxThree;
    private CheckBox Replace_checkboxFour, Replace_checkboxFive, Replace_checkboxSix;
    private CheckBox Replace_checkboxSeven, Replace_checkboxEight, Replace_checkboxNine;

    private CheckBox checkboxTRIAL1, checkboxTRIAL2;

    private Button buttonPlus, buttonMinus;
    private TextView textDays;

    private int daysCount = 1;
    private Toast activeToast;

    // ----------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_fill_three, container, false);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            clientId = getArguments().getString("clientId");
        }

        if (isRealClientId()) {
            fillThreeRef = db.collection(COLLECTION_NAME)
                    .document(clientId)
                    .collection("pages")
                    .document("fill_three");
        }

        initializeUIElements(root);
        setupSpinner();
        setupStepperButtons();

        if (isRealClientId()) loadExistingData();

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

        selectEmply = root.findViewById(R.id.fill_three_selectEmply);

        checkboxCapacitor = root.findViewById(R.id.fill_three_checkboxCapasitor);
        checkboxDisplay = root.findViewById(R.id.fill_three_checkboxDisplay);
        checkboxFAN = root.findViewById(R.id.fill_three_checkboxFAN);
        checkboxCC = root.findViewById(R.id.fill_three_checkboxCC);

        firstRemarks = root.findViewById(R.id.fill_three_firstRemarks);

        Repair_checkboxOne = root.findViewById(R.id.fill_three_Repair_checkboxOne);
        Repair_checkboxTwo = root.findViewById(R.id.fill_three_Repair_checkboxTwo);
        Repair_checkboxThree = root.findViewById(R.id.fill_three_Repair_checkboxThree);
        Repair_checkboxFour = root.findViewById(R.id.fill_three_Repair_checkboxFour);
        Repair_checkboxFive = root.findViewById(R.id.fill_three_Repair_checkboxFive);
        Repair_checkboxSix = root.findViewById(R.id.fill_three_Repair_checkboxSix);

        Replace_checkboxOne = root.findViewById(R.id.fill_three_Replace_checkboxOne);
        Replace_checkboxTwo = root.findViewById(R.id.fill_three_Replace_checkboxTwo);
        Replace_checkboxThree = root.findViewById(R.id.fill_three_Replace_checkboxThree);
        Replace_checkboxFour = root.findViewById(R.id.fill_three_Replace_checkboxFour);
        Replace_checkboxFive = root.findViewById(R.id.fill_three_Replace_checkboxFive);
        Replace_checkboxSix = root.findViewById(R.id.fill_three_Replace_checkboxSix);
        Replace_checkboxSeven = root.findViewById(R.id.fill_three_Replace_checkboxSeven);
        Replace_checkboxEight = root.findViewById(R.id.fill_three_Replace_checkboxEight);
        Replace_checkboxNine = root.findViewById(R.id.fill_three_Replace_checkboxNine);

        checkboxTRIAL1 = root.findViewById(R.id.fill_three_checkboxTRIAL1);
        checkboxTRIAL2 = root.findViewById(R.id.fill_three_checkboxTRIAL2);

        buttonPlus = root.findViewById(R.id.buttonPlus);
        buttonMinus = root.findViewById(R.id.buttonMinus);
        textDays = root.findViewById(R.id.textDays);
    }

    // ----------------------------------------------------
    // Stepper Buttons
    // ----------------------------------------------------
    private void setupStepperButtons() {
        textDays.setText(String.valueOf(daysCount));

        buttonPlus.setOnClickListener(v -> {
            if (daysCount < 100) {
                daysCount++;
                textDays.setText(String.valueOf(daysCount));
            }
        });

        buttonMinus.setOnClickListener(v -> {
            if (daysCount > 1) {
                daysCount--;
                textDays.setText(String.valueOf(daysCount));
            }
        });
    }

    // ----------------------------------------------------
    // Spinner
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

        selectEmply.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long id) {
                if (i > 0 && isAdded()) {
                    showToastSafe("Selected: " + parent.getItemAtPosition(i).toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // ----------------------------------------------------
    // Load Existing Data
    // ----------------------------------------------------
    private void loadExistingData() {
        if (fillThreeRef == null) return;

        LoadingDialog.getInstance().show(requireContext());

        fillThreeRef.get().addOnSuccessListener(doc -> {
                    LoadingDialog.getInstance().hide();
                    if (!doc.exists()) return;

                    // Spinner
                    String emp = doc.getString("select_emp");
                    if (emp != null && selectEmply.getAdapter() != null) {
                        selectEmply.setSelection(getSpinnerIndex(selectEmply, emp));
                    }

                    // Checkboxes & Text
                    checkboxCapacitor.setChecked(getBoolean(doc, "checkboxCapacitor"));
                    checkboxDisplay.setChecked(getBoolean(doc, "checkboxDisplay"));
                    checkboxFAN.setChecked(getBoolean(doc, "checkboxFAN"));
                    checkboxCC.setChecked(getBoolean(doc, "checkboxCC"));

                    firstRemarks.setText(doc.getString("enter_first_remarks"));

                    // Repair
                    Repair_checkboxOne.setChecked(getBoolean(doc, "repair_checkboxOne"));
                    Repair_checkboxTwo.setChecked(getBoolean(doc, "repair_checkboxTwo"));
                    Repair_checkboxThree.setChecked(getBoolean(doc, "repair_checkboxThree"));
                    Repair_checkboxFour.setChecked(getBoolean(doc, "repair_checkboxFour"));
                    Repair_checkboxFive.setChecked(getBoolean(doc, "repair_checkboxFive"));
                    Repair_checkboxSix.setChecked(getBoolean(doc, "repair_checkboxSix"));

                    // Replace
                    Replace_checkboxOne.setChecked(getBoolean(doc, "replace_checkboxOne"));
                    Replace_checkboxTwo.setChecked(getBoolean(doc, "replace_checkboxTwo"));
                    Replace_checkboxThree.setChecked(getBoolean(doc, "replace_checkboxThree"));
                    Replace_checkboxFour.setChecked(getBoolean(doc, "replace_checkboxFour"));
                    Replace_checkboxFive.setChecked(getBoolean(doc, "replace_checkboxFive"));
                    Replace_checkboxSix.setChecked(getBoolean(doc, "replace_checkboxSix"));
                    Replace_checkboxSeven.setChecked(getBoolean(doc, "replace_checkboxSeven"));
                    Replace_checkboxEight.setChecked(getBoolean(doc, "replace_checkboxEight"));
                    Replace_checkboxNine.setChecked(getBoolean(doc, "replace_checkboxNine"));

                    // Trials
                    checkboxTRIAL1.setChecked(getBoolean(doc, "checkboxTrial1"));
                    checkboxTRIAL2.setChecked(getBoolean(doc, "checkboxTrial2"));

                    // Days
                    Long savedDays = doc.getLong("number_picker_value");
                    daysCount = savedDays != null ? savedDays.intValue() : 1;
                    textDays.setText(String.valueOf(daysCount));

                    showToastSafe("Data loaded 3");
                })
                .addOnFailureListener(e -> {
                    LoadingDialog.getInstance().hide();
                    showToastSafe("Load failed");
                });
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value))
                return i;
        }
        return 0;
    }

    private boolean getBoolean(DocumentSnapshot doc, String field) {
        Object obj = doc.get(field);
        if (obj == null) return false;

        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof String) return "true".equalsIgnoreCase((String) obj);
        if (obj instanceof Long) return ((Long) obj) == 1L;
        if (obj instanceof Integer) return ((Integer) obj) == 1;

        return false;
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
                .document("fill_three");

        LoadingDialog.getInstance().show(requireContext());

        Map<String, Object> data = buildFirestoreData();

        WriteBatch batch = db.batch();
        batch.set(pageRef, data);
        batch.update(
                db.collection(COLLECTION_NAME).document(clientId),
                "progress", 75,
                "lastUpdated", System.currentTimeMillis()
        );

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    LoadingDialog.getInstance().hide();
                    showToastSafe("Step 3 saved!");
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

        data.put("select_emp", selectEmply.getSelectedItem().toString());

        data.put("checkboxCapacitor", checkboxCapacitor.isChecked());
        data.put("checkboxDisplay", checkboxDisplay.isChecked());
        data.put("checkboxFAN", checkboxFAN.isChecked());
        data.put("checkboxCC", checkboxCC.isChecked());

        data.put("enter_first_remarks", firstRemarks.getText().toString().trim());

        // Repair
        data.put("repair_checkboxOne", Repair_checkboxOne.isChecked());
        data.put("repair_checkboxTwo", Repair_checkboxTwo.isChecked());
        data.put("repair_checkboxThree", Repair_checkboxThree.isChecked());
        data.put("repair_checkboxFour", Repair_checkboxFour.isChecked());
        data.put("repair_checkboxFive", Repair_checkboxFive.isChecked());
        data.put("repair_checkboxSix", Repair_checkboxSix.isChecked());

        // Replace
        data.put("replace_checkboxOne", Replace_checkboxOne.isChecked());
        data.put("replace_checkboxTwo", Replace_checkboxTwo.isChecked());
        data.put("replace_checkboxThree", Replace_checkboxThree.isChecked());
        data.put("replace_checkboxFour", Replace_checkboxFour.isChecked());
        data.put("replace_checkboxFive", Replace_checkboxFive.isChecked());
        data.put("replace_checkboxSix", Replace_checkboxSix.isChecked());
        data.put("replace_checkboxSeven", Replace_checkboxSeven.isChecked());
        data.put("replace_checkboxEight", Replace_checkboxEight.isChecked());
        data.put("replace_checkboxNine", Replace_checkboxNine.isChecked());

        // Trials
        data.put("checkboxTrial1", checkboxTRIAL1.isChecked());
        data.put("checkboxTrial2", checkboxTRIAL2.isChecked());

        data.put("number_picker_value", daysCount);
        data.put("completed", true);
        data.put("timestamp", System.currentTimeMillis());

        return data;
    }

    // ----------------------------------------------------
    // Lifecycle Cleanup
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

    private void showToastSafe(String message) {
        if (!isAdded() || getContext() == null) return;

        if (activeToast != null) activeToast.cancel();

        activeToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT);
        activeToast.show();
    }
}
