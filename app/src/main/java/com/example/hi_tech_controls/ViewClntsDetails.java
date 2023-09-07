package com.example.hi_tech_controls;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ViewClntsDetails extends AppCompatActivity {

    ImageView viewClientDtls_Back1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_clnts_details);

        viewClientDtls_Back1 = findViewById(R.id.viewClientDtls_Back);

        viewClientDtls_Back1.setOnClickListener(v -> onBackPressed());
    }
}