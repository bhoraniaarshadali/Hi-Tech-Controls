package com.example.hi_tech_controls;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class AddDetailsActivity extends AppCompatActivity {

    // Text values for the TextSwitcher
    private final String[] switcherValues = {
            "Inward Details",
            "Initial Observation",
            "Repairs Details",
            "Final Trial Check"
    };
    ImageView addClientDtls_Back1;
    ImageView addClientDtls_Next1;
    // Fragments
    Fragment fillOneFragment;
    Fragment fillTwoFragment;
    Fragment fillThreeFragment;
    Fragment fillFourFragment;

    // Current fragment index
    int currentFragmentIndex = 0;
    TextSwitcher textSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

        // Initialize Fragments
        fillOneFragment = new com.example.hi_tech_controls.fragments.fill_one_fragment();
        fillTwoFragment = new com.example.hi_tech_controls.fragments.fill_two_fragment();
        fillThreeFragment = new com.example.hi_tech_controls.fragments.fill_three_fragment();
        fillFourFragment = new com.example.hi_tech_controls.fragments.fill_four_fragment();

        // Load First Fragment (Fill_one)
        loadFragment(fillOneFragment);

        // Find BACK
        addClientDtls_Back1 = findViewById(R.id.addClientDtls_Back);
        // Implement BACK button
        addClientDtls_Back1.setOnClickListener(v -> goBack());

        // Find NEXT
        addClientDtls_Next1 = findViewById(R.id.addClientDtls_Next);
        // Implement NEXT button
        addClientDtls_Next1.setOnClickListener(v -> loadNextFragment());

        // Find TextSwitcher
        textSwitcher = findViewById(R.id.textSwitcher);
        textSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView textView = new TextView(AddDetailsActivity.this);
                textView.setTextSize(20);
                return textView;
            }
        });

        // Set initial text
        textSwitcher.setText(switcherValues[currentFragmentIndex]);
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
            } else if (currentFragmentIndex == 2) {
                loadFragment(fillThreeFragment);
            } else if (currentFragmentIndex == 3) {
                loadFragment(fillFourFragment);
            }

            textSwitcher.setText(switcherValues[currentFragmentIndex]);
        } else {
            // Handle as needed when all values have been cycled through
        }
    }

    // Method to navigate back to the previous fragment
    private void goBack() {
        if (currentFragmentIndex > 0) {
            currentFragmentIndex--;

            if (currentFragmentIndex == 0) {
                loadFragment(fillOneFragment);
            } else if (currentFragmentIndex == 1) {
                loadFragment(fillTwoFragment);
            } else if (currentFragmentIndex == 2) {
                loadFragment(fillThreeFragment);
            }

            textSwitcher.setText(switcherValues[currentFragmentIndex]);
        } else {
            // If the current fragment index is 0, handle as needed (e.g., go back to the previous activity)
            super.onBackPressed();
        }
    }
}
