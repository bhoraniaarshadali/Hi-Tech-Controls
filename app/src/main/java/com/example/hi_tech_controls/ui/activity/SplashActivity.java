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
        // Safety Timeout: If Firebase takes > 5 seconds, proceed anyway
        final boolean[] isResponded = {false};
        android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (!isResponded[0]) {
                isResponded[0] = true;
                Log.w(TAG, "Config fetch timed out → proceeding with local state");
                proceedWithLocalState();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 5000);

        AdminManager.fetchConfig(new AdminManager.ConfigCallback() {
            @Override
            public void onResult(String username, String password, boolean maintenance) {
                if (isResponded[0]) return;
                isResponded[0] = true;
                timeoutHandler.removeCallbacks(timeoutRunnable);

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
                                    runOnUiThread(() -> {
                                        Log.e(TAG, "Device check error: " + error);
                                        goToMain();
                                    });
                                }
                            });
                });
            }

            @Override
            public void onError(String error) {
                if (isResponded[0]) return;
                isResponded[0] = true;
                timeoutHandler.removeCallbacks(timeoutRunnable);

                runOnUiThread(() -> {
                    Log.e(TAG, "Config fetch failed: " + error);
                    proceedWithLocalState();
                });
            }
        });
    }

    private void proceedWithLocalState() {
        boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                .getBoolean("flag", false);
        if (isLoggedIn) goToMain();
        else goToLogin();
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
