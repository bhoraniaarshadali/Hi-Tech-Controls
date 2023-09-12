package com.example.hi_tech_controls.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;

public class fill_two_fragment extends Fragment {

    private RadioGroup radioGroup;
    private EditText localEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_two, container, false);

        // Initialize UI elements
        radioGroup = rootView.findViewById(R.id.radioGroupCMD);
        localEditText = rootView.findViewById(R.id.localEditText);
        Spinner spinner = rootView.findViewById(R.id.spinner);
        RadioButton radioButtonLocal = rootView.findViewById(R.id.radioButtonLocal);
        RadioButton radioButtonRemote = rootView.findViewById(R.id.radioButtonRemote);
        RadioButton radioButtonComm = rootView.findViewById(R.id.radioButtonComm);


        // Set up radio button listener
        // Set up radio button listener
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Check which radio button is selected
                if (checkedId == R.id.radioButtonLocal) {
                    // If "Local" is selected, show the EditText field
                    localEditText.setVisibility(View.VISIBLE);
                } else {
                    // If other options are selected, hide the EditText field
                    localEditText.setVisibility(View.GONE);
                }
            }
        });


        // Define an array of employee names
        String[] employees = {"Select employee name", "employee 1", "employee 2", "employee 3", "employee 4"};

        // Create an ArrayAdapter to populate the Spinner with employee names
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, employees);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the adapter for the Spinner
        spinner.setAdapter(adapter);

        // Set the text color of the selected item (excluding the default item)
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (selectedItemView instanceof TextView) {
                    if (position == 0) {
                        ((TextView) selectedItemView).setTextColor(getResources().getColor(R.color.grey)); // Change to your desired color
                    } else {
                        ((TextView) selectedItemView).setTextColor(getResources().getColor(R.color.blue)); // Change to your desired color
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

        return rootView;
    }
}