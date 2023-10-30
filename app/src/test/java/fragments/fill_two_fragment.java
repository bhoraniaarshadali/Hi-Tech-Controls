package fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;

import java.util.Objects;

public class fill_two_fragment extends Fragment {

    private RadioGroup radioGroup;
    private EditText localEditText;
    private Spinner selectEmply;
    private EditText enterFRrate;
    private RadioButton radioButtonLocal, radioButtonRemote, radioButtonComm, radioButtonDIODE, radioButtonSCR;
    private CheckBox input_POS_checkbox_U, input_POS_checkbox_V, input_POS_checkbox_W, input_NEG_checkbox_U, input_NEG_checkbox_V,
            input_NEG_checkbox_W, output_POS_checkbox_U, output_POS_checkbox_V, output_POS_checkbox_W, output_NEG_checkbox_U, output_NEG_checkbox_V, output_NEG_checkbox_W;
    private EditText clientObsText, ourObsText, lastFaultText;
    private SharedPrefHelper sharedPref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_two, container, false);

        initializeUIElements(rootView);
        setUpRadioGroupListener();
        setUpSpinner(rootView);


        // Initialize sharedPref
        sharedPref = new SharedPrefHelper(requireContext());
        return rootView;
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize your View elements using findViewById on the view
        selectEmply = view.findViewById(R.id.fill_two_selectEmply);
        enterFRrate = view.findViewById(R.id.fill_two_enterFRrate);
        radioButtonLocal = view.findViewById(R.id.fill_two_radioButtonLocal);
        radioButtonRemote = view.findViewById(R.id.fill_two_radioButtonRemote);
        radioButtonComm = view.findViewById(R.id.fill_two_radioButtonComm);
        localEditText = view.findViewById(R.id.fill_two_localEditText);
        radioButtonDIODE = view.findViewById(R.id.fill_two_radioButtonDIODE);
        radioButtonSCR = view.findViewById(R.id.fill_two_radioButtonSCR);
        input_POS_checkbox_U = view.findViewById(R.id.fill_two_input_POS_checkbox_U);
        input_POS_checkbox_V = view.findViewById(R.id.fill_two_input_POS_checkbox_V);
        input_POS_checkbox_W = view.findViewById(R.id.fill_two_input_POS_checkbox_W);
        input_NEG_checkbox_U = view.findViewById(R.id.fill_two_input_NEG_checkbox_U);
        input_NEG_checkbox_V = view.findViewById(R.id.fill_two_input_NEG_checkbox_V);
        input_NEG_checkbox_W = view.findViewById(R.id.fill_two_input_NEG_checkbox_W);
        output_POS_checkbox_U = view.findViewById(R.id.fill_two_output_POS_checkbox_U);
        output_POS_checkbox_V = view.findViewById(R.id.fill_two_output_POS_checkbox_V);
        output_POS_checkbox_W = view.findViewById(R.id.fill_two_output_POS_checkbox_W);
        output_NEG_checkbox_U = view.findViewById(R.id.fill_two_output_NEG_checkbox_U);
        output_NEG_checkbox_V = view.findViewById(R.id.fill_two_output_NEG_checkbox_V);
        output_NEG_checkbox_W = view.findViewById(R.id.fill_two_output_NEG_checkbox_W);
        clientObsText = view.findViewById(R.id.fill_two_clientObs_text);
        ourObsText = view.findViewById(R.id.fill_two_ourObs_text);
        lastFaultText = view.findViewById(R.id.fill_two_lastFault_text);


        // Load saved values and set them to the UI elements
        String selectedEmployee = sharedPref.getString("select_emp", "");
        if (!selectedEmployee.isEmpty()) {
            // Find the position of the selectedEmployee in the employees array
            int position = getPositionOfEmployee(selectedEmployee);
            if (position >= 0) {
                selectEmply.setSelection(position);
            }
        }
        enterFRrate.setText(sharedPref.getString("fr_rate", ""));

        boolean isLocal = sharedPref.getBoolean("local_radio_checked", false);
        boolean isRemote = sharedPref.getBoolean("remote_radio_checked", false);
        boolean isComm = sharedPref.getBoolean("comm_radio_checked", false);
        radioButtonLocal.setChecked(isLocal);
        radioButtonRemote.setChecked(isRemote);
        radioButtonComm.setChecked(isComm);
        localEditText.setText(sharedPref.getString("localEditText", ""));

        boolean isDIODE = sharedPref.getBoolean("diode_radio_checked", false);
        boolean isSCR = sharedPref.getBoolean("scr_radio_checked", false);
        radioButtonDIODE.setChecked(isDIODE);
        radioButtonSCR.setChecked(isSCR);

        input_POS_checkbox_U.setChecked(sharedPref.getBoolean("input_pos_checkbox_U", false));
        input_POS_checkbox_V.setChecked(sharedPref.getBoolean("input_pos_checkbox_V", false));
        input_POS_checkbox_W.setChecked(sharedPref.getBoolean("input_pos_checkbox_W", false));
        input_NEG_checkbox_U.setChecked(sharedPref.getBoolean("input_neg_checkbox_U", false));
        input_NEG_checkbox_V.setChecked(sharedPref.getBoolean("input_neg_checkbox_V", false));
        input_NEG_checkbox_W.setChecked(sharedPref.getBoolean("input_neg_checkbox_W", false));

        output_POS_checkbox_U.setChecked(sharedPref.getBoolean("output_pos_checkbox_U", false));
        output_POS_checkbox_V.setChecked(sharedPref.getBoolean("output_pos_checkbox_V", false));
        output_POS_checkbox_W.setChecked(sharedPref.getBoolean("output_pos_checkbox_W", false));
        output_NEG_checkbox_U.setChecked(sharedPref.getBoolean("output_neg_checkbox_U", false));
        output_NEG_checkbox_V.setChecked(sharedPref.getBoolean("output_neg_checkbox_V", false));
        output_NEG_checkbox_W.setChecked(sharedPref.getBoolean("output_neg_checkbox_W", false));
        clientObsText.setText(sharedPref.getString("client_obs", ""));
        ourObsText.setText(sharedPref.getString("our_obs", ""));
        lastFaultText.setText(sharedPref.getString("last_fault", ""));

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

    private void initializeUIElements(View rootView) {
        radioGroup = rootView.findViewById(R.id.radioGroupCMD);
        localEditText = rootView.findViewById(R.id.fill_two_localEditText);
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
        if (checkedId == R.id.fill_two_radioButtonLocal) {
            localEditText.setVisibility(View.VISIBLE);
        } else {
            localEditText.setVisibility(View.GONE);
        }
    }

    private void setUpSpinner(View rootView) {
        Spinner spinner = rootView.findViewById(R.id.fill_two_selectEmply);
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
        sharedPref.saveString("fr_rate", enterFRrate.getText().toString());

        sharedPref.saveBoolean("local_radio_checked", radioButtonLocal.isChecked());
        sharedPref.saveBoolean("remote_radio_checked", radioButtonRemote.isChecked());
        sharedPref.saveBoolean("comm_radio_checked", radioButtonComm.isChecked());
        sharedPref.saveBoolean("diode_radio_checked", radioButtonDIODE.isChecked());
        sharedPref.saveBoolean("scr_radio_checked", radioButtonSCR.isChecked());
        sharedPref.saveString("localEditText", localEditText.getText().toString());

        sharedPref.saveBoolean("input_pos_checkbox_U", input_POS_checkbox_U.isChecked());
        sharedPref.saveBoolean("input_pos_checkbox_V", input_POS_checkbox_V.isChecked());
        sharedPref.saveBoolean("input_pos_checkbox_W", input_POS_checkbox_W.isChecked());
        sharedPref.saveBoolean("input_neg_checkbox_U", input_NEG_checkbox_U.isChecked());
        sharedPref.saveBoolean("input_neg_checkbox_V", input_NEG_checkbox_V.isChecked());
        sharedPref.saveBoolean("input_neg_checkbox_W", input_NEG_checkbox_W.isChecked());
        sharedPref.saveBoolean("output_pos_checkbox_U", output_POS_checkbox_U.isChecked());
        sharedPref.saveBoolean("output_pos_checkbox_V", output_POS_checkbox_V.isChecked());
        sharedPref.saveBoolean("output_pos_checkbox_W", output_POS_checkbox_W.isChecked());
        sharedPref.saveBoolean("output_neg_checkbox_U", output_NEG_checkbox_U.isChecked());
        sharedPref.saveBoolean("output_neg_checkbox_V", output_NEG_checkbox_V.isChecked());
        sharedPref.saveBoolean("output_neg_checkbox_W", output_NEG_checkbox_W.isChecked());


        sharedPref.saveString("client_obs", clientObsText.getText().toString());
        sharedPref.saveString("our_obs", ourObsText.getText().toString());
        sharedPref.saveString("last_fault", lastFaultText.getText().toString());
    }

    @Override
    public void onStop() {
        super.onStop();
        saveValuesToSharedPreferences();
    }
}
