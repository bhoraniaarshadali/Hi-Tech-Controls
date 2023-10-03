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

public class mediaUpload extends AppCompatActivity {

    ImageView mediaActivity_Back1;
    private static final int CAMERA_REQUEST = 1;
    TextView add_more_Button1;
    LinearLayout boxContainer1;
    ImageView[] boxes; // Array to hold all the boxes
    ImageView[] boxes2; // Array to hold all the boxes2
    Bitmap[] boxImages; // Array to hold the images for each box
    int boxCount; // Initial number of boxes
    boolean addButtonClicked = false; // Track if the button has been clicked

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_upload);

        mediaActivity_Back1 = findViewById(R.id.mediaActivity_Back);
        boxContainer1 = findViewById(R.id.boxContainer);
        add_more_Button1 = findViewById(R.id.add_more_Button);

        // Initialize the boxes array
        boxes = new ImageView[]{
                findViewById(R.id.boxOne),
                findViewById(R.id.boxTwo),
                findViewById(R.id.boxThree),
                findViewById(R.id.boxFour),
                findViewById(R.id.boxFive),
                findViewById(R.id.boxSix)
        };

        // Initialize the boxImages array
        boxImages = new Bitmap[boxes.length];

        // Initially hide the "Add More Images" button
        add_more_Button1.setVisibility(View.VISIBLE);

        Camera();

        mediaActivity_Back1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Set an OnClickListener for the "Add More" button
        add_more_Button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!addButtonClicked) {
                    // Increase the box count and add a new box
                    boxCount++;
                    addNewBox(boxCount);

                    // Hide the "Add More Images" button after clicking
                    add_more_Button1.setVisibility(View.GONE);

                    // Disable the button after one click
                    addButtonClicked = true;
                }
            }
        });
    }

    private void addNewBox(int boxNumber) {
        // Create a new box layout (you can design this layout as needed)
        View newBox = getLayoutInflater().inflate(R.layout.new_box_layout, null);

        // Add the new box to the container
        boxContainer1.addView(newBox);

        // Initialize the boxes2 array for the new box
        boxes2 = new ImageView[]{
                newBox.findViewById(R.id.boxSeven),
                newBox.findViewById(R.id.boxEight),
                newBox.findViewById(R.id.boxNine)
        };

        for (int i = 0; i < boxes2.length; i++) {
            final int index = boxCount * 3 + i; // Calculate the unique index for each box2
            boxes2[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Check permission
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED) {
                        // Start the camera for the specified index
                        startCamera(index);
                    } else {
                        ActivityCompat.requestPermissions(mediaUpload.this, new String[]{Manifest.permission.CAMERA}, 1);
                    }
                }
            });
        }
    }

    public void Camera() {
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Check permission
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED) {
                        // Start the camera for the specified index
                        startCamera(index);
                    } else {
                        ActivityCompat.requestPermissions(mediaUpload.this, new String[]{Manifest.permission.CAMERA}, 1);
                    }
                }
            });
        }
    }

    private void startCamera(int index) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, index); // Pass the index as the request code
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
            if (requestCode >= 0 && requestCode < boxes.length) {
                // Store the captured photo in the corresponding boxImages array index
                boxImages[requestCode] = photo;
                // Set the image to the corresponding ImageView
                boxes[requestCode].setImageBitmap(photo);
            }
        }
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, do nothing here, as the camera will be started in the onClick method
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
