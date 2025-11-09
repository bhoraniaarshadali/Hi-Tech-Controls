package com.example.hi_tech_controls.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.adapter.InwardClient;
import com.example.hi_tech_controls.adapter.LoadingDialog;
import com.example.hi_tech_controls.adapter.PdfGenerator;
import com.example.hi_tech_controls.adapter.PermissionUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class View_data_fragment extends Fragment {

    private static final String COLLECTION_NAME = "hi_tech_controls_dataset_JUNE";

    private final int W = 1080, H = 1500, M = 60;
    private final List<PdfDocument.Page> pages = new ArrayList<>();
    private final int y = 100;
    private final int page = 1;
    private View rootView;
    private InwardClient client;
    private PdfDocument pdf;
    private Canvas canvas;
    // UI Components for shimmer and refresh
    private ShimmerFrameLayout shimmerLayout;
    private RelativeLayout contentContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Toast management
    private android.widget.Toast activeToast;
    private String currentClientId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_data_fragement, container, false);

        // Initialize UI components
        initializeUIComponents();

        if (getArguments() != null) {
            currentClientId = getArguments().getString("clientId");
            client = new InwardClient();
            // Show shimmer and fetch data
            showShimmer();
            fetchData(currentClientId);
        }
        return rootView;
    }

    private void initializeUIComponents() {
        shimmerLayout = rootView.findViewById(R.id.shimmerLayout);
        contentContainer = rootView.findViewById(R.id.contentContainer);
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);

        // Set up pull to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentClientId != null && !currentClientId.isEmpty()) {
                refreshData();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Set up print button
        View printBtn = rootView.findViewById(R.id.printpdf);
        if (printBtn != null) {
            printBtn.setOnClickListener(v -> {
                if (client != null) {
                    generatePDF(client);
                }
            });
        }
    }

    private void showShimmer() {
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }
        if (contentContainer != null) {
            contentContainer.setVisibility(View.GONE);
        }
        // Hide print button while loading
        View printBtn = rootView.findViewById(R.id.printpdf);
        if (printBtn != null) {
            printBtn.setVisibility(View.GONE);
        }
    }

    private void hideShimmer() {
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (contentContainer != null) {
            contentContainer.setVisibility(View.VISIBLE);
        }
        // Show print button after loading
        View printBtn = rootView.findViewById(R.id.printpdf);
        if (printBtn != null) {
            printBtn.setVisibility(View.VISIBLE);
        }
    }

    private void refreshData() {
        if (currentClientId == null || currentClientId.isEmpty()) {
            swipeRefreshLayout.setRefreshing(false);
            showToastSafe("No client ID available");
            return;
        }

        showShimmer();
        fetchData(currentClientId);
    }

//    private void fetchData(String id) {
//        if (id == null || id.isEmpty()) {
//            showToastSafe("Invalid client id");
//            hideShimmer();
//            swipeRefreshLayout.setRefreshing(false);
//            return;
//        }
//
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
//        List<String> docNames = Arrays.asList("fill_one", "fill_two", "fill_three", "fill_four");
//
//        for (String name : docNames) {
//            Task<DocumentSnapshot> t = db.collection(COLLECTION_NAME)
//                    .document(id)
//                    .collection("pages")
//                    .document(name)
//                    .get();
//            tasks.add(t);
//        }
//
//        Tasks.whenAllSuccess(tasks)
//                .addOnSuccessListener(results -> {
//                    // Reset client data
//                    client = new InwardClient();
//
//                    for (Object obj : results) {
//                        if (!(obj instanceof DocumentSnapshot)) continue;
//                        DocumentSnapshot d = (DocumentSnapshot) obj;
//                        if (!d.exists()) continue;
//                        switch (d.getId()) {
//                            case "fill_one":
//                                fillOne(client, d);
//                                break;
//                            case "fill_two":
//                                fillTwo(client, d);
//                                break;
//                            case "fill_three":
//                                fillThree(client, d);
//                                break;
//                            case "fill_four":
//                                fillFour(client, d);
//                                break;
//                        }
//                    }
//
//                    requireActivity().runOnUiThread(() -> {
//                        hideShimmer();
//                        swipeRefreshLayout.setRefreshing(false);
//                        populateUI(client);
//                    });
//                })
//                .addOnFailureListener(e -> {
//                    requireActivity().runOnUiThread(() -> {
//                        hideShimmer();
//                        swipeRefreshLayout.setRefreshing(false);
//                        showToastSafe("Error fetching data");
//                    });
//                });
//    }

    private void fetchData(String id) {
        if (id == null || id.isEmpty()) {
            showToastSafe("Invalid client id");
            hideShimmer();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // --- 🔐 Ensure Firebase is initialized ---
        try {
            if (com.google.firebase.FirebaseApp.getApps(requireContext()).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(requireContext());
                Log.i("FirebaseInit", "Firebase initialized manually in fragment.");
            }
        } catch (Exception e) {
            Log.e("FirebaseInitError", "Failed to initialize Firebase: " + e.getMessage());
            requireActivity().runOnUiThread(() -> {
                hideShimmer();
                swipeRefreshLayout.setRefreshing(false);
                showToastSafe("Firebase init failed: " + e.getMessage());
            });
            return;
        }

        // --- 🔥 Safe Firestore Access ---
        FirebaseFirestore db;
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("FirestoreError", "Firestore instance error: ", e);
            hideShimmer();
            swipeRefreshLayout.setRefreshing(false);
            showToastSafe("Firestore not available");
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        List<String> docNames = Arrays.asList("fill_one", "fill_two", "fill_three", "fill_four");

        try {
            for (String name : docNames) {
                Task<DocumentSnapshot> t = db.collection(COLLECTION_NAME)
                        .document(id)
                        .collection("pages")
                        .document(name)
                        .get();
                tasks.add(t);
            }
        } catch (Exception e) {
            Log.e("FirestoreFetch", "Collection read error: ", e);
            hideShimmer();
            swipeRefreshLayout.setRefreshing(false);
            showToastSafe("Error reading Firestore");
            return;
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    try {
                        client = new InwardClient();

                        for (Object obj : results) {
                            if (!(obj instanceof DocumentSnapshot)) continue;
                            DocumentSnapshot d = (DocumentSnapshot) obj;
                            if (!d.exists()) continue;

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

                        if (!isAdded()) return; // fragment is no longer attached

                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            hideShimmer();
                            swipeRefreshLayout.setRefreshing(false);
                            populateUI(client);
                        });

                    } catch (Exception e) {
                        Log.e("DataProcessError", "Error mapping data: ", e);
                        requireActivity().runOnUiThread(() -> {
                            hideShimmer();
                            swipeRefreshLayout.setRefreshing(false);
                            showToastSafe("Error processing data");
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreFail", "Data fetch failed: ", e);
                    requireActivity().runOnUiThread(() -> {
                        hideShimmer();
                        swipeRefreshLayout.setRefreshing(false);
                        showToastSafe("Error fetching data: " + e.getMessage());
                    });
                });
    }


    // === FILL MAPPERS ===
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
        c.select_emp_fill_two = d.getString("select_emp");
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
        c.output_pos_checkbox_U = b(d, "output_pos_checkbox_U");
        c.output_pos_checkbox_V = b(d, "output_pos_checkbox_V");
        c.output_pos_checkbox_W = b(d, "output_pos_checkbox_W");
        c.output_neg_checkbox_U = b(d, "output_neg_checkbox_U");
        c.output_neg_checkbox_V = b(d, "output_neg_checkbox_V");
        c.output_neg_checkbox_W = b(d, "output_neg_checkbox_W");
    }

    private void fillThree(InwardClient c, DocumentSnapshot d) {
        c.select_emp_fill_three = d.getString("select_emp");
        c.enter_first_remarks = d.getString("enter_first_remarks");
        c.checkboxCapacitor = b(d, "checkboxCapacitor");
        c.checkboxDisplay = b(d, "checkboxDisplay");
        c.checkboxFAN = b(d, "checkboxFAN");
        c.checkboxCC = b(d, "checkboxCC");
        c.repair_checkboxOne = b(d, "repair_checkboxOne");
        c.repair_checkboxTwo = b(d, "repair_checkboxTwo");
        c.repair_checkboxThree = b(d, "repair_checkboxThree");
        c.repair_checkboxFour = b(d, "repair_checkboxFour");
        c.repair_checkboxFive = b(d, "repair_checkboxFive");
        c.repair_checkboxSix = b(d, "repair_checkboxSix");
        c.replace_checkboxOne = b(d, "replace_checkboxOne");
        c.replace_checkboxTwo = b(d, "replace_checkboxTwo");
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

    // === UI population ===
    @SuppressLint("SetTextI18n")
    private void populateUI(InwardClient c) {
        // Populate all form fields
        setText(R.id.fill_one_enterName, g(c.name));
        setText(R.id.fill_one_enterNumber, g(c.client_number));
        setText(R.id.fill_one_enterGPNumber, g(c.gp_number));
        setText(R.id.fill_one_enterDate, g(c.gp_date));
        setText(R.id.fill_one_enterMakeName, g(c.make_name));
        setText(R.id.fill_one_enterModelName, g(c.model_name));
        setText(R.id.fill_one_enterHPrate, g(c.hp_rate));
        setText(R.id.fill_one_enterSerialNumber, g(c.serial_number));

        // fill two
        setText(R.id.fill_two_selectEmply, g(c.select_emp_fill_two));
        setText(R.id.fill_two_enterFRrate, g(c.fr_rate));
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

        // output checkboxes
        setCB(R.id.fill_two_output_POS_checkbox_U, c.output_pos_checkbox_U);
        setCB(R.id.fill_two_output_POS_checkbox_V, c.output_pos_checkbox_V);
        setCB(R.id.fill_two_output_POS_checkbox_W, c.output_pos_checkbox_W);
        setCB(R.id.fill_two_output_NEG_checkbox_U, c.output_neg_checkbox_U);
        setCB(R.id.fill_two_output_NEG_checkbox_V, c.output_neg_checkbox_V);
        setCB(R.id.fill_two_output_NEG_checkbox_W, c.output_neg_checkbox_W);

        setEdit(R.id.fill_two_clientObs_text, g(c.client_obs));
        setEdit(R.id.fill_two_ourObs_text, g(c.our_obs));
        setEdit(R.id.fill_two_lastFault_text, g(c.last_fault));

        // fill three
        setText(R.id.fill_three_selectEmply, g(c.select_emp_fill_three));
        setText(R.id.fill_three_firstRemarks, g(c.enter_first_remarks));
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
        if (tv != null) tv.setText(c.number_picker_value + " /days");

        // fill four
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

    // ================================= PDF Generation =================================
    private void generatePDF(InwardClient c) {
        LoadingDialog.getInstance().show(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PdfGenerator pdfGenerator = new PdfGenerator(requireContext(), client, currentClientId);
                File pdfFile = pdfGenerator.generate(); // internally saves in app folder

                requireActivity().runOnUiThread(() -> {
                    LoadingDialog.getInstance().hide();
                    String message = "✅ PDF Saved Successfully!" + "File: " + pdfFile.getName() +
                            "\nLocation:\n" + pdfFile.getAbsolutePath();
                    showToastSafe(message);
                    openPdfFile(pdfFile);
                });
            } catch (Exception e) {
                Log.e("PDF_ERROR", "PDF generation failed: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    LoadingDialog.getInstance().hide();
                    showToastSafe("❌ PDF generation failed: " + e.getMessage());
                });
            }
        });
    }

    private void openPdfFile(File file) {
        if (!isAdded() || requireContext() == null) return;
        try {
            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    file
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            showToastSafe("No PDF viewer found");
        }
    }


    // === HELPERS ===
    private String g(String s) {
        return s != null ? s : "";
    }

    private boolean b(DocumentSnapshot d, String f) {
        Object v = d.get(f);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return "true".equalsIgnoreCase((String) v);
        return false;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
        }
        if (activeToast != null) {
            activeToast.cancel();
            activeToast = null;
        }
        try {
            LoadingDialog.getInstance().dismiss();
        } catch (Exception ignored) {
        }
    }

    private void showPermissionDialog() {
        new SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Permission Required")
                .setContentText("Storage permission is needed to save PDF files.\nPlease allow it to continue.")
                .setConfirmText("Allow")
                .setCancelText("Cancel")
                .setConfirmClickListener(dialog -> {
                    dialog.dismissWithAnimation();
                    PermissionUtils.requestStoragePermissions(requireActivity(), 1002);
                })
                .setCancelClickListener(SweetAlertDialog::dismissWithAnimation)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1002) {
            if (PermissionUtils.allPermissionsGranted(grantResults)) {
                showToastSafe("Permission granted ✅ Generating PDF...");
                if (client != null) {
                    generatePDF(client); // ✅ Correct variable name
                } else {
                    showToastSafe("No client data found to generate PDF.");
                }
            } else {
                showToastSafe("Permission denied ❌ Please allow to save PDF.");
                new Handler(Looper.getMainLooper()).postDelayed(this::showPermissionDialog, 800);
            }
        }
    }


    // Toast helper (single active toast)
    private void showToastSafe(String message) {
        if (!isAdded() || getContext() == null) return;
        if (activeToast != null) activeToast.cancel();
        activeToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT);
        activeToast.show();
    }
}