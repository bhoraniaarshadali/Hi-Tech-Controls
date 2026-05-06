package com.example.hi_tech_controls.helper;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.example.hi_tech_controls.helper.FirestoreUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AdminManager — Central Firebase admin config handler.
 *
 * Firestore structure:
 * admin/
 * config (document)
 * - username: "admin"
 * - password: "1234"
 * - maintenance: false
 * devices/ (sub-collection)
 * {deviceId} (document)
 * - deviceId: "..."
 * - deviceName: "..."
 * - lastLogin: timestamp (long)
 * - blocked: false
 */
public class AdminManager {

    private static final String TAG = "AdminManager";
    private static final String COL_ADMIN = "admin";
    private static final String DOC_CONFIG = "config";
    private static final String COL_DEVICES = "devices";

    // ------------------------------------------------------------------
    // CALLBACKS
    // ------------------------------------------------------------------

    public interface ConfigCallback {
        void onResult(String username, String password, boolean maintenance);

        void onError(String error);
    }

    public interface MaintenanceCallback {
        void onChange(boolean isMaintenance);
    }

    public interface DeviceStatusCallback {
        void onResult(boolean isBlocked);

        void onError(String error);
    }

    public interface EmployeeListCallback {
        void onResult(List<String> employees);

        void onError(String error);
    }

    // ------------------------------------------------------------------
    // 1. FETCH CONFIG ONCE (used at login time)
    // ------------------------------------------------------------------
    public static void fetchConfig(ConfigCallback callback) {
        Log.d(TAG, "fetchConfig() called via transaction");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference configRef = db.collection(COL_ADMIN).document(DOC_CONFIG);

        db.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(configRef);
            if (!doc.exists()) {
                Log.d(TAG, "Atomic init: Config missing, creating defaults");
                Map<String, Object> defaults = new HashMap<>();
                defaults.put("username", "admin");
                defaults.put("password", "1234");
                defaults.put("maintenance", false);
                transaction.set(configRef, defaults);
                return defaults;
            }
            return doc.getData();
        }).addOnSuccessListener(data -> {
            if (data != null) {
                String username = FirestoreUtils.getStringSafe(data, "username");
                String password = FirestoreUtils.getStringSafe(data, "password");
                boolean isMaintenance = FirestoreUtils.getBooleanSafe(data, "maintenance");

                if (username.isEmpty() || password.isEmpty()) {
                    Log.e(TAG, "Config data corrupted: username or password missing");
                    callback.onError("Invalid config data in database");
                    return;
                }

                Log.d(TAG, "Config loaded atomically. maintenance=" + isMaintenance);
                callback.onResult(username, password, isMaintenance);
            } else {
                callback.onError("Failed to load config data");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "fetchConfig transaction failed: " + e.getMessage());
            callback.onError(e.getMessage());
        });
    }

    // ------------------------------------------------------------------
    // 2. REAL-TIME MAINTENANCE LISTENER (attach in MainActivity)
    // ------------------------------------------------------------------
    public static ListenerRegistration listenMaintenance(MaintenanceCallback callback) {
        Log.d(TAG, "Attaching maintenance real-time listener");
        return FirebaseFirestore.getInstance()
                .collection(COL_ADMIN)
                .document(DOC_CONFIG)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Maintenance listener error: " + e.getMessage());
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        boolean isMaintenance = FirestoreUtils.getBooleanSafe(doc, "maintenance");
                        Log.d(TAG, "Maintenance state changed: " + isMaintenance);
                        callback.onChange(isMaintenance);
                    }
                });
    }

    // ------------------------------------------------------------------
    // 3. REGISTER DEVICE ON LOGIN (called after successful credential check)
    // ------------------------------------------------------------------
    public static void registerDevice(Context context) {
        String deviceId = getDeviceId(context);
        String deviceName = Build.MANUFACTURER + " " + Build.MODEL;
        Log.d(TAG, "registerDevice() deviceId=" + deviceId + " deviceName=" + deviceName);

        DocumentReference ref = getDeviceRef(context);

        // First check if document already exists to avoid overwriting 'blocked'
        ref.get().addOnSuccessListener(doc -> {
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", deviceId);
            data.put("deviceName", deviceName);

            // Format current time into readable string (Use Locale.US for consistency)
            String readableTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(new Date());
            data.put("lastLogin", readableTime);

            if (!doc.exists()) {
                // New device — set blocked: false by default
                data.put("blocked", false);
                ref.set(data)
                        .addOnSuccessListener(v -> Log.d(TAG, "New device registered: " + deviceId))
                        .addOnFailureListener(err -> Log.e(TAG, "Device register failed: " + err.getMessage()));
            } else {
                // Existing device — only update login info, keep blocked value intact
                ref.set(data, SetOptions.merge())
                        .addOnSuccessListener(v -> Log.d(TAG, "Device lastLogin updated: " + deviceId))
                        .addOnFailureListener(err -> Log.e(TAG, "Device update failed: " + err.getMessage()));
            }
        }).addOnFailureListener(e -> Log.e(TAG, "registerDevice pre-check failed: " + e.getMessage()));
    }

    // ------------------------------------------------------------------
    // 4. CHECK DEVICE BLOCKED (one-time check, e.g., on splash/login)
    // ------------------------------------------------------------------
    public static void checkDeviceBlocked(Context context, DeviceStatusCallback callback) {
        Log.d(TAG, "checkDeviceBlocked() called");
        getDeviceRef(context)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        boolean isBlocked = FirestoreUtils.getBooleanSafe(doc, "blocked");
                        Log.d(TAG, "Device blocked=" + isBlocked);
                        callback.onResult(isBlocked);
                    } else {
                        // Device not in DB yet → not blocked
                        Log.d(TAG, "Device document not found → not blocked");
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkDeviceBlocked failed: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public static ListenerRegistration listenDeviceBlock(Context context, DeviceStatusCallback callback) {
        Log.d(TAG, "Attaching device block real-time listener");
        return getDeviceRef(context)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Device block listener error: " + e.getMessage());
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        boolean isBlocked = FirestoreUtils.getBooleanSafe(doc, "blocked");
                        Log.d(TAG, "Device block state changed: " + isBlocked);
                        callback.onResult(isBlocked);
                    }
                });
    }

    // ------------------------------------------------------------------
    // 6. REAL-TIME EMPLOYEE LIST LISTENER
    // ------------------------------------------------------------------
    public static ListenerRegistration listenEmployees(EmployeeListCallback callback) {
        Log.d(TAG, "Attaching employee list real-time listener");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference configRef = db.collection(COL_ADMIN).document(DOC_CONFIG);

        return configRef.addSnapshotListener((doc, e) -> {
            if (e != null) {
                Log.e(TAG, "Employee listener error: " + e.getMessage());
                callback.onError(e.getMessage());
                return;
            }

            if (doc != null && doc.exists()) {
                List<String> list = (List<String>) doc.get("employee_list");

                if (list == null || list.isEmpty()) {
                    Log.d(TAG, "Employee list missing/empty, creating defaults");
                    List<String> defaults = Arrays.asList(
                            "Sahil fb", "Ali fb", "Vishal fb", "Hasnain fb");

                    configRef.set(Collections.singletonMap("employee_list", defaults), SetOptions.merge())
                            .addOnSuccessListener(v -> Log.d(TAG, "Default employee list created"))
                            .addOnFailureListener(
                                    err -> Log.e(TAG, "Failed to create default employees: " + err.getMessage()));

                    callback.onResult(defaults);
                } else {
                    Log.d(TAG, "Employee list loaded: " + list.size() + " items");
                    callback.onResult(list);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private static DocumentReference getDeviceRef(Context context) {
        return FirebaseFirestore.getInstance()
                .collection(COL_ADMIN)
                .document(DOC_CONFIG)
                .collection(COL_DEVICES)
                .document(getDeviceId(context));
    }
}
