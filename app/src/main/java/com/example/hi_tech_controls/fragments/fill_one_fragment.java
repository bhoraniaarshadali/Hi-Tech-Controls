package com.example.hi_tech_controls.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;

import java.util.Calendar;

public class fill_one_fragment extends Fragment implements DatePickerDialog.OnDateSetListener {


    public static String clientIdValue;
    private EditText enterName, enterNumber, enterGPNumber, enterDate, enterMakeName, enterModelName, enterHPrate, enterSerialNumber;
    private SharedPrefHelper sharedPref;

    // date picker
    private EditText dateTextField1;
    private DatePickerDialog datePickerDialog1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_one, container, false);
        dateTextField1 = rootView.findViewById(R.id.fill_one_enterDate);
        initDatePicker();

        sharedPref = new SharedPrefHelper(requireContext());
        return rootView;
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //textView Id
        TextView clientId = view.findViewById(R.id.clientId);
        clientIdValue = clientId.getText().toString();

        // Initialize your EditText fields
        enterName = view.findViewById(R.id.fill_one_enterName);
        enterNumber = view.findViewById(R.id.fill_one_enterNumber);
        enterGPNumber = view.findViewById(R.id.fill_one_enterGPNumber);
        enterDate = view.findViewById(R.id.fill_one_enterDate);
        enterMakeName = view.findViewById(R.id.fill_one_enterMakeName);
        enterModelName = view.findViewById(R.id.fill_one_enterModelName);
        enterHPrate = view.findViewById(R.id.fill_one_enterHPrate);
        enterSerialNumber = view.findViewById(R.id.fill_one_enterSerialNumber);

        // Load saved values from SharedPreferences and set them to EditText fields
        enterName.setText(sharedPref.getString("name", ""));
        enterNumber.setText(sharedPref.getString("number", ""));
        enterGPNumber.setText(sharedPref.getString("gp_number", ""));
        enterDate.setText(sharedPref.getString("date", ""));
        enterMakeName.setText(sharedPref.getString("make_name", ""));
        enterModelName.setText(sharedPref.getString("model_name", ""));
        enterHPrate.setText(sharedPref.getString("hp_rate", ""));
        enterSerialNumber.setText(sharedPref.getString("serial_number", ""));
    }

    private void initDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        datePickerDialog1 = new DatePickerDialog(requireActivity(), (view, year1, month1, dayOfMonth) -> {
            month1 += 1;
            String date = dayOfMonth + "/" + month1 + "/" + year1;
            dateTextField1.setText(date);
        }, year, month, day);
        datePickerDialog1.setCancelable(true);

        dateTextField1.setOnClickListener(v -> datePickerDialog1.show());
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // This method is called when the date is set in the DatePickerDialog
    }

    private void saveValuesToSharedPreferences() {
        sharedPref.saveString("name", enterName.getText().toString());
        sharedPref.saveString("number", enterNumber.getText().toString());
        sharedPref.saveString("gp_number", enterGPNumber.getText().toString());
        sharedPref.saveString("date", enterDate.getText().toString());
        sharedPref.saveString("make_name", enterMakeName.getText().toString());
        sharedPref.saveString("model_name", enterModelName.getText().toString());
        sharedPref.saveString("hp_rate", enterHPrate.getText().toString());
        sharedPref.saveString("serial_number", enterSerialNumber.getText().toString());
    }

    @Override
    public void onStop() {
        super.onStop();
        saveValuesToSharedPreferences();
    }
}
