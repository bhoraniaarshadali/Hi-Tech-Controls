package com.example.hi_tech_controls;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ViewDetailsActivity extends AppCompatActivity {

    ImageView viewClientDtls_Back1;

    Button searchButton1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_details);

        viewClientDtls_Back1 = findViewById(R.id.viewClientDtls_Back);
        searchButton1 = findViewById(R.id.searchButton);

        viewClientDtls_Back1.setOnClickListener(v -> onBackPressed());
    }
}