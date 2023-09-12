package com.example.hi_tech_controls.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

        initializeUIElements(rootView);
        setUpRadioGroupListener();
        setUpSpinner(rootView);

        return rootView;
    }

    private void initializeUIElements(View rootView) {
        radioGroup = rootView.findViewById(R.id.radioGroupCMD);
        localEditText = rootView.findViewById(R.id.localEditText);
    }

    private void setUpRadioGroupListener() {
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                handleRadioButtonChange(checkedId);
            }
        });
    }

    private void handleRadioButtonChange(int checkedId) {
        if (checkedId == R.id.radioButtonLocal) {
            localEditText.setVisibility(View.VISIBLE);
        } else {
            localEditText.setVisibility(View.GONE);
        }
    }

    private void setUpSpinner(View rootView) {
        Spinner spinner = rootView.findViewById(R.id.spinner);
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
}
