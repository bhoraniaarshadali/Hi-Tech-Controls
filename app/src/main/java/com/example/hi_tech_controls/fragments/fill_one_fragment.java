package com.example.hi_tech_controls.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class fill_one_fragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";
    static final String DOCUMENT_LAST_ID = "last_id";
    static final String DOCUMENT_FILL_TWO = "fill_two";
    static final String DOCUMENT_FILL_THREE = "fill_three";
    static final String DOCUMENT_FILL_FOUR = "fill_four";
    static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String DOCUMENT_FILL_ONE = "fill_one";
    public static Map<String, String> fillOneData = new HashMap<>();
    public static long currentId1;
    private static TextView clientIdTv;
    private static EditText enterName;
    private static EditText enterNumber;
    private static EditText enterGPNumber;
    private static EditText enterDate;
    private static EditText enterMakeName;
    private static EditText enterModelName;
    private static EditText enterHPrate;
    private static EditText enterSerialNumber;
    private DatePickerDialog datePickerDialog;
    private SharedPrefHelper sharedPref;

    public static void insertDataToFirestore_FillOne(Context context) {
        checkAndCreateCollection(context, () -> {
            if (validateFields(context)) {
                fillOneData.put("name", enterName.getText().toString());
                fillOneData.put("client_number", enterNumber.getText().toString());
                fillOneData.put("gp_number", enterGPNumber.getText().toString());
                fillOneData.put("gp_date", enterDate.getText().toString());
                fillOneData.put("make_name", enterMakeName.getText().toString());
                fillOneData.put("model_name", enterModelName.getText().toString());
                fillOneData.put("hp_rate", enterHPrate.getText().toString());
                fillOneData.put("serial_number", enterSerialNumber.getText().toString());

                db.collection(COLLECTION_NAME).document(String.valueOf(currentId1)).set(fillOneData)
                        .addOnSuccessListener(aVoid -> {
                            CollectionReference subCollectionRef = db.collection(COLLECTION_NAME)
                                    .document(String.valueOf(currentId1))
                                    .collection("pages");

                            subCollectionRef.document(DOCUMENT_FILL_ONE).set(fillOneData)
                                    .addOnSuccessListener(aVoid1 -> {
                                        Map<String, Object> idUpdate = new HashMap<>();
                                        idUpdate.put("lastId", currentId1);
                                        db.collection(COLLECTION_NAME).document(DOCUMENT_LAST_ID).set(idUpdate)
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Toast.makeText(context, "Data saved successfully!", Toast.LENGTH_SHORT).show();
                                                });
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });
    }

    private static boolean validateFields(Context context) {
        if (enterName.getText().toString().isEmpty() ||
                enterGPNumber.getText().toString().isEmpty() ||
                enterDate.getText().toString().isEmpty() ||
                enterMakeName.getText().toString().isEmpty() ||
                enterModelName.getText().toString().isEmpty() ||
                enterHPrate.getText().toString().isEmpty() ||
                enterSerialNumber.getText().toString().isEmpty()) {
            Toast.makeText(context, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static void clearFields_FillOne(Context context) {
        enterName.setText("");
        enterNumber.setText("");
        enterGPNumber.setText("");
        enterDate.setText("");
        enterMakeName.setText("");
        enterModelName.setText("");
        enterHPrate.setText("");
        enterSerialNumber.setText("");
        SharedPrefHelper.clearAll(); // Assuming there's a method in SharedPrefHelper to clear all SharedPreferences
        //Toast.makeText(context, "SharedPreferences cleared successfully!", Toast.LENGTH_SHORT).show();
    }

    private static void checkAndCreateCollection(Context context, Runnable onSuccess) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Collection does not exist, create the first document
                        Map<String, Object> initDoc = new HashMap<>();
                        initDoc.put("initial", true);
                        db.collection(COLLECTION_NAME).document("initialDoc").set(initDoc)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Collection created and initialized.", Toast.LENGTH_SHORT).show();
                                    onSuccess.run();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to create collection: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // Collection exists
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to check collection existence: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void initializeViews(View rootView) {
        enterName = rootView.findViewById(R.id.fill_one_enterName);
        enterNumber = rootView.findViewById(R.id.fill_one_enterNumber);
        enterGPNumber = rootView.findViewById(R.id.fill_one_enterGPNumber);
        enterDate = rootView.findViewById(R.id.fill_one_enterDate);
        enterMakeName = rootView.findViewById(R.id.fill_one_enterMakeName);
        enterModelName = rootView.findViewById(R.id.fill_one_enterModelName);
        enterHPrate = rootView.findViewById(R.id.fill_one_enterHPrate);
        enterSerialNumber = rootView.findViewById(R.id.fill_one_enterSerialNumber);
        clientIdTv = rootView.findViewById(R.id.clientId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_one, container, false);
        sharedPref = new SharedPrefHelper(requireContext()); // Initialize SharedPrefHelper
        initializeViews(rootView);
        initDatePicker();
        fetchCurrentId(requireContext());
        populateFieldsFromSharedPreferences(); // Populate fields with shared preferences
        return rootView;
    }

    private void fetchCurrentId(Context context) {
        checkAndCreateCollection(context, () -> {
            db.collection(COLLECTION_NAME)
                    .document(DOCUMENT_LAST_ID)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.getLong("lastId") != null) {
                            currentId1 = documentSnapshot.getLong("lastId") + 1;
                            Toast.makeText(context, "Current ID: " + currentId1, Toast.LENGTH_SHORT).show();
                        } else {
                            currentId1 = 2001;
                            Toast.makeText(context, "No ID found. Starting with 2001.", Toast.LENGTH_SHORT).show();
                        }
                        clientIdTv.setText("ID: " + currentId1);
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to fetch client ID", Toast.LENGTH_LONG).show());
        });
    }

    private void initDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        datePickerDialog = new DatePickerDialog(requireActivity(), this, year, month, day);

        enterDate.setOnClickListener(v -> datePickerDialog.show());
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        month += 1;
        String date = dayOfMonth + "/" + month + "/" + year;
        enterDate.setText(date);
    }

    private void saveValuesToSharedPreferences() {
        sharedPref.saveString("name", enterName.getText().toString());
        sharedPref.saveString("client_number", enterNumber.getText().toString());
        sharedPref.saveString("gp_number", enterGPNumber.getText().toString());
        sharedPref.saveString("gp_date", enterDate.getText().toString());
        sharedPref.saveString("make_name", enterMakeName.getText().toString());
        sharedPref.saveString("model_name", enterModelName.getText().toString());
        sharedPref.saveString("hp_rate", enterHPrate.getText().toString());
    }

    private void populateFieldsFromSharedPreferences() {
        enterName.setText(sharedPref.getString("name", ""));
        enterNumber.setText(sharedPref.getString("client_number", ""));
        enterGPNumber.setText(sharedPref.getString("gp_number", ""));
        enterDate.setText(sharedPref.getString("gp_date", ""));
        enterMakeName.setText(sharedPref.getString("make_name", ""));
        enterModelName.setText(sharedPref.getString("model_name", ""));
        enterHPrate.setText(sharedPref.getString("hp_rate", ""));
        enterSerialNumber.setText(sharedPref.getString("serial_number", ""));
    }

    @Override
    public void onStop() {
        super.onStop();
        saveValuesToSharedPreferences();
    }
}
