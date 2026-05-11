package com.example.hi_tech_controls.helper;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;

/**
 * Utility class for safely parsing Firestore data.
 * Handles various type mismatches and null values.
 */
public class FirestoreUtils {
    private static final String TAG = "FirestoreParse";

    // ----------------------------------------------------
    // Boolean Parsing
    // ----------------------------------------------------

    public static boolean getBooleanSafe(DocumentSnapshot doc, String key) {
        return parseBoolean(doc.get(key), key);
    }

    public static boolean getBooleanSafe(Map<String, Object> data, String key) {
        return data != null ? parseBoolean(data.get(key), key) : false;
    }

    private static boolean parseBoolean(Object value, String key) {
        try {
            if (value == null) return false;
            
            if (value instanceof Boolean) return (Boolean) value;
            
            if (value instanceof String) {
                String s = ((String) value).trim();
                // Handle both "true" and "1" as boolean true
                return s.equalsIgnoreCase("true") || s.equals("1");
            }
            
            if (value instanceof Number) {
                return ((Number) value).intValue() == 1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing boolean for key: " + key, e);
        }

        return false;
    }

    // ----------------------------------------------------
    // String Parsing
    // ----------------------------------------------------

    public static String getStringSafe(DocumentSnapshot doc, String key) {
        return parseString(doc.get(key));
    }

    public static String getStringSafe(Map<String, Object> data, String key) {
        return data != null ? parseString(data.get(key)) : "";
    }

    private static String parseString(Object value) {
        // Prioritize actual String objects to avoid unexpected conversions
        if (value instanceof String) return ((String) value).trim();
        
        // Return string representation for other types if they exist, 
        // ensuring we never return null.
        return value != null ? String.valueOf(value).trim() : "";
    }

    // ----------------------------------------------------
    // Integer Parsing
    // ----------------------------------------------------

    public static int getIntSafe(DocumentSnapshot doc, String key) {
        return parseInt(doc.get(key), key);
    }

    public static int getIntSafe(Map<String, Object> data, String key) {
        return data != null ? parseInt(data.get(key), key) : 0;
    }

    private static int parseInt(Object value, String key) {
        if (value instanceof Number) return ((Number) value).intValue();
        
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    // ----------------------------------------------------
    // Long Parsing
    // ----------------------------------------------------

    public static long getLongSafe(DocumentSnapshot doc, String key) {
        return parseLong(doc.get(key), key);
    }

    public static long getLongSafe(Map<String, Object> data, String key) {
        return data != null ? parseLong(data.get(key), key) : 0L;
    }

    private static long parseLong(Object value, String key) {
        if (value instanceof Number) return ((Number) value).longValue();
        
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (Exception e) {
                return 0L;
            }
        }
        return 0L;
    }
}
