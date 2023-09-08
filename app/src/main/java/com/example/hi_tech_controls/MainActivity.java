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
    private ImageView logout_btn_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logout_btn_layout = findViewById(R.id.logout_btn);
        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);

        logout_btn_layout.setOnClickListener(v -> onBackPressed());


        addClientBtn1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddDetailsActivity.class);
            startActivity(intent);
        });

        viewClientBtn1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewDetailsActivity.class);
            startActivity(intent);
        });
    }
}