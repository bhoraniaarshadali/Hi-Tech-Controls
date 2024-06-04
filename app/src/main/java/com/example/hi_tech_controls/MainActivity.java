package com.example.hi_tech_controls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.adapter.AddDetailsAdp;
import com.example.hi_tech_controls.adapter.DetailsModel;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    public static String clientIdValue;
    public static TextView statusText1;
    private static ProgressBar progressBar;
    // Define your BroadcastReceiver
    private final BroadcastReceiver progressUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("com.example.hi_tech_controls.PROGRESS_UPDATE")) {
                int progress = intent.getIntExtra("progress", 0);
                progressBar.setProgress(progress);

                // Check if the progress is 100% and update the text accordingly
                if (progress == 100) {
                    statusText1.setText("Completed");

                    // Send a broadcast to inform MainActivity
                    Intent broadcastIntent = new Intent("com.example.hi_tech_controls.PROGRESS_UPDATE");
                    broadcastIntent.putExtra("progress", progress);
                    sendBroadcast(broadcastIntent);
                }
            }
        }
    };
    RecyclerView recyclerViewDiscovery1;
    TextView showId;
    Button addClientBtn1;
    Button viewClientBtn1;
    SharedPrefHelper sharedPref;
    private ImageView logout_btn_layout;
    private CardView cardView_1;

    // Register your BroadcastReceiver in onResume
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("com.example.hi_tech_controls.PROGRESS_UPDATE");
        registerReceiver(progressUpdateReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    // Unregister your BroadcastReceiver in onPause to avoid leaks
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(progressUpdateReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addClientBtn1 = findViewById(R.id.addClientBtn);
        viewClientBtn1 = findViewById(R.id.viewClientBtn);


        sharedPref = new SharedPrefHelper(this);


        logout_btn_layout = findViewById(R.id.logout_btn);
        logout_btn_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitConfirmationDialog();
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

        recyclerViewDiscovery1 = findViewById(R.id.recyclerViewDiscovery);

        ArrayList<DetailsModel> DetailsData = new ArrayList<>();

        DetailsModel details = new DetailsModel();
        details.setUId(2009);
        details.setuName("Dev Dave");
        details.setProgress(80);


        DetailsModel details1 = new DetailsModel();
        details1.setUId(2006);
        details1.setuName("Arshad ali Bhorania");
        details1.setProgress(60);

        DetailsModel details2 = new DetailsModel();
        details2.setUId(2004);
        details2.setuName("Martin Elliott");
        details2.setProgress(80);

        DetailsModel details3 = new DetailsModel();
        details3.setUId(2003);
        details3.setuName("John Brush");
        details3.setProgress(60);

        DetailsModel details4 = new DetailsModel();
        details4.setUId(2010);
        details4.setuName("Stark Hally");
        details4.setProgress(20);


        DetailsData.add(details);
        DetailsData.add(details1);
        DetailsData.add(details2);
        DetailsData.add(details3);
        DetailsData.add(details4);

        AddDetailsAdp obj = new AddDetailsAdp(this, DetailsData);

        recyclerViewDiscovery1.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDiscovery1.setAdapter(obj);

        //setProgressBarStatus();
    }

    @Override
    public void onBackPressed() {
        //check if the user is logged in
        SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("flag", false);
        if (isLoggedIn) {
            showExitConfirmationDialog(); // if logged in , show the confirmation dialog
        } else {
            super.onBackPressed();
        }
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
        // clear the user's login state by setting "flag" to false in shared preferences
        SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("flag", false);
        editor.apply();
        // After clearing data, you can start the LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear back stack
        startActivity(intent);
        //finish the current activity (main activity)
        finish();
    }

}