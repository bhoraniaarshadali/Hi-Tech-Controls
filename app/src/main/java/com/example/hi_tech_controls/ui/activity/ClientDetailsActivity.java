package com.example.hi_tech_controls.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.FileProvider;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.InwardClient;
import com.example.hi_tech_controls.mediaControl.PdfGenerator;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ClientDetailsActivity extends BaseActivity {

    private static final String TAG = "ClientDetailsActivity";
    private FirebaseFirestore db;
    private String clientId;
    private InwardClient client;
    private boolean isEditMode = false;

    // === VIEWS ===
    // Fill One – EDITABLE
    private EditText etName, etNumber, etGpNumber, etGpDate, etMakeName, etModelName, etHprate, etSrlNumber;

    // Fill Two – EDITABLE
    private TextView tvSelectEmpTwo, tvFrRate; // TextView for display
    private EditText etLocalEditText, etClientObs, etOurObs, etLastFault; // EditText for editing
    private RadioGroup rgCmd, rgInput;
    private RadioButton rbLocal, rbRemote, rbComm, rbDiode, rbScr;
    private CheckBox cbPosU, cbPosV, cbPosW, cbNegU, cbNegV, cbNegW;
    private CheckBox cbOutPosU, cbOutPosV, cbOutPosW, cbOutNegU, cbOutNegV, cbOutNegW;

    // Fill Three – EDITABLE
    private TextView tvSelectEmpThree, tvFirstRemarks, tvDays; // TextView for display (NON-EDITABLE)
    private CheckBox cbCapacitor, cbDisplay, cbFan, cbCC;
    private CheckBox cbRepair1, cbRepair2, cbRepair3, cbRepair4, cbRepair5, cbRepair6;
    private CheckBox cbReplace1, cbReplace2, cbReplace3, cbReplace4, cbReplace5, cbReplace6, cbReplace7, cbReplace8, cbReplace9;
    private CheckBox cbTrial1, cbTrial2;

    // Fill Four – EDITABLE
    private TextView tvSelectEmpFour; // TextView for display (NON-EDITABLE)
    private CheckBox cb1HP, cb10HP, cb30HP;
    private EditText etAmpDisp, etAmpClamp;
    private CheckBox cbAmpU, cbAmpV, cbAmpW;
    private EditText etDcDisp, etDcMet, etOutDisp, etOutMet;
    private EditText etRH, etReplay, etFan, etBody, etIO, etClean, etParam;
    //private android.app.ProgressDialog progressDialog;
    private SweetAlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_details);

        //load ads
//        AdView adView = findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        adView.loadAd(adRequest);

        loadAd();
        AppCompatImageButton editBtn = findViewById(R.id.editBtn);
        AppCompatImageButton saveBtn = findViewById(R.id.saveBtn);
        AppCompatImageButton backBtn = findViewById(R.id.backBtn);
        androidx.appcompat.widget.AppCompatButton generatePdfBtn = findViewById(R.id.generatePdfBtn);
        generatePdfBtn.setOnClickListener(v -> generateAndDownloadPdf());

        backBtn.setOnClickListener(v -> {
            if (isEditMode) {
                showCancelConfirmation();
            } else {
                finish();
            }
        });


        editBtn.setOnClickListener(v -> {
            Log.d(TAG, "Edit clicked");
            setEditMode(true);
        });

        saveBtn.setOnClickListener(v -> {
            Log.d(TAG, "Save clicked");
            saveAllData();
        });


        Log.d(TAG, "Activity onCreate started");
        long startTime = System.currentTimeMillis();

        db = FirebaseFirestore.getInstance();
        client = new InwardClient();

        // Fix: Try all possible keys
        clientId = getIntent().getStringExtra("CLIENT_ID");
        if (clientId == null) clientId = getIntent().getStringExtra("clientId");
        if (clientId == null) clientId = getIntent().getStringExtra("client_id");
        if (clientId == null) clientId = getIntent().getStringExtra("id");

        Log.d(TAG, "Client ID received: " + clientId);

        if (clientId == null || clientId.isEmpty()) {
            Log.e(TAG, "Client ID is null or empty");
            Toast.makeText(this, "Client not found!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        loadData();
        setEditMode(false); // Start in view mode

        long endTime = System.currentTimeMillis();
        Log.d(TAG, "Activity onCreate completed in " + (endTime - startTime) + "ms");
    }

    void loadAd() {
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void initViews() {
        Log.d(TAG, "Initializing views");

        // Fill One – EDITABLE (EditText)
        etName = findViewById(R.id.fill_one_enterName);
        etNumber = findViewById(R.id.fill_one_enterNumber);
        etGpNumber = findViewById(R.id.fill_one_enterGPNumber);
        etGpDate = findViewById(R.id.fill_one_enterDate);
        etMakeName = findViewById(R.id.fill_one_enterMakeName);
        etModelName = findViewById(R.id.fill_one_enterModelName);
        etHprate = findViewById(R.id.fill_one_enterHPrate);
        etSrlNumber = findViewById(R.id.fill_one_enterSerialNumber);

        // Fill Two
        tvSelectEmpTwo = findViewById(R.id.fill_two_selectEmply); // TextView (NON-EDITABLE)
        tvFrRate = findViewById(R.id.fill_two_enterFRrate); // TextView
        etLocalEditText = findViewById(R.id.fill_two_localEditText); // EditText
        etClientObs = findViewById(R.id.fill_two_clientObs_text); // EditText
        etOurObs = findViewById(R.id.fill_two_ourObs_text); // EditText
        etLastFault = findViewById(R.id.fill_two_lastFault_text); // EditText

        rgCmd = findViewById(R.id.radioGroupCMD);
        rgInput = findViewById(R.id.radioGroupInputSply);

        rbLocal = findViewById(R.id.fill_two_radioButtonLocal);
        rbRemote = findViewById(R.id.fill_two_radioButtonRemote);
        rbComm = findViewById(R.id.fill_two_radioButtonComm);
        rbDiode = findViewById(R.id.fill_two_radioButtonDIODE);
        rbScr = findViewById(R.id.fill_two_radioButtonSCR);

        cbPosU = findViewById(R.id.fill_two_input_POS_checkbox_U);
        cbPosV = findViewById(R.id.fill_two_input_POS_checkbox_V);
        cbPosW = findViewById(R.id.fill_two_input_POS_checkbox_W);
        cbNegU = findViewById(R.id.fill_two_input_NEG_checkbox_U);
        cbNegV = findViewById(R.id.fill_two_input_NEG_checkbox_V);
        cbNegW = findViewById(R.id.fill_two_input_NEG_checkbox_W);
        cbOutPosU = findViewById(R.id.fill_two_output_POS_checkbox_U);
        cbOutPosV = findViewById(R.id.fill_two_output_POS_checkbox_V);
        cbOutPosW = findViewById(R.id.fill_two_output_POS_checkbox_W);
        cbOutNegU = findViewById(R.id.fill_two_output_NEG_checkbox_U);
        cbOutNegV = findViewById(R.id.fill_two_output_NEG_checkbox_V);
        cbOutNegW = findViewById(R.id.fill_two_output_NEG_checkbox_W);

        // Fill Three
        tvSelectEmpThree = findViewById(R.id.fill_three_selectEmply); // TextView (NON-EDITABLE)
        tvFirstRemarks = findViewById(R.id.fill_three_firstRemarks); // TextView (NON-EDITABLE)
        tvDays = findViewById(R.id.textDays); // TextView (NON-EDITABLE)

        cbCapacitor = findViewById(R.id.fill_three_checkboxCapasitor);
        cbDisplay = findViewById(R.id.fill_three_checkboxDisplay);
        cbFan = findViewById(R.id.fill_three_checkboxFAN);
        cbCC = findViewById(R.id.fill_three_checkboxCC);

        cbRepair1 = findViewById(R.id.fill_three_Repair_checkboxOne);
        cbRepair2 = findViewById(R.id.fill_three_Repair_checkboxTwo);
        cbRepair3 = findViewById(R.id.fill_three_Repair_checkboxThree);
        cbRepair4 = findViewById(R.id.fill_three_Repair_checkboxFour);
        cbRepair5 = findViewById(R.id.fill_three_Repair_checkboxFive);
        cbRepair6 = findViewById(R.id.fill_three_Repair_checkboxSix);

        cbReplace1 = findViewById(R.id.fill_three_Replace_checkboxOne);
        cbReplace2 = findViewById(R.id.fill_three_Replace_checkboxTwo);
        cbReplace3 = findViewById(R.id.fill_three_Replace_checkboxThree);
        cbReplace4 = findViewById(R.id.fill_three_Replace_checkboxFour);
        cbReplace5 = findViewById(R.id.fill_three_Replace_checkboxFive);
        cbReplace6 = findViewById(R.id.fill_three_Replace_checkboxSix);
        cbReplace7 = findViewById(R.id.fill_three_Replace_checkboxSeven);
        cbReplace8 = findViewById(R.id.fill_three_Replace_checkboxEight);
        cbReplace9 = findViewById(R.id.fill_three_Replace_checkboxNine);

        cbTrial1 = findViewById(R.id.fill_three_checkboxTRIAL1);
        cbTrial2 = findViewById(R.id.fill_three_checkboxTRIAL2);

        // Fill Four
        tvSelectEmpFour = findViewById(R.id.fill_four_selectEmply); // TextView (NON-EDITABLE)

        cb1HP = findViewById(R.id.fill_four_checkbox1HP);
        cb10HP = findViewById(R.id.fill_four_checkbox10HP);
        cb30HP = findViewById(R.id.fill_four_checkbox30HP);

        etAmpDisp = findViewById(R.id.fill_four_OnDisplay_text); // EditText
        etAmpClamp = findViewById(R.id.fill_four_OnClamp_text); // EditText

        cbAmpU = findViewById(R.id.fill_four_checkboxAMP_U);
        cbAmpV = findViewById(R.id.fill_four_checkboxAMP_V);
        cbAmpW = findViewById(R.id.fill_four_checkboxAMP_W);

        etDcDisp = findViewById(R.id.fill_four_DC_DISP_text); // EditText
        etDcMet = findViewById(R.id.fill_four_DC_MET_text); // EditText
        etOutDisp = findViewById(R.id.fill_four_OUTPUT_DISP_text); // EditText
        etOutMet = findViewById(R.id.fill_four_OUTPUT_MET_text); // EditText

        etRH = findViewById(R.id.fill_four_enterRH_text); // EditText
        etReplay = findViewById(R.id.fill_four_enterReplayOP_text); // EditText
        etFan = findViewById(R.id.fill_four_enterFANOpr_text); // EditText
        etBody = findViewById(R.id.fill_four_enterBODYCondition_text); // EditText
        etIO = findViewById(R.id.fill_four_enterIOcheck_text); // EditText
        etClean = findViewById(R.id.fill_four_enterClean_Text); // EditText
        etParam = findViewById(R.id.fill_four_enterPramCopy_Text); // EditText

        // Set click listeners for non-editable fields to show toast
        setNonEditableFieldListeners();

        Log.d(TAG, "All views initialized successfully");
    }

    private void setNonEditableFieldListeners() {
        Log.d(TAG, "Setting up non-editable field listeners");

        // Fill Two - Employee field (non-editable)
        tvSelectEmpTwo.setOnClickListener(v -> {
            if (isEditMode) {
                Toast.makeText(this, "Employee selection is not editable", Toast.LENGTH_SHORT).show();
            }
        });

        // Fill Three - Employee field (non-editable)
        tvSelectEmpThree.setOnClickListener(v -> {
            if (isEditMode) {
                Toast.makeText(this, "Employee selection is not editable", Toast.LENGTH_SHORT).show();
            }
        });

        // Fill Three - Repair time days field (non-editable)
        tvDays.setOnClickListener(v -> {
            if (isEditMode) {
                Toast.makeText(this, "Repair time days are not editable", Toast.LENGTH_SHORT).show();
            }
        });

        // Fill Four - Employee field (non-editable)
        tvSelectEmpFour.setOnClickListener(v -> {
            if (isEditMode) {
                Toast.makeText(this, "Employee selection is not editable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadData() {
        Log.d(TAG, "Starting data load from Firestore");
        long startTime = System.currentTimeMillis();

        CollectionReference pages = db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId).collection("pages");

        // Use source CACHE for faster loading if available
        pages.get(Source.CACHE).addOnSuccessListener(snapshot -> {
            long cacheTime = System.currentTimeMillis();
            Log.d(TAG, "Cache data loaded in " + (cacheTime - startTime) + "ms");

            if (snapshot.isEmpty()) {
                Log.d(TAG, "No data found in cache, trying server");
                // If cache is empty, try server
                loadFromServer();
            } else {
                processSnapshot(snapshot);
                // Also update from server in background for latest data
                loadFromServer();
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Cache load failed, trying server", e);
            loadFromServer();
        });
    }

    private void loadFromServer() {
        Log.d(TAG, "Loading data from server");
        long serverStartTime = System.currentTimeMillis();

        CollectionReference pages = db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId).collection("pages");

        pages.get(Source.SERVER).addOnSuccessListener(snapshot -> {
            long serverEndTime = System.currentTimeMillis();
            Log.d(TAG, "Server data loaded in " + (serverEndTime - serverStartTime) + "ms");

            if (snapshot.isEmpty()) {
                Log.e(TAG, "No data found for client: " + clientId);
                Toast.makeText(this, "No data found for this client", Toast.LENGTH_SHORT).show();
                return;
            }
            processSnapshot(snapshot);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading data from server: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void processSnapshot(QuerySnapshot snapshot) {
        Log.d(TAG, "Processing snapshot with " + snapshot.size() + " documents");

        for (DocumentSnapshot doc : snapshot) {
            String pageName = doc.getId();
            Log.d(TAG, "Processing page: " + pageName);

            switch (pageName) {
                case "fill_one":
                    fillOne(doc);
                    break;
                case "fill_two":
                    fillTwo(doc);
                    break;
                case "fill_three":
                    fillThree(doc);
                    break;
                case "fill_four":
                    fillFour(doc);
                    break;
            }
        }
        Log.d(TAG, "All pages processed successfully");
    }

    private void fillOne(DocumentSnapshot doc) {
        Log.d(TAG, "Filling fill_one data");

        client.name = getStringSafe(doc, "name");
        client.client_number = getStringSafe(doc, "client_number");
        client.gp_number = getStringSafe(doc, "gp_number");
        client.gp_date = getStringSafe(doc, "gp_date");
        client.make_name = getStringSafe(doc, "make_name");
        client.model_name = getStringSafe(doc, "model_name");
        client.hp_rate = getStringSafe(doc, "hp_rate");
        client.serial_number = getStringSafe(doc, "serial_number");

        etName.setText(client.name);
        etNumber.setText(client.client_number);
        etGpNumber.setText(client.gp_number);
        etGpDate.setText(formatDateForEdit(client.gp_date));
        etMakeName.setText(client.make_name);
        etModelName.setText(client.model_name);
        etHprate.setText(client.hp_rate);
        etSrlNumber.setText(client.serial_number);
    }

    private void fillTwo(DocumentSnapshot doc) {
        Log.d(TAG, "Filling fill_two data");

        client.select_emp_fill_two = getStringSafe(doc, "select_emp");
        client.fr_rate = getStringSafe(doc, "fr_rate");
        client.localEditText = getStringSafe(doc, "localEditText");
        client.client_obs = getStringSafe(doc, "client_obs");
        client.our_obs = getStringSafe(doc, "our_obs");
        client.last_fault = getStringSafe(doc, "last_fault");

        // Radio buttons
        client.local_radio_checked = getBooleanSafe(doc, "local_radio_checked");
        client.remote_radio_checked = getBooleanSafe(doc, "remote_radio_checked");
        client.comm_radio_checked = getBooleanSafe(doc, "comm_radio_checked");
        client.diode_radio_checked = getBooleanSafe(doc, "diode_radio_checked");
        client.scr_radio_checked = getBooleanSafe(doc, "scr_radio_checked");

        // Checkboxes
        client.input_pos_checkbox_U = getBooleanSafe(doc, "input_pos_checkbox_U");
        client.input_pos_checkbox_V = getBooleanSafe(doc, "input_pos_checkbox_V");
        client.input_pos_checkbox_W = getBooleanSafe(doc, "input_pos_checkbox_W");
        client.input_neg_checkbox_U = getBooleanSafe(doc, "input_neg_checkbox_U");
        client.input_neg_checkbox_V = getBooleanSafe(doc, "input_neg_checkbox_V");
        client.input_neg_checkbox_W = getBooleanSafe(doc, "input_neg_checkbox_W");
        client.output_pos_checkbox_U = getBooleanSafe(doc, "output_pos_checkbox_U");
        client.output_pos_checkbox_V = getBooleanSafe(doc, "output_pos_checkbox_V");
        client.output_pos_checkbox_W = getBooleanSafe(doc, "output_pos_checkbox_W");
        client.output_neg_checkbox_U = getBooleanSafe(doc, "output_neg_checkbox_U");
        client.output_neg_checkbox_V = getBooleanSafe(doc, "output_neg_checkbox_V");
        client.output_neg_checkbox_W = getBooleanSafe(doc, "output_neg_checkbox_W");

        // Update UI
        tvSelectEmpTwo.setText(client.select_emp_fill_two);
        tvFrRate.setText(client.fr_rate);
        etLocalEditText.setText(client.localEditText);
        etClientObs.setText(client.client_obs);
        etOurObs.setText(client.our_obs);
        etLastFault.setText(client.last_fault);

        rbLocal.setChecked(client.local_radio_checked);
        rbRemote.setChecked(client.remote_radio_checked);
        rbComm.setChecked(client.comm_radio_checked);
        rbDiode.setChecked(client.diode_radio_checked);
        rbScr.setChecked(client.scr_radio_checked);

        cbPosU.setChecked(client.input_pos_checkbox_U);
        cbPosV.setChecked(client.input_pos_checkbox_V);
        cbPosW.setChecked(client.input_pos_checkbox_W);
        cbNegU.setChecked(client.input_neg_checkbox_U);
        cbNegV.setChecked(client.input_neg_checkbox_V);
        cbNegW.setChecked(client.input_neg_checkbox_W);
        cbOutPosU.setChecked(client.output_pos_checkbox_U);
        cbOutPosV.setChecked(client.output_pos_checkbox_V);
        cbOutPosW.setChecked(client.output_pos_checkbox_W);
        cbOutNegU.setChecked(client.output_neg_checkbox_U);
        cbOutNegV.setChecked(client.output_neg_checkbox_V);
        cbOutNegW.setChecked(client.output_neg_checkbox_W);
    }

    private void fillThree(DocumentSnapshot doc) {
        Log.d(TAG, "Filling fill_three data");

        client.select_emp_fill_three = getStringSafe(doc, "select_emp");
        client.enter_first_remarks = getStringSafe(doc, "enter_first_remarks");

        // Checkboxes
        client.checkboxCapacitor = getBooleanSafe(doc, "checkboxCapacitor");
        client.checkboxDisplay = getBooleanSafe(doc, "checkboxDisplay");
        client.checkboxFAN = getBooleanSafe(doc, "checkboxFAN");
        client.checkboxCC = getBooleanSafe(doc, "checkboxCC");

        client.repair_checkboxOne = getBooleanSafe(doc, "repair_checkboxOne");
        client.repair_checkboxTwo = getBooleanSafe(doc, "repair_checkboxTwo");
        client.repair_checkboxThree = getBooleanSafe(doc, "repair_checkboxThree");
        client.repair_checkboxFour = getBooleanSafe(doc, "repair_checkboxFour");
        client.repair_checkboxFive = getBooleanSafe(doc, "repair_checkboxFive");
        client.repair_checkboxSix = getBooleanSafe(doc, "repair_checkboxSix");

        client.replace_checkboxOne = getBooleanSafe(doc, "replace_checkboxOne");
        client.replace_checkboxTwo = getBooleanSafe(doc, "replace_checkboxTwo");
        client.replace_checkboxThree = getBooleanSafe(doc, "replace_checkboxThree");
        client.replace_checkboxFour = getBooleanSafe(doc, "replace_checkboxFour");
        client.replace_checkboxFive = getBooleanSafe(doc, "replace_checkboxFive");
        client.replace_checkboxSix = getBooleanSafe(doc, "replace_checkboxSix");
        client.replace_checkboxSeven = getBooleanSafe(doc, "replace_checkboxSeven");
        client.replace_checkboxEight = getBooleanSafe(doc, "replace_checkboxEight");
        client.replace_checkboxNine = getBooleanSafe(doc, "replace_checkboxNine");

        client.checkboxTrial1 = getBooleanSafe(doc, "checkboxTrial1");
        client.checkboxTrial2 = getBooleanSafe(doc, "checkboxTrial2");

        // Number picker
        Long numberValue = doc.getLong("number_picker_value");
        client.number_picker_value = numberValue != null ? numberValue.intValue() : 0;

        // Update UI
        tvSelectEmpThree.setText(client.select_emp_fill_three);
        tvFirstRemarks.setText(client.enter_first_remarks);
        tvDays.setText(String.valueOf(client.number_picker_value));

        cbCapacitor.setChecked(client.checkboxCapacitor);
        cbDisplay.setChecked(client.checkboxDisplay);
        cbFan.setChecked(client.checkboxFAN);
        cbCC.setChecked(client.checkboxCC);

        cbRepair1.setChecked(client.repair_checkboxOne);
        cbRepair2.setChecked(client.repair_checkboxTwo);
        cbRepair3.setChecked(client.repair_checkboxThree);
        cbRepair4.setChecked(client.repair_checkboxFour);
        cbRepair5.setChecked(client.repair_checkboxFive);
        cbRepair6.setChecked(client.repair_checkboxSix);

        cbReplace1.setChecked(client.replace_checkboxOne);
        cbReplace2.setChecked(client.replace_checkboxTwo);
        cbReplace3.setChecked(client.replace_checkboxThree);
        cbReplace4.setChecked(client.replace_checkboxFour);
        cbReplace5.setChecked(client.replace_checkboxFive);
        cbReplace6.setChecked(client.replace_checkboxSix);
        cbReplace7.setChecked(client.replace_checkboxSeven);
        cbReplace8.setChecked(client.replace_checkboxEight);
        cbReplace9.setChecked(client.replace_checkboxNine);

        cbTrial1.setChecked(client.checkboxTrial1);
        cbTrial2.setChecked(client.checkboxTrial2);
    }

    private void fillFour(DocumentSnapshot doc) {
        Log.d(TAG, "Filling fill_four data");

        client.select_emp_fill_four = getStringSafe(doc, "select_emp");

        // Checkboxes
        client.checkbox_1HP = getBooleanSafe(doc, "checkbox_1HP");
        client.checkbox_10HP = getBooleanSafe(doc, "checkbox_10HP");
        client.checkbox_30HP = getBooleanSafe(doc, "checkbox_30HP");

        client.On_Display = getStringSafe(doc, "On_Display");
        client.On_Clamp = getStringSafe(doc, "On_Clamp");

        client.checkbox_u = getBooleanSafe(doc, "checkbox_u");
        client.checkbox_v = getBooleanSafe(doc, "checkbox_v");
        client.checkbox_w = getBooleanSafe(doc, "checkbox_w");

        client.dc_dsp = getStringSafe(doc, "dc_dsp");
        client.dc_met = getStringSafe(doc, "dc_met");
        client.output_dsp = getStringSafe(doc, "output_dsp");
        client.output_met = getStringSafe(doc, "output_met");

        client.enter_RH = getStringSafe(doc, "enter_RH");
        client.enterReplayOP = getStringSafe(doc, "enterReplayOP");
        client.enter_FANOpr = getStringSafe(doc, "enter_FANOpr");
        client.enter_BODY_Condition = getStringSafe(doc, "enter_BODY_Condition");
        client.enter_io_check = getStringSafe(doc, "enter_io_check");
        client.enterClean = getStringSafe(doc, "enterClean");
        client.enterPramCopy = getStringSafe(doc, "enterPramCopy");

        // Update UI
        tvSelectEmpFour.setText(client.select_emp_fill_four);

        cb1HP.setChecked(client.checkbox_1HP);
        cb10HP.setChecked(client.checkbox_10HP);
        cb30HP.setChecked(client.checkbox_30HP);

        etAmpDisp.setText(client.On_Display);
        etAmpClamp.setText(client.On_Clamp);

        cbAmpU.setChecked(client.checkbox_u);
        cbAmpV.setChecked(client.checkbox_v);
        cbAmpW.setChecked(client.checkbox_w);

        etDcDisp.setText(client.dc_dsp);
        etDcMet.setText(client.dc_met);
        etOutDisp.setText(client.output_dsp);
        etOutMet.setText(client.output_met);

        etRH.setText(client.enter_RH);
        etReplay.setText(client.enterReplayOP);
        etFan.setText(client.enter_FANOpr);
        etBody.setText(client.enter_BODY_Condition);
        etIO.setText(client.enter_io_check);
        etClean.setText(client.enterClean);
        etParam.setText(client.enterPramCopy);
    }

    // Helper methods
    private String getStringSafe(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value : "";
    }

    private boolean getBooleanSafe(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return value != null && value;
    }

//    private String formatDate(String dateStr) {
//        if (dateStr == null || dateStr.isEmpty()) return "N/A";
//        try {
//            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
//            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
//            return outputFormat.format(inputFormat.parse(dateStr));
//        } catch (Exception e) {
//            return dateStr;
//        }
//    }

    private String formatDateForEdit(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(Objects.requireNonNull(inputFormat.parse(dateStr)));
        } catch (Exception e) {
            return dateStr;
        }
    }

    // === EDIT MODE FUNCTIONALITY ===

    private void setEditMode(boolean editMode) {
        Log.d(TAG, "Setting edit mode: " + editMode);
        isEditMode = editMode;

        setFieldsEnabled(editMode);

        findViewById(R.id.editBtn).setVisibility(editMode ? View.GONE : View.VISIBLE);
        findViewById(R.id.saveBtn).setVisibility(editMode ? View.VISIBLE : View.GONE);
    }


    private void setFieldsEnabled(boolean enabled) {
        Log.d(TAG, "Setting fields enabled: " + enabled);

        // Fill One - EDITABLE
        etName.setEnabled(enabled);
        etNumber.setEnabled(enabled);
        etGpNumber.setEnabled(enabled);
        etGpDate.setEnabled(enabled);
        etMakeName.setEnabled(enabled);
        etModelName.setEnabled(enabled);
        etHprate.setEnabled(enabled);
        etSrlNumber.setEnabled(enabled);

        // Fill Two - EDITABLE (except employee field)
        tvFrRate.setEnabled(enabled);
        etLocalEditText.setEnabled(enabled);
        etClientObs.setEnabled(enabled);
        etOurObs.setEnabled(enabled);
        etLastFault.setEnabled(enabled);

        enableRadioGroup(rgCmd, enabled);
        enableRadioGroup(rgInput, enabled);

        setCheckboxEnabled(cbPosU, enabled);
        setCheckboxEnabled(cbPosV, enabled);
        setCheckboxEnabled(cbPosW, enabled);
        setCheckboxEnabled(cbNegU, enabled);
        setCheckboxEnabled(cbNegV, enabled);
        setCheckboxEnabled(cbNegW, enabled);
        setCheckboxEnabled(cbOutPosU, enabled);
        setCheckboxEnabled(cbOutPosV, enabled);
        setCheckboxEnabled(cbOutPosW, enabled);
        setCheckboxEnabled(cbOutNegU, enabled);
        setCheckboxEnabled(cbOutNegV, enabled);
        setCheckboxEnabled(cbOutNegW, enabled);

        // Fill Three - EDITABLE (except employee and days fields)
        setCheckboxEnabled(cbCapacitor, enabled);
        setCheckboxEnabled(cbDisplay, enabled);
        setCheckboxEnabled(cbFan, enabled);
        setCheckboxEnabled(cbCC, enabled);
        tvFirstRemarks.setEnabled(enabled);
        setCheckboxEnabled(cbRepair1, enabled);
        setCheckboxEnabled(cbRepair2, enabled);
        setCheckboxEnabled(cbRepair3, enabled);
        setCheckboxEnabled(cbRepair4, enabled);
        setCheckboxEnabled(cbRepair5, enabled);
        setCheckboxEnabled(cbRepair6, enabled);
        setCheckboxEnabled(cbReplace1, enabled);
        setCheckboxEnabled(cbReplace2, enabled);
        setCheckboxEnabled(cbReplace3, enabled);
        setCheckboxEnabled(cbReplace4, enabled);
        setCheckboxEnabled(cbReplace5, enabled);
        setCheckboxEnabled(cbReplace6, enabled);
        setCheckboxEnabled(cbReplace7, enabled);
        setCheckboxEnabled(cbReplace8, enabled);
        setCheckboxEnabled(cbReplace9, enabled);
        setCheckboxEnabled(cbTrial1, enabled);
        setCheckboxEnabled(cbTrial2, enabled);

        // Fill Four - EDITABLE (except employee field)
        etAmpDisp.setEnabled(enabled);
        etAmpClamp.setEnabled(enabled);
        etDcDisp.setEnabled(enabled);
        etDcMet.setEnabled(enabled);
        etOutDisp.setEnabled(enabled);
        etOutMet.setEnabled(enabled);
        etRH.setEnabled(enabled);
        etReplay.setEnabled(enabled);
        etFan.setEnabled(enabled);
        etBody.setEnabled(enabled);
        etIO.setEnabled(enabled);
        etClean.setEnabled(enabled);
        etParam.setEnabled(enabled);

        setCheckboxEnabled(cb1HP, enabled);
        setCheckboxEnabled(cb10HP, enabled);
        setCheckboxEnabled(cb30HP, enabled);
        setCheckboxEnabled(cbAmpU, enabled);
        setCheckboxEnabled(cbAmpV, enabled);
        setCheckboxEnabled(cbAmpW, enabled);
    }

    private void enableRadioGroup(RadioGroup radioGroup, boolean enabled) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            View child = radioGroup.getChildAt(i);
            if (child instanceof RadioButton) {
                child.setEnabled(enabled);
            }
        }
    }

    private void setCheckboxEnabled(CheckBox checkBox, boolean enabled) {
        if (checkBox != null) {
            checkBox.setEnabled(enabled);
        }
    }

    private void showCancelConfirmation() {
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Discard Changes?")
                .setContentText("Are you sure you want to discard all changes?")
                .setConfirmText("Discard")
                .setCancelText("Continue Editing")
                .setConfirmButtonBackgroundColor(Color.parseColor("#FF0000"))
                .setCancelButtonBackgroundColor(Color.parseColor("#7C7C7C"))
                .showCancelButton(true)
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();
                    Log.d(TAG, "User confirmed discard changes");
                    loadData(); // Reload original data
                    setEditMode(false);
                })
                .setCancelClickListener(SweetAlertDialog::dismissWithAnimation)
                .show();
    }

    private void saveAllData() {
        Log.d(TAG, "Starting save all data process");

        updateClientFromUI();

        // Show confirmation dialog before saving
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Save Changes?")
                .setContentText("Are you sure you want to save all changes?")
                .setConfirmText("Save")
                .setCancelText("Cancel")
                .setConfirmButtonBackgroundColor(Color.parseColor("#28a745"))
                .setCancelButtonBackgroundColor(Color.parseColor("#7C7C7C"))
                .showCancelButton(true)
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();
                    long saveStartTime = System.currentTimeMillis();
                    saveToFirestore();

                    long saveEndTime = System.currentTimeMillis();
                    Log.d(TAG, "Save process completed in " + (saveEndTime - saveStartTime) + "ms");

                    // Show success dialog
                    new SweetAlertDialog(ClientDetailsActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Saved Successfully!")
                            .setContentText("All changes have been saved successfully.")
                            .setConfirmText("OK")
                            .setConfirmClickListener(d -> {
                                d.dismissWithAnimation();
                                setEditMode(false);
                            })
                            .show();
                })
                .setCancelClickListener(SweetAlertDialog::dismissWithAnimation)
                .show();
    }

    private void updateClientFromUI() {
        Log.d(TAG, "Updating client object from UI");

        // Fill One - EDITABLE
        client.name = etName.getText().toString();
        client.client_number = etNumber.getText().toString();
        client.gp_number = etGpNumber.getText().toString();
        client.gp_date = etGpDate.getText().toString();
        client.make_name = etMakeName.getText().toString();
        client.model_name = etModelName.getText().toString();
        client.hp_rate = etHprate.getText().toString();
        client.serial_number = etSrlNumber.getText().toString();

        // Fill Two
        client.localEditText = etLocalEditText.getText().toString();
        client.client_obs = etClientObs.getText().toString();
        client.our_obs = etOurObs.getText().toString();
        client.last_fault = etLastFault.getText().toString();

        client.local_radio_checked = rbLocal.isChecked();
        client.remote_radio_checked = rbRemote.isChecked();
        client.comm_radio_checked = rbComm.isChecked();
        client.diode_radio_checked = rbDiode.isChecked();
        client.scr_radio_checked = rbScr.isChecked();

        client.input_pos_checkbox_U = cbPosU.isChecked();
        client.input_pos_checkbox_V = cbPosV.isChecked();
        client.input_pos_checkbox_W = cbPosW.isChecked();
        client.input_neg_checkbox_U = cbNegU.isChecked();
        client.input_neg_checkbox_V = cbNegV.isChecked();
        client.input_neg_checkbox_W = cbNegW.isChecked();
        client.output_pos_checkbox_U = cbOutPosU.isChecked();
        client.output_pos_checkbox_V = cbOutPosV.isChecked();
        client.output_pos_checkbox_W = cbOutPosW.isChecked();
        client.output_neg_checkbox_U = cbOutNegU.isChecked();
        client.output_neg_checkbox_V = cbOutNegV.isChecked();
        client.output_neg_checkbox_W = cbOutNegW.isChecked();

        // Fill Three
        client.checkboxCapacitor = cbCapacitor.isChecked();
        client.checkboxDisplay = cbDisplay.isChecked();
        client.checkboxFAN = cbFan.isChecked();
        client.checkboxCC = cbCC.isChecked();

        client.repair_checkboxOne = cbRepair1.isChecked();
        client.repair_checkboxTwo = cbRepair2.isChecked();
        client.repair_checkboxThree = cbRepair3.isChecked();
        client.repair_checkboxFour = cbRepair4.isChecked();
        client.repair_checkboxFive = cbRepair5.isChecked();
        client.repair_checkboxSix = cbRepair6.isChecked();

        client.replace_checkboxOne = cbReplace1.isChecked();
        client.replace_checkboxTwo = cbReplace2.isChecked();
        client.replace_checkboxThree = cbReplace3.isChecked();
        client.replace_checkboxFour = cbReplace4.isChecked();
        client.replace_checkboxFive = cbReplace5.isChecked();
        client.replace_checkboxSix = cbReplace6.isChecked();
        client.replace_checkboxSeven = cbReplace7.isChecked();
        client.replace_checkboxEight = cbReplace8.isChecked();
        client.replace_checkboxNine = cbReplace9.isChecked();

        client.checkboxTrial1 = cbTrial1.isChecked();
        client.checkboxTrial2 = cbTrial2.isChecked();

        // Fill Four
        client.On_Display = etAmpDisp.getText().toString();
        client.On_Clamp = etAmpClamp.getText().toString();
        client.dc_dsp = etDcDisp.getText().toString();
        client.dc_met = etDcMet.getText().toString();
        client.output_dsp = etOutDisp.getText().toString();
        client.output_met = etOutMet.getText().toString();
        client.enter_RH = etRH.getText().toString();
        client.enterReplayOP = etReplay.getText().toString();
        client.enter_FANOpr = etFan.getText().toString();
        client.enter_BODY_Condition = etBody.getText().toString();
        client.enter_io_check = etIO.getText().toString();
        client.enterClean = etClean.getText().toString();
        client.enterPramCopy = etParam.getText().toString();

        client.checkbox_1HP = cb1HP.isChecked();
        client.checkbox_10HP = cb10HP.isChecked();
        client.checkbox_30HP = cb30HP.isChecked();
        client.checkbox_u = cbAmpU.isChecked();
        client.checkbox_v = cbAmpV.isChecked();
        client.checkbox_w = cbAmpW.isChecked();

        Log.d(TAG, "Client object updated successfully from UI");
    }

    private void saveToFirestore() {
        Log.d(TAG, "Starting Firestore save operation");
        long firestoreStartTime = System.currentTimeMillis();

        // Use batch write for faster multiple document updates
        WriteBatch batch = db.batch();

        // Save fill_one data
        DocumentReference fillOneRef = db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId).collection("pages").document("fill_one");

        Map<String, Object> fillOneData = new HashMap<>();
        fillOneData.put("name", client.name);
        fillOneData.put("client_number", client.client_number);
        fillOneData.put("gp_number", client.gp_number);
        fillOneData.put("gp_date", client.gp_date);
        fillOneData.put("make_name", client.make_name);
        fillOneData.put("model_name", client.model_name);
        fillOneData.put("hp_rate", client.hp_rate);
        fillOneData.put("serial_number", client.serial_number);

        batch.set(fillOneRef, fillOneData, SetOptions.merge());

        // Save fill_two data
        DocumentReference fillTwoRef = db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId).collection("pages").document("fill_two");

        Map<String, Object> fillTwoData = new HashMap<>();
        fillTwoData.put("localEditText", client.localEditText);
        fillTwoData.put("client_obs", client.client_obs);
        fillTwoData.put("our_obs", client.our_obs);
        fillTwoData.put("last_fault", client.last_fault);
        fillTwoData.put("local_radio_checked", client.local_radio_checked);
        fillTwoData.put("remote_radio_checked", client.remote_radio_checked);
        fillTwoData.put("comm_radio_checked", client.comm_radio_checked);
        fillTwoData.put("diode_radio_checked", client.diode_radio_checked);
        fillTwoData.put("scr_radio_checked", client.scr_radio_checked);
        fillTwoData.put("input_pos_checkbox_U", client.input_pos_checkbox_U);
        fillTwoData.put("input_pos_checkbox_V", client.input_pos_checkbox_V);
        fillTwoData.put("input_pos_checkbox_W", client.input_pos_checkbox_W);
        fillTwoData.put("input_neg_checkbox_U", client.input_neg_checkbox_U);
        fillTwoData.put("input_neg_checkbox_V", client.input_neg_checkbox_V);
        fillTwoData.put("input_neg_checkbox_W", client.input_neg_checkbox_W);
        fillTwoData.put("output_pos_checkbox_U", client.output_pos_checkbox_U);
        fillTwoData.put("output_pos_checkbox_V", client.output_pos_checkbox_V);
        fillTwoData.put("output_pos_checkbox_W", client.output_pos_checkbox_W);
        fillTwoData.put("output_neg_checkbox_U", client.output_neg_checkbox_U);
        fillTwoData.put("output_neg_checkbox_V", client.output_neg_checkbox_V);
        fillTwoData.put("output_neg_checkbox_W", client.output_neg_checkbox_W);

        batch.set(fillTwoRef, fillTwoData, SetOptions.merge());

        // Save fill_three data
        DocumentReference fillThreeRef = db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId).collection("pages").document("fill_three");

        Map<String, Object> fillThreeData = new HashMap<>();
        fillThreeData.put("checkboxCapacitor", client.checkboxCapacitor);
        fillThreeData.put("checkboxDisplay", client.checkboxDisplay);
        fillThreeData.put("checkboxFAN", client.checkboxFAN);
        fillThreeData.put("checkboxCC", client.checkboxCC);
        fillThreeData.put("repair_checkboxOne", client.repair_checkboxOne);
        fillThreeData.put("repair_checkboxTwo", client.repair_checkboxTwo);
        fillThreeData.put("repair_checkboxThree", client.repair_checkboxThree);
        fillThreeData.put("repair_checkboxFour", client.repair_checkboxFour);
        fillThreeData.put("repair_checkboxFive", client.repair_checkboxFive);
        fillThreeData.put("repair_checkboxSix", client.repair_checkboxSix);
        fillThreeData.put("replace_checkboxOne", client.replace_checkboxOne);
        fillThreeData.put("replace_checkboxTwo", client.replace_checkboxTwo);
        fillThreeData.put("replace_checkboxThree", client.replace_checkboxThree);
        fillThreeData.put("replace_checkboxFour", client.replace_checkboxFour);
        fillThreeData.put("replace_checkboxFive", client.replace_checkboxFive);
        fillThreeData.put("replace_checkboxSix", client.replace_checkboxSix);
        fillThreeData.put("replace_checkboxSeven", client.replace_checkboxSeven);
        fillThreeData.put("replace_checkboxEight", client.replace_checkboxEight);
        fillThreeData.put("replace_checkboxNine", client.replace_checkboxNine);
        fillThreeData.put("checkboxTrial1", client.checkboxTrial1);
        fillThreeData.put("checkboxTrial2", client.checkboxTrial2);

        batch.set(fillThreeRef, fillThreeData, SetOptions.merge());

        // Save fill_four data
        DocumentReference fillFourRef = db.collection("hi_tech_controls_dataset_JUNE")
                .document(clientId).collection("pages").document("fill_four");

        Map<String, Object> fillFourData = new HashMap<>();
        fillFourData.put("checkbox_1HP", client.checkbox_1HP);
        fillFourData.put("checkbox_10HP", client.checkbox_10HP);
        fillFourData.put("checkbox_30HP", client.checkbox_30HP);
        fillFourData.put("On_Display", client.On_Display);
        fillFourData.put("On_Clamp", client.On_Clamp);
        fillFourData.put("checkbox_u", client.checkbox_u);
        fillFourData.put("checkbox_v", client.checkbox_v);
        fillFourData.put("checkbox_w", client.checkbox_w);
        fillFourData.put("dc_dsp", client.dc_dsp);
        fillFourData.put("dc_met", client.dc_met);
        fillFourData.put("output_dsp", client.output_dsp);
        fillFourData.put("output_met", client.output_met);
        fillFourData.put("enter_RH", client.enter_RH);
        fillFourData.put("enterReplayOP", client.enterReplayOP);
        fillFourData.put("enter_FANOpr", client.enter_FANOpr);
        fillFourData.put("enter_BODY_Condition", client.enter_BODY_Condition);
        fillFourData.put("enter_io_check", client.enter_io_check);
        fillFourData.put("enterClean", client.enterClean);
        fillFourData.put("enterPramCopy", client.enterPramCopy);

        batch.set(fillFourRef, fillFourData, SetOptions.merge());

        // Commit the batch
        batch.commit().addOnSuccessListener(aVoid -> {
            long firestoreEndTime = System.currentTimeMillis();
            Log.d(TAG, "Firestore batch write completed successfully in " + (firestoreEndTime - firestoreStartTime) + "ms");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore batch write failed: " + e.getMessage(), e);
            Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void generateAndDownloadPdf() {
        Log.d(TAG, "Starting PDF generation process");

        // Check permissions first
        if (!checkStoragePermissions()) {
            return;
        }

        showProgressDialog("Generating PDF", "Please wait while we create your report...");

        try {
            PdfGenerator pdfGenerator = new PdfGenerator(this, client, clientId);

            new Thread(() -> {
                try {
                    File pdfFile = pdfGenerator.generate();

                    runOnUiThread(() -> {
                        dismissProgressDialog();
                        handlePdfSuccess(pdfFile);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "PDF generation failed: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        dismissProgressDialog();
                        handlePdfError(e);
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "PDF setup failed: " + e.getMessage(), e);
            dismissProgressDialog();
            handlePdfError(e);
        }
    }

    private void handlePdfSuccess(File pdfFile) {
        Log.d(TAG, "PDF generated successfully: " + pdfFile.getAbsolutePath());

        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("PDF Generated Successfully!")
                .setContentText("Your service report has been saved to:\n" +
                        pdfFile.getParentFile().getName() + "/" + pdfFile.getName())
                .setConfirmText("Open PDF")
                .setCancelText("Share")
                .showCancelButton(true)
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();
                    openPdfFile(pdfFile);
                })
                .setCancelClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();
                    sharePdfFile(pdfFile);
                })
                .show();

        // Add to gallery
        addPdfToGallery(pdfFile);
    }

    private void handlePdfError(Exception e) {
        Log.e(TAG, "PDF generation error: " + e.getMessage(), e);

        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("PDF Generation Failed")
                .setContentText("Error: " + e.getMessage() + "\n\nPlease check storage permissions and try again.")
                .setConfirmText("Retry")
                .setCancelText("Cancel")
                .showCancelButton(true)
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();
                    generateAndDownloadPdf();
                })
                .setCancelClickListener(SweetAlertDialog::dismissWithAnimation)
                .show();
    }

    private void openPdfFile(File pdfFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri pdfUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider", pdfFile);

            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Show no PDF viewer dialog
                new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("No PDF Viewer Found")
                        .setContentText("No PDF viewer app found on your device. Would you like to install one?")
                        .setConfirmText("Install")
                        .setCancelText("Cancel")
                        .showCancelButton(true)
                        .setConfirmClickListener(sDialog -> {
                            sDialog.dismissWithAnimation();
                            // Open Play Store to install PDF viewer
                            try {
                                Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                marketIntent.setData(Uri.parse("market://details?id=com.adobe.reader"));
                                startActivity(marketIntent);
                            } catch (Exception e) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                                browserIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.adobe.reader"));
                                startActivity(browserIntent);
                            }
                        })
                        .setCancelClickListener(SweetAlertDialog::dismissWithAnimation)
                        .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF: " + e.getMessage());
            Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdfFile(File pdfFile) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            Uri pdfUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider", pdfFile);

            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share PDF via"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing PDF: " + e.getMessage());
            Toast.makeText(this, "Error sharing PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void addPdfToGallery(File pdfFile) {
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(pdfFile);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);
            Log.d(TAG, "PDF added to gallery: " + pdfFile.getAbsolutePath());
        } catch (Exception e) {
            Log.w(TAG, "Failed to add PDF to gallery: " + e.getMessage());
        }
    }


    private void showProgressDialog(String title, String message) {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
            progressDialog.setTitleText(title);
            progressDialog.setContentText(message);
            progressDialog.setCancelable(false);
            progressDialog.show();
        });
    }

    private void dismissProgressDialog() {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Storage Permission Required")
                        .setContentText("PDF download requires storage permission to save files.")
                        .setConfirmText("Grant Permission")
                        .setCancelText("Cancel")
                        .showCancelButton(true)
                        .setConfirmClickListener(sDialog -> {
                            sDialog.dismissWithAnimation();
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
                        })
                        .setCancelClickListener(sDialog -> {
                            sDialog.dismissWithAnimation();
                            Toast.makeText(ClientDetailsActivity.this, "PDF download cancelled", Toast.LENGTH_SHORT).show();
                        })
                        .show();
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateAndDownloadPdf();
            } else {
                Toast.makeText(this, "Storage permission required for PDF download", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            showCancelConfirmation();
        } else {
            super.onBackPressed();
        }
    }


}