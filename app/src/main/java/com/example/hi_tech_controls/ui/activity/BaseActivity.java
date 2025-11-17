package com.example.hi_tech_controls.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.helper.NetworkUtils;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View offlineBanner;
    private TextView offlineText;
    private ImageView bannerIcon;

    private BroadcastReceiver networkReceiver;
    private boolean wasOffline = false;

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
        Log.d(TAG, "onResume() - Registering network receiver");
        registerNetworkReceiver();
        updateOfflineStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() - Unregistering network receiver");
        unregisterNetworkReceiver();
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

            offlineText.setText("Back Online!");
            bannerIcon.setImageResource(R.drawable.ic_online);
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

        offlineText.setText("Offline – Changes will sync when online");
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
}
