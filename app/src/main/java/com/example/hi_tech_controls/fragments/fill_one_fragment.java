package com.example.hi_tech_controls.fragments;

import android.app.DatePickerDialog;
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

    public static void insertDataToFirestore() {
        Map<String, String> fillOneData = new HashMap<>();
        fillOneData.put("name", enterName.getText().toString());
        fillOneData.put("client_number", enterNumber.getText().toString());
        fillOneData.put("gp_number", enterGPNumber.getText().toString());
        fillOneData.put("gp_date", enterDate.getText().toString());
        fillOneData.put("make_name", enterMakeName.getText().toString());
        fillOneData.put("model_name", enterModelName.getText().toString());
        fillOneData.put("hp_rate", enterHPrate.getText().toString());
        fillOneData.put("serial_number", enterSerialNumber.getText().toString());

        CollectionReference mainCollectionRef = db.collection("04June2024");
        mainCollectionRef.add(fillOneData)
                .addOnSuccessListener(documentReference -> {
                    //long newId = currentId1 + 1;
                    long newId = currentId1;


                    CollectionReference subCollectionRef = mainCollectionRef
                            .document(String.valueOf(newId))
                            .collection("pages");

                    subCollectionRef.document("fill_one").set(fillOneData)
                            .addOnSuccessListener(aVoid -> {
                                currentId1 = newId;
                                clientIdTv.setText("ID: " + currentId1);

                                Map<String, Object> idUpdate = new HashMap<>();
                                idUpdate.put("lastId", currentId1);
                                db.collection("04June2024").document("last_id").set(idUpdate)
                                        .addOnSuccessListener(aVoid1 -> clearInputFields());
                            });
                });
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
        fetchCurrentId();
        return rootView;
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

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // Not needed for now
    }

    private void fetchCurrentId() {
        db.collection("04June2024")
                .document("last_id")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getLong("lastId") != null) {
                        currentId1 = documentSnapshot.getLong("lastId") + 1;
                        Toast.makeText(requireContext(), "Current ID: " + currentId1, Toast.LENGTH_SHORT).show();
                    } else {
                        currentId1 = 1; // Start with 1 if no ID exists
                    }
                    clientIdTv.setText("ID: " + currentId1);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to fetch client ID", Toast.LENGTH_LONG).show());
    }

}
