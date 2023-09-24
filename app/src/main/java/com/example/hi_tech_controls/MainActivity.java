package com.example.hi_tech_controls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {
    public static String clientIdValue;
    TextView showId;
    private ProgressBar progressBar;
    private ImageView logout_btn_layout;

    private CardView cardView_1;
    Button addClientBtn1;
    Button viewClientBtn1;
    SharedPrefHelper sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize your UI elements
        showId = findViewById(R.id.showId);
        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);
        cardView_1 = findViewById(R.id.cardView_1);

        sharedPref = new SharedPrefHelper(this);

        logout_btn_layout = findViewById(R.id.logout_btn);
        logout_btn_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        cardView_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddDetailsActivity.class);
                startActivity(intent);
            }
        });

        addClientBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddDetailsActivity.class);
                startActivity(intent);
            }
        });

        viewClientBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ViewDetailsActivity.class);
                startActivity(intent);
            }
        });

        setProgressBarStatus();
    }

    @Override
    public void onBackPressed() {
        showExitConfirmationDialog();
    }

    private void setProgressBarStatus() {
        progressBar = findViewById(R.id.progressStatusBar);

        // Retrieve the progress index from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int progressIndex = sharedPreferences.getInt("progressIndex", 0);

        // Set the progress on the progress bar based on the index
        progressBar.setProgress(AddDetailsActivity.progressValues[progressIndex]);
    }

    private void showExitConfirmationDialog() {
        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        dialog.setTitleText("Logout");
        dialog.setContentText("Are you sure you want to logout the app?");

        // Set custom text color for Confirm button
        dialog.setConfirmButtonBackgroundColor(Color.parseColor("#E91E63"));
        dialog.setConfirmText("Logout");

        // Set custom text color for Cancel button
        dialog.setCancelButtonBackgroundColor(Color.parseColor("#7C7C7C"));
        dialog.setCancelText("Cancel");

        dialog.showCancelButton(true);

        dialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sDialog) {
                // Handle cancel button click (dismiss the dialog)
                sDialog.dismissWithAnimation();
            }
        });

        dialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sDialog) {
                // Handle confirm button click (perform logout)
                logout();

                // Dismiss the dialog
                sDialog.dismissWithAnimation();
            }
        });

        // Show the dialog
        dialog.show();
    }

    private void logout() {
        // Add your logout logic here, such as clearing user data or preferences
        // For example, you can use shared preferences to store login state and clear it
//        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.clear(); // Clear user data
//        editor.apply();

        // After clearing data, you can start the login activity or perform any other necessary actions
        Intent intent = new Intent(MainActivity.this, LoginActivity.class); // Replace LoginActivity with your login activity
        startActivity(intent);

        // Finish the current activity (main activity)
        finish();
    }
}
