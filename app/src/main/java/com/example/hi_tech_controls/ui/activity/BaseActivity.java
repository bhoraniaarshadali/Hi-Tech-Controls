package com.example.hi_tech_controls.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.graphics.Rect;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.AdminManager;
import com.example.hi_tech_controls.helper.NetworkUtils;
import com.google.firebase.firestore.ListenerRegistration;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View offlineBanner;
    private TextView offlineText;
    private ImageView bannerIcon;

    private BroadcastReceiver networkReceiver;
    private boolean wasOffline = false;

    private ListenerRegistration maintenanceListener;
    private ListenerRegistration deviceBlockListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called - waiting for child layout");
    }

    @Override
    public void setContentView(int layoutResID) {

        Log.d(TAG, "setContentView() called. Injecting Base Layout");

        super.setContentView(R.layout.activity_base);

        initializeBannerViews();
        inflateChildLayout(layoutResID);

        Log.d(TAG, "Child layout inflated. Updating offline banner state...");
        updateOfflineStatus();
    }

    private void initializeBannerViews() {
        offlineBanner = findViewById(R.id.offlineBanner);
        offlineText = findViewById(R.id.offlineText);
        bannerIcon = findViewById(R.id.bannerIcon);

        Log.d(TAG, "Offline banner views initialized");
    }

    private void inflateChildLayout(int layoutId) {
        try {
            getLayoutInflater().inflate(layoutId, findViewById(R.id.base_content), true);
            Log.d(TAG, "inflateChildLayout(): Layout inflated successfully");
        } catch (Exception e) {
            Log.e(TAG, "inflateChildLayout(): Failed inflating child layout", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() - Registering network receiver and admin listeners");
        registerNetworkReceiver();
        updateOfflineStatus();
        startAdminListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() - Unregistering network receiver and admin listeners");
        unregisterNetworkReceiver();
        stopAdminListeners();
    }

    private void startAdminListeners() {
        // 1. Listen for maintenance mode
        maintenanceListener = AdminManager.listenMaintenance(isMaintenance -> {
            if (isMaintenance) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "⚠️ App is under maintenance. Please try again later.", Toast.LENGTH_LONG).show();
                    baseLogout();
                });
            }
        });

        // 2. Listen for device blocking
        deviceBlockListener = AdminManager.listenDeviceBlock(this, new AdminManager.DeviceStatusCallback() {
            @Override
            public void onResult(boolean isBlocked) {
                if (isBlocked) {
                    runOnUiThread(() -> {
                        Toast.makeText(BaseActivity.this, "🚫 Your device has been blocked. Contact admin.", Toast.LENGTH_LONG).show();
                        baseLogout();
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Device block listener error: " + error);
            }
        });
    }

    private void stopAdminListeners() {
        if (maintenanceListener != null) {
            maintenanceListener.remove();
            maintenanceListener = null;
        }
        if (deviceBlockListener != null) {
            deviceBlockListener.remove();
            deviceBlockListener = null;
        }
    }

    protected void baseLogout() {
        Log.d(TAG, "baseLogout() called");
        getSharedPreferences("Login", MODE_PRIVATE)
                .edit().putBoolean("flag", false).apply();

        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // -----------------------------------------------------------
    // 🔵 NETWORK RECEIVER MANAGEMENT
    // -----------------------------------------------------------
    private void registerNetworkReceiver() {
        if (networkReceiver != null) {
            Log.w(TAG, "registerNetworkReceiver(): Receiver already registered.");
            return;
        }

        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(TAG, "Network state changed → checking internet");

                NetworkUtils.resetCache();
                boolean isOnline = NetworkUtils.isInternetAvailable(BaseActivity.this);

                Log.d(TAG, "Internet Available: " + isOnline);

                updateOfflineStatus(isOnline);
                onNetworkStateChanged(isOnline);
            }
        };

        try {
            registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            Log.d(TAG, "NetworkReceiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network receiver", e);
        }
    }

    private void unregisterNetworkReceiver() {
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
                Log.d(TAG, "NetworkReceiver unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
            networkReceiver = null;
        }
    }

    // -----------------------------------------------------------
    // 🔵 OFFLINE / ONLINE STATUS BANNER
    // -----------------------------------------------------------
    private void updateOfflineStatus(boolean isOnline) {

        Log.d(TAG, "updateOfflineStatus(): isOnline = " + isOnline);

        if (isOnline) {
            handleBackOnlineBanner();
        } else {
            handleOfflineBanner();
        }
    }

    private void handleBackOnlineBanner() {

        if (wasOffline) {
            Log.d(TAG, "Device is back online – showing green banner");

            offlineText.setText("You are online now");
            bannerIcon.setImageResource(R.drawable.ic_wifi_online);
            offlineBanner.setBackgroundColor(0xFF4CAF50); // Green
            offlineBanner.setVisibility(View.VISIBLE);

            handler.postDelayed(() -> {

                boolean stillOnline = NetworkUtils.isInternetAvailable(this);
                Log.d(TAG, "Back Online banner timeout → stillOnline=" + stillOnline);

                if (stillOnline) offlineBanner.setVisibility(View.GONE);

            }, 2000);

        } else {
            Log.d(TAG, "Online and not previously offline → hiding banner");
            offlineBanner.setVisibility(View.GONE);
        }

        wasOffline = false;
    }

    private void handleOfflineBanner() {

        Log.d(TAG, "Device OFFLINE → showing orange banner");

        offlineText.setText("You are Offline");
        bannerIcon.setImageResource(R.drawable.ic_offline);


        offlineBanner.setBackgroundColor(0xFFFF9800); // Orange
        offlineBanner.setVisibility(View.VISIBLE);
        wasOffline = true;
    }

    private void updateOfflineStatus() {
        boolean isOnline = NetworkUtils.isInternetAvailable(this);
        Log.d(TAG, "updateOfflineStatus() → initial check = " + isOnline);
        updateOfflineStatus(isOnline);
    }

    // -----------------------------------------------------------
    // 🔵 CALLBACK FOR CHILD ACTIVITIES TO OVERRIDE
    // -----------------------------------------------------------
    protected void onNetworkStateChanged(boolean isOnline) {
        Log.d(TAG, "onNetworkStateChanged callback: " + isOnline);
    }

    // -----------------------------------------------------------
    // 🔵 GLOBAL KEYBOARD MANAGEMENT
    // -----------------------------------------------------------
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                
                // If touch is outside the current focused EditText
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    
                    // Check if we are touching another EditText
                    if (!isTouchOnAnyEditText(getWindow().getDecorView(), event)) {
                        v.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * Helper to check if the touch event is on any EditText in the hierarchy.
     */
    private boolean isTouchOnAnyEditText(View view, MotionEvent event) {
        if (view instanceof EditText) {
            Rect outRect = new Rect();
            view.getGlobalVisibleRect(outRect);
            return outRect.contains((int) event.getRawX(), (int) event.getRawY());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (isTouchOnAnyEditText(group.getChildAt(i), event)) return true;
            }
        }
        return false;
    }

    // -----------------------------------------------------------
    // 🔵 AUTO-SCROLL ON TYPE
    // -----------------------------------------------------------
    /**
     * Recursively finds all EditTexts in a layout and adds a TextWatcher
     * that ensures the field remains visible while typing.
     */
    public void setupAutoScrollOnType(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                setupAutoScrollOnType(group.getChildAt(i));
            }
        } else if (root instanceof EditText) {
            EditText et = (EditText) root;
            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (et.hasFocus()) {
                        et.post(() -> {
                            Rect rect = new Rect(0, 0, et.getWidth(), et.getHeight());
                            et.requestRectangleOnScreen(rect, false);
                        });
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
}
