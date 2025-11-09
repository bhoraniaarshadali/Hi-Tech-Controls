package com.example.hi_tech_controls.adapter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkUtils {
    private static Boolean isOnline = null;

    public static boolean isInternetAvailable(Context context) {
        if (isOnline != null) return isOnline;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            isOnline = false;
            return false;
        }

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            isOnline = false;
            return false;
        }

        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        isOnline = caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
        return isOnline;
    }

    public static void resetCache() {
        isOnline = null;
    }
}