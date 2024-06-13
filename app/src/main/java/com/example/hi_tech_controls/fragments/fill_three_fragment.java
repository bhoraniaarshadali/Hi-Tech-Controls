package com.example.hi_tech_controls.fragments;

import static com.example.hi_tech_controls.fragments.fill_one_fragment.COLLECTION_NAME;
import static com.example.hi_tech_controls.fragments.fill_one_fragment.DOCUMENT_FILL_THREE;
import static com.example.hi_tech_controls.fragments.fill_one_fragment.DOCUMENT_LAST_ID;
import static com.example.hi_tech_controls.fragments.fill_one_fragment.currentId1;
import static com.example.hi_tech_controls.fragments.fill_one_fragment.db;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;
import com.google.firebase.firestore.CollectionReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class fill_three_fragment extends Fragment {
    // Static Map to store form data
    public static Map<String, String> fillThreeData = new HashMap<>();
    private static final String PREF_KEY_NUMBER_PICKER_VALUE = "number_picker_value";
    static NumberPicker np;
    Button npbutton;
    private static Spinner selectEmply;
    private static CheckBox checkboxCapacitor;
    private static CheckBox checkboxDisplay;
    private static CheckBox checkboxFAN;
    private static CheckBox checkboxCC;
    private static EditText firstRemarks;
    private static CheckBox Repair_checkboxOne;
    private static CheckBox Repair_checkboxTwo;
    private static CheckBox Repair_checkboxThree;
    private static CheckBox Repair_checkboxFour;
    private static CheckBox Repair_checkboxFive;
    private static CheckBox Repair_checkboxSix;
    private static CheckBox Replace_checkboxOne;
    private static CheckBox Replace_checkboxTwo;
    private static CheckBox Replace_checkboxThree;
    private static CheckBox Replace_checkboxFour;
    private static CheckBox Replace_checkboxFive;
    private static CheckBox Replace_checkboxSix;
    private static CheckBox Replace_checkboxSeven;
    private static CheckBox Replace_checkboxEight;
    private static CheckBox Replace_checkboxNine;
    private static CheckBox checkboxTRIAL1;
    private static CheckBox checkboxTRIAL2;
    private SharedPrefHelper sharedPref;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_three, container, false);

        fill_two_fragment.insertDataToFirestore_FillTwo(requireContext());

        setUpSpinner(rootView);
        setupNumberPicker(rootView);
        setupShowNumberButton(rootView);
        return rootView;
    }

    public static void insertDataToFirestore_FillThree(Context context) {
        fillThreeData.put("select_emp", selectEmply.getSelectedItem().toString());

        fillThreeData.put("checkboxCapacitor", String.valueOf(checkboxCapacitor.isChecked()));
        fillThreeData.put("checkboxDisplay", String.valueOf(checkboxDisplay.isChecked()));
        fillThreeData.put("checkboxFAN", String.valueOf(checkboxFAN.isChecked()));
        fillThreeData.put("checkboxCC", String.valueOf(checkboxCC.isChecked()));

        fillThreeData.put("enter_first_remarks", firstRemarks.getText().toString());

        fillThreeData.put("repair_checkboxOne", String.valueOf(Repair_checkboxOne.isChecked()));
        fillThreeData.put("repair_checkboxTwo", String.valueOf(Repair_checkboxTwo.isChecked()));
        fillThreeData.put("repair_checkboxThree", String.valueOf(Repair_checkboxThree.isChecked()));
        fillThreeData.put("repair_checkboxFour", String.valueOf(Repair_checkboxFour.isChecked()));
        fillThreeData.put("repair_checkboxFive", String.valueOf(Repair_checkboxFive.isChecked()));
        fillThreeData.put("repair_checkboxSix", String.valueOf(Repair_checkboxSix.isChecked()));

        fillThreeData.put("replace_checkboxOne", String.valueOf(Replace_checkboxOne.isChecked()));
        fillThreeData.put("replace_checkboxTwo", String.valueOf(Replace_checkboxTwo.isChecked()));
        fillThreeData.put("replace_checkboxThree", String.valueOf(Replace_checkboxThree.isChecked()));
        fillThreeData.put("replace_checkboxFour", String.valueOf(Replace_checkboxFour.isChecked()));
        fillThreeData.put("replace_checkboxFive", String.valueOf(Replace_checkboxFive.isChecked()));
        fillThreeData.put("replace_checkboxSix", String.valueOf(Replace_checkboxSix.isChecked()));
        fillThreeData.put("replace_checkboxSeven", String.valueOf(Replace_checkboxSeven.isChecked()));
        fillThreeData.put("replace_checkboxEight", String.valueOf(Replace_checkboxEight.isChecked()));
        fillThreeData.put("replace_checkboxNine", String.valueOf(Replace_checkboxNine.isChecked()));

        fillThreeData.put("checkboxTrial1", String.valueOf(checkboxTRIAL1.isChecked()));
        fillThreeData.put("checkboxTrial2", String.valueOf(checkboxTRIAL2.isChecked()));

        db.collection(COLLECTION_NAME).document(String.valueOf(currentId1)).set(fillThreeData)
                .addOnSuccessListener(aVoid -> {
                    CollectionReference subCollectionRef = db.collection(COLLECTION_NAME)
                            .document(String.valueOf(currentId1))
                            .collection("pages");

                    subCollectionRef.document(DOCUMENT_FILL_THREE).set(fillThreeData)
                            .addOnSuccessListener(aVoid1 -> {
                                Map<String, Object> idUpdate = new HashMap<>();
                                idUpdate.put("lastId", currentId1);
                                db.collection(COLLECTION_NAME).document(DOCUMENT_LAST_ID).set(idUpdate)
                                        .addOnSuccessListener(aVoid2 -> {
                                            Toast.makeText(context, "Data saved successfully!", Toast.LENGTH_SHORT).show();
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Helper method to get the position of the selected employee in the Spinner
    private int getPositionOfEmployee(String selectedEmployee) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) selectEmply.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (Objects.equals(adapter.getItem(i), selectedEmployee)) {
                return i;
            }
        }
        return -1; // Employee not found in the Spinner
    }

    private void setUpSpinner(View rootView) {
        Spinner spinner = rootView.findViewById(R.id.fill_three_selectEmply);
        String[] employees = {"Select employee name", "employee 1", "employee 2", "employee 3", "employee 4"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, employees);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                handleSpinnerItemSelected(selectedItemView, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    private void handleSpinnerItemSelected(View selectedItemView, int position) {
        if (selectedItemView instanceof TextView) {
            if (position == 0) {
                ((TextView) selectedItemView).setTextColor(getResources().getColor(R.color.grey));
            } else {
                ((TextView) selectedItemView).setTextColor(getResources().getColor(R.color.blue));
            }
        }
    }

    private void setupNumberPicker(View rootView) {
        np = rootView.findViewById(R.id.npId);
        np.setMinValue(1); // Set the minimum value for the NumberPicker
        np.setMaxValue(100); // Set the maximum value for the NumberPicker

        // Add a listener to save the NumberPicker value when it changes
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                // Save the new NumberPicker value to SharedPreferences
                sharedPref.saveInt(PREF_KEY_NUMBER_PICKER_VALUE, newVal);
            }
        });
    }

    private void setupShowNumberButton(View rootView) {
        npbutton = rootView.findViewById(R.id.npButton);
        npbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedNumber = np.getValue();
                showToast("Duration: " + selectedNumber + "/days");
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static void clearFields_FillThree(Context context) {
        // Clear Spinner selection
        selectEmply.setSelection(0);

        // Clear CheckBox selections
        checkboxCapacitor.setChecked(false);
        checkboxDisplay.setChecked(false);
        checkboxFAN.setChecked(false);
        checkboxCC.setChecked(false);
        Repair_checkboxOne.setChecked(false);
        Repair_checkboxTwo.setChecked(false);
        Repair_checkboxThree.setChecked(false);
        Repair_checkboxFour.setChecked(false);
        Repair_checkboxFive.setChecked(false);
        Repair_checkboxSix.setChecked(false);
        Replace_checkboxOne.setChecked(false);
        Replace_checkboxTwo.setChecked(false);
        Replace_checkboxThree.setChecked(false);
        Replace_checkboxFour.setChecked(false);
        Replace_checkboxFive.setChecked(false);
        Replace_checkboxSix.setChecked(false);
        Replace_checkboxSeven.setChecked(false);
        Replace_checkboxEight.setChecked(false);
        Replace_checkboxNine.setChecked(false);
        checkboxTRIAL1.setChecked(false);
        checkboxTRIAL2.setChecked(false);

        // Clear EditText field
        firstRemarks.setText("");

        // Reset NumberPicker value to 1
        np.setValue(1);

        SharedPrefHelper.clearAll(); // Assuming there's a method in SharedPrefHelper to clear all SharedPreferences
        //Toast.makeText(context, "SharedPreferences cleared successfully!", Toast.LENGTH_SHORT).show();
    }
    private void saveValuesToSharedPreferences() {

        sharedPref.saveString("select_emp", selectEmply.getSelectedItem().toString());

        sharedPref.saveBoolean("checkboxCapacitor", checkboxCapacitor.isChecked());
        sharedPref.saveBoolean("checkboxDisplay", checkboxDisplay.isChecked());
        sharedPref.saveBoolean("checkboxFAN", checkboxFAN.isChecked());
        sharedPref.saveBoolean("checkboxCC", checkboxCC.isChecked());

        sharedPref.saveString("enter_first_remarks", firstRemarks.getText().toString());

        sharedPref.saveBoolean("repair_checkboxOne", Repair_checkboxOne.isChecked());
        sharedPref.saveBoolean("repair_checkboxTwo", Repair_checkboxTwo.isChecked());
        sharedPref.saveBoolean("repair_checkboxThree", Repair_checkboxThree.isChecked());
        sharedPref.saveBoolean("repair_checkboxFour", Repair_checkboxFour.isChecked());
        sharedPref.saveBoolean("repair_checkboxFive", Repair_checkboxFive.isChecked());
        sharedPref.saveBoolean("repair_checkboxSix", Repair_checkboxSix.isChecked());

        sharedPref.saveBoolean("replace_checkboxOne", Replace_checkboxOne.isChecked());
        sharedPref.saveBoolean("replace_checkboxTwo", Replace_checkboxTwo.isChecked());
        sharedPref.saveBoolean("replace_checkboxThree", Replace_checkboxThree.isChecked());
        sharedPref.saveBoolean("replace_checkboxFour", Replace_checkboxFour.isChecked());
        sharedPref.saveBoolean("replace_checkboxFive", Replace_checkboxFive.isChecked());
        sharedPref.saveBoolean("replace_checkboxSix", Replace_checkboxSix.isChecked());
        sharedPref.saveBoolean("replace_checkboxSeven", Replace_checkboxSeven.isChecked());
        sharedPref.saveBoolean("replace_checkboxEight", Replace_checkboxEight.isChecked());
        sharedPref.saveBoolean("replace_checkboxNine", Replace_checkboxNine.isChecked());

        sharedPref.saveBoolean("checkboxTrial1", checkboxTRIAL1.isChecked());
        sharedPref.saveBoolean("checkboxTrial2", checkboxTRIAL2.isChecked());
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //fill_two_fragment.insertDataToFirestoreFill_two(requireContext());

        // Initialize your View elements using findViewById on the view
        selectEmply = view.findViewById(R.id.fill_three_selectEmply);

        checkboxCapacitor = view.findViewById(R.id.fill_three_checkboxCapasitor);
        checkboxDisplay = view.findViewById(R.id.fill_three_checkboxDisplay);
        checkboxFAN = view.findViewById(R.id.fill_three_checkboxFAN);
        checkboxCC = view.findViewById(R.id.fill_three_checkboxCC);

        firstRemarks = view.findViewById(R.id.fill_three_firstRemarks);

        Repair_checkboxOne = view.findViewById(R.id.fill_three_Repair_checkboxOne);
        Repair_checkboxTwo = view.findViewById(R.id.fill_three_Repair_checkboxTwo);
        Repair_checkboxThree = view.findViewById(R.id.fill_three_Repair_checkboxThree);
        Repair_checkboxFour = view.findViewById(R.id.fill_three_Repair_checkboxFour);
        Repair_checkboxFive = view.findViewById(R.id.fill_three_Repair_checkboxFive);
        Repair_checkboxSix = view.findViewById(R.id.fill_three_Repair_checkboxSix);

        Replace_checkboxOne = view.findViewById(R.id.fill_three_Replace_checkboxOne);
        Replace_checkboxTwo = view.findViewById(R.id.fill_three_Replace_checkboxTwo);
        Replace_checkboxThree = view.findViewById(R.id.fill_three_Replace_checkboxThree);
        Replace_checkboxFour = view.findViewById(R.id.fill_three_Replace_checkboxFour);
        Replace_checkboxFive = view.findViewById(R.id.fill_three_Replace_checkboxFive);
        Replace_checkboxSix = view.findViewById(R.id.fill_three_Replace_checkboxSix);
        Replace_checkboxSeven = view.findViewById(R.id.fill_three_Replace_checkboxSeven);
        Replace_checkboxEight = view.findViewById(R.id.fill_three_Replace_checkboxEight);
        Replace_checkboxNine = view.findViewById(R.id.fill_three_Replace_checkboxNine);

        checkboxTRIAL1 = view.findViewById(R.id.fill_three_checkboxTRIAL1);
        checkboxTRIAL2 = view.findViewById(R.id.fill_three_checkboxTRIAL2);

        // Load saved values and set them to the UI elements
        sharedPref = new SharedPrefHelper(requireContext());

        String selectedEmployee = sharedPref.getString("select_emp", "");
        if (!selectedEmployee.isEmpty()) {
            // Find the position of the selectedEmployee in the employees array
            int position = getPositionOfEmployee(selectedEmployee);
            if (position >= 0) {
                selectEmply.setSelection(position);
            }
        }

        checkboxCapacitor.setChecked(sharedPref.getBoolean("checkboxCapacitor", false));
        checkboxDisplay.setChecked(sharedPref.getBoolean("checkboxDisplay", false));
        checkboxFAN.setChecked(sharedPref.getBoolean("checkboxFAN", false));
        checkboxCC.setChecked(sharedPref.getBoolean("checkboxCC", false));

        firstRemarks.setText(sharedPref.getString("enter_first_remarks", ""));

        Repair_checkboxOne.setChecked(sharedPref.getBoolean("repair_checkboxOne", false));
        Repair_checkboxTwo.setChecked(sharedPref.getBoolean("repair_checkboxTwo", false));
        Repair_checkboxThree.setChecked(sharedPref.getBoolean("repair_checkboxThree", false));
        Repair_checkboxFour.setChecked(sharedPref.getBoolean("repair_checkboxFour", false));
        Repair_checkboxFive.setChecked(sharedPref.getBoolean("repair_checkboxFive", false));
        Repair_checkboxSix.setChecked(sharedPref.getBoolean("repair_checkboxSix", false));

        Replace_checkboxOne.setChecked(sharedPref.getBoolean("replace_checkboxOne", false));
        Replace_checkboxTwo.setChecked(sharedPref.getBoolean("replace_checkboxTwo", false));
        Replace_checkboxThree.setChecked(sharedPref.getBoolean("replace_checkboxThree", false));
        Replace_checkboxFour.setChecked(sharedPref.getBoolean("replace_checkboxFour", false));
        Replace_checkboxFive.setChecked(sharedPref.getBoolean("replace_checkboxFive", false));
        Replace_checkboxSix.setChecked(sharedPref.getBoolean("replace_checkboxSix", false));
        Replace_checkboxSeven.setChecked(sharedPref.getBoolean("replace_checkboxSeven", false));
        Replace_checkboxEight.setChecked(sharedPref.getBoolean("replace_checkboxEight", false));
        Replace_checkboxNine.setChecked(sharedPref.getBoolean("replace_checkboxNine", false));

        checkboxTRIAL1.setChecked(sharedPref.getBoolean("checkboxTrial1", false));
        checkboxTRIAL2.setChecked(sharedPref.getBoolean("checkboxTrial2", false));

        // Load the NumberPicker value from SharedPreferences and set it
        int savedNumberPickerValue = sharedPref.getInt(PREF_KEY_NUMBER_PICKER_VALUE, 1);
        np.setValue(savedNumberPickerValue);
    }

    public void onStop() {
        super.onStop();
        saveValuesToSharedPreferences();
    }

}