package com.example.hi_tech_controls.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.hi_tech_controls.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LottieAnimationView anim = findViewById(R.id.splashAnim);

        anim.setImageAssetsFolder("splash/images");

        anim.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

                boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                        .getBoolean("flag", false);

                Intent i = new Intent(
                        SplashActivity.this,
                        isLoggedIn ? MainActivity.class : LoginActivity.class
                );

                startActivity(i);
                finish();
            }
        });
    }
}
