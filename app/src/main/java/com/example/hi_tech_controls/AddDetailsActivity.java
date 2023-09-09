package com.example.hi_tech_controls;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class AddDetailsActivity extends AppCompatActivity {

    ImageView addClientDtls_Back1;
    ImageView addClientDtls_Next1;

    // Fragments
    Fragment fillOneFragment;
    Fragment fillTwoFragment;
    Fragment fillThreeFragment;
    Fragment fillFourFragment;

    // Current fragment index
    int currentFragmentIndex = 0;

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
        currentFragmentIndex++;

        if (currentFragmentIndex == 1) {
            loadFragment(fillTwoFragment);
        } else if (currentFragmentIndex == 2) {
            loadFragment(fillThreeFragment);
        } else if (currentFragmentIndex == 3) {
            loadFragment(fillFourFragment);
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
        } else {
            // If the current fragment index is 0, handle as needed (e.g., go back to the previous activity)
            super.onBackPressed();
        }
    }
}
