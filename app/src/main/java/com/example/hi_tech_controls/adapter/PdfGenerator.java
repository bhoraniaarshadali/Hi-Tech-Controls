package com.example.hi_tech_controls.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import com.example.hi_tech_controls.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfGenerator {
    private static final int W = 1080, H = 1500, M = 60;
    private static final String TAG = "PdfGenerator";
    private final InwardClient client;
    private final Context context;
    private final String reportId;
    private final String clientId; // ✅ Naya field add kiya hai
    private PdfDocument pdf;
    private List<PdfDocument.Page> pages;
    private Canvas canvas;
    private int y;
    private int currentPageNumber;

    // ✅ Constructor update kiya hai clientId parameter ke saath
    public PdfGenerator(Context context, InwardClient client, String clientId) {
        this.context = context;
        this.client = client;
        this.clientId = clientId; // ✅ Client ID store kiya hai
        this.reportId = "HT" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
    }

    // ==================== MAIN GENERATE METHOD ====================
    public File generate() throws Exception {
        pdf = new PdfDocument();
        pages = new ArrayList<>();
        currentPageNumber = 1;

        try {
            startNewPage();
            drawHeader();
            drawWatermark();
            drawAllSections();
            //drawSignature();
            drawFooterWithQR();

            if (!pages.isEmpty()) {
                pdf.finishPage(pages.get(pages.size() - 1));
            }

            return savePdf();
        } finally {
            if (pdf != null) {
                pdf.close();
            }
        }
    }

    // ==================== PAGE HANDLING ====================
    private void startNewPage() {
        if (canvas != null && !pages.isEmpty()) {
            pdf.finishPage(pages.get(pages.size() - 1));
        }
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(W, H, currentPageNumber).create();
        PdfDocument.Page newPage = pdf.startPage(pageInfo);
        pages.add(newPage);
        canvas = newPage.getCanvas();
        y = 120;
        currentPageNumber++;
    }

    private void checkSpace(int requiredHeight) {
        if (y + requiredHeight > H - 200) {
            drawFooterWithQR();
            startNewPage();
            drawHeader();
            drawWatermark();
        }
    }

    // ==================== SAVE PDF WITH BETTER ERROR HANDLING ====================
    private File savePdf() throws Exception {
        String fileName = "Service_Report_" + (clientId != null ? clientId + "_" : "") + reportId + ".pdf";
        // Try multiple storage locations
        File file = null;
        Exception lastException = null;

        // Try 1: External Downloads (only if permissions granted)
        if (hasStoragePermissions()) {
            try {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File hiTechDir = new File(downloadsDir, "HiTech_Reports");

                if (!hiTechDir.exists()) {
                    boolean dirCreated = hiTechDir.mkdirs();
                    if (!dirCreated) {
                        Log.w(TAG, "Failed to create directory: " + hiTechDir.getAbsolutePath());
                    }
                }

                if (hiTechDir.exists() && hiTechDir.isDirectory()) {
                    file = new File(hiTechDir, fileName);
                } else {
                    file = new File(downloadsDir, fileName);
                    Log.w(TAG, "Using fallback location: " + file.getAbsolutePath());
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    pdf.writeTo(fos);
                    Log.i(TAG, "PDF saved in external storage: " + file.getAbsolutePath());
                    return file;
                }

            } catch (Exception e) {
                lastException = e;
                Log.e(TAG, "Failed to save in external storage: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Storage permissions not granted, skipping external storage");
        }

        // Try 2: Internal storage fallback
        try {
            File internalDir = new File(context.getFilesDir(), "HiTech_Reports");
            if (!internalDir.exists()) {
                internalDir.mkdirs();
            }
            file = new File(internalDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdf.writeTo(fos);
                Log.i(TAG, "PDF saved in internal storage: " + file.getAbsolutePath());
                return file;
            }

        } catch (Exception e) {
            lastException = e;
            Log.e(TAG, "Failed to save in internal storage: " + e.getMessage());
        }

        // Try 3: Cache directory as last resort
        try {
            File cacheDir = new File(context.getCacheDir(), "HiTech_Reports");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            file = new File(cacheDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdf.writeTo(fos);
                Log.i(TAG, "PDF saved in cache: " + file.getAbsolutePath());
                return file;
            }

        } catch (Exception e) {
            lastException = e;
            Log.e(TAG, "Failed to save in cache: " + e.getMessage());
        }

        if (lastException != null) {
            throw lastException;
        } else {
            throw new Exception("Failed to save PDF in any storage location");
        }
    }

    // Check storage permissions
    private boolean hasStoragePermissions() {
        return PermissionUtils.hasStoragePermissions(context);
    }

    // ==================== WATERMARK ====================
    private void drawWatermark() {
        try {
            Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.logo);
            if (logo != null) {
                Paint watermarkPaint = new Paint();
                watermarkPaint.setAlpha(15); // Very transparent

                int watermarkSize = 400;
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, watermarkSize, watermarkSize, true);

                int x = (W - watermarkSize) / 2;
                int watermarkY = (H - watermarkSize) / 2;
                canvas.drawBitmap(scaledLogo, x, watermarkY, watermarkPaint);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to draw watermark: " + e.getMessage());
        }
    }

    // ==================== HEADER ====================
    private void drawHeader() {
        Paint bgPaint = getPaint(Color.rgb(240, 240, 240), Paint.Style.FILL);
        canvas.drawRect(0, 0, W, 110, bgPaint);

        Paint logoText = getTextPaint(34, Typeface.BOLD, Color.rgb(30, 30, 30));
        Paint subText = getTextPaint(20, Typeface.NORMAL, Color.DKGRAY);

        // Logo (left)
        try {
            Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.logo);
            if (logo != null) {
                int logoHeight = 80;
                float ratio = (float) logo.getWidth() / logo.getHeight();
                int logoWidth = (int) (logoHeight * ratio);
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, false);
                canvas.drawBitmap(scaledLogo, M, 15, null);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to draw logo: " + e.getMessage());
        }

        // Title (center)
        canvas.drawText("HI-TECH CONTROLS", W / 2 - 180, 55, logoText);
        canvas.drawText("DRIVE REPAIR SERVICE REPORT", W / 2 - 180, 90, subText);

        // Report ID aur Client ID (right side) - UPDATED
        Paint idPaint = getTextPaint(18, Typeface.BOLD, Color.rgb(60, 120, 240));

        // Client ID added
        String clientIdText = "Client ID: " + (clientId != null ? clientId : "N/A");
        //String reportIdText = "Report ID: " + reportId;

        float clientIdWidth = idPaint.measureText(clientIdText);
        //float reportIdWidth = idPaint.measureText(reportIdText);

        // Client ID top par
        canvas.drawText(clientIdText, W - M - clientIdWidth - 10, 40, idPaint);
        // Report ID niche
        //canvas.drawText(reportIdText, W - M - reportIdWidth - 10, 70, idPaint);

        // Line separator
        Paint line = getPaint(Color.LTGRAY, Paint.Style.STROKE);
        line.setStrokeWidth(2);
        canvas.drawLine(M, 110, W - M, 110, line);
    }

    // ==================== ALL SECTIONS IN SINGLE FLOW ====================
    private void drawAllSections() {
        drawSection1(); // CLIENT INFORMATION
        drawSection2(); // INITIAL OBSERVATIONS
        drawSection3(); // INITIAL CHECK DETAILS
        drawSection4(); // FINAL TRIAL CHECK
    }

    // ==================== SECTION 1: CLIENT INFORMATION ====================
    private void drawSection1() {
        Paint accent = getPaint(Color.rgb(60, 120, 240), Paint.Style.FILL);
        Paint border = getPaint(Color.LTGRAY, Paint.Style.STROKE);
        Paint text = getTextPaint(20, Typeface.NORMAL, Color.BLACK);
        Paint bold = getTextPaint(22, Typeface.BOLD, Color.BLACK);
        Paint label = getTextPaint(19, Typeface.NORMAL, Color.DKGRAY);

        int left = M, col1 = 400, rowHeight = 45;

        drawMainHeading("1. CLIENT INFORMATION", accent, bold, left, rowHeight);

        drawDataRow("Client Id", clientId, left, col1, rowHeight, label, text, border);
        drawDataRow("Client Name", client.name, left, col1, rowHeight, label, text, border);

        drawDataRow("Client Number", client.client_number, left, col1, rowHeight, label, text, border);
        drawDataRow("GP Number", client.gp_number, left, col1, rowHeight, label, text, border);
        drawDataRow("GP Date", client.gp_date, left, col1, rowHeight, label, text, border);
        drawDataRow("Make Name", client.make_name, left, col1, rowHeight, label, text, border);
        drawDataRow("Model", client.model_name, left, col1, rowHeight, label, text, border);
        drawDataRow("Rating (HP)", client.hp_rate, left, col1, rowHeight, label, text, border);
        drawDataRow("Serial Number", client.serial_number, left, col1, rowHeight, label, text, border);

        y += 20;
    }

    // ==================== SECTION 2: INITIAL OBSERVATIONS ====================
    private void drawSection2() {
        Paint accent = getPaint(Color.rgb(60, 120, 240), Paint.Style.FILL);
        Paint border = getPaint(Color.LTGRAY, Paint.Style.STROKE);
        Paint text = getTextPaint(20, Typeface.NORMAL, Color.BLACK);
        Paint bold = getTextPaint(22, Typeface.BOLD, Color.BLACK);
        Paint label = getTextPaint(19, Typeface.NORMAL, Color.DKGRAY);

        int left = M, col1 = 400, col2 = 400, rowHeight = 45;

        drawMainHeading("2. INITIAL OBSERVATIONS", accent, bold, left, rowHeight);

        drawDataRow("Checked by", client.select_emp_fill_two, left, col1, rowHeight, label, text, border);
        drawDataRow("Frequency Ref.", client.fr_rate, left, col1, rowHeight, label, text, border);

        drawSubHeading("Start Command", left, text, border);
        drawRadioRow("Local / Remote / Communication",
                new boolean[]{client.local_radio_checked, client.remote_radio_checked, client.comm_radio_checked},
                left, col2, rowHeight, text, border);
        drawDataRow("Local Edit", client.localEditText, left, col1, rowHeight, label, text, border);

        drawSubHeading("Input Supply", left, text, border);
        drawRadioRow("DIODE / SCR",
                new boolean[]{client.diode_radio_checked, client.scr_radio_checked},
                left, col2, rowHeight, text, border);


        drawSubHeading("POS (U/V/W)", left, text, border);
        drawCheckboxGroup("U / V / W",
                new boolean[]{client.input_pos_checkbox_U, client.input_pos_checkbox_V, client.input_pos_checkbox_W},
                left, col2, rowHeight, text, border);

        drawSubHeading("NEG (U/V/W)", left, text, border);
        drawCheckboxGroup("U / V / W",
                new boolean[]{client.input_neg_checkbox_U, client.input_neg_checkbox_V, client.input_neg_checkbox_W},
                left, col2, rowHeight, text, border);

        //drawMainHeading("FAULT DIAGNOSIS", accent, bold, left, rowHeight);
        drawSubHeading("Fault Diagnosis", left, text, border);
        drawDataRow("Client Observation", client.client_obs, left, col1, rowHeight, label, text, border);
        drawDataRow("Our Observation", client.our_obs, left, col1, rowHeight, label, text, border);
        drawDataRow("Last Fault", client.last_fault, left, col1, rowHeight, label, text, border);

        y += 20;
        drawFooterWithQR();
        //startNewPage(); // force new page before this section
    }

    // ==================== SECTION 3: INITIAL CHECK DETAILS ====================
    private void drawSection3() {
        startNewPage(); //Start fresh page
        Paint accent = getPaint(Color.rgb(60, 120, 240), Paint.Style.FILL);
        Paint accentLight = getPaint(Color.rgb(110, 158, 255), Paint.Style.FILL);

        Paint border = getPaint(Color.LTGRAY, Paint.Style.STROKE);
        Paint text = getTextPaint(20, Typeface.NORMAL, Color.BLACK);
        Paint bold = getTextPaint(22, Typeface.BOLD, Color.BLACK);
        Paint label = getTextPaint(19, Typeface.NORMAL, Color.DKGRAY);

        int left = M, col1 = 400, col2 = 400, rowHeight = 45;

        drawMainHeading("3. INITIAL CHECK DETAILS", accent, bold, left, rowHeight);
        drawDataRow("Checked by", client.select_emp_fill_three, left, col1, rowHeight, label, text, border);


        drawCheckboxGroup("Capacitor / Fan ", new boolean[]{client.checkboxCapacitor, client.checkboxFAN},
                left, col2, rowHeight, text, border);
        drawCheckboxGroup("Display / Control ", new boolean[]{client.checkboxDisplay, client.checkboxCC},
                left, col2, rowHeight, text, border);


//        drawCheckboxRow("Capacitor", client.checkboxCapacitor, left, col2, rowHeight, text, border);
//        drawCheckboxRow("Display", client.checkboxDisplay, left, col2, rowHeight, text, border);
//        drawCheckboxRow("Fan", client.checkboxFAN, left, col2, rowHeight, text, border);
//        drawCheckboxRow("Control Card", client.checkboxCC, left, col2, rowHeight, text, border);

        drawDataRow("Remarks", client.enter_first_remarks, left, col1, rowHeight, label, text, border);

//        drawMultiLineDataRow("Remarks", client.enter_first_remarks, left, col1, label, text, border);

        drawMainHeading("REPAIRS DETAILS", accentLight, bold, left, rowHeight);
        drawSubHeading("Repair Details", left, text, border);

        drawCheckboxGroup("SMPS / Firing / CT Section", new boolean[]{
                        client.repair_checkboxOne, client.repair_checkboxTwo, client.repair_checkboxThree},
                left, col2, rowHeight, text, border);
        drawCheckboxGroup("DC Section / Control Card / Other", new boolean[]{
                        client.repair_checkboxFour, client.repair_checkboxFive, client.repair_checkboxSix},
                left, col2, rowHeight, text, border);
//        drawCheckboxRow("SMPS", client.repair_checkboxOne, left, col2, rowHeight, text, border);
//        drawCheckboxRow("Firing", client.repair_checkboxTwo, left, col2, rowHeight, text, border);
//        drawCheckboxRow("DC Section", client.repair_checkboxThree, left, col2, rowHeight, text, border);
//        drawCheckboxRow("CT Section", client.repair_checkboxFour, left, col2, rowHeight, text, border);
//        drawCheckboxRow("Control Card", client.repair_checkboxFive, left, col2, rowHeight, text, border);
//        drawCheckboxRow("Other", client.repair_checkboxSix, left, col2, rowHeight, text, border);

        drawSubHeading("Replace Details", left, text, border);

        drawCheckboxGroup("IGBT / Rectifier / FUSE", new boolean[]{
                        client.replace_checkboxOne, client.replace_checkboxTwo, client.replace_checkboxFour},
                left, col2, rowHeight, text, border);

        drawCheckboxGroup("CT / Control Card / Power Card", new boolean[]{
                        client.replace_checkboxThree, client.replace_checkboxFive, client.replace_checkboxSix},
                left, col2, rowHeight, text, border);

        drawCheckboxGroup("Display / Fan / Other", new boolean[]{
                        client.replace_checkboxSeven, client.replace_checkboxEight, client.replace_checkboxNine},
                left, col2, rowHeight, text, border);

//1        drawCheckboxRow("IGBT", client.replace_checkboxOne, left, col2, rowHeight, text, border);
//2        drawCheckboxRow("Rectifier", client.replace_checkboxTwo, left, col2, rowHeight, text, border);
//4        drawCheckboxRow("CT", client.replace_checkboxFour, left, col2, rowHeight, text, border);
//7        drawCheckboxRow("Display", client.replace_checkboxSeven, left, col2, rowHeight, text, border);
//3        drawCheckboxRow("Fuse", client.replace_checkboxThree, left, col2, rowHeight, text, border);
//8        drawCheckboxRow("Fan", client.replace_checkboxEight, left, col2, rowHeight, text, border);
//5        drawCheckboxRow("Control Card", client.replace_checkboxFive, left, col2, rowHeight, text, border);
//6        drawCheckboxRow("Power Card", client.replace_checkboxSix, left, col2, rowHeight, text, border);
//9        drawCheckboxRow("Other", client.replace_checkboxNine, left, col2, rowHeight, text, border);

        drawMainHeading("TRIAL CHECK", accentLight, bold, left, rowHeight);

        drawCheckboxGroup("TRIAL 1 / TRIAL ", new boolean[]{
                        client.checkboxTrial1, client.checkboxTrial2},
                left, col2, rowHeight, text, border);


//        drawCheckboxRow("TRIAL 1", client.checkboxTrial1, left, col2, rowHeight, text, border);
//        drawCheckboxRow("TRIAL 2", client.checkboxTrial2, left, col2, rowHeight, text, border);


        drawDataRow("Repair Duration", client.number_picker_value + " days", left, col1, rowHeight, label, text, border);
//        drawDataRow("Repair Duration", client.number_picker_value + " days", left, col1, rowHeight, label, text, border);

        y += 20;
    }

    // ==================== SECTION 4: FINAL TRIAL CHECK ====================
    private void drawSection4() {
        Paint accent = getPaint(Color.rgb(60, 120, 240), Paint.Style.FILL);
        Paint accentLight = getPaint(Color.rgb(110, 158, 255), Paint.Style.FILL);
        Paint border = getPaint(Color.LTGRAY, Paint.Style.STROKE);
        Paint text = getTextPaint(20, Typeface.NORMAL, Color.BLACK);
        Paint bold = getTextPaint(22, Typeface.BOLD, Color.BLACK);
        Paint label = getTextPaint(19, Typeface.NORMAL, Color.DKGRAY);

        int left = M, col1 = 400, col2 = 700, rowHeight = 45;

        drawMainHeading("4. FINAL TRIAL CHECK", accent, bold, left, rowHeight);
        drawDataRow("Checked by", client.select_emp_fill_four, left, col1, rowHeight, label, text, border);

        drawSubHeading("Motor Details", left, text, border);
        drawCheckboxGroup("01 HP / 10 HP / 30 HP",
                new boolean[]{client.checkbox_1HP, client.checkbox_10HP, client.checkbox_30HP},
                left, col2, rowHeight, text, border);

        drawSubHeading("AMP Details", left, text, border);
        drawDataRow("On Display", client.On_Display, left, col1, rowHeight, label, text, border);
        drawDataRow("On Clamp", client.On_Clamp, left, col1, rowHeight, label, text, border);
        drawCheckboxGroup("U / V / W",
                new boolean[]{client.checkbox_u, client.checkbox_v, client.checkbox_w},
                left, col2, rowHeight, text, border);

        drawFooterWithQR();
        startNewPage(); //Start fresh page

        drawMainHeading("DIAGNOSIS", accentLight, bold, left, rowHeight);
        drawSubHeading("DC Voltage", left, text, border);
        drawDataRow("Display", client.dc_dsp, left, col1, rowHeight, label, text, border);
        drawDataRow("Meter", client.dc_met, left, col1, rowHeight, label, text, border);

        drawSubHeading("Output Volts", left, text, border);
        drawDataRow("Display", client.output_dsp, left, col1, rowHeight, label, text, border);
        drawDataRow("Meter", client.output_met, left, col1, rowHeight, label, text, border);

        drawDataRow("Run Hours", client.enter_RH, left, col1, rowHeight, label, text, border);
        drawDataRow("Replay Output", client.enterReplayOP, left, col1, rowHeight, label, text, border);
        drawDataRow("Fan Operation", client.enter_FANOpr, left, col1, rowHeight, label, text, border);
        drawDataRow("Body Condition", client.enter_BODY_Condition, left, col1, rowHeight, label, text, border);
        drawDataRow("I/O Check", client.enter_io_check, left, col1, rowHeight, label, text, border);
        drawDataRow("Cleanliness", client.enterClean, left, col1, rowHeight, label, text, border);
        drawDataRow("Parameter Copy", client.enterPramCopy, left, col1, rowHeight, label, text, border);
    }

    // ==================== SHARED DRAW METHODS ====================
    private void drawMainHeading(String title, Paint bgPaint, Paint textPaint, int left, int rowHeight) {
        checkSpace(rowHeight);
        canvas.drawRect(left, y, W - M, y + rowHeight, bgPaint);
        canvas.drawRect(left, y, W - M, y + rowHeight, getPaint(Color.DKGRAY, Paint.Style.STROKE));
        Paint whiteBold = new Paint(textPaint);
        whiteBold.setColor(Color.WHITE);
        canvas.drawText(title, left + 20, y + 30, whiteBold);
        y += rowHeight;
    }

    private void drawSubHeading(String title, int left, Paint text, Paint border) {
        int rowHeight = 40;
        checkSpace(rowHeight);

        // Soft bold look
        Paint semiBold = new Paint(text);
        semiBold.setFakeBoldText(true); // 👈 lighter bold than Typeface.BOLD

        // Border rectangle
        canvas.drawRect(left, y, W - M, y + rowHeight, border);

        // Text draw
        canvas.drawText(title, left + 20, y + 28, semiBold);

        y += rowHeight;
    }

    private void drawDataRow(String label, String value, int left, int col1, int rowHeight, Paint labelPaint, Paint textPaint, Paint border) {
        checkSpace(rowHeight);
        canvas.drawRect(left, y, W - M, y + rowHeight, border);
        canvas.drawText(label, left + 20, y + 30, labelPaint);
        canvas.drawText(getSafeString(value), col1 + 15, y + 30, textPaint);
        y += rowHeight;
    }

    private void drawMultiLineDataRow(String label, String value, int left, int col1, Paint labelPaint, Paint textPaint, Paint border) {
        String[] lines = splitText(value, 50);
        int lineHeight = 25;
        int totalHeight = Math.max(lineHeight, lines.length * lineHeight);
        checkSpace(totalHeight);
        canvas.drawRect(left, y, W - M, y + totalHeight, border);
        canvas.drawText(label, left + 15, y + 25, labelPaint);
        for (int i = 0; i < lines.length; i++)
            canvas.drawText(lines[i], col1 + 15, y + 25 + (i * lineHeight), textPaint);
        y += totalHeight;
    }

    private void drawCheckboxRow(String label, boolean checked, int left, int col2, int rowHeight, Paint text, Paint border) {
        checkSpace(rowHeight);
        canvas.drawRect(left, y, W - M, y + rowHeight, border);
        canvas.drawText(label, left + 15, y + 30, text);
        drawCheckBox(canvas, text, col2 + 15, y + 10, checked);
        y += rowHeight;
    }

    private void drawRadioRow(String label, boolean[] states, int left, int col2, int rowHeight, Paint text, Paint border) {
        checkSpace(rowHeight);
        canvas.drawRect(left, y, W - M, y + rowHeight, border);

        // Colors
        Paint blueFill = getPaint(Color.rgb(60, 120, 240), Paint.Style.FILL);
        Paint circleBorder = getPaint(Color.DKGRAY, Paint.Style.STROKE);
        circleBorder.setStrokeWidth(1.5f);

        // Split labels
        String[] labels = label.split("/");

        // Positions
        float startX = left + 20;
        float baseY = y + 30;
        float circleRadius = 10;
        float labelToCircleSpace = 30;
        float groupSpacing = 85;

        for (int i = 0; i < labels.length && i < states.length; i++) {
            String lbl = labels[i].trim();

            // Label text
            canvas.drawText(lbl, startX, baseY, text);

            // Circle center
            float circleX = startX + text.measureText(lbl) + labelToCircleSpace;
            float circleY = baseY - 10;

            // Outer circle
            canvas.drawCircle(circleX, circleY, circleRadius, circleBorder);

            // Inner dot for selected state
            if (states[i]) {
                canvas.drawCircle(circleX, circleY, circleRadius - 4, blueFill); // ✅ Perfect radio dot
            }

            startX = circleX + groupSpacing;
        }

        y += rowHeight;
    }

    private void drawCheckboxGroup(String title, boolean[] checks, int left, int col2, int rowHeight, Paint text, Paint border) {
        checkSpace(rowHeight);

        // Row border
        canvas.drawRect(left, y, W - M, y + rowHeight, border);

        // Split labels (e.g. "SMPS / Firing / CT Section")
        String[] labels = title.split("/");
        int baseY = y + 30;
        int boxY = y + 10;

        // Paint setup
        Paint labelPaint = new Paint(text);
        labelPaint.setFakeBoldText(false);
        labelPaint.setTextSize(text.getTextSize() - 1);

        // Fixed column X positions (so alignment stays perfect)
        int[] labelColumns = {
                left + 15,    // first label start
                left + 220,   // second label start
                left + 440    // third label start (adjust if needed)
        };
        int[] boxColumns = {
                left + 130,   // first checkbox
                left + 350,   // second checkbox
                left + 580    // third checkbox
        };

        // Draw labels + checkboxes in their fixed columns
        for (int i = 0; i < checks.length && i < labels.length && i < 3; i++) {
            String lbl = labels[i].trim();

            // Label
            canvas.drawText(lbl, labelColumns[i], baseY, labelPaint);

            // Checkbox aligned vertically in its column
            drawCheckBox(canvas, text, boxColumns[i], boxY, checks[i]);
        }

        y += rowHeight;
    }

    // ==================== FOOTER WITH QR CODE ====================
    private void drawFooterWithQR() {
        Paint grayLine = getPaint(Color.LTGRAY, Paint.Style.STROKE);
        canvas.drawLine(M, H - 120, W - M, H - 120, grayLine);

        Paint footerText = getTextPaint(18, Typeface.NORMAL, Color.DKGRAY);
        String pageNum = "Page " + (currentPageNumber - 1);
        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());

        // Left side - Date and page info
        canvas.drawText("Generated on: " + date, M, H - 95, footerText);
        canvas.drawText("Report ID: " + reportId, M, H - 70, footerText);

        // ✅ Client ID bhi footer mein add kiya hai
        canvas.drawText("Client ID: " + (clientId != null ? clientId : "N/A"), M, H - 45, footerText);

        // Right side - Page number
        float w = footerText.measureText(pageNum);
        canvas.drawText(pageNum, W - M - w, H - 95, footerText);

        // QR Code
        drawQRCode(W - M - 80, H - 85, 60);

        // Copyright
        Paint copyright = getTextPaint(16, Typeface.NORMAL, Color.GRAY);
        canvas.drawText("© Hi-Tech Controls 2025", W / 2 - 100, H - 40, copyright);
    }

    // ==================== QR CODE GENERATION ====================
    private void drawQRCode(int x, int y, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            // More structured QR content
            String qrContent = "Hi Tech Controls\n" +
                    "Date: " + getSafeString(client.gp_date) + "\n" +
                    "Client: " + getSafeString(client.name) + "\n" +
                    "Client ID: " + (clientId != null ? clientId : "N/A");

            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, size, size);

            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    bitmap.setPixel(i, j, bitMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }

            canvas.drawBitmap(bitmap, x, y, null);

        } catch (WriterException e) {
            // If QR code fails, draw a simple box with report ID and client ID
            Paint border = getPaint(Color.BLACK, Paint.Style.STROKE);
            Paint text = getTextPaint(8, Typeface.NORMAL, Color.BLACK);
            canvas.drawRect(x, y, x + size, y + size, border);
            canvas.drawText("CID:" + clientId, x + 2, y + size / 2 - 5, text);
            canvas.drawText("RID:" + reportId, x + 2, y + size / 2 + 5, text);
        }
    }

    // ==================== HELPERS ====================
    // blue right checkbox from gpt
//    private void drawCheckBox(Canvas canvas, Paint text, int x, int y, boolean checked) {
//        // Draw border box
//        Paint border = getPaint(Color.DKGRAY, Paint.Style.STROKE);
//        canvas.drawRect(x, y, x + 20, y + 20, border);
//
//        if (checked) {
//            // Draw proper tick mark ✓
//            Paint tick = getPaint(Color.rgb(60, 120, 240), Paint.Style.STROKE);
//            tick.setStrokeWidth(3f);
//
//            // Coordinates for natural tick shape
//            float startX = x + 4;
//            float startY = y + 10;
//            float midX = x + 9;
//            float midY = y + 17;
//            float endX = x + 18;
//            float endY = y + 5;
//
//            // Draw two lines for the tick
//            canvas.drawLine(startX, startY, midX, midY, tick);
//            canvas.drawLine(midX, midY, endX, endY, tick);
//        }
//    }
    private void drawCheckBox(Canvas canvas, Paint text, int x, int y, boolean checked) {
        //given by deepseek
        // Border
        Paint border = getPaint(Color.DKGRAY, Paint.Style.STROKE);
        border.setStrokeWidth(1);
        canvas.drawRect(x, y, x + 20, y + 20, border);

        if (checked) {
            // Blue background
            Paint fillBg = getPaint(Color.rgb(60, 120, 240), Paint.Style.FILL);
            canvas.drawRect(x + 2, y + 2, x + 18, y + 18, fillBg);

            // White tick mark with smooth anti-aliasing
            Paint tick = new Paint();
            tick.setColor(Color.WHITE);
            tick.setStrokeWidth(2.5f);
            tick.setAntiAlias(true);
            tick.setStyle(Paint.Style.STROKE);
            tick.setStrokeJoin(Paint.Join.ROUND);

            // Draw checkmark (✓)
            canvas.drawLine(x + 4, y + 10, x + 8, y + 14, tick);
            canvas.drawLine(x + 8, y + 14, x + 16, y + 6, tick);
        }
    }

    private void drawRadioButton(Canvas canvas, int cx, int cy, boolean checked) {
        Paint border = getPaint(Color.DKGRAY, Paint.Style.STROKE);
        canvas.drawCircle(cx, cy, 10, border);
        if (checked) {
            Paint fill = getPaint(Color.rgb(60, 120, 240), Paint.Style.FILL);
            canvas.drawCircle(cx, cy, 6, fill);
        }
    }

    private Paint getPaint(int color, Paint.Style style) {
        Paint p = new Paint();
        p.setColor(color);
        p.setStyle(style);
        p.setAntiAlias(true);
        return p;
    }

    private Paint getTextPaint(int size, int style, int color) {
        Paint p = new Paint();
        p.setTextSize(size);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, style));
        p.setColor(color);
        p.setAntiAlias(true);
        return p;
    }

    private String getSafeString(String str) {
        return str == null ? "-" : str.trim().isEmpty() ? "-" : str;
    }

    private String[] splitText(String text, int maxLen) {
        if (text == null) return new String[]{""};
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() > maxLen) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        lines.add(line.toString());
        return lines.toArray(new String[0]);
    }
}