package com.example.hi_tech_controls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    EditText emailEditText, passwordEditText;
    Button loginButton;
    String username, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
//            LoginCred();

            Intent intent = new Intent(LoginActivity.this,MainActivity.class);
            startActivity(intent);
        });

    }

    public void LoginCred(){
        username = emailEditText.getText().toString();
        password = passwordEditText.getText().toString();

        // Check if username or password is empty
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter both Username and Password", Toast.LENGTH_LONG).show();
        } else if ("Admin".equals(username) && "Admin".equals(password)) {
            Toast.makeText(LoginActivity.this, "Successfully Login", Toast.LENGTH_LONG).show();

            SharedPreferences preferences = getSharedPreferences("Login",MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("flag",true);
            editor.apply();
            Intent Home = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(Home);

            Log.d("LoginActivity", "Login status saved: " + true);

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            // Display a message for incorrect login credentials
            Toast.makeText(LoginActivity.this, "Wrong login credentials, please try again", Toast.LENGTH_LONG).show();
        }
    }
}
