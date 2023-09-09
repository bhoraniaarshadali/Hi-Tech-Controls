package com.example.hi_tech_controls.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;

import java.util.Calendar;

public class fill_one_fragment extends Fragment implements DatePickerDialog.OnDateSetListener {

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

        dateTextField1 = rootView.findViewById(R.id.dateTextField);

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        datePickerDialog1 = new DatePickerDialog(getActivity(), (view, year1, month1, dayOfMonth) -> {
            month1 += 1;
            String date = dayOfMonth + "/" + month1 + "/" + year1;
            dateTextField1.setText(date);
        }, year, month, day);
        datePickerDialog1.setCancelable(true);

        dateTextField1.setOnClickListener(v -> datePickerDialog1.show());

        return rootView;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // This method is called when the date is set in the DatePickerDialog
    }
}
