package com.example.hi_tech_controls;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
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
                new Handler().postDelayed(() -> imageView.startAnimation(slideCenterToTopAnimation), 500);
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
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        imageView.startAnimation(slideBottomToCenterAnimation);
    }
}
