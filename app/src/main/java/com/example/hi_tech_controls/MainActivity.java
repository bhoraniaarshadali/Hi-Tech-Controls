package com.example.hi_tech_controls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.adapter.AddDetailsAdp;
import com.example.hi_tech_controls.adapter.DetailsModel;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerViewDiscovery1;

    public static String clientIdValue;
    TextView showId;
    public static TextView statusText1;
    private ImageView logout_btn_layout;

    private CardView cardView_1;
    private static ProgressBar progressBar;
    Button addClientBtn1;
    Button viewClientBtn1;
    SharedPrefHelper sharedPref;
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

    // Register your BroadcastReceiver in onResume
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("com.example.hi_tech_controls.PROGRESS_UPDATE");
        registerReceiver(progressUpdateReceiver, intentFilter);
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
        //anim();

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

        recyclerViewDiscovery1 = (RecyclerView) findViewById(R.id.recyclerViewDiscovery);

        ArrayList<DetailsModel> DetailsData = new ArrayList<>();

        DetailsModel details = new DetailsModel();

        details.setUId(125);
        details.setuName("Dev Dave");
        details.setProgress(100);


        DetailsModel details1 = new DetailsModel();

        details1.setUId(125);
        details1.setuName("Arshad ali Bhorania");
        details1.setProgress(60);

        DetailsModel details2 = new DetailsModel();

        details2.setUId(125);
        details2.setuName("Amit Suthar");
        details2.setProgress(80);

        DetailsData.add(details);
        DetailsData.add(details1);
        DetailsData.add(details2);

        AddDetailsAdp obj = new AddDetailsAdp(this, DetailsData);

        recyclerViewDiscovery1.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDiscovery1.setAdapter(obj);

        //setProgressBarStatus();
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
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Clear user data
        editor.apply();

        // After clearing data, you can start the login activity or perform any other necessary actions
        Intent intent = new Intent(MainActivity.this, LoginActivity.class); // Replace LoginActivity with your login activity
        startActivity(intent);

        // Finish the current activity (main activity)
        finish();
    }


//    public void anim() {
//        CardView cardView_1 = findViewById(R.id.cardView_1);
//        cardView_1.setAlpha(0f);
//        cardView_1.setTranslationY(50);
//        cardView_1.animate().alpha(1f).translationYBy(-50).setDuration(1000);
//    }
}
