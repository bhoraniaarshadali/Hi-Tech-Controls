// ============================================
// fill_three_fragment.java (Optimized & Safe Version)
// ============================================
package com.example.hi_tech_controls.fragments;

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

import com.example.hi_tech_controls.AddDetailsActivity;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.LoadingDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class fill_three_fragment extends Fragment {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";

    private FirebaseFirestore db;
    private String clientId;
    private DocumentReference fillThreeRef;

    // UI Elements
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_three, container, false);

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

        initializeUIElements(rootView);
        setUpSpinner();
        setupStepperButtons();

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
        selectEmply = rootView.findViewById(R.id.fill_three_selectEmply);
        checkboxCapacitor = rootView.findViewById(R.id.fill_three_checkboxCapasitor);
        checkboxDisplay = rootView.findViewById(R.id.fill_three_checkboxDisplay);
        checkboxFAN = rootView.findViewById(R.id.fill_three_checkboxFAN);
        checkboxCC = rootView.findViewById(R.id.fill_three_checkboxCC);
        firstRemarks = rootView.findViewById(R.id.fill_three_firstRemarks);

        Repair_checkboxOne = rootView.findViewById(R.id.fill_three_Repair_checkboxOne);
        Repair_checkboxTwo = rootView.findViewById(R.id.fill_three_Repair_checkboxTwo);
        Repair_checkboxThree = rootView.findViewById(R.id.fill_three_Repair_checkboxThree);
        Repair_checkboxFour = rootView.findViewById(R.id.fill_three_Repair_checkboxFour);
        Repair_checkboxFive = rootView.findViewById(R.id.fill_three_Repair_checkboxFive);
        Repair_checkboxSix = rootView.findViewById(R.id.fill_three_Repair_checkboxSix);

        Replace_checkboxOne = rootView.findViewById(R.id.fill_three_Replace_checkboxOne);
        Replace_checkboxTwo = rootView.findViewById(R.id.fill_three_Replace_checkboxTwo);
        Replace_checkboxThree = rootView.findViewById(R.id.fill_three_Replace_checkboxThree);
        Replace_checkboxFour = rootView.findViewById(R.id.fill_three_Replace_checkboxFour);
        Replace_checkboxFive = rootView.findViewById(R.id.fill_three_Replace_checkboxFive);
        Replace_checkboxSix = rootView.findViewById(R.id.fill_three_Replace_checkboxSix);
        Replace_checkboxSeven = rootView.findViewById(R.id.fill_three_Replace_checkboxSeven);
        Replace_checkboxEight = rootView.findViewById(R.id.fill_three_Replace_checkboxEight);
        Replace_checkboxNine = rootView.findViewById(R.id.fill_three_Replace_checkboxNine);

        checkboxTRIAL1 = rootView.findViewById(R.id.fill_three_checkboxTRIAL1);
        checkboxTRIAL2 = rootView.findViewById(R.id.fill_three_checkboxTRIAL2);
        buttonPlus = rootView.findViewById(R.id.buttonPlus);
        buttonMinus = rootView.findViewById(R.id.buttonMinus);
        textDays = rootView.findViewById(R.id.textDays);
    }

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

    // LOAD DATA ONLY FOR REAL CLIENTS
    private void loadExistingData() {
        if (fillThreeRef == null) return;

        LoadingDialog.getInstance().show(requireContext());

        fillThreeRef.get().addOnSuccessListener(document -> {
            LoadingDialog.getInstance().hide();
            if (document.exists()) {
                String emp = document.getString("select_emp");
                if (emp != null && selectEmply.getAdapter() != null) {
                    selectEmply.setSelection(getSpinnerIndex(selectEmply, emp));
                }

                checkboxCapacitor.setChecked(getBoolean(document, "checkboxCapacitor"));
                checkboxDisplay.setChecked(getBoolean(document, "checkboxDisplay"));
                checkboxFAN.setChecked(getBoolean(document, "checkboxFAN"));
                checkboxCC.setChecked(getBoolean(document, "checkboxCC"));
                firstRemarks.setText(document.getString("enter_first_remarks"));

                Repair_checkboxOne.setChecked(getBoolean(document, "repair_checkboxOne"));
                Repair_checkboxTwo.setChecked(getBoolean(document, "repair_checkboxTwo"));
                Repair_checkboxThree.setChecked(getBoolean(document, "repair_checkboxThree"));
                Repair_checkboxFour.setChecked(getBoolean(document, "repair_checkboxFour"));
                Repair_checkboxFive.setChecked(getBoolean(document, "repair_checkboxFive"));
                Repair_checkboxSix.setChecked(getBoolean(document, "repair_checkboxSix"));

                Replace_checkboxOne.setChecked(getBoolean(document, "replace_checkboxOne"));
                Replace_checkboxTwo.setChecked(getBoolean(document, "replace_checkboxTwo"));
                Replace_checkboxThree.setChecked(getBoolean(document, "replace_checkboxThree"));
                Replace_checkboxFour.setChecked(getBoolean(document, "replace_checkboxFour"));
                Replace_checkboxFive.setChecked(getBoolean(document, "replace_checkboxFive"));
                Replace_checkboxSix.setChecked(getBoolean(document, "replace_checkboxSix"));
                Replace_checkboxSeven.setChecked(getBoolean(document, "replace_checkboxSeven"));
                Replace_checkboxEight.setChecked(getBoolean(document, "replace_checkboxEight"));
                Replace_checkboxNine.setChecked(getBoolean(document, "replace_checkboxNine"));

                checkboxTRIAL1.setChecked(getBoolean(document, "checkboxTrial1"));
                checkboxTRIAL2.setChecked(getBoolean(document, "checkboxTrial2"));

                Long savedDays = document.getLong("number_picker_value");
                daysCount = savedDays != null ? savedDays.intValue() : 1;
                textDays.setText(String.valueOf(daysCount));

                showToastSafe("Data loaded 3");
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

    /**
     * Safely read a checkbox value from Firestore.
     * Handles Boolean, String ("true"/"false"), Long/Integer (1/0) and null.
     */
    private boolean getBoolean(DocumentSnapshot doc, String field) {
        Object obj = doc.get(field);
        if (obj == null) return false;

        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof String) {
            return "true".equalsIgnoreCase((String) obj);
        }
        if (obj instanceof Long) {
            return ((Long) obj) == 1L;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj) == 1;
        }
        // Unknown type → treat as false
        return false;
    }

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

        Map<String, Object> data = new HashMap<>();
        data.put("select_emp", selectEmply.getSelectedItem().toString());
        data.put("checkboxCapacitor", checkboxCapacitor.isChecked());
        data.put("checkboxDisplay", checkboxDisplay.isChecked());
        data.put("checkboxFAN", checkboxFAN.isChecked());
        data.put("checkboxCC", checkboxCC.isChecked());
        data.put("enter_first_remarks", firstRemarks.getText().toString().trim());

        data.put("repair_checkboxOne", Repair_checkboxOne.isChecked());
        data.put("repair_checkboxTwo", Repair_checkboxTwo.isChecked());
        data.put("repair_checkboxThree", Repair_checkboxThree.isChecked());
        data.put("repair_checkboxFour", Repair_checkboxFour.isChecked());
        data.put("repair_checkboxFive", Repair_checkboxFive.isChecked());
        data.put("repair_checkboxSix", Repair_checkboxSix.isChecked());

        data.put("replace_checkboxOne", Replace_checkboxOne.isChecked());
        data.put("replace_checkboxTwo", Replace_checkboxTwo.isChecked());
        data.put("replace_checkboxThree", Replace_checkboxThree.isChecked());
        data.put("replace_checkboxFour", Replace_checkboxFour.isChecked());
        data.put("replace_checkboxFive", Replace_checkboxFive.isChecked());
        data.put("replace_checkboxSix", Replace_checkboxSix.isChecked());
        data.put("replace_checkboxSeven", Replace_checkboxSeven.isChecked());
        data.put("replace_checkboxEight", Replace_checkboxEight.isChecked());
        data.put("replace_checkboxNine", Replace_checkboxNine.isChecked());

        data.put("checkboxTrial1", checkboxTRIAL1.isChecked());
        data.put("checkboxTrial2", checkboxTRIAL2.isChecked());
        data.put("number_picker_value", daysCount);
        data.put("completed", true);
        data.put("timestamp", System.currentTimeMillis());

        // ✅ Atomic batch write
        WriteBatch batch = db.batch();
        batch.set(pageRef, data);
        DocumentReference rootRef = db.collection(COLLECTION_NAME).document(clientId);
        batch.update(rootRef, "progress", 75, "lastUpdated", System.currentTimeMillis());

        batch.commit().addOnSuccessListener(aVoid -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Step 3 saved!");
            callback.onSaveComplete(true);
        }).addOnFailureListener(e -> {
            LoadingDialog.getInstance().hide();
            showToastSafe("Save failed");
            callback.onSaveComplete(false);
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

        // Cancel any currently showing toast
        if (activeToast != null) {
            activeToast.cancel();
        }
        // Create a new one and show
        activeToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT);
        activeToast.show();
    }
}