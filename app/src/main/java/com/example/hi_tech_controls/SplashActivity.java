package com.example.hi_tech_controls;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;


public class SplashActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        imageView = findViewById(R.id.imageView);

        Animation slideBottomToCenterAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_bottom_to_center);
        Animation pauseAnimation = AnimationUtils.loadAnimation(this, R.anim.pause);
        Animation slideCenterToTopAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_center_to_top);

        slideBottomToCenterAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.startAnimation(pauseAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        pauseAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.startAnimation(slideCenterToTopAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        slideCenterToTopAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                handleUserAuthentication();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        imageView.startAnimation(slideBottomToCenterAnimation);
    }

    private void handleUserAuthentication() {
        SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("flag", false);
        Intent nextIntent;

        if (isLoggedIn) {
            nextIntent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            nextIntent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(nextIntent);
        finish();
    }

}
