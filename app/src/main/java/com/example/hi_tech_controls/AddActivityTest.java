package com.example.hi_tech_controls;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.fragments.fill_four_fragment;
import com.example.hi_tech_controls.fragments.fill_one_fragment;
import com.example.hi_tech_controls.fragments.fill_three_fragment;
import com.example.hi_tech_controls.fragments.fill_two_fragment;

public class AddActivityTest extends AppCompatActivity {

    public static final int[] progressValues = {0, 20, 60, 80, 100};
    //TextSwitcher
    private final String[] switcherValues = {
            "Inward Details",
            "Initial Observation",
            "Repairs Details",
            "Final Trial Check"};
    //findViewById()
    ImageView addClientDtls_Back1 = findViewById(R.id.addClientDtls_Back);
    ImageView addClientDtls_Next1 = findViewById(R.id.addClientDtls_Next);
    TextSwitcher textSwitcher = findViewById(R.id.textSwitcher);
    //Fragments
    private Fragment fillOneFragment;
    private Fragment fillTwoFragment;
    private Fragment fillThreeFragment;
    private Fragment fillFourFragment;
    private final int currentFragmentIndex = 0;
    //ProgressBar
    private ProgressBar progressBar;
    //SharedPreferences
    private SharedPreferences sharedPreferences;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_details);

//initialize Fragments
        fillOneFragment = new fill_one_fragment();
        fillOneFragment = new fill_two_fragment();
        fillOneFragment = new fill_three_fragment();
        fillOneFragment = new fill_four_fragment();

        //LoadFirstFragment
//        loadFragment(fillOneFragment);


        addClientDtls_Next1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                loadTextSwitcher();
            }
        });

        addClientDtls_Back1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });


    }


    public void loadTextSwitcher() {
        TextSwitcher textSwitcher = findViewById(R.id.textSwitcher);
        textSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView textView = new TextView(AddActivityTest.this);
                textView.setTextSize(17);
                return textView;
            }
        });
        textSwitcher.setText(switcherValues[currentFragmentIndex]);
    }
}
