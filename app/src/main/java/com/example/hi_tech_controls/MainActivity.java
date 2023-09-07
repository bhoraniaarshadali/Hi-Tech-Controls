package com.example.hi_tech_controls;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class
MainActivity extends AppCompatActivity {

    Button addClientBtn1;
    Button viewClientBtn1;
    private ImageView mainactivity_Back1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainactivity_Back1 = findViewById(R.id.mainactivity_Back);
        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);


        mainactivity_Back1.setOnClickListener(v -> onBackPressed());

        viewClientBtn1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewClntsDetails.class);
            startActivity(intent);
        });
    }
}