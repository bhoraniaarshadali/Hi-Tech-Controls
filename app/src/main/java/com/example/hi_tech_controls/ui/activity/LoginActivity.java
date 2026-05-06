package com.example.hi_tech_controls.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.AdminManager;
import com.example.hi_tech_controls.helper.LoadingDialog;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    EditText emailEditText, passwordEditText;
    Button loginButton;
    ImageView passwordVisibilityToggle;
    private boolean isPasswordVisible = false;

    private Toast currentToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() started");
        super.onCreate(savedInstanceState);

        // If already logged in, check device/maintenance then open main
        boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                .getBoolean("flag", false);

        if (isLoggedIn) {
            Log.d(TAG, "Already logged in → going to MainActivity");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        Log.d(TAG, "Login layout loaded");

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        passwordVisibilityToggle = findViewById(R.id.passwordVisibilityToggle);

        emailEditText.requestFocus();

        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        passwordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility());
        loginButton.setOnClickListener(v -> handleLogin());

        runEntryAnimation();
    }



    // ------------------------------------------------------------------
    // LOGIN FLOW
    // ------------------------------------------------------------------
    private void handleLogin() {
        String username = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty()) {
            emailEditText.setError("Enter username");
            YoYo.with(Techniques.Shake).duration(200).repeat(1).playOn(emailEditText);
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Enter password");
            YoYo.with(Techniques.Shake).duration(200).repeat(1).playOn(passwordEditText);
            return;
        }

        hideKeyboard();
        setUIEnabled(false);
        loginButton.setText("Verifying...");

        Log.d(TAG, "Fetching config from Firebase...");

        // Step 1 — Fetch credentials + maintenance from Firebase
        AdminManager.fetchConfig(new AdminManager.ConfigCallback() {
            @Override
            public void onResult(String fbUsername, String fbPassword, boolean maintenance) {
                runOnUiThread(() -> {
                    // Step 2 — Check maintenance
                    if (maintenance) {
                        setUIEnabled(true);
                        loginButton.setText("Login");
                        Log.w(TAG, "App is under maintenance");
                        showCleanToast("⚠️ App is under maintenance. Please try again later.");
                        return;
                    }

                    // Step 3 — Compare credentials
                    if (username.equals(fbUsername) && password.equals(fbPassword)) {
                        Log.d(TAG, "Credentials valid → checking device block...");
                        checkDeviceAndProceed();
                    } else {
                        setUIEnabled(true);
                        loginButton.setText("Login");
                        Log.w(TAG, "Wrong credentials");
                        showCleanToast("Wrong username or password");
                        emailEditText.setText("");
                        passwordEditText.setText("");
                        YoYo.with(Techniques.Shake).duration(250).repeat(1).playOn(emailEditText);
                        YoYo.with(Techniques.Shake).duration(250).repeat(1).playOn(passwordEditText);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setUIEnabled(true);
                    loginButton.setText("Login");
                    Log.e(TAG, "Config fetch error: " + error);
                    showCleanToast("Network error. Please check your connection.");
                });
            }
        });
    }

    // Step 4 — Check if this device is blocked
    private void checkDeviceAndProceed() {
        AdminManager.checkDeviceBlocked(this, new AdminManager.DeviceStatusCallback() {
            @Override
            public void onResult(boolean isBlocked) {
                runOnUiThread(() -> {
                    if (isBlocked) {
                        setUIEnabled(true);
                        loginButton.setText("Login");
                        Log.w(TAG, "Device is blocked by admin");
                        showCleanToast("Your device has been blocked. Contact admin.");
                        YoYo.with(Techniques.Shake).duration(300).repeat(2).playOn(loginButton);
                    } else {
                        // Step 5 — All clear: save login + register device + open app
                        getSharedPreferences("Login", MODE_PRIVATE)
                                .edit().putBoolean("flag", true).apply();

                        AdminManager.registerDevice(LoginActivity.this);

                        Log.d(TAG, "Login successful → opening MainActivity");
                        showCleanToast("Welcome!");
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setUIEnabled(true);
                    loginButton.setText("Login");
                    Log.e(TAG, "Device check error: " + error);
                    showCleanToast("Network error. Please try again.");
                });
            }
        });
    }

    // ------------------------------------------------------------------
    // PASSWORD TOGGLE
    // ------------------------------------------------------------------
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordEditText.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordVisibilityToggle.setImageResource(R.drawable.ic_password_visibility_on);
        } else {
            passwordEditText.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordVisibilityToggle.setImageResource(R.drawable.ic_password_visibility_off);
        }
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------
    private void setUIEnabled(boolean enabled) {
        emailEditText.setEnabled(enabled);
        passwordEditText.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        passwordVisibilityToggle.setEnabled(enabled);

        // Visual feedback
        float alpha = enabled ? 1.0f : 0.6f;
        emailEditText.setAlpha(alpha);
        passwordEditText.setAlpha(alpha);
        loginButton.setAlpha(alpha);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(loginButton.getWindowToken(), 0);
    }

    private void runEntryAnimation() {
        int[] ids = {
                R.id.logoImageView, R.id.welcomeTextView, R.id.subHeadingTextView,
                R.id.emailEditText, R.id.passwordEditText, R.id.loginButton,
                R.id.versionTextView, R.id.passwordVisibilityToggle
        };
        for (int id : ids) {
            android.view.View v = findViewById(id);
            if (v == null) continue;
            v.setAlpha(0f);
            v.setTranslationY(50);
            v.animate().alpha(1f).translationYBy(-50).setDuration(900);
        }
    }

    private void showCleanToast(String msg) {
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        currentToast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoadingDialog.getInstance().dismiss();
    }
}
