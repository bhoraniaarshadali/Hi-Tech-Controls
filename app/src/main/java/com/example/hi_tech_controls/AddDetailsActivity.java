package com.example.hi_tech_controls;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class AddDetailsActivity extends AppCompatActivity {

    ImageView addClientDtls_Back1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

        addClientDtls_Back1 = findViewById(R.id.addClientDtls_Back);

        addClientDtls_Back1.setOnClickListener(v -> onBackPressed());

    }
}