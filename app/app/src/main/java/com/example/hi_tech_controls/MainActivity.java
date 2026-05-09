package com.example.hi_tech_controls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.adapter.AddDetailsAdp;
import com.example.hi_tech_controls.adpter.DetailsModel;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerViewDiscovery1;
    private Button addClientBtn1, viewClientBtn1;
    private ImageView logout_btn_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);
        logout_btn_layout = findViewById(R.id.logout_btn);
        recyclerViewDiscovery1 = findViewById(R.id.recyclerViewDiscovery);

        logout_btn_layout.setOnClickListener(v -> showExitConfirmationDialog());

        addClientBtn1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddDetailsActivity.class);
            startActivity(intent);
        });

        viewClientBtn1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewDetailsActivity.class);
            startActivity(intent);
        });

        ArrayList<DetailsModel> detailsData = new ArrayList<>();
        AddDetailsAdp adapter = new AddDetailsAdp(this, detailsData);

        recyclerViewDiscovery1.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDiscovery1.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
        if (preferences.getBoolean("flag", false)) {
            showExitConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showExitConfirmationDialog() {
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Logout")
                .setContentText("Are you sure you want to logout?")
                .setConfirmButtonBackgroundColor(Color.parseColor("#E91E63"))
                .setConfirmText("Logout")
                .setCancelButtonBackgroundColor(Color.parseColor("#7C7C7C"))
                .setCancelText("Cancel")
                .setConfirmClickListener(sDialog -> {
                    logout();
                    sDialog.dismissWithAnimation();
                })
                .showCancelButton(true)
                .show();
    }

    private void logout() {
        getSharedPreferences("Login", MODE_PRIVATE).edit().putBoolean("flag", false).apply();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}