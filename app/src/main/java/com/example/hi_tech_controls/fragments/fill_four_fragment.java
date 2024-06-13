package com.example.hi_tech_controls.fragments;

import static com.example.hi_tech_controls.fragments.fill_one_fragment.COLLECTION_NAME;
import static com.example.hi_tech_controls.fragments.fill_one_fragment.DOCUMENT_FILL_FOUR;
import static com.example.hi_tech_controls.fragments.fill_one_fragment.DOCUMENT_LAST_ID;
import static com.example.hi_tech_controls.fragments.fill_one_fragment.currentId1;
import static com.example.hi_tech_controls.fragments.fill_one_fragment.db;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;
import com.google.firebase.firestore.CollectionReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class fill_four_fragment extends Fragment {
    // Static Map to store form data
    public static Map<String, String> fillFourData = new HashMap<>();
    private static Spinner selectEmply;
    private static CheckBox checkbox1HP;
    private static CheckBox checkbox10HP;
    private static CheckBox checkbox30HP;
    private static EditText OnDisplay_text;
    private static EditText OnClamp_text;
    private static CheckBox checkboxAMP_U;
    private static CheckBox checkboxAMP_V;
    private static CheckBox checkboxAMP_W;
    private static EditText DC_DISP_text;
    private static EditText DC_MET_text;
    private static EditText OUTPUT_DISP_text;
    private static EditText OUTPUT_MET_text;
    private static EditText enterRH_text;
    private static EditText enterReplayOP_text;
    private static EditText enterFANOpr_text;
    private static EditText enterBODYCondition_text;
    private static EditText enterIOcheck_text;
    private static EditText enterClean_Text;
    private static EditText enterPramCopy_Text;
    private SharedPrefHelper sharedPref;

    public static void insertDataToFirestore_FillFour(Context context) {
        fillFourData.put("select_emp", selectEmply.getSelectedItem().toString());

        fillFourData.put("checkbox_1HP", String.valueOf(checkbox1HP.isChecked()));
        fillFourData.put("checkbox_10HP", String.valueOf(checkbox10HP.isChecked()));
        fillFourData.put("checkbox_30HP", String.valueOf(checkbox30HP.isChecked()));

        fillFourData.put("On_Display", OnDisplay_text.getText().toString());
        fillFourData.put("On_Clamp", OnClamp_text.getText().toString());

        fillFourData.put("checkbox_u", String.valueOf(checkboxAMP_U.isChecked()));
        fillFourData.put("checkbox_v", String.valueOf(checkboxAMP_V.isChecked()));
        fillFourData.put("checkbox_w", String.valueOf(checkboxAMP_W.isChecked()));

        fillFourData.put("dc_dsp", DC_DISP_text.getText().toString());
        fillFourData.put("dc_met", DC_MET_text.getText().toString());

        fillFourData.put("output_dsp", OUTPUT_DISP_text.getText().toString());
        fillFourData.put("output_met", OUTPUT_MET_text.getText().toString());

        fillFourData.put("enter_RH", enterRH_text.getText().toString());
        fillFourData.put("enterReplayOP", enterReplayOP_text.getText().toString());
        fillFourData.put("enter_FANOpr", enterFANOpr_text.getText().toString());
        fillFourData.put("enter_BODY_Condition", enterBODYCondition_text.getText().toString());
        fillFourData.put("enter_io_check", enterIOcheck_text.getText().toString());
        fillFourData.put("enterClean", enterClean_Text.getText().toString());
        fillFourData.put("enterPramCopy", enterPramCopy_Text.getText().toString());

        // Save data to Firestore
        db.collection(COLLECTION_NAME).document(String.valueOf(currentId1)).set(fillFourData)
                .addOnSuccessListener(aVoid -> {
                    // Create sub-collection "pages" and save "fill_three" document
                    CollectionReference subCollectionRef = db.collection(COLLECTION_NAME)
                            .document(String.valueOf(currentId1))
                            .collection("pages");

                    subCollectionRef.document(DOCUMENT_FILL_FOUR).set(fillFourData)
                            .addOnSuccessListener(aVoid1 -> {
                                // Update lastId document
                                Map<String, Object> idUpdate = new HashMap<>();
                                idUpdate.put("lastId", currentId1);
                                db.collection(COLLECTION_NAME).document(DOCUMENT_LAST_ID).set(idUpdate)
                                        .addOnSuccessListener(aVoid2 -> {
                                            Toast.makeText(context, "Data saved successfully fill_four!!", Toast.LENGTH_SHORT).show();
                                        });
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initializing   View elements using findViewById on the view
        selectEmply = view.findViewById(R.id.fill_four_selectEmply);
        checkbox1HP = view.findViewById(R.id.fill_four_checkbox1HP);
        checkbox10HP = view.findViewById(R.id.fill_four_checkbox10HP);
        checkbox30HP = view.findViewById(R.id.fill_four_checkbox30HP);
        OnDisplay_text = view.findViewById(R.id.fill_four_OnDisplay_text);
        OnClamp_text = view.findViewById(R.id.fill_four_OnClamp_text);
        checkboxAMP_U = view.findViewById(R.id.fill_four_checkboxAMP_U);
        checkboxAMP_V = view.findViewById(R.id.fill_four_checkboxAMP_V);
        checkboxAMP_W = view.findViewById(R.id.fill_four_checkboxAMP_W);
        DC_DISP_text = view.findViewById(R.id.fill_four_DC_DISP_text);
        DC_MET_text = view.findViewById(R.id.fill_four_DC_MET_text);
        OUTPUT_DISP_text = view.findViewById(R.id.fill_four_OUTPUT_DISP_text);
        OUTPUT_MET_text = view.findViewById(R.id.fill_four_OUTPUT_MET_text);
        enterRH_text = view.findViewById(R.id.fill_four_enterRH_text);
        enterReplayOP_text = view.findViewById(R.id.fill_four_enterReplayOP_text);
        enterFANOpr_text = view.findViewById(R.id.fill_four_enterFANOpr_text);
        enterBODYCondition_text = view.findViewById(R.id.fill_four_enterBODYCondition_text);
        enterIOcheck_text = view.findViewById(R.id.fill_four_enterIOcheck_text);
        enterClean_Text = view.findViewById(R.id.fill_four_enterClean_Text);
        enterPramCopy_Text = view.findViewById(R.id.fill_four_enterPramCopy_Text);


        // Load saved values and set them to the UI elements
        String selectedEmployee = sharedPref.getString("select_emp", "");
        if (!selectedEmployee.isEmpty()) {
            // Find the position of the selectedEmployee in the employees array
            int position = getPositionOfEmployee(selectedEmployee);
            if (position >= 0) {
                selectEmply.setSelection(position);
            }
        }
        // checkbox
        checkbox1HP.setChecked(sharedPref.getBoolean("checkbox_1HP", false));
        checkbox10HP.setChecked(sharedPref.getBoolean("checkbox_10HP", false));
        checkbox30HP.setChecked(sharedPref.getBoolean("checkbox_30HP", false));

        //edit text
        OnDisplay_text.setText(sharedPref.getString("On_Display", ""));
        OnClamp_text.setText(sharedPref.getString("On_Clamp", ""));

        //checkbox
        checkboxAMP_U.setChecked(sharedPref.getBoolean("checkbox_u", false));
        checkboxAMP_V.setChecked(sharedPref.getBoolean("checkbox_v", false));
        checkboxAMP_W.setChecked(sharedPref.getBoolean("checkbox_w", false));

        //edit text
        DC_DISP_text.setText(sharedPref.getString("dc_dsp", ""));
        DC_MET_text.setText(sharedPref.getString("dc_met", ""));
        OUTPUT_DISP_text.setText(sharedPref.getString("output_dsp", ""));
        OUTPUT_MET_text.setText(sharedPref.getString("output_met", ""));

        enterRH_text.setText(sharedPref.getString("enter_RH", ""));
        enterReplayOP_text.setText(sharedPref.getString("enterReplayOP", ""));
        enterFANOpr_text.setText(sharedPref.getString("enter_FANOpr", ""));
        enterBODYCondition_text.setText(sharedPref.getString("enter_BODY_Condition", ""));
        enterIOcheck_text.setText(sharedPref.getString("enter_io_check", ""));
        enterClean_Text.setText(sharedPref.getString("enterClean", ""));
        enterPramCopy_Text.setText(sharedPref.getString("enterPramCopy", ""));
    }


    // Helper method to get the position of the selected employee in the Spinner
    private int getPositionOfEmployee(String selectedEmployee) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) selectEmply.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (Objects.equals(adapter.getItem(i), selectedEmployee)) {
                return i;
            }
        }
        return -1; // Employee not found in the Spinner
    }

    private void setUpSpinner(View rootView) {
        Spinner spinner = rootView.findViewById(R.id.fill_four_selectEmply);
        String[] employees = {"Select employee name", "employee 1", "employee 2", "employee 3", "employee 4"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, employees);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                handleSpinnerItemSelected(selectedItemView, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    private void handleSpinnerItemSelected(View selectedItemView, int position) {
        if (selectedItemView instanceof TextView) {
            if (position == 0) {
                ((TextView) selectedItemView).setTextColor(getResources().getColor(R.color.grey));
            } else {
                ((TextView) selectedItemView).setTextColor(getResources().getColor(R.color.blue));
            }
        }
    }

    private void saveValuesToSharedPreferences() {
        sharedPref.saveString("select_emp", selectEmply.getSelectedItem().toString());

        sharedPref.saveBoolean("checkbox_1HP", checkbox1HP.isChecked());
        sharedPref.saveBoolean("checkbox_10HP", checkbox10HP.isChecked());
        sharedPref.saveBoolean("checkbox_30HP", checkbox30HP.isChecked());

        sharedPref.saveString("On_Display", OnDisplay_text.getText().toString());
        sharedPref.saveString("On_Clamp", OnClamp_text.getText().toString());

        sharedPref.saveBoolean("checkbox_u", checkboxAMP_U.isChecked());
        sharedPref.saveBoolean("checkbox_v", checkboxAMP_V.isChecked());
        sharedPref.saveBoolean("checkbox_w", checkboxAMP_W.isChecked());

        sharedPref.saveString("dc_dsp", DC_DISP_text.getText().toString());
        sharedPref.saveString("dc_met", DC_MET_text.getText().toString());

        sharedPref.saveString("output_dsp", OUTPUT_DISP_text.getText().toString());
        sharedPref.saveString("output_met", OUTPUT_MET_text.getText().toString());

        sharedPref.saveString("enter_RH", enterRH_text.getText().toString());
        sharedPref.saveString("enterReplayOP", enterReplayOP_text.getText().toString());
        sharedPref.saveString("enter_FANOpr", enterFANOpr_text.getText().toString());
        sharedPref.saveString("enter_BODY_Condition", enterBODYCondition_text.getText().toString());
        sharedPref.saveString("enter_io_check", enterIOcheck_text.getText().toString());
        sharedPref.saveString("enterClean", enterClean_Text.getText().toString());
        sharedPref.saveString("enterPramCopy", enterPramCopy_Text.getText().toString());
    }

    public static void clearFields_FillFour(Context context) {
        // Clear Spinner selection
        selectEmply.setSelection(0);

        // Clear CheckBox selections
        checkbox1HP.setChecked(false);
        checkbox10HP.setChecked(false);
        checkbox30HP.setChecked(false);
        checkboxAMP_U.setChecked(false);
        checkboxAMP_V.setChecked(false);
        checkboxAMP_W.setChecked(false);

        // Clear EditText fields
        OnDisplay_text.setText("");
        OnClamp_text.setText("");
        DC_DISP_text.setText("");
        DC_MET_text.setText("");
        OUTPUT_DISP_text.setText("");
        OUTPUT_MET_text.setText("");
        enterRH_text.setText("");
        enterReplayOP_text.setText("");
        enterFANOpr_text.setText("");
        enterBODYCondition_text.setText("");
        enterIOcheck_text.setText("");
        enterClean_Text.setText("");
        enterPramCopy_Text.setText("");
    }


    @Override
    public void onStop() {
        super.onStop();
        saveValuesToSharedPreferences();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_four, container, false);
        sharedPref = new SharedPrefHelper(requireContext());

        fill_three_fragment.insertDataToFirestore_FillThree(requireContext());
        //insertDataToFirestore_FillFour(requireContext());
        setUpSpinner(rootView);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        insertDataToFirestore_FillFour(requireContext());
        fill_one_fragment.clearFields_FillOne(requireContext());
        //fill_two_fragment.clearFields_FillTwo(requireContext());
        fill_three_fragment.clearFields_FillThree(requireContext());
        fill_four_fragment.clearFields_FillFour(requireContext());
    }
}