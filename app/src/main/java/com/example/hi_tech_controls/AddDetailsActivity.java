package com.example.hi_tech_controls;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class AddDetailsActivity extends AppCompatActivity {

    ImageView addClientDtls_Back1;
    ImageView addClientDtls_Next1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

        // Load First Fragment (Fill_one)
        loadFragment(new com.example.hi_tech_controls.fragments.fill_one_fragment());

        //find BACK
        addClientDtls_Back1 = findViewById(R.id.addClientDtls_Back);
        //implement
        addClientDtls_Back1.setOnClickListener(v -> onBackPressed());

        //find NEXT
        addClientDtls_Next1 = findViewById(R.id.addClientDtls_Next);
        //implement
        addClientDtls_Next1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Load Second Fragment (Fill_two)
                //loadFragment(new com.example.hi_tech_controls.fragments.fill_two_fragment());
            }
        });
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
}
