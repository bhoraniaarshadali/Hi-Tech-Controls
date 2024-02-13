package com.example.hi_tech_controls;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;
// Import statements...

// Import statements...

public class MediaUpload extends AppCompatActivity {

    // Constants...
    private static final int CAMERA_REQUEST_BASE = 1000;
    private static final int PERMISSION_CAMERA = 1;
    private static final int MAX_BOXES = 6;
    private static final int MAX_ADD_MORE_TAPS = 2;

    private LinearLayout boxContainer;
    private TextView addMoreButton;
    private ImageView[][] boxes;
    private Bitmap[][] boxImages;
    private int boxCount = 0;
    private int addMoreTaps = 0;
    private ImageView[] allBoxes;

    // Define ImageView objects for boxOne to boxSix...
    private ImageView boxOne;
    private ImageView boxTwo;
    private ImageView boxThree;
    private ImageView boxFour;
    private ImageView boxFive;
    private ImageView boxSix;

    private ImageView mediaActivity_Back1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_upload);

        boxContainer = findViewById(R.id.boxContainer);
        addMoreButton = findViewById(R.id.add_more_Button);
        boxOne = findViewById(R.id.boxOne);
        boxTwo = findViewById(R.id.boxTwo);
        boxThree = findViewById(R.id.boxThree);
        boxFour = findViewById(R.id.boxFour);
        boxFive = findViewById(R.id.boxFive);
        boxSix = findViewById(R.id.boxSix);

        mediaActivity_Back1 = findViewById(R.id.mediaActivity_Back);
        mediaActivity_Back1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Initialize arrays...
        boxes = new ImageView[MAX_BOXES][3];
        boxImages = new Bitmap[MAX_BOXES][3];
        allBoxes = new ImageView[MAX_BOXES * 3];

        // Initialization...
        addMoreButton.setOnClickListener(this::handleAddMoreButtonClick);
        initializeBoxClickListeners();
    }

    private void handleAddMoreButtonClick(View view) {
        if (boxCount < MAX_BOXES && addMoreTaps < MAX_ADD_MORE_TAPS) {
            boxCount++;
            addNewBox(boxCount);

            if (boxCount >= MAX_BOXES) {
                addMoreButton.setVisibility(View.GONE);
            }

            addMoreTaps++;

            if (addMoreTaps >= MAX_ADD_MORE_TAPS) {
                addMoreButton.setVisibility(View.GONE);
                showToast("Maximum images added");
            }
        }
    }

    private void initializeBoxClickListeners() {
        // Set click listeners for boxOne to boxSix...
        setBoxClickListener(boxOne, 0);
        setBoxClickListener(boxTwo, 1);
        setBoxClickListener(boxThree, 2);
        setBoxClickListener(boxFour, 3);
        setBoxClickListener(boxFive, 4);
        setBoxClickListener(boxSix, 5);
    }

    private void setBoxClickListener(ImageView box, int index) {
        box.setOnClickListener(v -> checkCameraAndVideoPermissionAndStart(index));
    }

    private void addNewBox(int boxNumber) {
        View newBox = getLayoutInflater().inflate(R.layout.new_box_layout, null);

        if (newBox != null) {
            boxContainer.addView(newBox);

            final ImageView[] newBoxes = new ImageView[]{
                    newBox.findViewById(R.id.boxSeven),
                    newBox.findViewById(R.id.boxEight),
                    newBox.findViewById(R.id.boxNine)
            };

            addNewBoxClickListeners(newBoxes);
        } else {
            showToast("Error creating a new box");
        }
    }

    private void addNewBoxClickListeners(ImageView[] newBoxes) {
        for (int i = 0; i < newBoxes.length; i++) {
            final int index = (boxCount - 1) * 3 + i;
            newBoxes[i].setOnClickListener(v -> checkCameraAndVideoPermissionAndStart(index));
            allBoxes[index] = newBoxes[i];
        }
    }

    private void checkCameraAndVideoPermissionAndStart(int index) {
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int videoPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (cameraPermission == PackageManager.PERMISSION_GRANTED && videoPermission == PackageManager.PERMISSION_GRANTED) {
            startCameraOrVideo(index);
        } else {
            String[] permissions = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_CAMERA);
        }
    }

    private void startCameraOrVideo(int index) {
        Intent captureIntent = new Intent();
        captureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);

        Intent chooserIntent = Intent.createChooser(captureIntent, "Capture Image or Video");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{videoIntent});

        int uniqueRequestCode = CAMERA_REQUEST_BASE + index;
        startActivityForResult(chooserIntent, uniqueRequestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            int boxIndex = (requestCode - CAMERA_REQUEST_BASE) / 3;
            int imageIndex = (requestCode - CAMERA_REQUEST_BASE) % 3;

            if (isValidBoxIndex(boxIndex) && isValidImageIndex(imageIndex)) {
                processActivityResult(data, boxIndex, imageIndex);
            }
        }
    }

    private boolean isValidBoxIndex(int boxIndex) {
        return boxIndex >= 0 && boxIndex < MAX_BOXES;
    }

    private boolean isValidImageIndex(int imageIndex) {
        return imageIndex >= 0;
    }

    private void processActivityResult(Intent data, int boxIndex, int imageIndex) {
        if (data != null) {
            if (data.getData() != null) {
                // Handle video capture...
                showToast("Video capture not implemented");
            } else {
                handleImageCapture(data, boxIndex, imageIndex);
            }
        }
    }

    private void handleImageCapture(Intent data, int boxIndex, int imageIndex) {
        Bitmap photo = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
        if (photo != null) {
            boxImages[boxIndex][imageIndex] = photo;
            allBoxes[(boxIndex * 3) + imageIndex].setImageBitmap(photo);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA) {
            handleCameraPermissionResult(grantResults);
        }
    }

    private void handleCameraPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, do nothing here, as the camera will be started in the onClick method
        } else {
            showToast("Camera and audio recording permission denied");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
