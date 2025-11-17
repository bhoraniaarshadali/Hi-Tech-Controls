package com.example.hi_tech_controls.helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class PermissionUtils {

    private static final String TAG = "PermissionUtils";

    private static final String[] STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String[] STORAGE_PERMISSIONS_API33 = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
    };

    // -------------------------------------------------------------------------
    // Check Permission
    // -------------------------------------------------------------------------
    public static boolean hasStoragePermissions(Context context) {
        Log.d(TAG, "hasStoragePermissions() check started…");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean images = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
            boolean videos = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;

            Log.d(TAG, "API 33+ → READ_MEDIA_IMAGES=" + images + ", READ_MEDIA_VIDEO=" + videos);

            return images && videos;
        } else {
            boolean read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            boolean write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

            Log.d(TAG, "Below API 33 → READ_EXTERNAL=" + read + ", WRITE_EXTERNAL=" + write);

            return read && write;
        }
    }

    // -------------------------------------------------------------------------
    // Get Required Permissions
    // -------------------------------------------------------------------------
    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "getStoragePermissions() → returning API33 permissions");
            return STORAGE_PERMISSIONS_API33;
        } else {
            Log.d(TAG, "getStoragePermissions() → returning legacy permissions");
            return STORAGE_PERMISSIONS;
        }
    }

    // -------------------------------------------------------------------------
    // Request Permission
    // -------------------------------------------------------------------------
    public static void requestStoragePermissions(Activity activity, int requestCode) {
        Log.d(TAG, "requestStoragePermissions() invoked");

        List<String> permissionsToRequest = new ArrayList<>();

        for (String p : getStoragePermissions()) {
            boolean granted = ContextCompat.checkSelfPermission(activity, p) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Permission check → " + p + " = " + granted);

            if (!granted) {
                permissionsToRequest.add(p);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);
            ActivityCompat.requestPermissions(activity,
                    permissionsToRequest.toArray(new String[0]),
                    requestCode);
        } else {
            Log.d(TAG, "All storage permissions already granted.");
        }
    }

    // -------------------------------------------------------------------------
    // Verify Results
    // -------------------------------------------------------------------------
    public static boolean allPermissionsGranted(int[] grantResults) {
        if (grantResults.length == 0) {
            Log.d(TAG, "allPermissionsGranted() → empty results = false");
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "allPermissionsGranted() → permission denied");
                return false;
            }
        }

        Log.d(TAG, "allPermissionsGranted() → all granted = true");
        return true;
    }
}
