// ============================================
// fill_three_fragment.java
// ============================================
package com.example.hi_tech_controls.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.LoadingDialog;
import com.example.hi_tech_controls.ui.activity.AddDetailsActivity;
import com.example.hi_tech_controls.ui.activity.BaseActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.example.hi_tech_controls.helper.AdminManager;
import com.example.hi_tech_controls.helper.FirestoreUtils;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.firestore.ListenerRegistration;

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
    private ShimmerFrameLayout shimmerLayout;
    private View contentContainer;
    private Toast activeToast;
    private Map<String, Object> lastSavedData = new HashMap<>();

    private ListenerRegistration employeeListener;
    private String restoredEmpName = "";

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
        setupSpinnerRealtime();
        setupStepperButtons();

        if (isRealClientId()) loadExistingData();

        // Ensure focused field stays visible while typing
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).setupAutoScrollOnType(root);
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
        shimmerLayout = root.findViewById(R.id.shimmer_layout);
        contentContainer = root.findViewById(R.id.content_container);
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
    // Spinner (Realtime)
    // ----------------------------------------------------
    private void setupSpinnerRealtime() {
        employeeListener = AdminManager.listenEmployees(new AdminManager.EmployeeListCallback() {
            @Override
            public void onResult(List<String> employees) {
                if (!isAdded()) return;

                List<String> finalEmployees = new ArrayList<>();
                finalEmployees.add("Select Employee");
                finalEmployees.addAll(employees);

                if (getActivity() == null || !isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    Context context = getContext();
                    if (context == null) return;

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            context,
                            R.layout.spinner_item,
                            finalEmployees
                    ) {
                        @Override
                        public boolean isEnabled(int position) {
                            return position != 0;
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                            View view = super.getDropDownView(position, convertView, parent);
                            TextView tv = (TextView) view;
                            if (position == 0) {
                                tv.setTextColor(Color.parseColor("#AAAAAA"));
                            } else {
                                tv.setTextColor(Color.WHITE);
                            }
                            return view;
                        }
                    };

                    adapter.setDropDownViewResource(
                            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item
                    );

                    selectEmply.setAdapter(adapter);

                    if (!restoredEmpName.isEmpty()) {
                        int pos = adapter.getPosition(restoredEmpName);
                        if (pos >= 0) selectEmply.setSelection(pos);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    showToastSafe("Employee load failed: " + error);
                }
            }
        });

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

        showShimmer();
        fillThreeRef.get().addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    hideShimmer();
                    if (!doc.exists()) return;

                    // Spinner
                    String emp = FirestoreUtils.getStringSafe(doc, "select_emp");
                    if (!emp.isEmpty()) {
                        restoredEmpName = emp;
                        if (selectEmply.getAdapter() != null) {
                            selectEmply.setSelection(getSpinnerIndex(selectEmply, emp));
                        }
                    }

                    // Checkboxes & Text
                    checkboxCapacitor.setChecked(FirestoreUtils.getBooleanSafe(doc, "checkboxCapacitor"));
                    checkboxDisplay.setChecked(FirestoreUtils.getBooleanSafe(doc, "checkboxDisplay"));
                    checkboxFAN.setChecked(FirestoreUtils.getBooleanSafe(doc, "checkboxFAN"));
                    checkboxCC.setChecked(FirestoreUtils.getBooleanSafe(doc, "checkboxCC"));

                    firstRemarks.setText(FirestoreUtils.getStringSafe(doc, "enter_first_remarks"));

                    // Repair
                    Repair_checkboxOne.setChecked(FirestoreUtils.getBooleanSafe(doc, "repair_checkboxOne"));
                    Repair_checkboxTwo.setChecked(FirestoreUtils.getBooleanSafe(doc, "repair_checkboxTwo"));
                    Repair_checkboxThree.setChecked(FirestoreUtils.getBooleanSafe(doc, "repair_checkboxThree"));
                    Repair_checkboxFour.setChecked(FirestoreUtils.getBooleanSafe(doc, "repair_checkboxFour"));
                    Repair_checkboxFive.setChecked(FirestoreUtils.getBooleanSafe(doc, "repair_checkboxFive"));
                    Repair_checkboxSix.setChecked(FirestoreUtils.getBooleanSafe(doc, "repair_checkboxSix"));

                    // Replace
                    Replace_checkboxOne.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxOne"));
                    Replace_checkboxTwo.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxTwo"));
                    Replace_checkboxThree.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxThree"));
                    Replace_checkboxFour.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxFour"));
                    Replace_checkboxFive.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxFive"));
                    Replace_checkboxSix.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxSix"));
                    Replace_checkboxSeven.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxSeven"));
                    Replace_checkboxEight.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxEight"));
                    Replace_checkboxNine.setChecked(FirestoreUtils.getBooleanSafe(doc, "replace_checkboxNine"));

                    // Trials
                    checkboxTRIAL1.setChecked(FirestoreUtils.getBooleanSafe(doc, "checkboxTrial1"));
                    checkboxTRIAL2.setChecked(FirestoreUtils.getBooleanSafe(doc, "checkboxTrial2"));

                    // Days
                    textDays.setText(String.valueOf(daysCount));

                    lastSavedData = buildFirestoreData(); // Cache initial state
//                    showToastSafe("Data loaded 3");
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        hideShimmer();
                        showToastSafe("Load failed");
                    }
                });
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value))
                return i;
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
                .document("fill_three");

        Map<String, Object> data = buildFirestoreData();

        if (lastSavedData != null && !lastSavedData.isEmpty() && !isDataChanged(data)) {
            Log.d("fill_three", "No changes in fill_three, skipping save");
            callback.onSaveComplete(true);
            return;
        }

        Context context = getContext();
        if (isAdded() && context != null) {
            LoadingDialog.getInstance().show(context);
        }

        WriteBatch batch = db.batch();
        batch.set(pageRef, data);
        batch.update(
                db.collection(COLLECTION_NAME).document(clientId),
                "progress", 75,
                "lastUpdated", System.currentTimeMillis()
        );

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        LoadingDialog.getInstance().hide();
                        lastSavedData = new HashMap<>(data); // Update cache
                        showToastSafe("Step 3 saved!");
                    }
                    callback.onSaveComplete(true);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        LoadingDialog.getInstance().hide();
                        showToastSafe("Save failed");
                    }
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
                Log.d("fill_three", "Change detected in: " + key + " (old=" + oldVal + " new=" + newVal + ")");
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------
    // Lifecycle Cleanup
    // ----------------------------------------------------
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (employeeListener != null) {
            employeeListener.remove();
            employeeListener = null;
        }
        if (activeToast != null) {
            activeToast.cancel();
            activeToast = null;
        }
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
        }
        LoadingDialog.getInstance().dismiss();
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

    private void showToastSafe(String message) {
        Context context = getContext();
        if (!isAdded() || context == null) return;
        if (activeToast != null) activeToast.cancel();
        activeToast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        activeToast.show();
    }
}
