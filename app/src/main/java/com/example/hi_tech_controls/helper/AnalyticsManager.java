package com.example.hi_tech_controls.helper;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * AnalyticsManager — Helper to log Firebase Analytics events.
 * Appends version name to event names for easy tracking as requested.
 */
public class AnalyticsManager {

    private static final String TAG = "AnalyticsManager";
    private static FirebaseAnalytics mFirebaseAnalytics;
    private static FirebaseCrashlytics mCrashlytics;

    public static void logEvent(Context context, String eventName) {
        logEvent(context, eventName, null);
    }

    public static void logEvent(Context context, String eventName, Bundle params) {
        try {
            if (mFirebaseAnalytics == null) {
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
            }
            if (mCrashlytics == null) {
                mCrashlytics = FirebaseCrashlytics.getInstance();
            }

            // Get Version Name and sanitize it (e.g., 1.2 -> 1_2)
            String rawVersion = "unknown";
            try {
                rawVersion = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).versionName;
                mCrashlytics.setCustomKey("app_version", rawVersion);
            } catch (Exception e) {
                Log.e(TAG, "Version fetch failed", e);
            }
            
            String sanitizedVersion = rawVersion.replace(".", "_").replace(" ", "_");

            // Format: eventName_v1_2
            String finalEventName = eventName + "_v" + sanitizedVersion;

            // Firebase Event Naming Rules:
            // 1. Max 40 characters
            // 2. Alphanumeric and underscores only
            // 3. Must start with a letter
            finalEventName = finalEventName.replaceAll("[^a-zA-Z0-9_]", "_");
            if (finalEventName.length() > 40) {
                finalEventName = finalEventName.substring(0, 40);
            }

            if (params == null) {
                params = new Bundle();
            }
            params.putString("full_version", rawVersion);

            mFirebaseAnalytics.logEvent(finalEventName, params);
            Log.d(TAG, "Logged Event: " + finalEventName);

        } catch (Exception e) {
            Log.e(TAG, "Failed to log event: " + eventName, e);
        }
    }

    public static void setUserId(Context context, String userId) {
        try {
            if (mCrashlytics == null) {
                mCrashlytics = FirebaseCrashlytics.getInstance();
            }
            mCrashlytics.setUserId(userId);
            Log.d(TAG, "Crashlytics User ID set: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set Crashlytics User ID", e);
        }
    }
}
