# Hi-Tech Controls Project Structure

This document provides a comprehensive overview of the project's file structure and directory organization.

## Root Directory
```text
Hi-Tech-Controls/
├── .gitignore
├── PROJECT_STRUCTURE.md
├── README.md
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
├── settings.gradle
├── image.jpg
├── app/
│   ├── build.gradle
│   ├── google-services.json
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/hi_tech_controls/
│       │   │   ├── adapter/
│       │   │   │   ├── AddDetailsAdp.java
│       │   │   │   ├── ClientAdapter.java
│       │   │   │   └── InwardClient.java
│       │   │   ├── helper/
│       │   │   │   ├── AdminManager.java
│       │   │   │   ├── AnalyticsManager.java
│       │   │   │   ├── FirestoreUtils.java
│       │   │   │   ├── LoadingDialog.java
│       │   │   │   ├── NetworkUtils.java
│       │   │   │   ├── OfflineSyncManager.java
│       │   │   │   └── PermissionUtils.java
│       │   │   ├── mediaControl/
│       │   │   │   ├── GlideAppModule.java
│       │   │   │   ├── PdfGenerator.java
│       │   │   │   ├── VideoCompressor.java
│       │   │   │   └── WebPCompressor.java
│       │   │   ├── model/
│       │   │   │   ├── ClientModel.java
│       │   │   │   └── DetailsModel.java
│       │   │   ├── supabaseMedia/
│       │   │   │   └── SupabaseClient.java
│       │   │   └── ui/
│       │   │       ├── activity/
│       │   │       │   ├── AddDetailsActivity.java
│       │   │       │   ├── BaseActivity.java
│       │   │       │   ├── ClientDetailsActivity.java
│       │   │       │   ├── LoginActivity.java
│       │   │       │   ├── MainActivity.java
│       │   │       │   ├── MediaUploadActivity.java
│       │   │       │   ├── SplashActivity.java
│       │   │       │   └── ViewDetailsActivity.java
│       │   │       └── fragments/
│       │   │           ├── fill_four_fragment.java
│       │   │           ├── fill_one_fragment.java
│       │   │           ├── fill_three_fragment.java
│       │   │           ├── fill_two_fragment.java
│       │   │           └── View_data_fragment.java
│       │   └── res/
│       │       ├── layout/
│       │       │   ├── activity_add_details.xml
│       │       │   ├── activity_base.xml
│       │       │   ├── activity_client_details.xml
│       │       │   ├── activity_login.xml
│       │       │   ├── activity_main.xml
│       │       │   ├── activity_media_upload.xml
│       │       │   ├── activity_splash.xml
│       │       │   ├── activity_view_details.xml
│       │       │   ├── add_cardview.xml
│       │       │   ├── box_item.xml
│       │       │   ├── content_view_data.xml
│       │       │   ├── fragment_fill_four.xml
│       │       │   ├── fragment_fill_one.xml
│       │       │   ├── fragment_fill_three.xml
│       │       │   ├── fragment_fill_two.xml
│       │       │   ├── fragment_view_data_fragement.xml
│       │       │   ├── item_client_card.xml
│       │       │   ├── item_client_shimmer.xml
│       │       │   ├── offline_banner.xml
│       │       │   ├── shimmer_box_item.xml
│       │       │   ├── shimmer_card_layout.xml
│       │       │   ├── shimmer_fill_four.xml
│       │       │   ├── shimmer_fill_one.xml
│       │       │   ├── shimmer_fill_three.xml
│       │       │   ├── shimmer_fill_two.xml
│       │       │   ├── shimmer_item_row.xml
│       │       │   ├── shimmer_media_upload.xml
│       │       │   ├── shimmer_view_data.xml
│       │       │   └── spinner_item.xml
│       │       ├── drawable/
│       │       │   ├── (Various icons and shape XMLs)
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── dimens.xml
│       │       │   ├── strings.xml
│       │       │   ├── styles.xml
│       │       │   └── themes.xml
│       │       └── font/
│       │           ├── poppins_bold.otf
│       │           ├── poppins_medium.otf
│       │           ├── poppins_regular.otf
│       │           ├── poppins_semibold.otf
│       │           └── poppins_thin.otf
│       ├── androidTest/
│       └── test/
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

## Key Components

### 1. `ui/activity/`
Contains the main screen controllers of the application.
- `MainActivity.java`: The entry point after login.
- `MediaUploadActivity.java`: Handles image and video selection, compression, and uploading.
- `AddDetailsActivity.java`: Form-based intake for new client records.

### 2. `mediaControl/`
Core logic for media processing.
- `VideoCompressor.java`: Uses the Transcoder engine for ultra-fast hardware-accelerated video compression.
- `WebPCompressor.java`: Optimizes images into WebP format to save bandwidth.
- `PdfGenerator.java`: Logic for generating PDF reports.

### 3. `supabaseMedia/`
- `SupabaseClient.java`: Handles direct file uploads to Supabase Storage using the PUT method for reliability.

### 4. `helper/`
- `FirestoreUtils.java`: Simplified interaction with Google Firestore.
- `OfflineSyncManager.java`: Manages data queuing when the device is offline.
- `AnalyticsManager.java`: Tracks user events and app performance.

### 5. `res/layout/`
- Layouts for activities, fragments, and custom items.
- Includes a large set of **shimmer** layouts (e.g., `shimmer_media_upload.xml`) for premium loading experiences.
