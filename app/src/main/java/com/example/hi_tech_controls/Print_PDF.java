package com.example.hi_tech_controls;
import android.Manifest;
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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Print_PDF extends AppCompatActivity {
    private static final int REQUEST_CODE = 123;
    private RelativeLayout mainLayout;
    private RelativeLayout frag2;
    private RelativeLayout frag3;
    private RelativeLayout frag4;
    private final int pageWidth = (int) (8.5f * 72); // 8.5 inches converted to points
    private int pageHeight;
    private int notificationId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_view_data_fragement);

        Button printButton = findViewById(R.id.printpdf);
        mainLayout = findViewById(R.id.main_container);
        frag2 = findViewById(R.id.frag_2);
        frag3 = findViewById(R.id.frag_3);
        frag4 = findViewById(R.id.frag_4);

        printButton.setOnClickListener(v ->{
            checkAndRequestPermission();



        });

    }

    public void checkAndRequestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            showFileNameDialog();
        }
    }

    private void showFileNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save PDF");
        builder.setMessage("Enter a file name for the PDF:");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String fileName = input.getText().toString();
            if (!fileName.isEmpty()) {
                createPdf(fileName);
            } else {
                Toast.makeText(Print_PDF.this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Failed to create the 'Service Report' folder", Toast.LENGTH_SHORT).show();
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
                    pdfDoc.writeTo(new FileOutputStream(pdfFile));
                }
                pdfDoc.close();



                // Show a toast message upon successful PDF creation
                Toast.makeText(this, "PDF saved as " + fileName + ".pdf", Toast.LENGTH_SHORT).show();

                // Show a notification upon successful PDF creation
                showNotification(pdfFile);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "External storage not available or writable", Toast.LENGTH_SHORT).show();
        }
    }
    private int pageNumber = 1;
    private void addPageFromViewWithBorder(PdfDocument pdfDoc, View view, boolean isFirstPage) {
        // 11 inches converted to points
        int pageHeight = (int) (12f * 72);
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
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
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
        emailSymbolPaint.setColor(Color.BLUE);
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
        canvas.drawText(" hitech@gmail.com", emailSymbolX + (int) emailSymbolPaint.measureText(" ✉ ") +10, numberY, emailTextPaint);

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
        int lineY =10;
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
        drawRoundedHorizontalLine(canvas, lineY + 120, lineEndX, 2, Color.BLACK);
    }
    private void drawRoundedHorizontalLine(Canvas canvas, int y, int width, int strokeWidth, int color) {
        // Draw the rounded horizontal line
        Paint linePaint = new Paint();
        linePaint.setColor(color);
        linePaint.setStrokeWidth(strokeWidth);



        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);

        Path path = new Path();
        path.moveTo(20, y); // Adjust the starting X position
        path.lineTo(width, y); // Adjust the ending X position
        path.lineTo(width, y + strokeWidth);
        path.lineTo(20, y + strokeWidth);
        path.close(); // Close the path to make it rounded

        canvas.drawPath(path, paint);

        // Draw the rectangle box
        int rectTop = y + strokeWidth + 10; // Adjust the top position of the rectangle
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

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            // Create an intent to open the PDF
            Intent openPdfIntent = new Intent(Intent.ACTION_VIEW);
            openPdfIntent.setDataAndType(pdfFileToUri(pdfFile), "application/pdf");
            openPdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            PendingIntent pdfPendingIntent = PendingIntent.getActivity(this, 0, openPdfIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.pdflogo) // Replace with your notification icon
                    .setContentTitle("PDF Downloaded")
                    .setContentText("Your PDF has been downloaded successfully.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pdfPendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            notificationManagerCompat.notify(notificationId, builder.build());
        }
    }
    // Convert a File to a Uri
    private Uri pdfFileToUri(File pdfFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", pdfFile);
        } else {
            return Uri.fromFile(pdfFile);
        }
    }
}