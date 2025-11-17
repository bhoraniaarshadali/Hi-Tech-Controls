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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    EditText emailEditText, passwordEditText;
    Button loginButton;
    ImageView passwordVisibilityToggle;

    private Toast currentToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() started");
        super.onCreate(savedInstanceState);

        boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                .getBoolean("flag", false);

        Log.d(TAG, "Stored login flag = " + isLoggedIn);

        if (isLoggedIn) {
            Log.d(TAG, "User already logged in → Opening MainActivity");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        Log.d(TAG, "Login layout loaded");

        loadAd();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        passwordVisibilityToggle = findViewById(R.id.passwordVisibilityToggle);

        Log.d(TAG, "UI elements initialized");

        emailEditText.requestFocus();

        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            Log.d(TAG, "EditorAction: actionId=" + actionId);

            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                Log.d(TAG, "Enter key pressed → Triggering Login");
                loginButton.performClick();
                return true;
            }
            return false;
        });

        passwordVisibilityToggle.setOnClickListener(v -> {
            Log.d(TAG, "Password visibility toggle clicked");
            togglePasswordVisibility();
        });

        loginButton.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            handleLogin();
        });

        runEntryAnimation();
        Log.d(TAG, "Entry animation started");
    }


    void loadAd() {
        try {
            Log.d(TAG, "Loading AdMob banner...");
            AdView adView = findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
            Log.d(TAG, "Ad requested successfully");
        } catch (Exception e) {
            Log.e(TAG, "Ad load failed: " + e.getMessage());
        }
    }


    private void handleLogin() {
        Log.d(TAG, "handleLogin() called");

        String username = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        Log.d(TAG, "Entered username: " + username);
        Log.d(TAG, "Entered password length: " + password.length());

        if (username.isEmpty()) {
            Log.w(TAG, "Username empty");
            emailEditText.setError("Enter username");
            YoYo.with(Techniques.Shake).duration(200).repeat(1).playOn(emailEditText);
            return;
        }

        if (password.isEmpty()) {
            Log.w(TAG, "Password empty");
            passwordEditText.setError("Enter password");
            YoYo.with(Techniques.Shake).duration(200).repeat(1).playOn(passwordEditText);
            return;
        }

        hideKeyboard();
        loginButton.setEnabled(false);
        Log.d(TAG, "Login button temporarily disabled to prevent double click");

        loginButton.postDelayed(() -> loginButton.setEnabled(true), 1000);

        if (username.equals("Admin") && password.equals("Admin")) {
            Log.d(TAG, "Credentials valid → Logging in");

            SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
            preferences.edit().putBoolean("flag", true).apply();
            Log.d(TAG, "Login flag saved to SharedPreferences");

            showCleanToast("Welcome");

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Log.w(TAG, "Invalid credentials entered");

            showCleanToast("Wrong credentials");

            emailEditText.setText("");
            passwordEditText.setText("");

            YoYo.with(Techniques.Shake).duration(250).repeat(1).playOn(emailEditText);
            YoYo.with(Techniques.Shake).duration(250).repeat(1).playOn(passwordEditText);
        }
    }

    private void togglePasswordVisibility() {
        Log.d(TAG, "togglePasswordVisibility() called");

        if (passwordEditText.getInputType() ==
                (android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {

            Log.d(TAG, "Switching to visible password");
            passwordEditText.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );
            passwordVisibilityToggle.setImageResource(R.drawable.ic_password_visibility_on);

        } else {
            Log.d(TAG, "Switching to hidden password");
            passwordEditText.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            passwordVisibilityToggle.setImageResource(R.drawable.ic_password_visibility_off);
        }

        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    private void hideKeyboard() {
        Log.d(TAG, "Hiding keyboard");
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(loginButton.getWindowToken(), 0);
    }


    private void runEntryAnimation() {
        Log.d(TAG, "Starting entry animation");

        int[] ids = {
                R.id.logoImageView, R.id.welcomeTextView, R.id.subHeadingTextView,
                R.id.emailEditText, R.id.passwordEditText, R.id.loginButton,
                R.id.versionTextView, R.id.passwordVisibilityToggle
        };

        for (int id : ids) {
            android.view.View v = findViewById(id);
            if (v == null) {
                Log.w(TAG, "Missing view for animation: ID=" + id);
                continue;
            }

            v.setAlpha(0f);
            v.setTranslationY(50);
            v.animate().alpha(1f).translationYBy(-50).setDuration(900);
        }
    }

    private void showCleanToast(String msg) {
        Log.d(TAG, "Toast: " + msg);
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }

}
