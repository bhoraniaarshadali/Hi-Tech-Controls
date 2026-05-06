package com.example.hi_tech_controls.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.AdminManager;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LottieAnimationView anim = findViewById(R.id.splashAnim);
        TextView versionText = findViewById(R.id.versionText);

        // Show app version
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionText.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("Version 1.1");
        }

        anim.setImageAssetsFolder("splash/images");

        anim.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "Splash animation ended → checking maintenance & device status");
                checkAndNavigate();
            }
        });
    }

    /**
     * After animation ends:
     * 1. Fetch admin config from Firebase
     * 2. If maintenance=true → go to Login (LoginActivity also shows the warning)
     * 3. If user is logged in → check device block → go to Main or Login
     * 4. If not logged in → go to Login
     */
    private void checkAndNavigate() {
        AdminManager.fetchConfig(new AdminManager.ConfigCallback() {
            @Override
            public void onResult(String username, String password, boolean maintenance) {
                runOnUiThread(() -> {
                    if (maintenance) {
                        Log.w(TAG, "App under maintenance → sending to LoginActivity");
                        Toast.makeText(SplashActivity.this,
                                "⚠️ App is under maintenance. Please try again later.",
                                Toast.LENGTH_LONG).show();
                        goToLogin();
                        return;
                    }

                    boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                            .getBoolean("flag", false);

                    if (!isLoggedIn) {
                        Log.d(TAG, "Not logged in → going to LoginActivity");
                        goToLogin();
                        return;
                    }

                    // Logged in — check if device is blocked
                    AdminManager.checkDeviceBlocked(SplashActivity.this,
                            new AdminManager.DeviceStatusCallback() {
                                @Override
                                public void onResult(boolean isBlocked) {
                                    runOnUiThread(() -> {
                                        if (isBlocked) {
                                            Log.w(TAG, "Device blocked → forcing logout");
                                            forceLogout();
                                        } else {
                                            Log.d(TAG, "All clear → going to MainActivity");
                                            goToMain();
                                        }
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    // On error: be safe, still open MainActivity
                                    runOnUiThread(() -> {
                                        Log.e(TAG, "Device check error on splash: " + error);
                                        goToMain();
                                    });
                                }
                            });
                });
            }

            @Override
            public void onError(String error) {
                // Firebase unreachable — fall back to normal local flow
                runOnUiThread(() -> {
                    Log.e(TAG, "Config fetch failed on splash: " + error);
                    boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                            .getBoolean("flag", false);
                    if (isLoggedIn) goToMain();
                    else goToLogin();
                });
            }
        });
    }

    private void forceLogout() {
        getSharedPreferences("Login", MODE_PRIVATE)
                .edit().putBoolean("flag", false).apply();
        Toast.makeText(this,
                "🚫 Your device has been blocked. Contact admin.",
                Toast.LENGTH_LONG).show();
        goToLogin();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
