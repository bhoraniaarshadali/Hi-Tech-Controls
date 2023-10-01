package com.example.hi_tech_controls;

import static android.Manifest.permission.CAMERA;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class camera_video extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1;
    private static final int VIDEO_CAPTURE_REQUEST = 2;
    private ImageView imageView;
    private VideoView videoView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_video);

        ExtendedFloatingActionButton captureMediaButton = findViewById(R.id.captureMediaButton);
        FloatingActionButton captureImageButton = findViewById(R.id.captureImageButton);
        FloatingActionButton captureVideoButton = findViewById(R.id.captureVideoButton);
        imageView = findViewById(R.id.image_view1);
        videoView = findViewById(R.id.video_view1);

        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermissionAndCapturePhoto();
            }
        });

        captureVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermissionAndCaptureVideo();
            }
        });

        captureMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the visibility of the capture options
                if (captureImageButton.getVisibility() == View.VISIBLE) {
                    captureImageButton.setVisibility(View.GONE);
                    captureVideoButton.setVisibility(View.GONE);
                } else {
                    captureImageButton.setVisibility(View.VISIBLE);
                    captureVideoButton.setVisibility(View.VISIBLE);
                }
            }
        });

    }


    private void checkCameraPermissionAndCapturePhoto() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            // Continue the action
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else {
            // Permission not granted
            // Ask for the permission
            ActivityCompat.requestPermissions(this, new String[]{CAMERA}, 1);
        }
    }

    private void checkCameraPermissionAndCaptureVideo() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            // Continue the action
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takeVideoIntent, VIDEO_CAPTURE_REQUEST);
            }
        } else {
            // Permission not granted
            // Ask for the permission
            ActivityCompat.requestPermissions(this, new String[]{CAMERA}, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 1) {
                checkCameraPermissionAndCapturePhoto();
            } else if (requestCode == 2) {
                checkCameraPermissionAndCaptureVideo();
            }
        } else {
            // Permission denied
            Toast.makeText(this, "You cannot capture without camera permission", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(photo);
                videoView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                saveImageToGallery(photo);
            }
        } else if (requestCode == VIDEO_CAPTURE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Uri videoUri = data.getData();
                videoView.setVideoURI(videoUri);
                videoView.setMediaController(new MediaController(this));
                videoView.requestFocus();
                videoView.start();
                imageView.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                saveVideoToGallery(videoUri);
            }
        }
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "CapturedImage",
                "Image captured by Camera"
        );

        if (savedImageURL != null) {
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save image to gallery", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveVideoToGallery(Uri videoUri) {
        String videoPath = videoUri.getPath();

        if (videoPath != null) {
            File videoFile = new File(videoPath);
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{videoFile.toString()},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(camera_video.this, "Video saved to gallery", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
            );
        } else {
            Toast.makeText(this, "Failed to save video to gallery", Toast.LENGTH_SHORT).show();
        }
    }

}


