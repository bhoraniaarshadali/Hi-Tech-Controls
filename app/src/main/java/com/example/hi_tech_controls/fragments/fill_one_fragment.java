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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class fill_one_fragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";
    private static final String DOCUMENT_LAST_ID = "last_id";
    private static final String DOCUMENT_FILL_ONE = "fill_one";
    private static final String DOCUMENT_FILL_TWO = "fill_two";
    private static final String DOCUMENT_FILL_THREE = "fill_three";
    private static final String DOCUMENT_FILL_FOUR = "fill_four";

    private static TextView clientIdTv;
    private static long currentId1;
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static EditText enterName;
    private static EditText enterNumber;
    private static EditText enterGPNumber;
    private static EditText enterDate;
    private static EditText enterMakeName;
    private static EditText enterModelName;
    private static EditText enterHPrate;
    private static EditText enterSerialNumber;
    private DatePickerDialog datePickerDialog;

    // Global data map for storing form data
    private static Map<String, String> fillOneData;

    public static void insertDataToFirestore_FillOne(Context context) {
        if (validateFields(context)) {
            // Populate the global data map
            fillOneData.put("name", enterName.getText().toString());
            fillOneData.put("client_number", enterNumber.getText().toString());
            fillOneData.put("gp_number", enterGPNumber.getText().toString());
            fillOneData.put("gp_date", enterDate.getText().toString());
            fillOneData.put("make_name", enterMakeName.getText().toString());
            fillOneData.put("model_name", enterModelName.getText().toString());
            fillOneData.put("hp_rate", enterHPrate.getText().toString());
            fillOneData.put("serial_number", enterSerialNumber.getText().toString());

            // Create document with specific ID
            db.collection(COLLECTION_NAME).document(String.valueOf(currentId1)).set(fillOneData)
                    .addOnSuccessListener(aVoid -> {
                        CollectionReference subCollectionRef = db.collection(COLLECTION_NAME)
                                .document(String.valueOf(currentId1))
                                .collection("pages");

                        // Create and set data for "fill_one"
                        subCollectionRef.document(DOCUMENT_FILL_ONE).set(fillOneData)
                                .addOnSuccessListener(aVoid1 -> {
                                    // Create and set data for "fill_two"
                                    subCollectionRef.document(DOCUMENT_FILL_TWO).set(fill_two_fragment.fillTwoData)
                                            .addOnSuccessListener(aVoid2 -> {
                                                // Create and set data for "fill_three"
                                                subCollectionRef.document(DOCUMENT_FILL_THREE).set(fillOneData)
                                                        .addOnSuccessListener(aVoid3 -> {
                                                            // Create and set data for "fill_four"
                                                            subCollectionRef.document(DOCUMENT_FILL_FOUR).set(fillOneData)
                                                                    .addOnSuccessListener(aVoid4 -> {
                                                                        clientIdTv.setText("ID: " + currentId1);

                                                                        Map<String, Object> idUpdate = new HashMap<>();
                                                                        idUpdate.put("lastId", currentId1);
                                                                        db.collection(COLLECTION_NAME).document(DOCUMENT_LAST_ID).set(idUpdate)
                                                                                .addOnSuccessListener(aVoid5 -> clearInputFields());
                                                                    });
                                                        });
                                            });
                                });
                    });
        }
    }

    private static boolean validateFields(Context context) {
        if (enterName.getText().toString().isEmpty() ||
                //enterNumber.getText().toString().isEmpty() ||
                enterGPNumber.getText().toString().isEmpty() ||
                enterDate.getText().toString().isEmpty() ||
                enterMakeName.getText().toString().isEmpty() ||
                enterModelName.getText().toString().isEmpty() ||
                enterHPrate.getText().toString().isEmpty() ||
                enterSerialNumber.getText().toString().isEmpty()) {
            Toast.makeText(enterName.getContext(), "All fields must be filled", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private static void clearInputFields() {
        enterName.setText("");
        enterNumber.setText("");
        enterGPNumber.setText("");
        enterDate.setText("");
        enterMakeName.setText("");
        enterModelName.setText("");
        enterHPrate.setText("");
        enterSerialNumber.setText("");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_one, container, false);
        initializeViews(rootView);
        initDatePicker();
        fetchCurrentId(requireContext());
        return rootView;
    }

    private void initializeViews(View rootView) {
        // Initialize global data map
        fillOneData = new HashMap<>();

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

    private void fetchCurrentId(Context context) {
        db.collection(COLLECTION_NAME)
                .document(DOCUMENT_LAST_ID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getLong("lastId") != null) {
                        currentId1 = documentSnapshot.getLong("lastId") + 1;
                        Toast.makeText(requireContext(), "Current ID: " + currentId1, Toast.LENGTH_SHORT).show();
                    } else {
                        currentId1 = 2001; // Start with 2001 if no ID exists
                        Toast.makeText(requireContext(), "No ID found. Starting with 2001.", Toast.LENGTH_SHORT).show();
                    }
                    clientIdTv.setText("ID: " + currentId1);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to fetch client ID", Toast.LENGTH_LONG).show());
    }

    private void initDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        datePickerDialog = new DatePickerDialog(requireActivity(), (view, year1, month1, dayOfMonth) -> {
            month1 += 1;
            String date = dayOfMonth + "/" + month1 + "/" + year1;
            enterDate.setText(date);
        }, year, month, day);

        enterDate.setOnClickListener(v -> datePickerDialog.show());
    }

    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // Not needed for now
    }
}