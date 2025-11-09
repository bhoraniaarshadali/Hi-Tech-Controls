// app/src/main/java/com/example/hi_tech_controls/BaseActivity.java
package com.example.hi_tech_controls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hi_tech_controls.adapter.NetworkUtils;

public abstract class BaseActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View offlineBanner;
    private TextView offlineText;
    private ImageView bannerIcon;
    private BroadcastReceiver networkReceiver;
    private boolean wasOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // DO NOT setContentView here
    }

    @Override
    public void setContentView(int layoutResID) {
        // Inflate base layout with banner
        super.setContentView(R.layout.activity_base);

        // Find views
        offlineBanner = findViewById(R.id.offlineBanner);
        offlineText = findViewById(R.id.offlineText);
        bannerIcon = findViewById(R.id.bannerIcon);

        // Inflate child layout
        getLayoutInflater().inflate(layoutResID, findViewById(R.id.base_content), true);

        updateOfflineStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerNetworkReceiver();
        updateOfflineStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkReceiver();
    }

    private void registerNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkUtils.resetCache();
                boolean isOnline = NetworkUtils.isInternetAvailable(BaseActivity.this);
                updateOfflineStatus(isOnline);
                onNetworkStateChanged(isOnline);
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    private void unregisterNetworkReceiver() {
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (Exception e) {
            }
            networkReceiver = null;
        }
    }

    private void updateOfflineStatus(boolean isOnline) {
        if (isOnline) {
            if (wasOffline) {
                // Show "Back Online"
                offlineText.setText("Back Online!");
                bannerIcon.setImageResource(R.drawable.ic_online);
                offlineBanner.setBackgroundColor(0xFF4CAF50); // Green
                offlineBanner.setVisibility(View.VISIBLE);

                // Hide after 2 sec
                handler.postDelayed(() -> {
                    if (NetworkUtils.isInternetAvailable(this)) {
                        offlineBanner.setVisibility(View.GONE);
                    }
                }, 2000);
            } else {
                offlineBanner.setVisibility(View.GONE);
            }
            wasOffline = false;
        } else {
            // Offline
            offlineText.setText("Offline – Changes will sync when online");
            bannerIcon.setImageResource(R.drawable.ic_offline);
            offlineBanner.setBackgroundColor(0xFFFF9800); // Orange
            offlineBanner.setVisibility(View.VISIBLE);
            wasOffline = true;
        }
    }

    private void updateOfflineStatus() {
        updateOfflineStatus(NetworkUtils.isInternetAvailable(this));
    }

    protected void onNetworkStateChanged(boolean isOnline) {
    }
}