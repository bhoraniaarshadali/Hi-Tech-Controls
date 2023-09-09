package com.example.hi_tech_controls;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddDetailsActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    ImageView addClientDtls_Back1;

    // date picker
    private EditText dateTextField1;
    private DatePickerDialog datePickerDialog1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

        addClientDtls_Back1 = findViewById(R.id.addClientDtls_Back);
        addClientDtls_Back1.setOnClickListener(v -> onBackPressed());

        dateTextField1 = findViewById(R.id.dateTextField);

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        datePickerDialog1 = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            month1 += 1;
            String date = dayOfMonth + "/" + month1 + "/" + year1;
            dateTextField1.setText(date);
        }, year, month, day);
        datePickerDialog1.setCancelable(true);

        dateTextField1.setOnClickListener(v -> datePickerDialog1.show());
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

    }
}
