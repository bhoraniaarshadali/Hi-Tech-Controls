package com.example.hi_tech_controls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class LoginActivity extends AppCompatActivity {

    EditText emailEditText, passwordEditText;
    Button loginButton;
    ImageView passwordVisibilityToggle;

    private Toast currentToast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isLoggedIn = getSharedPreferences("Login", MODE_PRIVATE)
                .getBoolean("flag", false);

        if (isLoggedIn) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

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
        loginButton.setEnabled(false);
        loginButton.postDelayed(() -> loginButton.setEnabled(true), 1000);

        if (username.equals("Admin") && password.equals("Admin")) {

            SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
            preferences.edit().putBoolean("flag", true).apply();

            //Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
            showCleanToast("Welcome");
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            //Toast.makeText(this, "Wrong credentials", Toast.LENGTH_LONG).show();
            showCleanToast("Wrong credentials");

            emailEditText.setText("");
            passwordEditText.setText("");

            YoYo.with(Techniques.Shake).duration(250).repeat(1).playOn(emailEditText);
            YoYo.with(Techniques.Shake).duration(250).repeat(1).playOn(passwordEditText);
        }
    }

    private void togglePasswordVisibility() {
        if (passwordEditText.getInputType() ==
                (android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {

            passwordEditText.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );
            passwordVisibilityToggle.setImageResource(R.drawable.ic_password_visibility_on);
        } else {
            passwordEditText.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            passwordVisibilityToggle.setImageResource(R.drawable.ic_password_visibility_off);
        }

        passwordEditText.setSelection(passwordEditText.getText().length());
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
            v.setAlpha(0f);
            v.setTranslationY(50);
            v.animate().alpha(1f).translationYBy(-50).setDuration(900);
        }
    }

    private void showCleanToast(String msg) {
        if (currentToast != null) currentToast.cancel(); // destroy old toast
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }

}
