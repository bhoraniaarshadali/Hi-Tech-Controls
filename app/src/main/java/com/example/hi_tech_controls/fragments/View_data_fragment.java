package com.example.hi_tech_controls.fragments;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget. RadioButton;

import android.widget.TextView;

import com.example.hi_tech_controls.Print_PDF;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;

public class View_data_fragment extends Fragment {


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_view_data_fragement, container, false);
        // Find the button in the fragment's layout
        Button generatePdfButton = rootView.findViewById(R.id.printpdf); // Replace with your button ID


// Initialize the pdfCreator when the fragment is created


        // Initialize SharedPrefHelper
        SharedPrefHelper sharedPref = new SharedPrefHelper(requireContext());

        // Initialize TextViews
        TextView clientNameTextView = rootView.findViewById(R.id.fill_one_enterName);
        TextView clientNumberTextView = rootView.findViewById(R.id.fill_one_enterNumber);
        TextView gpNumberTextView = rootView.findViewById(R.id.fill_one_enterGPNumber);
        TextView gpDateTextView = rootView.findViewById(R.id.fill_one_enterDate);
        TextView makeNameTextView = rootView.findViewById(R.id.fill_one_enterMakeName);
        TextView modelNameTextView = rootView.findViewById(R.id.fill_one_enterModelName);
        TextView hpRateTextView = rootView.findViewById(R.id.fill_one_enterHPrate);
        TextView serialNumberTextView = rootView.findViewById(R.id.fill_one_enterSerialNumber);

        // Retrieve stored values and set them to TextViews
        clientNameTextView.setText(sharedPref.getString("name", ""));
        clientNumberTextView.setText(sharedPref.getString("number", ""));
        gpNumberTextView.setText(sharedPref.getString("gp_number", ""));
        gpDateTextView.setText(sharedPref.getString("date", ""));
        makeNameTextView.setText(sharedPref.getString("make_name", ""));
        modelNameTextView.setText(sharedPref.getString("model_name", ""));
        hpRateTextView.setText(sharedPref.getString("hp_rate", ""));
        serialNumberTextView.setText(sharedPref.getString("serial_number", ""));

        clientNameTextView.setEnabled(false);
        clientNumberTextView.setEnabled(false);
        gpNumberTextView.setEnabled(false);
        gpDateTextView.setEnabled(false);
        makeNameTextView.setEnabled(false);
        modelNameTextView.setEnabled(false);
        hpRateTextView.setEnabled(false);
        serialNumberTextView.setEnabled(false);

        // FRAGMENT 2
        // Initialize TextViews
        TextView frequeceTextView = rootView.findViewById(R.id.fill_two_enterFRrate);
        RadioButton RadioButtonLocal = rootView.findViewById(R.id.radioButtonLocal);
        RadioButton RadioButtonRemote = rootView.findViewById(R.id.radioButtonRemote);
        RadioButton RadioButtoncomm = rootView.findViewById(R.id.radioButtonComm);
        RadioButton RadioButtonDIODE = rootView.findViewById(R.id.radioButtonDIODE);
        RadioButton RadioButtonSCR = rootView.findViewById(R.id.radioButtonSCR);

        CheckBox inputPOSU = rootView.findViewById(R.id.detail_checkbox_U);
        CheckBox inputPOSV = rootView.findViewById(R.id.detail_checkbox_V);
        CheckBox inputPOSW = rootView.findViewById(R.id.detail_checkbox_W);
        CheckBox inputNEGU = rootView.findViewById(R.id.detail_neg_checkboxU);
        CheckBox inputNEGV = rootView.findViewById(R.id.detail_neg_checkboxV);
        CheckBox inputNEGW = rootView.findViewById(R.id.detail_neg_checkboxW);

        CheckBox outputPOSU = rootView.findViewById(R.id.detail_output_POS_U);
        CheckBox outputPOSV = rootView.findViewById(R.id.detail_output_POS_V);
        CheckBox outputPOSW = rootView.findViewById(R.id.detail_output_POS_W);
        CheckBox outputNEGU = rootView.findViewById(R.id.detail_output_NEG_U);
        CheckBox outputNEGV = rootView.findViewById(R.id.detail_output_NEG_V);
        CheckBox outputNEGW = rootView.findViewById(R.id.detail_output_NEG_W);

        TextView clientobsTextView = rootView.findViewById(R.id.client_observationfield);
        TextView employeeTextView = rootView.findViewById(R.id.emp_observationfield);
        TextView lastfaultTextView = rootView.findViewById(R.id.lastfault_observationfield);

        // Retrieve stored values and set them to TextViews
        frequeceTextView.setText(sharedPref.getString("fr_rate", ""));
        clientobsTextView.setText(sharedPref.getString("client_obs", ""));
        employeeTextView.setText(sharedPref.getString("our_obs", ""));
        lastfaultTextView.setText(sharedPref.getString("last_fault", ""));

        boolean localcheked = sharedPref.getBoolean("local_radio_checked", false);
        boolean remotecheked = sharedPref.getBoolean("remote_radio_checked", false);
        boolean commcheked = sharedPref.getBoolean("comm_radio_checked", false);
        boolean DIODEchecked = sharedPref.getBoolean("diode_radio_checked", false);
        boolean SCRchecked = sharedPref.getBoolean("scr_radio_checked", false);

        RadioButtonLocal.setChecked(localcheked);
        RadioButtonRemote.setChecked(remotecheked);
        RadioButtoncomm.setChecked(commcheked);
        RadioButtonDIODE.setChecked(DIODEchecked);
        RadioButtonSCR.setChecked(SCRchecked);

        // CHECKBOX INPUT POS & NEG input
        boolean input_POS_checkbox_U_checked = sharedPref.getBoolean("input_pos_checkbox_U", false);
        boolean input_POS_checkbox_V_checked = sharedPref.getBoolean("input_pos_checkbox_V", false);
        boolean input_POS_checkbox_W_checked = sharedPref.getBoolean("input_pos_checkbox_W", false);
        boolean input_NEG_checkbox_U_checked = sharedPref.getBoolean("input_neg_checkbox_U", false);
        boolean input_NEG_checkbox_V_checked = sharedPref.getBoolean("input_neg_checkbox_V", false);
        boolean input_NEG_checkbox_W_checked = sharedPref.getBoolean("input_neg_checkbox_W", false);
        // OUTPUT
        boolean output_POS_checkbox_U_checked = sharedPref.getBoolean("output_pos_checkbox_U", false);
        boolean output_POS_checkbox_V_checked = sharedPref.getBoolean("output_pos_checkbox_V", false);
        boolean output_POS_checkbox_W_checked = sharedPref.getBoolean("output_pos_checkbox_W", false);
        boolean output_NEG_checkbox_U_checked = sharedPref.getBoolean("output_neg_checkbox_U", false);
        boolean output_NEG_checkbox_V_checked = sharedPref.getBoolean("output_neg_checkbox_V", false);
        boolean output_NEG_checkbox_W_checked = sharedPref.getBoolean("output_neg_checkbox_W", false);

        // TO SET THE CHECKED VALUE AND TO SEE
        inputPOSU.setChecked(input_POS_checkbox_U_checked);
        inputPOSV.setChecked(input_POS_checkbox_V_checked);
        inputPOSW.setChecked(input_POS_checkbox_W_checked);
        inputNEGU.setChecked(input_NEG_checkbox_U_checked);
        inputNEGV.setChecked(input_NEG_checkbox_V_checked);
        inputNEGW.setChecked(input_NEG_checkbox_W_checked);
        outputPOSU.setChecked(output_POS_checkbox_U_checked);
        outputPOSV.setChecked(output_POS_checkbox_V_checked);
        outputPOSW.setChecked(output_POS_checkbox_W_checked);
        outputNEGU.setChecked(output_NEG_checkbox_U_checked);
        outputNEGV.setChecked(output_NEG_checkbox_V_checked);
        outputNEGW.setChecked(output_NEG_checkbox_W_checked);

        inputPOSU.setEnabled(false);
        inputPOSV.setEnabled(false);
        inputPOSW.setEnabled(false);
        inputNEGU.setEnabled(false);
        inputNEGV.setEnabled(false);
        inputNEGW.setEnabled(false);
        outputPOSU.setEnabled(false);
        outputPOSV.setEnabled(false);
        outputPOSW.setEnabled(false);
        outputNEGU.setEnabled(false);
        outputNEGV.setEnabled(false);
        outputNEGW.setEnabled(false);

        TextView filltwoemp = rootView.findViewById(R.id.fill_two_selectEmply);
        TextView fillthreeemp = rootView.findViewById(R.id.fill_three_selectEmply);
        TextView fillfouremp = rootView.findViewById(R.id.fill_four_selectEmply);
        String emplspinner = sharedPref.getString("select_emp", "");
        String emplspinner2 = sharedPref.getString("select_emp", "");
        String emplspinner3 = sharedPref.getString("select_emp", "");

        filltwoemp.setText(emplspinner);
        fillthreeemp.setText(emplspinner2);
        fillfouremp.setText(emplspinner3);

        // FRAGMENT 3
        // Retrieve stored values and set them to TextViews
        String remarkText = sharedPref.getString("enter_first_remarks", "");
        boolean CapacitorChecked = sharedPref.getBoolean("checkboxCapacitor", false);
        boolean DisplayChecked = sharedPref.getBoolean("checkboxDisplay", false);
        boolean FANChecked = sharedPref.getBoolean("checkboxFAN", false);
        boolean CCChecked = sharedPref.getBoolean("checkboxCC", false);

        boolean Repair_checkboxOne = sharedPref.getBoolean("repair_checkboxOne", false);
        boolean Repair_checkboxTwo = sharedPref.getBoolean("repair_checkboxTwo", false);
        boolean Repair_checkboxThree = sharedPref.getBoolean("repair_checkboxThree", false);
        boolean Repair_checkboxFour = sharedPref.getBoolean("repair_checkboxFour", false);
        boolean Repair_checkboxFive = sharedPref.getBoolean("repair_checkboxFive", false);
        boolean Repair_checkboxSix = sharedPref.getBoolean("repair_checkboxSix", false);

        boolean Replace_checkboxOne = sharedPref.getBoolean("replace_checkboxOne", false);
        boolean Replace_checkboxTwo = sharedPref.getBoolean("replace_checkboxTwo", false);
        boolean Replace_checkboxThree = sharedPref.getBoolean("replace_checkboxThree", false);
        boolean Replace_checkboxFour = sharedPref.getBoolean("replace_checkboxFour", false);
        boolean Replace_checkboxFive = sharedPref.getBoolean("replace_checkboxFive", false);
        boolean Replace_checkboxSix = sharedPref.getBoolean("replace_checkboxSix", false);
        boolean Replace_checkboxSeven = sharedPref.getBoolean("replace_checkboxSeven", false);
        boolean Replace_checkboxEight = sharedPref.getBoolean("replace_checkboxEight", false);
        boolean Replace_checkboxNine = sharedPref.getBoolean("replace_checkboxNine", false);

        boolean trialOne = sharedPref.getBoolean("checkboxTrial1", false);
        boolean trialTwo = sharedPref.getBoolean("checkboxTrial2", false);

        // Initialize TextViews
        // Find the checkboxes in the layout IN DETAIL FRAGMENT
        CheckBox CapacitorCheckbox = rootView.findViewById(R.id.CB_Capasitor);
        CheckBox DisplayCheckbox = rootView.findViewById(R.id.CB_Display);
        CheckBox FANCheckbox = rootView.findViewById(R.id.CB_FAN);
        CheckBox CCCheckbox = rootView.findViewById(R.id.CB_CC);
        TextView remarkTextView = rootView.findViewById(R.id.detail_entertext);
        CheckBox repairOne = rootView.findViewById(R.id.CB_SMPS);
        CheckBox repairTwo = rootView.findViewById(R.id.CB_CtStation);
        CheckBox repairThree = rootView.findViewById(R.id.CB_Firing);
        CheckBox repairFour = rootView.findViewById(R.id.CB_ControlCard);
        CheckBox repairFive = rootView.findViewById(R.id.CB_DCstation);
        CheckBox repairSix = rootView.findViewById(R.id.CB_Other);
        CheckBox replaceOne = rootView.findViewById(R.id.CB_IGBT_Replace);
        CheckBox replaceTwo = rootView.findViewById(R.id.CB_CT_Replace);
        CheckBox replaceThree = rootView.findViewById(R.id.CB_Display_Replace);
        CheckBox replaceFour = rootView.findViewById(R.id.CB_Rectifier_Replace);
        CheckBox replaceFive = rootView.findViewById(R.id.CB_Controlcard_Replace);
        CheckBox replaceSix = rootView.findViewById(R.id.CB_fAN_Replace);
        CheckBox replaceSeven = rootView.findViewById(R.id.CB_Fuse_Replace);
        CheckBox replaceEight = rootView.findViewById(R.id.CB_Powercard_Replace);
        CheckBox replaceNine = rootView.findViewById(R.id.CB_Other_Replace);
        CheckBox Trial1 = rootView.findViewById(R.id.CB_TRIAL1);
        CheckBox Trial2 = rootView.findViewById(R.id.CB_TRIAL2);

        // Set the checkbox states based on the values retrieved from SharedPreferences
        CapacitorCheckbox.setChecked(CapacitorChecked);
        DisplayCheckbox.setChecked(DisplayChecked);
        FANCheckbox.setChecked(FANChecked);
        CCCheckbox.setChecked(CCChecked);
        repairOne.setChecked(Repair_checkboxOne);
        repairTwo.setChecked(Repair_checkboxTwo);
        repairThree.setChecked(Repair_checkboxThree);
        repairFour.setChecked(Repair_checkboxFour);
        repairFive.setChecked(Repair_checkboxFive);
        repairSix.setChecked(Repair_checkboxSix);
        replaceOne.setChecked(Replace_checkboxOne);
        replaceTwo.setChecked(Replace_checkboxTwo);
        replaceThree.setChecked(Replace_checkboxThree);
        replaceFour.setChecked(Replace_checkboxFour);
        replaceFive.setChecked(Replace_checkboxFive);
        replaceSix.setChecked(Replace_checkboxSix);
        replaceSeven.setChecked(Replace_checkboxSeven);
        replaceEight.setChecked(Replace_checkboxEight);
        replaceNine.setChecked(Replace_checkboxNine);
        Trial1.setChecked(trialOne);
        Trial2.setChecked(trialTwo);
        remarkTextView.setText(remarkText);
        CapacitorCheckbox.setEnabled(false);
        DisplayCheckbox.setEnabled(false);
        FANCheckbox.setEnabled(false);
        CCCheckbox.setEnabled(false);
        repairOne.setEnabled(false);
        repairTwo.setEnabled(false);
        repairThree.setEnabled(false);
        repairFour.setEnabled(false);
        repairFive.setEnabled(false);
        repairSix.setEnabled(false);
        replaceOne.setEnabled(false);
        replaceTwo.setEnabled(false);
        replaceThree.setEnabled(false);
        replaceFour.setEnabled(false);
        replaceFive.setEnabled(false);
        replaceSix.setEnabled(false);
        replaceSeven.setEnabled(false);
        replaceEight.setEnabled(false);
        replaceNine.setEnabled(false);
        Trial1.setEnabled(false);
        Trial2.setEnabled(false);

        // FRAGMENT 4
        boolean MotorDetails1 = sharedPref.getBoolean("checkbox_1HP", false);
        boolean MotorDetails2 = sharedPref.getBoolean("checkbox_10HP", false);
        boolean MotorDetails3 = sharedPref.getBoolean("checkbox_30HP", false);
        boolean AMPDetailsU = sharedPref.getBoolean("checkbox_u", false);
        boolean AMPDetailsV = sharedPref.getBoolean("checkbox_v", false);
        boolean AMPDetailsW = sharedPref.getBoolean("checkbox_w", false);

        // Initialize TextViews
        // Find the checkboxes in the layout IN DETAIL FRAGMENT
        CheckBox HP1Checkbox = rootView.findViewById(R.id.CB_1HP);
        CheckBox HP10Checkbox = rootView.findViewById(R.id.CB_10HP);
        CheckBox HP30Checkbox = rootView.findViewById(R.id.CB_30HP);
        CheckBox AMPCheckboxU = rootView.findViewById(R.id.CB_AMP_U);
        CheckBox AMPCheckboxV = rootView.findViewById(R.id.CB_AMP_V);
        CheckBox AMPCheckboxW = rootView.findViewById(R.id.CB_AMP_W);

        // Set the checkbox states based on the values retrieved from SharedPreferences
        HP1Checkbox.setChecked(MotorDetails1);
        HP10Checkbox.setChecked(MotorDetails2);
        HP30Checkbox.setChecked(MotorDetails3);
        AMPCheckboxU.setChecked(AMPDetailsU);
        AMPCheckboxV.setChecked(AMPDetailsV);
        AMPCheckboxW.setChecked(AMPDetailsW);

        HP1Checkbox.setEnabled(false);
        HP10Checkbox.setEnabled(false);
        HP30Checkbox.setEnabled(false);
        AMPCheckboxU.setEnabled(false);
        AMPCheckboxV.setEnabled(false);
        AMPCheckboxW.setEnabled(false);

        // Initialize TextViews
        TextView AMPdetailsOnDisplay = rootView.findViewById(R.id.OnDisplay_text);
        TextView AMPdetailsOnClamp = rootView.findViewById(R.id.OnClamp_text);
        TextView DCVolDISP = rootView.findViewById(R.id.DC_DISP_text);
        TextView DCVolMET = rootView.findViewById(R.id.DC_MET_text);
        TextView OutputVolDISP = rootView.findViewById(R.id.OUTPUT_DISP_text);
        TextView OUtputVolMET = rootView.findViewById(R.id.OUTPUT_MET_text);
        TextView RunHR = rootView.findViewById(R.id.RH_text);
        TextView Replay = rootView.findViewById(R.id.ReplayOP_text);
        TextView FAN = rootView.findViewById(R.id.FANOpr_text);
        TextView BodyCondition = rootView.findViewById(R.id.BODYCondition_text);
        TextView IOCheck = rootView.findViewById(R.id.IOcheck_text);
        TextView Cleanliness = rootView.findViewById(R.id.Clean_Text);
        TextView ParameterCopy = rootView.findViewById(R.id.PramCopy_Text);

        // Retrieve stored values and set them to TextViews
        AMPdetailsOnDisplay.setText(sharedPref.getString("On_Display", ""));
        AMPdetailsOnClamp.setText(sharedPref.getString("On_Clamp", ""));
        DCVolDISP.setText(sharedPref.getString("dc_dsp", ""));
        DCVolMET.setText(sharedPref.getString("dc_met", ""));
        OutputVolDISP.setText(sharedPref.getString("output_dsp", ""));
        OUtputVolMET.setText(sharedPref.getString("output_met", ""));
        RunHR.setText(sharedPref.getString("enter_RH", ""));
        Replay.setText(sharedPref.getString("enterReplayOP", ""));
        FAN.setText(sharedPref.getString("enter_FANOpr", ""));
        BodyCondition.setText(sharedPref.getString("enter_BODY_Condition", ""));
        IOCheck.setText(sharedPref.getString("enter_io_check", ""));
        Cleanliness.setText(sharedPref.getString("enterClean", ""));
        ParameterCopy.setText(sharedPref.getString("enterPramCopy", ""));

        AMPdetailsOnClamp.setEnabled(false);
        AMPdetailsOnDisplay.setEnabled(false);
        DCVolDISP.setEnabled(false);
        DCVolMET.setEnabled(false);
        OutputVolDISP.setEnabled(false);
        OUtputVolMET.setEnabled(false);
        RunHR.setEnabled(false);
        Replay.setEnabled(false);
        FAN.setEnabled(false);
        BodyCondition.setEnabled(false);
        IOCheck.setEnabled(false);
        Cleanliness.setEnabled(false);
        ParameterCopy.setEnabled(false);

        return rootView;
    }
}
