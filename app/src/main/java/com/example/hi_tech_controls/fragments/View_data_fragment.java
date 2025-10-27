package com.example.hi_tech_controls.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.InwardClient;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class View_data_fragment extends Fragment {

    private final int W = 1080, H = 1500, M = 60;
    private final List<PdfDocument.Page> pages = new ArrayList<>();
    private View rootView;
    private InwardClient client;
    private PdfDocument pdf;
    private Canvas canvas;
    private int y = 100, page = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_data_fragement, container, false);
        if (getArguments() != null) {
            String id = getArguments().getString("clientId");
            client = new InwardClient();
            fetchData(id);
        }
        return rootView;
    }

    private void fetchData(String id) {
        FirebaseFirestore.getInstance()
                .collection("hi_tech_controls_dataset_JUNE")
                .document(id).collection("pages").get()
                .addOnSuccessListener(s -> {
                    for (DocumentSnapshot d : s) {
                        switch (d.getId()) {
                            case "fill_one":
                                fillOne(client, d);
                                break;
                            case "fill_two":
                                fillTwo(client, d);
                                break;
                            case "fill_three":
                                fillThree(client, d);
                                break;
                            case "fill_four":
                                fillFour(client, d);
                                break;
                        }
                    }
                    requireActivity().runOnUiThread(() -> {
                        populateUI(client);
                        rootView.findViewById(R.id.printpdf).setOnClickListener(v -> generatePDF(client));
                    });
                });
    }

    // === FILL DATA (Same as before) ===
    private void fillOne(InwardClient c, DocumentSnapshot d) {
        c.name = d.getString("name");
        c.client_number = d.getString("client_number");
        c.gp_number = d.getString("gp_number");
        c.gp_date = d.getString("gp_date");
        c.make_name = d.getString("make_name");
        c.model_name = d.getString("model_name");
        c.hp_rate = d.getString("hp_rate");
        c.serial_number = d.getString("serial_number");
    }

    private void fillTwo(InwardClient c, DocumentSnapshot d) {
        c.select_emp_fill_one = d.getString("select_emp");
        c.fr_rate = d.getString("fr_rate");
        c.localEditText = d.getString("localEditText");
        c.client_obs = d.getString("client_obs");
        c.our_obs = d.getString("our_obs");
        c.last_fault = d.getString("last_fault");
        c.local_radio_checked = b(d, "local_radio_checked");
        c.remote_radio_checked = b(d, "remote_radio_checked");
        c.comm_radio_checked = b(d, "comm_radio_checked");
        c.diode_radio_checked = b(d, "diode_radio_checked");
        c.scr_radio_checked = b(d, "scr_radio_checked");
        c.input_pos_checkbox_U = b(d, "input_pos_checkbox_U");
        c.input_pos_checkbox_V = b(d, "input_pos_checkbox_V");
        c.input_pos_checkbox_W = b(d, "input_pos_checkbox_W");
        c.input_neg_checkbox_U = b(d, "input_neg_checkbox_U");
        c.input_neg_checkbox_V = b(d, "input_neg_checkbox_V");
        c.input_neg_checkbox_W = b(d, "input_neg_checkbox_W");
    }

    private void fillThree(InwardClient c, DocumentSnapshot d) {
        c.select_emp_fill_three = d.getString("select_emp");
        c.enter_first_remarks = d.getString("enter_first_remarks");
        c.checkboxCapacitor = b(d, "checkboxCapacitor");
        c.checkboxDisplay = b(d, "checkboxDisplay");
        c.checkboxFAN = b(d, "checkboxFAN");
        c.checkboxCC = b(d, "checkboxCC");
        c.repair_checkboxOne = b(d, "repair_checkboxOne"); // SMPS
        c.repair_checkboxTwo = b(d, "repair_checkboxTwo"); // CT Station
        c.repair_checkboxThree = b(d, "repair_checkboxThree"); // Firing
        c.repair_checkboxFour = b(d, "repair_checkboxFour"); // Control Card
        c.repair_checkboxFive = b(d, "repair_checkboxFive"); // DC Station
        c.repair_checkboxSix = b(d, "repair_checkboxSix"); // Other
        c.replace_checkboxOne = b(d, "replace_checkboxOne"); // IGBT
        c.replace_checkboxTwo = b(d, "replace_checkboxTwo"); // CT
        c.replace_checkboxThree = b(d, "replace_checkboxThree");
        c.replace_checkboxFour = b(d, "replace_checkboxFour");
        c.replace_checkboxFive = b(d, "replace_checkboxFive");
        c.replace_checkboxSix = b(d, "replace_checkboxSix");
        c.replace_checkboxSeven = b(d, "replace_checkboxSeven");
        c.replace_checkboxEight = b(d, "replace_checkboxEight");
        c.replace_checkboxNine = b(d, "replace_checkboxNine");
        c.checkboxTrial1 = b(d, "checkboxTrial1");
        c.checkboxTrial2 = b(d, "checkboxTrial2");
        Long v = d.getLong("number_picker_value");
        c.number_picker_value = v != null ? v.intValue() : 1;
    }

    private void fillFour(InwardClient c, DocumentSnapshot d) {
        c.select_emp_fill_four = d.getString("select_emp");
        c.checkbox_1HP = b(d, "checkbox_1HP");
        c.checkbox_10HP = b(d, "checkbox_10HP");
        c.checkbox_30HP = b(d, "checkbox_30HP");
        c.On_Display = d.getString("On_Display");
        c.On_Clamp = d.getString("On_Clamp");
        c.checkbox_u = b(d, "checkbox_u");
        c.checkbox_v = b(d, "checkbox_v");
        c.checkbox_w = b(d, "checkbox_w");
        c.dc_dsp = d.getString("dc_dsp");
        c.dc_met = d.getString("dc_met");
        c.output_dsp = d.getString("output_dsp");
        c.output_met = d.getString("output_met");
        c.enter_RH = d.getString("enter_RH");
        c.enterReplayOP = d.getString("enterReplayOP");
        c.enter_FANOpr = d.getString("enter_FANOpr");
        c.enter_BODY_Condition = d.getString("enter_BODY_Condition");
        c.enter_io_check = d.getString("enter_io_check");
        c.enterClean = d.getString("enterClean");
        c.enterPramCopy = d.getString("enterPramCopy");
    }

    private void populateUI(InwardClient c) {
        // === UI SETTING (same as your original) ===
        setText(R.id.client_name, "Client: " + g(c.name));
        setText(R.id.client_no, "No: " + g(c.client_number));
        setText(R.id.GP_no, "GP: " + g(c.gp_number));
        setText(R.id.Gp_date, "Date: " + g(c.gp_date));
        setText(R.id.Make_name, "Make: " + g(c.make_name));
        setText(R.id.Model_name, "Model: " + g(c.model_name));
        setText(R.id.Rating_hp, "HP: " + g(c.hp_rate));
        setText(R.id.Serial_no, "S/N: " + g(c.serial_number));

        setText(R.id.fill_two_selectEmply, g(c.select_emp_fill_two));
        setEdit(R.id.fill_two_enterFRrate, g(c.fr_rate));
        setRadio(R.id.fill_two_radioButtonLocal, c.local_radio_checked);
        setRadio(R.id.fill_two_radioButtonRemote, c.remote_radio_checked);
        setRadio(R.id.fill_two_radioButtonComm, c.comm_radio_checked);
        setEdit(R.id.fill_two_localEditText, g(c.localEditText));
        setRadio(R.id.fill_two_radioButtonDIODE, c.diode_radio_checked);
        setRadio(R.id.fill_two_radioButtonSCR, c.scr_radio_checked);

        setCB(R.id.fill_two_input_POS_checkbox_U, c.input_pos_checkbox_U);
        setCB(R.id.fill_two_input_POS_checkbox_V, c.input_pos_checkbox_V);
        setCB(R.id.fill_two_input_POS_checkbox_W, c.input_pos_checkbox_W);
        setCB(R.id.fill_two_input_NEG_checkbox_U, c.input_neg_checkbox_U);
        setCB(R.id.fill_two_input_NEG_checkbox_V, c.input_neg_checkbox_V);
        setCB(R.id.fill_two_input_NEG_checkbox_W, c.input_neg_checkbox_W);

        setEdit(R.id.fill_two_clientObs_text, g(c.client_obs));
        setEdit(R.id.fill_two_ourObs_text, g(c.our_obs));
        setEdit(R.id.fill_two_lastFault_text, g(c.last_fault));

        setText(R.id.fill_three_selectEmply, g(c.select_emp_fill_three));
        setEdit(R.id.fill_three_firstRemarks, g(c.enter_first_remarks));
        setCB(R.id.fill_three_checkboxCapasitor, c.checkboxCapacitor);
        setCB(R.id.fill_three_checkboxDisplay, c.checkboxDisplay);
        setCB(R.id.fill_three_checkboxFAN, c.checkboxFAN);
        setCB(R.id.fill_three_checkboxCC, c.checkboxCC);
        setCB(R.id.fill_three_Repair_checkboxOne, c.repair_checkboxOne);
        setCB(R.id.fill_three_Repair_checkboxTwo, c.repair_checkboxTwo);
        setCB(R.id.fill_three_Repair_checkboxThree, c.repair_checkboxThree);
        setCB(R.id.fill_three_Repair_checkboxFour, c.repair_checkboxFour);
        setCB(R.id.fill_three_Repair_checkboxFive, c.repair_checkboxFive);
        setCB(R.id.fill_three_Repair_checkboxSix, c.repair_checkboxSix);
        setCB(R.id.fill_three_Replace_checkboxOne, c.replace_checkboxOne);
        setCB(R.id.fill_three_Replace_checkboxTwo, c.replace_checkboxTwo);
        setCB(R.id.fill_three_Replace_checkboxThree, c.replace_checkboxThree);
        setCB(R.id.fill_three_Replace_checkboxFour, c.replace_checkboxFour);
        setCB(R.id.fill_three_Replace_checkboxFive, c.replace_checkboxFive);
        setCB(R.id.fill_three_Replace_checkboxSix, c.replace_checkboxSix);
        setCB(R.id.fill_three_Replace_checkboxSeven, c.replace_checkboxSeven);
        setCB(R.id.fill_three_Replace_checkboxEight, c.replace_checkboxEight);
        setCB(R.id.fill_three_Replace_checkboxNine, c.replace_checkboxNine);
        setCB(R.id.fill_three_checkboxTRIAL1, c.checkboxTrial1);
        setCB(R.id.fill_three_checkboxTRIAL2, c.checkboxTrial2);
        TextView tv = rootView.findViewById(R.id.textDays);
        if (tv != null) tv.setText(String.valueOf(c.number_picker_value));

        setText(R.id.fill_four_selectEmply, g(c.select_emp_fill_four));
        setCB(R.id.fill_four_checkbox1HP, c.checkbox_1HP);
        setCB(R.id.fill_four_checkbox10HP, c.checkbox_10HP);
        setCB(R.id.fill_four_checkbox30HP, c.checkbox_30HP);
        setEdit(R.id.fill_four_OnDisplay_text, g(c.On_Display));
        setEdit(R.id.fill_four_OnClamp_text, g(c.On_Clamp));
        setCB(R.id.fill_four_checkboxAMP_U, c.checkbox_u);
        setCB(R.id.fill_four_checkboxAMP_V, c.checkbox_v);
        setCB(R.id.fill_four_checkboxAMP_W, c.checkbox_w);
        setEdit(R.id.fill_four_DC_DISP_text, g(c.dc_dsp));
        setEdit(R.id.fill_four_DC_MET_text, g(c.dc_met));
        setEdit(R.id.fill_four_OUTPUT_DISP_text, g(c.output_dsp));
        setEdit(R.id.fill_four_OUTPUT_MET_text, g(c.output_met));
        setEdit(R.id.fill_four_enterRH_text, g(c.enter_RH));
        setEdit(R.id.fill_four_enterReplayOP_text, g(c.enterReplayOP));
        setEdit(R.id.fill_four_enterFANOpr_text, g(c.enter_FANOpr));
        setEdit(R.id.fill_four_enterBODYCondition_text, g(c.enter_BODY_Condition));
        setEdit(R.id.fill_four_enterIOcheck_text, g(c.enter_io_check));
        setEdit(R.id.fill_four_enterClean_Text, g(c.enterClean));
        setEdit(R.id.fill_four_enterPramCopy_Text, g(c.enterPramCopy));
    }

    // ================================= PDF: 4 SECTIONS IN ONE TABLE =================================
    private void generatePDF(InwardClient c) {
        pdf = new PdfDocument();
        pages.clear();
        page = 1;
        y = 100;
        newPage();
        drawHeader();
        drawBigTable(c);
        sign(g(c.select_emp_fill_one + c.select_emp_fill_two + c.select_emp_fill_three + c.select_emp_fill_four));
        footer();
        for (PdfDocument.Page p : pages) pdf.finishPage(p);
        save(c);
    }

    private void newPage() {
        PdfDocument.PageInfo i = new PdfDocument.PageInfo.Builder(W, H, page++).create();
        PdfDocument.Page p = pdf.startPage(i);
        canvas = p.getCanvas();
        pages.add(p);
        y = 100;
    }

    private void need(int h) {
        if (y + h > H - 120) {
            footer();
            pdf.finishPage(pages.remove(pages.size() - 1));
            newPage();
            drawHeader();
        }
    }

    private void drawHeader() {
        Paint red = new Paint();
        red.setColor(Color.rgb(220, 53, 69));
        Paint bold = new Paint();
        bold.setTextSize(36);
        bold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        Paint normal = new Paint();
        normal.setTextSize(20);

        canvas.drawRect(M, y, W - M, y + 80, red);
        bold.setColor(Color.WHITE);
        canvas.drawText("HI-TECH CONTROLS", W / 2 - 150, y + 55, bold);
        y += 90;

        bold.setColor(Color.BLACK);
        canvas.drawText("DRIVE REPAIR REPORT", W / 2 - 160, y, bold);
        y += 60;
    }

    private void drawBigTable(InwardClient c) {
        Paint blue = new Paint();
        blue.setColor(Color.rgb(0, 123, 255));
        Paint orange = new Paint();
        orange.setColor(Color.rgb(255, 159, 64));
        Paint border = new Paint();
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1.5f);
        Paint txt = new Paint();
        txt.setTextSize(19);
        Paint bold = new Paint();
        bold.setTextSize(20);
        bold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        int left = M, col1 = 320, col2 = 600, rowH = 40;

        // Helper
        java.util.function.Consumer<String[]> row = data -> {
            need(rowH);
            canvas.drawRect(left, y, W - M, y + rowH, border);
            canvas.drawText(data[0], left + 15, y + 28, txt);
            canvas.drawText(data[1], col1 + 15, y + 28, txt);
            if (data.length > 2 && data[2].equals("true"))
                drawCheckBox(canvas, txt, col2 + 15, y + 8, true);
            y += rowH;
        };

        // === SECTION 1 ===
        canvas.drawRect(left, y, W - M, y + rowH, blue);
        canvas.drawRect(left, y, W - M, y + rowH, border);
        bold.setColor(Color.WHITE);
        canvas.drawText("1. CLIENT & DRIVE DETAILS", left + 15, y + 28, bold);
        bold.setColor(Color.BLACK);
        y += rowH;

        row.accept(new String[]{"Client Name", g(c.name)});
        row.accept(new String[]{"GP No. & Date", g(c.gp_number) + " | " + g(c.gp_date)});
        row.accept(new String[]{"Make / Model", g(c.make_name) + " / " + g(c.model_name)});
        row.accept(new String[]{"HP Rating", g(c.hp_rate)});
        row.accept(new String[]{"Serial No.", g(c.serial_number)});
        y += 10;

        // === SECTION 2 ===
        canvas.drawRect(left, y, W - M, y + rowH, orange);
        canvas.drawRect(left, y, W - M, y + rowH, border);
        bold.setColor(Color.BLACK);
        canvas.drawText("2. INITIAL OBSERVATIONS", left + 15, y + 28, bold);
        y += rowH;

        row.accept(new String[]{"Freq. Reference", g(c.fr_rate)});
        row.accept(new String[]{"Start Command", startCmd(c)});
        row.accept(new String[]{"Local Edit", g(c.localEditText)});
        row.accept(new String[]{"Input Supply", inputSupply(c)});
        row.accept(new String[]{"Client Observation", g(c.client_obs)});
        row.accept(new String[]{"Our Observation", g(c.our_obs)});
        row.accept(new String[]{"Last Fault", g(c.last_fault)});

        row.accept(new String[]{"Input POS U", "", c.input_pos_checkbox_U + ""});
        row.accept(new String[]{"Input POS V", "", c.input_pos_checkbox_V + ""});
        row.accept(new String[]{"Input POS W", "", c.input_pos_checkbox_W + ""});
        row.accept(new String[]{"Input NEG U", "", c.input_neg_checkbox_U + ""});
        row.accept(new String[]{"Input NEG V", "", c.input_neg_checkbox_V + ""});
        row.accept(new String[]{"Input NEG W", "", c.input_neg_checkbox_W + ""});
        y += 10;

        // === SECTION 3 ===
        canvas.drawRect(left, y, W - M, y + rowH, blue);
        canvas.drawRect(left, y, W - M, y + rowH, border);
        bold.setColor(Color.WHITE);
        canvas.drawText("3. REPAIR & REPLACE", left + 15, y + 28, bold);
        bold.setColor(Color.BLACK);
        y += rowH;

        row.accept(new String[]{"Capacitor", "", c.checkboxCapacitor + ""});
        row.accept(new String[]{"Display", "", c.checkboxDisplay + ""});
        row.accept(new String[]{"FAN", "", c.checkboxFAN + ""});
        row.accept(new String[]{"Control Card", "", c.checkboxCC + ""});

        row.accept(new String[]{"Repair IGBT", "", c.repair_checkboxOne + ""});
        row.accept(new String[]{"Repair SCR", "", c.repair_checkboxTwo + ""});
        row.accept(new String[]{"Repair DIODE", "", c.repair_checkboxThree + ""});
        row.accept(new String[]{"Repair CAP", "", c.repair_checkboxFour + ""});
        row.accept(new String[]{"Repair FAN", "", c.repair_checkboxFive + ""});
        row.accept(new String[]{"Repair DISPLAY", "", c.repair_checkboxSix + ""});

        row.accept(new String[]{"Replace IGBT", "", c.replace_checkboxOne + ""});
        row.accept(new String[]{"Replace CONTROL CARD", "", c.replace_checkboxSeven + ""});
        y += 10;

        // === SECTION 4 ===
        canvas.drawRect(left, y, W - M, y + rowH, orange);
        canvas.drawRect(left, y, W - M, y + rowH, border);
        bold.setColor(Color.BLACK);
        canvas.drawText("4. FINAL TRIAL CHECK", left + 15, y + 28, bold);
        y += rowH;

        row.accept(new String[]{"Motor Load 1HP", "", c.checkbox_1HP + ""});
        row.accept(new String[]{"Motor Load 10HP", "", c.checkbox_10HP + ""});
        row.accept(new String[]{"Motor Load 30HP", "", c.checkbox_30HP + ""});

        row.accept(new String[]{"On Display (AMP)", g(c.On_Display)});
        row.accept(new String[]{"On Clamp (AMP)", g(c.On_Clamp)});
        row.accept(new String[]{"AMP U", "", c.checkbox_u + ""});
        row.accept(new String[]{"AMP V", "", c.checkbox_v + ""});
        row.accept(new String[]{"AMP W", "", c.checkbox_w + ""});

        row.accept(new String[]{"DC Voltage (Display)", g(c.dc_dsp)});
        row.accept(new String[]{"DC Voltage (Meter)", g(c.dc_met)});
        row.accept(new String[]{"Output Voltage (Display)", g(c.output_dsp)});
        row.accept(new String[]{"Output Voltage (Meter)", g(c.output_met)});

        row.accept(new String[]{"Run Hours", g(c.enter_RH)});
        row.accept(new String[]{"Replay O/P", g(c.enterReplayOP)});
        row.accept(new String[]{"FAN Operation", g(c.enter_FANOpr)});
        row.accept(new String[]{"Body Condition", g(c.enter_BODY_Condition)});
        row.accept(new String[]{"I/O Check", g(c.enter_io_check)});
        row.accept(new String[]{"Cleanliness", g(c.enterClean)});
        row.accept(new String[]{"Parameter Copy", g(c.enterPramCopy)});
    }

    private void drawCheckBox(Canvas canvas, Paint paint, float x, float y, boolean checked) {
        float s = 26;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        canvas.drawRect(x, y, x + s, y + s, paint);
        if (checked) {
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(4);
            canvas.drawLine(x + 5, y + s / 2, x + s / 3 + 3, y + s - 7, paint);
            canvas.drawLine(x + s / 3 + 1, y + s - 7, x + s - 5, y + 5, paint);
        }
    }

    private void sign(String name) {
        need(120);
        Paint p = new Paint();
        p.setTextSize(20);
        canvas.drawText("DIGITAL SIGNATURE", M, y, p);
        canvas.drawLine(M, y + 10, M + 350, y + 10, p);
        canvas.drawText("Technician: " + name, M, y += 40, p);
        canvas.drawText("Date: " + new SimpleDateFormat("dd MMM yyyy").format(new Date()), M, y += 30, p);
    }

    private void footer() {
        Paint p = new Paint();
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(16);
        String d = new SimpleDateFormat("dd MMM yyyy, HH:mm").format(new Date());
        canvas.drawText("Page " + (page - 1) + " | " + d, W / 2, H - 50, p);
        canvas.drawText("© Hi-Tech Controls 2025", W / 2, H - 25, p);
    }

    private void save(InwardClient c) {
        String fn = "Repair_Report_" + g(c.name).replace(" ", "_") + ".pdf";
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fn);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            pdf.writeTo(fos);
            pdf.close();
            Toast.makeText(requireContext(), "PDF Saved: " + fn, Toast.LENGTH_LONG).show();
            open(f);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void open(File f) {
        Uri u = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", f);
        startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/pdf")
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    // === HELPERS ===
    private String g(String s) {
        return s != null ? s : "";
    }

    // Fix: Boolean to String conversion
    private boolean b(DocumentSnapshot d, String f) {
        Object v = d.get(f);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return "true".equalsIgnoreCase((String) v);
        return false;
    }

    private String startCmd(InwardClient c) {
        return c.local_radio_checked ? "Local" : c.remote_radio_checked ? "Remote" : c.comm_radio_checked ? "Comm" : "";
    }

    private String inputSupply(InwardClient c) {
        return c.diode_radio_checked ? "DIODE" : c.scr_radio_checked ? "SCR" : "";
    }

    private void setText(int id, String t) {
        View v = rootView.findViewById(id);
        if (v instanceof TextView) ((TextView) v).setText(t);
    }

    private void setEdit(int id, String t) {
        View v = rootView.findViewById(id);
        if (v instanceof EditText) {
            ((EditText) v).setText(t);
            v.setEnabled(false);
        }
    }

    private void setCB(int id, boolean c) {
        View v = rootView.findViewById(id);
        if (v instanceof CheckBox) {
            ((CheckBox) v).setChecked(c);
            v.setEnabled(false);
        }
    }

    private void setRadio(int id, boolean c) {
        View v = rootView.findViewById(id);
        if (v instanceof RadioButton) {
            ((RadioButton) v).setChecked(c);
            v.setEnabled(false);
        }
    }
}