
# Hi‑Tech Controls — Drive Repair & Service App

A lightweight Android app to manage drive repair records: capture client & drive details, record step-by-step repair information, attach photos/videos, and export printable PDF repair reports.

This repository is the Java/Android implementation used in workshops to track repairs, generate professional repair reports, and keep media and metadata linked to each job.

---

Highlights
- Multi-step client/repair form (4-step flows) for structured data capture
- Cloud-backed metadata storage using Firebase Firestore
- Media upload hooks (MediaUpload activity) — demo-ready for Supabase or Firebase Storage
- In-app PDF generation (Formatted multi-section repair reports, saved to device)
- Simple modular structure suitable for incremental refactor (PDF/Firestore extraction already proposed)

Note: I scanned the repository to build this README — the analysis focused on the Android module, activities/fragments, PDF generation and Firestore usage. If you want a deeper, file-by-file analysis or a PR that adds example config files, I can prepare that next.

---

Quick demo (what you can test right away)
1. Install the debug APK built from this repo.
2. Open the app, add a new client and step through the four “fill” screens.
3. Use MediaUpload to attach photos/videos in the demo flow (or simulate).
4. Open the client in “View” and generate the PDF — it will save to the app downloads folder and open with a PDF viewer.

---

Table of contents
- Project overview
- Features
- Technologies
- Project structure
- Getting started (setup & run)
- App configuration (Firebase & Supabase notes)
- Code hotspots & recommended improvements
- Contributing
- License & contact

---

Project overview
Hi‑Tech Controls is an Android app designed for repair shops and service centers to:
- Capture customer and drive metadata (client, model, serial, observations)
- Record repair actions and checks (repair vs replace, trial checks)
- Attach media (images/videos) to repair records
- Export formatted, printable repair reports (PDF)

The app organizes each client record as a document containing page documents ("fill_one" - "fill_four") for modular storage of each step.

---

Features
- Structured 4-step repair form UI (fragments)
- Real-time Firestore save/load per page
- Media upload activity (MediaUpload) integrated in UI flow
- PDF generator that builds a multi-section report with pagination, footer and signature block
- Basic local state via SharedPreferences for UI progress

---

Technologies
- Android (Java)
- Firebase Firestore (text/metadata storage)
- Android PdfDocument (PDF creation)
- FileProvider for safe file opening
- (Optional) Supabase Storage recommended for open-source image/video storage (demo-ready)

---

Project structure (key files)
- app/src/main/java/com/example/hi_tech_controls/
  - AddDetailsActivity.java — multi-step controller and fragment manager
  - MediaUpload.java — media pick/capture/upload activity (hook for Supabase/Firebase)
  - MainActivity.java, LoginActivity.java, SplashActivity.java
  - SharedPrefHelper.java
  - fragments/
    - fill_one_fragment.java — client & drive details
    - fill_two_fragment.java — initial observations
    - fill_three_fragment.java — repair details
    - fill_four_fragment.java — final trial/checks
    - View_data_fragment.java — fetches pages, constructs InwardClient model, and generates PDF
- app/src/main/res/ — layouts, drawables, strings, themes
- PROJECT_STRUCTURE.md — scanned tree & notes

---

Getting started (developer)
Prerequisites
- Android Studio (Arctic Fox / Bumblebee or later recommended)
- JDK 11+ (as per Gradle settings)
- Android device or emulator (API 24+ recommended)

Build & run
1. Clone the repo:
   git clone https://github.com/bhoraniaarshadali/Hi-Tech-Controls.git
2. Open the project in Android Studio.
3. Let Gradle sync and install required SDK components.
4. Configure Firebase (see next section).
5. Run the app on a device or emulator:
   - Run > Run 'app' (or use ./gradlew assembleDebug)

---

App configuration — Firebase Firestore
This app stores textual metadata in a Firestore collection named by default:
- COLLECTION_NAME = "hi_tech_controls_dataset_JUNE"

Setup steps
1. Create a Firebase project and add an Android app (register package name).
2. Download google-services.json and place in `app/` module.
3. Add Firestore rules appropriate to your environment (the repo does not include rules).
4. Ensure the app-level Gradle file contains the Google Services plugin (if not, add config).

Security note: Do not leave Firestore in open mode for production — require authentication and limit access.

---

Media storage: Firebase Storage or Supabase Storage (recommended)
- The project includes MediaUpload hooks. For demo or production media, you can choose:
  - Firebase Storage: easiest integration with Firebase SDK for Android. Free quota suitable for small pilots.
  - Supabase Storage: open-source alternative, provides REST endpoints and Postgres-backed metadata. Good fit if you want vendor portability.

Recommended demo flow (Supabase):
1. Create a Supabase project and a `media` (or `uploads`) bucket.
2. For demo, public bucket is OK. For production, use private buckets + signed URLs.
3. Upload files via Retrofit/OkHttp to Supabase Storage REST endpoint and store the returned public URL in Firestore (or migrate metadata to Supabase Postgres).
4. For production: use signed uploads or server-side service-role tokens, and implement authentication (Supabase Auth or Firebase Auth).

I can provide a ready-to-drop MediaUpload demo using Retrofit for Supabase if you want to try it.

---

Key code hotspots & recommended improvements
(Short summary of high-priority refactors)
- Extract the large PDF creation code from `View_data_fragment` into a `PdfReportGenerator` utility (improves testability & moves heavy work off UI thread).
- Centralize Firestore read/write patterns into a `FirestoreRepository` class (reduces duplication and simplifies lifecycle management).
- Avoid hard-coded collection names: move to a configuration/constants file.
- Use WorkManager for background/resumable media uploads (for large files).
- Replace deprecated external storage patterns with scoped storage APIs; ensure runtime permission handling.
- Add Firestore security rules and environment-safe storage of API keys/secrets.

Note: I can produce PRs that add `PdfReportGenerator.java` and `FirestoreRepository.java` and update one fragment as an example.

---

Testing & QA
- Manual testing checklist:
  - Create new client → Fill pages 1–4 → Save each page and verify Firestore documents exist.
  - Use MediaUpload to attach an image/video → Verify URL is saved and preview works.
  - Generate PDF for a client → Confirm multi-page layout, footer and saved file open correctly.
  - Edge cases: incomplete pages, offline behavior, large file upload.
- Automate:
  - Add unit tests for repository layer (mock Firestore).
  - Add instrumentation tests for the UI flows.

---

Contribution & development workflow
- Fork the repo and open a pull request per feature/fix.
- Keep changes small and focused; include screens or short videos for UI changes.
- Add or update unit tests for new logic where possible.
- Use the issue tracker to describe bugs or feature requests (create labels for priority).

---

Changelog & releases
- The repo currently uses a demo flow. When releasing to testers:
  - Note changes to media storage (e.g., Supabase bucket, public vs private).
  - Include release notes for testers to validate uploads, PDF generation, and Firestore persistence.

---

License
- No license is present in the repository by default.
- If you want open-source contributions, add a permissive license (MIT or Apache-2.0) or your preferred license file.

---

Contact
- Maintainer: Arshadali Bhorania (GitHub: @bhoraniaarshadali)
- For testing issues, include: device model, Android version, exact steps and logs/screenshots.

---
App User Interface
<p align="center">
  <img width="600" src="https://github.com/bhoraniaarshadali/Hi-Tech-Controls/blob/09904d3ce89ffca0d0211f00e5fb6cf3e3b917be/image.jpg" alt="Hi-Tech Controls Image">
</p>
