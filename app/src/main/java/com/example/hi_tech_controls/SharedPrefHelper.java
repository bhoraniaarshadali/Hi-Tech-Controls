package com.example.hi_tech_controls;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefHelper {
    private static final String SHARED_PREF_NAME = "MySharedPref"; // Change this to your preferred name
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SharedPrefHelper(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Save a string value to SharedPreferences
    public void saveString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    // Save a boolean value to SharedPreferences
    public void saveBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    // Retrieve a string value from SharedPreferences
    public String getString(String key, String s) {
        return sharedPreferences.getString(key, "");
    }

    // Retrieve a boolean value from SharedPreferences
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // Clear a specific value from SharedPreferences
    public void clearValue(String key) {
        editor.remove(key);
        editor.apply();
    }

    // Clear all values in SharedPreferences
    public void clearAll() {
        editor.clear();
        editor.apply();
    }
}
