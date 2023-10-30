package com.example.hi_tech_controls;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hi_tech_controls.fragments.View_data_fragment;
import com.example.hi_tech_controls.fragments.fill_four_fragment;
import com.example.hi_tech_controls.fragments.fill_one_fragment;
import com.example.hi_tech_controls.fragments.fill_three_fragment;
import com.example.hi_tech_controls.fragments.fill_two_fragment;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddDetailsActivity extends AppCompatActivity {

    private final String[] switcherValues = {
            "Inward Details",
            "Initial Observation",
            "Repairs Details",
            "Final Trial Check"
    };

    private Fragment fillOneFragment;
    private Fragment fillTwoFragment;
    private Fragment fillThreeFragment;
    private Fragment fillFourFragment;
    private Fragment viewfragment;
    private int currentFragmentIndex = 0;
    public static final int[] progressValues = {0, 25, 50, 75, 100};
    private ProgressBar progressBar;
    private ProgressBar progressBarDUMP;
    private ProgressBar progressBarTEXTDUMPwhite;
    private ProgressBar progressBarTEXTDUMPblue;


    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

        Button cameraButton1 = findViewById(R.id.cameraButton);


        cameraButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddDetailsActivity.this, MediaUpload.class);
                startActivity(intent);
            }
        });

        // PreFix-Load-Fragment
        initializeFragments();
        loadFragment(fillOneFragment);
        //anim();

        // PostFix-Load
        initializeUIElements();
        initializeSharedPreferences();
        setInitialProgress();
        setButtonListeners();
    }

    // Initialize Fragments
    private void initializeFragments() {
        fillOneFragment = new fill_one_fragment();
        fillTwoFragment = new fill_two_fragment();
        fillThreeFragment = new fill_three_fragment();
        fillFourFragment = new fill_four_fragment();
        viewfragment = new View_data_fragment();
    }

    // Initialize UI elements
    private void initializeUIElements() {
        TextSwitcher textSwitcher = findViewById(R.id.textSwitcher);
        textSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView textView = new TextView(AddDetailsActivity.this);
                textView.setTextSize(17);
                return textView;
            }
        });
        textSwitcher.setText(switcherValues[currentFragmentIndex]);

        progressBarDUMP = findViewById(R.id.progressBarDUMP);
        progressBarDUMP.setEnabled(false);
        progressBarTEXTDUMPwhite = findViewById(R.id.progressBarTEXTDUMPwhite);
        progressBarTEXTDUMPwhite.setEnabled(false);
        progressBarTEXTDUMPblue = findViewById(R.id.progressBarTEXTDUMPblue);
        progressBarTEXTDUMPblue.setEnabled(false);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setEnabled(false);
    }

    // Initialize SharedPreferences
    private void initializeSharedPreferences() {
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
    }

    // Set initial progress based on the current progress index
    private void setInitialProgress() {
        currentFragmentIndex = sharedPreferences.getInt("progressIndex", 0);
        progressBar.setProgress(progressValues[currentFragmentIndex]);
    }

    // Set button click listeners
    private void setButtonListeners() {
        ImageView addClientDtls_Back1 = findViewById(R.id.addClientDtls_Back);
        addClientDtls_Back1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBack();
            }
        });

        ImageView addClientDtls_Next1 = findViewById(R.id.addClientDtls_Next);
        addClientDtls_Next1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNextFragment();
            }
        });
    }

    // Load a fragment into the FrameLayout
    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
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

    // Show a completion pop-up and set progress to 100%
    private void showCompletionPopup() {
        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        dialog.setTitleText("Data Stored Successfully!" + "Client Id: " + fill_one_fragment.clientIdValue);
        dialog.setContentText("You completed all the steps!");

        // Set a click listener for the confirmation button
        dialog.setConfirmButtonBackgroundColor(Color.parseColor("#181C5C"));
        dialog.setConfirmText("Okay");
        dialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sDialog) {
                sDialog.dismissWithAnimation();
                View_data_fragment();
            }
        });

        dialog.show();
        progressBar.setProgress(100);
    }


    // Navigate to MainActivity
    private void navigateToMainActivity() {
        Intent intent = new Intent(AddDetailsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void View_data_fragment() {
        if (viewfragment != null) {
            currentFragmentIndex++; // Update the current fragment index
            loadFragment(viewfragment); // Load the new fragment

            //navigateToMainActivity();
        }
    }


    // Navigate back to the previous fragment
    private void goBack() {
        if (currentFragmentIndex > 0) {
            currentFragmentIndex--;
            loadFragmentByIndex(currentFragmentIndex);
            updateProgressBar(progressValues[currentFragmentIndex]);
            updateTextSwitcher();
            saveProgressIndex(currentFragmentIndex);
        } else {
            navigateToMainActivity();
        }
    }

    // Update the TextSwitcher's text
    private void updateTextSwitcher() {
        TextSwitcher textSwitcher = findViewById(R.id.textSwitcher);
        textSwitcher.setText(switcherValues[currentFragmentIndex]);
    }

    // Load a fragment based on its index
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

    // Update the ProgressBar
    private void updateProgressBar(int progress) {
        progressBar.setProgress(progress);
    }

    // Helper method to save progress index using SharedPreferences
    private void saveProgressIndex(int index) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("progressIndex", index);
        editor.apply();
    }

    public void anim() {
        FrameLayout frameLayout1 = findViewById(R.id.frameLayout);

        frameLayout1.setAlpha(0f);
        frameLayout1.setTranslationY(50);
        frameLayout1.animate().alpha(1f).translationYBy(-50).setDuration(1000);
    }
}