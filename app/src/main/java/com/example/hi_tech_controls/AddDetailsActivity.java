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

    // Define the progress values array
    public static final int[] progressValues = {0, 20, 60, 80, 100}; // Updated progress values

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

        // Get the saved progress index (0 by default)
        currentFragmentIndex = sharedPreferences.getInt("progressIndex", 0);

        // Set the initial progress based on the current progress index
        progressBar.setProgress(progressValues[currentFragmentIndex]);
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

            // Load the corresponding fragment and update progress
            loadFragmentByIndex(currentFragmentIndex);

            // Update progress bar based on progressValues array
            updateProgressBar(progressValues[currentFragmentIndex]);
            updateTextSwitcher();

            // Save the current progress index
            saveProgressIndex(currentFragmentIndex);
        } else {
            // Handle the case when the user is on the last fragment and presses "Next"
            showCompletionPopup();
        }
    }

    // Method to show a completion pop-up and set progress to 100%
    private void showCompletionPopup() {
        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        dialog.setTitleText("Data Stored Successfully!" + "Client Id: " + fill_one_fragment.clientIdValue);
        dialog.setContentText("You completed all the steps!")
                .show();
        dialog.setConfirmButtonBackgroundColor(Color.parseColor("#181C5C"));
        dialog.setConfirmText("Okay");

        // Set progress to 100%
        progressBar.setProgress(100);
    }

    // Method to navigate back to the previous fragment
    private void goBack() {
        if (currentFragmentIndex > 0) {
            currentFragmentIndex--;

            // Load the corresponding fragment and update progress
            loadFragmentByIndex(currentFragmentIndex);

            // Update progress bar based on progressValues array
            updateProgressBar(progressValues[currentFragmentIndex]);
            updateTextSwitcher();

            // Save the current progress index
            saveProgressIndex(currentFragmentIndex);
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

    // Method to load a fragment based on its index
    private void loadFragmentByIndex(int index) {
        switch (index) {
            case 0:
                loadFragment(fillOneFragment);
                break;
            case 1:
                loadFragment(fillTwoFragment);
                break;
            case 2:
                loadFragment(fillThreeFragment);
                break;
            case 3:
                loadFragment(fillFourFragment);
                break;
            case 4:
                showCompletionPopup();
        }
    }

    // Method to update the ProgressBar
    private void updateProgressBar(int progress) {
        progressBar.setProgress(progress);
    }

    // Helper method to save progress index using SharedPreferences
    private void saveProgressIndex(int index) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("progressIndex", index);
        editor.apply();
    }
}
