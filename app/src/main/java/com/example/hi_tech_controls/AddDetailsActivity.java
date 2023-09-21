package com.example.hi_tech_controls;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hi_tech_controls.fragments.fill_four_fragment;
import com.example.hi_tech_controls.fragments.fill_one_fragment;
import com.example.hi_tech_controls.fragments.fill_three_fragment;
import com.example.hi_tech_controls.fragments.fill_two_fragment;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddDetailsActivity extends AppCompatActivity {

    // Text values for the TextSwitcher
    private final String[] switcherValues = {
            "Inward Details",
            "Initial Observation",
            "Repairs Details",
            "Final Trial Check"
    };

    // Fragments
    private Fragment fillOneFragment;
    private Fragment fillTwoFragment;
    private Fragment fillThreeFragment;
    private Fragment fillFourFragment;

    // Current fragment index
    private int currentFragmentIndex = 0;

    // ProgressBar
    private ProgressBar progressBar;

    private static final String PROGRESS_KEY = "progress_key";
    // SharedPreferences
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

        // Initialize Fragments
        fillOneFragment = new fill_one_fragment();
        fillTwoFragment = new fill_two_fragment();
        fillThreeFragment = new fill_three_fragment();
        fillFourFragment = new fill_four_fragment();

        // Load First Fragment (Fill_one)
        loadFragment(fillOneFragment);

        // Find BACK
        ImageView addClientDtls_Back1 = findViewById(R.id.addClientDtls_Back);

        // Implement BACK button
        addClientDtls_Back1.setOnClickListener(v -> goBack());

        // Find NEXT
        ImageView addClientDtls_Next1 = findViewById(R.id.addClientDtls_Next);

        // Implement NEXT button
        addClientDtls_Next1.setOnClickListener(v -> loadNextFragment());

        // Find TextSwitcher
        TextSwitcher textSwitcher = findViewById(R.id.textSwitcher);
        textSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView textView = new TextView(AddDetailsActivity.this);
                textView.setTextSize(17);
                return textView;
            }
        });

        // Set initial text
        textSwitcher.setText(switcherValues[currentFragmentIndex]);

        // Find ProgressBar
        progressBar = findViewById(R.id.progressBar);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        // Load the progress from SharedPreferences
        int savedProgress = sharedPreferences.getInt(PROGRESS_KEY, 0);
        updateProgressBar(savedProgress); // Set the progress bar to the saved value
    }

    // Fragment Method
    private void loadFragment(Fragment fragment) {
        // Create a FragmentManager
        FragmentManager fm = getSupportFragmentManager();

        // Create a FragmentTransaction to begin the transaction and replace the fragment
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
    }

    // Method to load the next fragment in sequence
    private void loadNextFragment() {
        if (currentFragmentIndex < switcherValues.length - 1) { // Check if there's a next fragment
            currentFragmentIndex++;

            if (currentFragmentIndex == 1) {
                loadFragment(fillTwoFragment);
                updateProgressBar(20); // Update progress to 20%
            } else if (currentFragmentIndex == 2) {
                loadFragment(fillThreeFragment);
                updateProgressBar(60); // Update progress to 60%
            } else if (currentFragmentIndex == 3) {
                loadFragment(fillFourFragment);
                updateProgressBar(80); // Update progress to 80%
            }
            updateTextSwitcher();
        } else {
            // Handle as needed when all values have been cycled through
            successMessage();
            updateProgressBar(100); // Update progress to 100%
        }
    }

    // Method to navigate back to the previous fragment
    private void goBack() {
        if (currentFragmentIndex > 0) {
            currentFragmentIndex--;

            if (currentFragmentIndex == 0) {
                loadFragment(fillOneFragment);
                updateProgressBar(0); // Update progress to 0%
            } else if (currentFragmentIndex == 1) {
                loadFragment(fillTwoFragment);
                updateProgressBar(20); // Update progress to 20%
            } else if (currentFragmentIndex == 2) {
                loadFragment(fillThreeFragment);
                updateProgressBar(60); // Update progress to 60%
            }

            updateTextSwitcher();
        } else {
            // If the current fragment index is 0, handle as needed (e.g., go back to the previous activity)
            super.onBackPressed();
        }
    }

    // Method to update the TextSwitcher's text
    private void updateTextSwitcher() {
        TextSwitcher textSwitcher = findViewById(R.id.textSwitcher);
        textSwitcher.setText(switcherValues[currentFragmentIndex]);
    }

    // Method to update the ProgressBar and save the progress value in SharedPreferences
    private void updateProgressBar(int progress) {
        progressBar.setProgress(progress);

        // Save the progress value in SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PROGRESS_KEY, progress);
        editor.apply();
    }

    public void successMessage() {
        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        dialog.setTitleText("Data Stored Successfully!" + "Client Id: " + fill_one_fragment.clientIdValue)
                .setContentText("You clicked the button!")
                .show();
        dialog.setConfirmButtonBackgroundColor(Color.parseColor("#181C5C"));
        dialog.setConfirmText("Okay");
    }
}
