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
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;

public class fill_three_fragment extends Fragment {

    private Spinner selectEmply;

    private CheckBox checkboxCapasitor, checkboxDisplay, checkboxFAN, checkboxCC;

    private EditText firstRemarks;

    private CheckBox Repair_checkboxOne, Repair_checkboxTwo, Repair_checkboxThree,
            Repair_checkboxFour, Repair_checkboxFive, Repair_checkboxSix;

    private CheckBox Replace_checkboxOne, Replace_checkboxTwo, Replace_checkboxThree,
            Replace_checkboxFour, Replace_checkboxFive, Replace_checkBoxSix, Replace_checkboxSeven,
            Replace_checkboxEight, Replace_checkboxNine;

    private CheckBox checkboxTRIAL1, checkboxTRIAL2;

    NumberPicker np;
    Button npbutton;

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

        checkboxCapasitor = view.findViewById(R.id.fill_three_checkboxCapasitor);
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
        Replace_checkBoxSix = view.findViewById(R.id.fill_three_Replace_checkboxSix);
        Replace_checkboxSeven = view.findViewById(R.id.fill_three_Replace_checkboxSeven);
        Replace_checkboxEight = view.findViewById(R.id.fill_three_Replace_checkboxEight);
        Replace_checkboxNine = view.findViewById(R.id.fill_three_Replace_checkboxNine);

        checkboxTRIAL1 = view.findViewById(R.id.fill_three_checkboxTRIAL1);
        checkboxTRIAL2 = view.findViewById(R.id.fill_three_checkboxTRIAL2);

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
}
