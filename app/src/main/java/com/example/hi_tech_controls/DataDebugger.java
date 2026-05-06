package com.example.hi_tech_controls;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class DataDebugger {
    public static void debugData() {
        FirebaseFirestore.getInstance().collection("hi_tech_controls_dataset_JUNE")
            .limit(5)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Log.d("DEBUG_DATA", "ID: " + doc.getId() + " => " + doc.getData());
                }
            })
            .addOnFailureListener(e -> Log.e("DEBUG_DATA", "Error", e));
    }
}
