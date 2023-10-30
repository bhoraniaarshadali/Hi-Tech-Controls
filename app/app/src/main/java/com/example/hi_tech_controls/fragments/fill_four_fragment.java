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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;

import java.util.Objects;

public class fill_four_fragment extends Fragment {

    private Spinner selectEmply;
    private CheckBox checkbox1HP, checkbox10HP, checkbox30HP;
    private EditText OnDisplay_text, OnClamp_text;
    private CheckBox checkboxAMP_U, checkboxAMP_V, checkboxAMP_W;
    private EditText DC_DISP_text, DC_MET_text, OUTPUT_DISP_text, OUTPUT_MET_text;
    private EditText enterRH_text, enterReplayOP_text, enterFANOpr_text, enterBODYCondition_text, enterIOcheck_text, enterClean_Text, enterPramCopy_Text;
    private SharedPrefHelper sharedPref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_four, container, false);
        sharedPref = new SharedPrefHelper(requireContext());
        setUpSpinner(rootView);
        return rootView;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initializing   View elements using findViewById on the view
        selectEmply = view.findViewById(R.id.fill_four_selectEmply);
        checkbox1HP = view.findViewById(R.id.fill_four_checkbox1HP);
        checkbox10HP = view.findViewById(R.id.fill_four_checkbox10HP);
        checkbox30HP = view.findViewById(R.id.fill_four_checkbox30HP);
        OnDisplay_text = view.findViewById(R.id.fill_four_OnDisplay_text);
        OnClamp_text = view.findViewById(R.id.fill_four_OnClamp_text);
        checkboxAMP_U = view.findViewById(R.id.fill_four_checkboxAMP_U);
        checkboxAMP_V = view.findViewById(R.id.fill_four_checkboxAMP_V);
        checkboxAMP_W = view.findViewById(R.id.fill_four_checkboxAMP_W);
        DC_DISP_text = view.findViewById(R.id.fill_four_DC_DISP_text);
        DC_MET_text = view.findViewById(R.id.fill_four_DC_MET_text);
        OUTPUT_DISP_text = view.findViewById(R.id.fill_four_OUTPUT_DISP_text);
        OUTPUT_MET_text = view.findViewById(R.id.fill_four_OUTPUT_MET_text);
        enterRH_text = view.findViewById(R.id.fill_four_enterRH_text);
        enterReplayOP_text = view.findViewById(R.id.fill_four_enterReplayOP_text);
        enterFANOpr_text = view.findViewById(R.id.fill_four_enterFANOpr_text);
        enterBODYCondition_text = view.findViewById(R.id.fill_four_enterBODYCondition_text);
        enterIOcheck_text = view.findViewById(R.id.fill_four_enterIOcheck_text);
        enterClean_Text = view.findViewById(R.id.fill_four_enterClean_Text);
        enterPramCopy_Text = view.findViewById(R.id.fill_four_enterPramCopy_Text);


        // Load saved values and set them to the UI elements
        String selectedEmployee = sharedPref.getString("select_emp", "");
        if (!selectedEmployee.isEmpty()) {
            // Find the position of the selectedEmployee in the employees array
            int position = getPositionOfEmployee(selectedEmployee);
            if (position >= 0) {
                selectEmply.setSelection(position);
            }
        }
        // checkbox
        checkbox1HP.setChecked(sharedPref.getBoolean("checkbox_1HP", false));
        checkbox10HP.setChecked(sharedPref.getBoolean("checkbox_10HP", false));
        checkbox30HP.setChecked(sharedPref.getBoolean("checkbox_30HP", false));

        //edit text
        OnDisplay_text.setText(sharedPref.getString("On_Display", ""));
        OnClamp_text.setText(sharedPref.getString("On_Clamp", ""));

        //checkbox
        checkboxAMP_U.setChecked(sharedPref.getBoolean("checkbox_u", false));
        checkboxAMP_V.setChecked(sharedPref.getBoolean("checkbox_v", false));
        checkboxAMP_W.setChecked(sharedPref.getBoolean("checkbox_w", false));

        //edit text
        DC_DISP_text.setText(sharedPref.getString("dc_dsp", ""));
        DC_MET_text.setText(sharedPref.getString("dc_met", ""));
        OUTPUT_DISP_text.setText(sharedPref.getString("output_dsp", ""));
        OUTPUT_MET_text.setText(sharedPref.getString("output_met", ""));

        enterRH_text.setText(sharedPref.getString("enter_RH", ""));
        enterReplayOP_text.setText(sharedPref.getString("enterReplayOP", ""));
        enterFANOpr_text.setText(sharedPref.getString("enter_FANOpr", ""));
        enterBODYCondition_text.setText(sharedPref.getString("enter_BODY_Condition", ""));
        enterIOcheck_text.setText(sharedPref.getString("enter_io_check", ""));
        enterClean_Text.setText(sharedPref.getString("enterClean", ""));
        enterPramCopy_Text.setText(sharedPref.getString("enterPramCopy", ""));
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
        Spinner spinner = rootView.findViewById(R.id.fill_four_selectEmply);
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

    private void saveValuesToSharedPreferences() {
        sharedPref.saveString("select_emp", selectEmply.getSelectedItem().toString());

        sharedPref.saveBoolean("checkbox_1HP", checkbox1HP.isChecked());
        sharedPref.saveBoolean("checkbox_10HP", checkbox10HP.isChecked());
        sharedPref.saveBoolean("checkbox_30HP", checkbox30HP.isChecked());

        sharedPref.saveString("On_Display", OnDisplay_text.getText().toString());
        sharedPref.saveString("On_Clamp", OnClamp_text.getText().toString());

        sharedPref.saveBoolean("checkbox_u", checkboxAMP_U.isChecked());
        sharedPref.saveBoolean("checkbox_v", checkboxAMP_V.isChecked());
        sharedPref.saveBoolean("checkbox_w", checkboxAMP_W.isChecked());

        sharedPref.saveString("dc_dsp", DC_DISP_text.getText().toString());
        sharedPref.saveString("dc_met", DC_MET_text.getText().toString());

        sharedPref.saveString("output_dsp", OUTPUT_DISP_text.getText().toString());
        sharedPref.saveString("output_met", OUTPUT_MET_text.getText().toString());

        sharedPref.saveString("enter_RH", enterRH_text.getText().toString());
        sharedPref.saveString("enterReplayOP", enterReplayOP_text.getText().toString());
        sharedPref.saveString("enter_FANOpr", enterFANOpr_text.getText().toString());
        sharedPref.saveString("enter_BODY_Condition", enterBODYCondition_text.getText().toString());
        sharedPref.saveString("enter_io_check", enterIOcheck_text.getText().toString());
        sharedPref.saveString("enterClean", enterClean_Text.getText().toString());
        sharedPref.saveString("enterPramCopy", enterPramCopy_Text.getText().toString());
    }

    @Override
    public void onStop() {
        super.onStop();
        saveValuesToSharedPreferences();
    }

}