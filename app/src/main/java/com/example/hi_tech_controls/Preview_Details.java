package com.example.hi_tech_controls;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Preview_Details extends AppCompatActivity {
    private TextView textViewOnPreview1;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_details);

        textViewOnPreview1 = findViewById(R.id.textOne);

        // Retrieve data passed from fill_one_fragment
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String name = extras.getString("name", "");
            String number = extras.getString("number", "");
            String gpNumber = extras.getString("gp_number", "");
            String date = extras.getString("date", "");
            String makeName = extras.getString("make_name", "");
            String modelName = extras.getString("model_name", "");
            String hpRate = extras.getString("hp_rate", "");
            String serialNumber = extras.getString("serial_number", "");

            // Display the data in your TextView or other UI elements as needed
            textViewOnPreview1.setText("Name: " + name + "\n" +
                    "Number: " + number + "\n" +
                    "GP Number: " + gpNumber + "\n" +
                    "Date: " + date + "\n" +
                    "Make Name: " + makeName + "\n" +
                    "Model Name: " + modelName + "\n" +
                    "HP Rate: " + hpRate + "\n" +
                    "Serial Number: " + serialNumber);
        }
    }
}
