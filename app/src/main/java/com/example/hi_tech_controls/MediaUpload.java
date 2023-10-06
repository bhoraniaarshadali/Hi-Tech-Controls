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

public class MediaUpload extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1;
    private static final int VIDEO_REQUEST = 2;
    private static final int PERMISSION_CAMERA = 1;
    private static final int PERMISSION_VIDEO = 2;
    private static final int MAX_BOXES = 6;
    private static final int MAX_ADD_MORE_TAPS = 2;

    private LinearLayout boxContainer;
    private TextView add_more_Button;
    private ImageView[] boxes;
    private Bitmap[] boxImages;
    private int boxCount = 0;
    private int addMoreTaps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_upload);

        ImageView mediaActivity_Back = findViewById(R.id.mediaActivity_Back);
        boxContainer = findViewById(R.id.boxContainer);
        add_more_Button = findViewById(R.id.add_more_Button);

        boxes = new ImageView[MAX_BOXES];
        boxes[0] = findViewById(R.id.boxOne);
        boxes[1] = findViewById(R.id.boxTwo);
        boxes[2] = findViewById(R.id.boxThree);
        boxes[3] = findViewById(R.id.boxFour);
        boxes[4] = findViewById(R.id.boxFive);
        boxes[5] = findViewById(R.id.boxSix);

        boxImages = new Bitmap[MAX_BOXES];

        add_more_Button.setVisibility(View.VISIBLE);

        mediaActivity_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Set an OnClickListener for boxes 1 to 6
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkCameraAndVideoPermissionAndStart(index);
                }
            });
        }

        add_more_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (boxCount < MAX_BOXES && addMoreTaps < MAX_ADD_MORE_TAPS) {
                    boxCount++;
                    addNewBox(boxCount);

                    if (boxCount >= MAX_BOXES) {
                        add_more_Button.setVisibility(View.GONE);
                    }

                    addMoreTaps++;

                    if (addMoreTaps >= MAX_ADD_MORE_TAPS) {
                        add_more_Button.setVisibility(View.GONE);
                    }
                }
            }
        });
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

            for (int i = 0; i < newBoxes.length; i++) {
                final int index = (boxCount - 1) * 3 + i;
                newBoxes[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkCameraAndVideoPermissionAndStart(index);
                    }
                });
            }
        } else {
            Toast.makeText(this, "Error creating a new box", Toast.LENGTH_SHORT).show();
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
        captureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // Set video quality if needed

        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10); // Set video duration limit if needed

        Intent chooserIntent = Intent.createChooser(captureIntent, "Capture Image or Video");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{videoIntent});

        startActivityForResult(chooserIntent, index);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode >= 0 && requestCode < MAX_BOXES) {
                if (data != null) {
                    if (data.getData() != null) {
                        // Handle video capture here (store video or display a video thumbnail)
                    } else {
                        Bitmap photo = (Bitmap) data.getExtras().get("data");
                        if (photo != null) {
                            boxImages[requestCode] = photo;
                            boxes[requestCode].setImageBitmap(photo);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, do nothing here, as the camera will be started in the onClick method
            } else {
                Toast.makeText(this, "Camera and audio recording permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
