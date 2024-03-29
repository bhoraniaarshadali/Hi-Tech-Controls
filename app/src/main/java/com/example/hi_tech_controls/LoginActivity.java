package com.example.hi_tech_controls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {
    EditText emailEditText, passwordEditText;
    Button loginButton;
    String username, password;
    ImageView passwordVisibilityToggle; // Reference to the password visibility toggle button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        passwordVisibilityToggle = findViewById(R.id.passwordVisibilityToggle);

        // Set an OnClickListener for the password visibility toggle button
        passwordVisibilityToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        loginButton.setOnClickListener(v -> {
            LoginCred();
        });
        //anim();
    }

    public void LoginCred() {
        username = emailEditText.getText().toString();
        password = passwordEditText.getText().toString();

        // Check if username or password is empty
        if (username.isEmpty()) {
            Snackbar.make(emailEditText, "Please enter username!", Snackbar.LENGTH_LONG).show();
            YoYo.with(Techniques.Shake)
                    .duration(200)
                    .repeat(2)
                    .playOn(findViewById(R.id.emailEditText));
        } else if (password.isEmpty()) {
            Snackbar.make(emailEditText, "Please enter password!", Snackbar.LENGTH_LONG).show();
            YoYo.with(Techniques.Shake)
                    .duration(200)
                    .repeat(2)
                    .playOn(findViewById(R.id.passwordEditText));
        } else if ("Admin".equals(username) && "Admin".equals(password)) {
            Toast.makeText(this, "Successful Login", Toast.LENGTH_LONG).show();

            SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("flag", true);
            editor.apply();

            Log.d("LoginActivity", "Login status saved: " + true);

            Intent Home = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(Home);

        } else {
            Snackbar.make(emailEditText, "Wrong login credentials, please try again", Snackbar.LENGTH_LONG).show();
            emailEditText.setText("");
            passwordEditText.setText("");
            YoYo.with(Techniques.Shake)
                    .duration(200)
                    .repeat(2)
                    .playOn(findViewById(R.id.emailEditText));
            YoYo.with(Techniques.Shake)
                    .duration(200)
                    .repeat(2)
                    .playOn(findViewById(R.id.passwordEditText));
        }
    }

    // Toggle password visibility based on the toggle button state
    private void togglePasswordVisibility() {
        int inputType = passwordEditText.getInputType();
        if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) > 0) {
            // Password is currently hidden, show it
            passwordVisibilityToggle.setImageResource(R.drawable.ic_password_visibility_on);
            passwordEditText.setInputType(inputType & ~android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            // Password is currently visible, hide it
            passwordVisibilityToggle.setImageResource(R.drawable.ic_password_visibility_off);
            passwordEditText.setInputType(inputType | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        // Move the cursor to the end of the text
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    public void anim() {
        TextView welcomeTextView1 = findViewById(R.id.welcomeTextView);
        TextView subHeadingTextView1 = findViewById(R.id.subHeadingTextView);
        ImageView logoImageView1 = findViewById(R.id.logoImageView);
        TextView usernameTitleTextView1 = findViewById(R.id.usernameTitleTextView);
        EditText emailEditText1 = findViewById(R.id.emailEditText);
        TextView passwordTitleTextView1 = findViewById(R.id.passwordTitleTextView);
        EditText passwordEditText1 = findViewById(R.id.passwordEditText);
        Button loginButton1 = findViewById(R.id.loginButton);
        FrameLayout bottomShape1 = findViewById(R.id.bottomShape);
        TextView versionTextView1 = findViewById(R.id.versionTextView);
        ImageView passwordVisibilityToggle1 = findViewById(R.id.passwordVisibilityToggle);

        //logo ImageView
        logoImageView1.setAlpha(0f);
        logoImageView1.setTranslationY(50);
        logoImageView1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //welcome Text
        welcomeTextView1.setAlpha(0f);
        welcomeTextView1.setTranslationY(50);
        welcomeTextView1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //subHeading Text
        subHeadingTextView1.setAlpha(0f);
        subHeadingTextView1.setTranslationY(50);
        subHeadingTextView1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //usernameTitleTextView1
        usernameTitleTextView1.setAlpha(0f);
        usernameTitleTextView1.setTranslationY(50);
        usernameTitleTextView1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //username EditText
        emailEditText1.setAlpha(0f);
        emailEditText1.setTranslationY(50);
        emailEditText1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //passwordTitleTextView1
        passwordTitleTextView1.setAlpha(0f);
        passwordTitleTextView1.setTranslationY(50);
        passwordTitleTextView1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //password EditText
        passwordEditText1.setAlpha(0f);
        passwordEditText1.setTranslationY(50);
        passwordEditText1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //login Button
        loginButton1.setAlpha(0f);
        loginButton1.setTranslationY(50);
        loginButton1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //bottom Shape
        bottomShape1.setAlpha(0f);
        bottomShape1.setTranslationY(50);
        bottomShape1.animate().alpha(1f).translationYBy(-50).setDuration(1000);

        //version TextView
        versionTextView1.setAlpha(0f);
        versionTextView1.setTranslationY(70);
        versionTextView1.animate().alpha(1f).translationYBy(-50).setDuration(2000);

        //version TextView
        passwordVisibilityToggle1.setAlpha(0f);
        passwordVisibilityToggle1.setTranslationY(70);
        passwordVisibilityToggle1.animate().alpha(1f).translationYBy(-50).setDuration(2000);
    }
}


