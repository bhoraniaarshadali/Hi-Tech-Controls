package com.example.hi_tech_controls;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class mediaUpload extends AppCompatActivity {

    ImageView mediaActivity_Back1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_upload);

        mediaActivity_Back1 = findViewById(R.id.mediaActivity_Back);

        mediaActivity_Back1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });


    }
}