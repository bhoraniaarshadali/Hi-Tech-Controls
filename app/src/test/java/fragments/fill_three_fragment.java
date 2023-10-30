package fragments;

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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;

import java.util.Objects;

public class fill_three_fragment extends Fragment {

    private static final String PREF_KEY_NUMBER_PICKER_VALUE = "number_picker_value";
    NumberPicker np;
    Button npbutton;
    private Spinner selectEmply;
    private CheckBox checkboxCapacitor, checkboxDisplay, checkboxFAN, checkboxCC;
    private EditText firstRemarks;
    private CheckBox Repair_checkboxOne, Repair_checkboxTwo, Repair_checkboxThree,
            Repair_checkboxFour, Repair_checkboxFive, Repair_checkboxSix;
    private CheckBox Replace_checkboxOne, Replace_checkboxTwo, Replace_checkboxThree,
            Replace_checkboxFour, Replace_checkboxFive, Replace_checkboxSix, Replace_checkboxSeven,
            Replace_checkboxEight, Replace_checkboxNine;
    private CheckBox checkboxTRIAL1, checkboxTRIAL2;
    private SharedPrefHelper sharedPref;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_three, container, false);

        setUpSpinner(rootView);
        setupNumberPicker(rootView);
        setupShowNumberButton(rootView);
        return rootView;
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

    public void onStop() {
        super.onStop();
        saveValuesToSharedPreferences();
    }
}
