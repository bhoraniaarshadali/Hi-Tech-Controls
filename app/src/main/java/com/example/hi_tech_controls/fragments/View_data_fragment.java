package com.example.hi_tech_controls.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class View_data_fragment extends Fragment {

    private static final int REQUEST_CODE = 123;
    private RelativeLayout mainLayout;
    private RelativeLayout frag2;
    private RelativeLayout frag3;
    private RelativeLayout frag4;
    private int pageHeight;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_view_data_fragement, container, false);
        Button printButton = rootView.findViewById(R.id.printpdf);
        mainLayout = rootView.findViewById(R.id.main_container);
        frag2 = rootView.findViewById(R.id.frag_2);
        frag3 = rootView.findViewById(R.id.frag_3);
        frag4 = rootView.findViewById(R.id.frag_4);

        printButton.setOnClickListener(v -> checkAndRequestPermission());


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

    public void checkAndRequestPermission() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            showFileNameDialog();
        }
    }

    private void showFileNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Save PDF");
        builder.setMessage("Enter a file name for the PDF:");

        final EditText input = new EditText(requireContext());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String fileName = input.getText().toString();
            if (!fileName.isEmpty()) {
                createPdf(fileName);
            } else {
                Toast.makeText(requireContext(), "File name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createPdf(String fileName) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (directory != null && directory.exists() && directory.canWrite()) {
            File ServiceReportFolder = new File(directory, "Customer Service Report");
            if (!ServiceReportFolder.exists()) {
                if (ServiceReportFolder.mkdirs()) {
                    // Successfully created the folder
                } else {
                    // Failed to create the folder
                    Toast.makeText(requireContext(), "Failed to create the 'Service Report' folder", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // Create the PDF file in the "Service Report" folder
            File pdfFile = new File(ServiceReportFolder, fileName + ".pdf");

            try {
                PdfDocument pdfDoc = new PdfDocument();

                // Add the main layout as the first page with page borders
                addPageFromViewWithBorder(pdfDoc, mainLayout, true);
                // Add pages for the content from frag2, frag3, and frag4 with page borders
                addPageFromViewWithBorder(pdfDoc, frag2, false);
                addPageFromViewWithBorder(pdfDoc, frag3, false);
                addPageFromViewWithBorder(pdfDoc, frag4, false);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pdfDoc.writeTo(Files.newOutputStream(pdfFile.toPath()));
                }
                pdfDoc.close();


                // Show a toast message upon successful PDF creation
                Toast.makeText(requireContext(), "PDF saved as " + fileName + ".pdf", Toast.LENGTH_SHORT).show();

                // Show a notification upon successful PDF creation
                showNotification(pdfFile);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "External storage not available or writable", Toast.LENGTH_SHORT).show();
        }
    }

    private int pageNumber = 1;

    private void addPageFromViewWithBorder(PdfDocument pdfDoc, View view, boolean isFirstPage) {
        // 11 inches converted to points
        int pageHeight = (int) (12f * 72);
        // 8.5 inches converted to points
        int pageWidth = (int) (8.5f * 72);
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDoc.getPages().size() + 1).create();
        PdfDocument.Page page = pdfDoc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // FOR THE PAGE BORDER
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(4);
        int borderLeft = 10;
        int borderTop = 10;
        int borderRight = pageWidth - 10;
        int borderBottom = pageHeight - 10;
        canvas.drawRect(borderLeft, borderTop, borderRight, borderBottom, borderPaint);

        if (isFirstPage) {
            // Add logo and subheading at the top near the border
            addLogoAndSubheading(canvas);
            addDateTimeToPage(canvas);

        }

        // Adjust this to your desired scale (0.5 for 50% of the original size)
        float scaleFactor = 0.4f;
        int viewWidth = (int) (view.getWidth() * scaleFactor);
        int viewHeight = (int) (view.getHeight() * scaleFactor);

        int left = borderLeft + (borderRight - borderLeft - viewWidth) / 2;
        int top = borderTop + (borderBottom - borderTop - viewHeight) / 2;
        int right = left + viewWidth;
        int bottom = top + viewHeight;

        view.layout(left, top, right, bottom);

        int saveCount = canvas.save();
        canvas.clipRect(left, top, right, bottom);
        canvas.translate(left, top);
        canvas.scale(scaleFactor, scaleFactor);
        view.draw(canvas);
        canvas.restoreToCount(saveCount);
// Display the page number for all pages
        drawPageNumber(canvas, pageNumber, borderRight, borderBottom);
        pdfDoc.finishPage(page);
        pageNumber++;
    }

    private void addDateTimeToPage(Canvas canvas) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
        String currentDate = sdfDate.format(new Date());

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(16);
        textPaint.setFakeBoldText(true);

        // Manually set the X positions for each element
        int dateX = 20;            // Adjust the X position for the date
        int callX = 200;           // Adjust the X position for the phone call symbol
        int numberX = 230;         // Adjust the X position for the phone number
        int emailSymbolX = 400;    // Adjust the X position for the email symbol

        int dateY = 126; // You can adjust Y positions as needed
        int callY = 126;
        int numberY = 129;
        int emailSymbolY = 129;

        // Draw the date symbol
        textPaint.setColor(Color.BLACK);
        canvas.drawText(" 🗓 ", dateX, dateY, textPaint);


        // Increase the size of the email symbol
        Paint emailSymbolPaint = new Paint();
        emailSymbolPaint.setColor(Color.BLACK);
        emailSymbolPaint.setTextSize(25); // You can adjust the size as needed

        // Draw the email symbol using the new Paint object
        canvas.drawText(" ✉ ", emailSymbolX, emailSymbolY, emailSymbolPaint);

        // Draw the blue phone call symbol
        textPaint.setColor(Color.BLUE);
        canvas.drawText(" \uD83D\uDCF1 ", callX, callY, textPaint);

        // Draw the phone number
        textPaint.setColor(Color.BLACK);
        canvas.drawText("+91-1234567901", numberX, numberY, textPaint);

        // Modify the email text size separately
        Paint emailTextPaint = new Paint();
        emailTextPaint.setColor(Color.BLACK);
        emailTextPaint.setTextSize(16);
        emailTextPaint.setFakeBoldText(true);
        // Adjust the email text size as needed
        canvas.drawText(" hitech@gmail.com", emailSymbolX + (int) emailSymbolPaint.measureText(" ✉ ") + 10, numberY, emailTextPaint);

        // Display the current date
        textPaint.setColor(Color.BLACK);
        canvas.drawText(currentDate, dateX + textPaint.measureText(" 🗓 "), dateY, textPaint);
    }


    private void drawPageNumber(Canvas canvas, int pageNumber, int pageWidth, int pageHeight) {
        Paint pageNumberPaint = new Paint();
        pageNumberPaint.setColor(Color.BLACK);
        pageNumberPaint.setTextSize(14);

        // Position the page number at the bottom right corner
        float pageNumberX = pageWidth - 10; // Adjust this position as needed
        float pageNumberY = pageHeight - 10; // Adjust this position as needed

        String pageNumberText = "Page " + pageNumber;
        canvas.drawText(pageNumberText, pageNumberX - pageNumberPaint.measureText(pageNumberText), pageNumberY, pageNumberPaint);
    }


    private void addLogoAndSubheading(Canvas canvas) {
        // Draw the horizontal line at the top
        int lineStartX = 20;
        int lineEndX = canvas.getWidth() - 20;
        int lineY = 10;
        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(2);
        canvas.drawLine(lineStartX, lineY, lineEndX, lineY, linePaint);

        // Draw the rectangle below the horizontal line
        addTextAndLine(canvas);

        // Load and draw the logo on the right side
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        Bitmap resizedLogo = resizeImage(logo);
        int logoX = canvas.getWidth() - resizedLogo.getWidth() - 20;
        int logoY = 10;
        canvas.drawBitmap(resizedLogo, logoX, logoY, null);
// Draw the rounded horizontal line below all content
        drawRoundedHorizontalLine(canvas, lineY + 120, lineEndX);
    }

    private void drawRoundedHorizontalLine(Canvas canvas, int y, int width) {
        // Draw the rounded horizontal line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(2);


        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);

        Path path = new Path();
        path.moveTo(20, y); // Adjust the starting X position
        path.lineTo(width, y); // Adjust the ending X position
        path.lineTo(width, y + 2);
        path.lineTo(20, y + 2);
        path.close(); // Close the path to make it rounded

        canvas.drawPath(path, paint);

        // Draw the rectangle box
        int rectTop = y + 2 + 10; // Adjust the top position of the rectangle
        int rectBottom = rectTop + 50; // Adjust the height of the rectangle
        int rectLeft = 20; // Adjust the left position of the rectangle
        int rectRight = width; // Adjust the right position of the rectangle

        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.LTGRAY); // Set the rectangle color

        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, rectPaint);

        // Draw the text inside the rectangle
        String heading = "Customer Service Report";
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(20);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        // Calculate the position to center the text horizontally and vertically within the rectangle
        float textX = rectLeft + (rectRight - rectLeft - textPaint.measureText(heading)) / 2;
        float textY = rectTop + (rectBottom - rectTop) / 2 + textPaint.getTextSize() / 3;

        canvas.drawText(heading, textX, textY, textPaint);

        // Draw the horizontal line below the rectangle
        int lineYBelowRect = rectBottom + 10; // Adjust the Y position below the rectangle

        canvas.drawLine(20, lineYBelowRect, width, lineYBelowRect, linePaint);


    }

    private void addTextAndLine(Canvas canvas) {
        // Calculate the position for the horizontal line at the top
        int lineStartX = 10;
        int lineEndX = canvas.getWidth() - 20;
        int lineY = 10;
        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(2);
        canvas.drawLine(lineStartX, lineY, lineEndX, lineY, linePaint);

        // Text for "HI-TECH CONTROLS"
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30); // Set the text size
        textPaint.setFakeBoldText(true); // Make the text bold

        String text = "HI-TECH CONTROLS";

        // Calculate the position to center the text horizontally
        float textX = (canvas.getWidth() - textPaint.measureText(text)) / 2;
        float textY = lineY + 40; // Move the text down from the line

        // Text for the tagline
        Paint taglinePaint = new Paint();
        taglinePaint.setColor(Color.BLACK);
        taglinePaint.setTextSize(18); // Set the tagline text size
        taglinePaint.setTextSkewX(-0.25f); // Apply italic skew

        String tagline = "\"Your service in your hand\"";

        // Calculate the position to center the tagline text horizontally
        float taglineX = (canvas.getWidth() - taglinePaint.measureText(tagline)) / 2;
        float taglineY = textY + taglinePaint.getTextSize() + 10; // Increase the gap below "HI-TECH CONTROLS" text

        // Draw the italic tagline text
        canvas.drawText(tagline, taglineX, taglineY, taglinePaint);

        // Draw the bold "HI-TECH CONTROLS" text
        canvas.drawText(text, textX, textY, textPaint);
    }


    private Bitmap resizeImage(Bitmap originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        float scaleWidth = ((float) 100) / width;
        float scaleHeight = ((float) 100) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(originalImage, 0, 0, width, height, matrix, true);
    }

    private void showNotification(File pdfFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "PDF_Download";
            CharSequence channelName = "PDF Download Channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);

            NotificationManager notificationManager = requireActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            // Create an intent to open the PDF
            Intent openPdfIntent = new Intent(Intent.ACTION_VIEW);
            openPdfIntent.setDataAndType(pdfFileToUri(pdfFile), "application/pdf");
            openPdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            PendingIntent pdfPendingIntent = PendingIntent.getActivity(requireContext(), 0, openPdfIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), channelId)
                    .setSmallIcon(R.drawable.pdflogo) // Replace with your notification icon
                    .setContentTitle("PDF Downloaded")
                    .setContentText("Your PDF has been downloaded successfully.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pdfPendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(requireContext());
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            int notificationId = 1;
            notificationManagerCompat.notify(notificationId, builder.build());
        }
    }

    // Convert a File to a Uri
    @SuppressLint("ObsoleteSdkInt")
    private Uri pdfFileToUri(File pdfFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", pdfFile);
        } else {
            return Uri.fromFile(pdfFile);
        }
    }
}