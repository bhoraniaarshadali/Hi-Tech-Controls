package com.example.hi_tech_controls.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.hi_tech_controls.R;

public class SplashActivity extends AppCompatActivity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LottieAnimationView anim = findViewById(R.id.splashAnim);
        TextView versionText = findViewById(R.id.versionText);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionText.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("Version 1.0");
        }

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
