package fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hi_tech_controls.AddDetailsActivity;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.SharedPrefHelper;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class fill_one_fragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    public static String clientIdValue;
    public EditText enterName;
    private EditText enterNumber;
    private EditText enterGPNumber;
    private EditText enterDate;
    private EditText enterMakeName;
    private EditText enterModelName;
    private EditText enterHPrate;
    private EditText enterSerialNumber;
    private SharedPrefHelper sharedPref;

    // Firebase
    private FirebaseFirestore db;
    private DocumentReference fillOneRef;
    private String clientId;

    // Date picker
    private EditText dateTextField1;
    private DatePickerDialog datePickerDialog1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // Get clientId from arguments
        if (getArguments() != null) {
            clientId = getArguments().getString("clientId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fill_one, container, false);

        sharedPref = new SharedPrefHelper(requireContext());
        dateTextField1 = rootView.findViewById(R.id.fill_one_enterDate);
        initDatePicker();

        // Firebase reference
        if (clientId != null) {
            fillOneRef = db.collection("hi_tech_controls_dataset_JUNE")
                    .document(clientId)
                    .collection("pages")
                    .document("fill_one");
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set clientId in TextView
        TextView clientIdTv = view.findViewById(R.id.clientId);
        if (clientId != null) {
            clientIdTv.setText(clientId);
            clientIdValue = clientId;
        }

        // Initialize EditTexts
        enterName = view.findViewById(R.id.fill_one_enterName);
        enterNumber = view.findViewById(R.id.fill_one_enterNumber);
        enterGPNumber = view.findViewById(R.id.fill_one_enterGPNumber);
        enterDate = view.findViewById(R.id.fill_one_enterDate);
        enterMakeName = view.findViewById(R.id.fill_one_enterMakeName);
        enterModelName = view.findViewById(R.id.fill_one_enterModelName);
        enterHPrate = view.findViewById(R.id.fill_one_enterHPrate);
        enterSerialNumber = view.findViewById(R.id.fill_one_enterSerialNumber);

        // Load from Firestore first, then SharedPref (fallback)
        loadFromFirestore();

        // Animation
        anim();
    }

    private void initDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        datePickerDialog1 = new DatePickerDialog(requireActivity(), (view, year1, month1, dayOfMonth) -> {
            month1 += 1;
            String date = dayOfMonth + "/" + month1 + "/" + year1;
            dateTextField1.setText(date);
        }, year, month, day);
        datePickerDialog1.setCancelable(true);

        dateTextField1.setOnClickListener(v -> datePickerDialog1.show());
    }

    private void loadFromFirestore() {
        if (fillOneRef == null) return;

        fillOneRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Load from Firestore
                enterName.setText(documentSnapshot.getString("name"));
                enterNumber.setText(documentSnapshot.getString("client_number"));
                enterGPNumber.setText(documentSnapshot.getString("gp_number"));
                enterDate.setText(documentSnapshot.getString("gp_date"));
                enterMakeName.setText(documentSnapshot.getString("make_name"));
                enterModelName.setText(documentSnapshot.getString("model_name"));
                enterHPrate.setText(documentSnapshot.getString("hp_rate"));
                enterSerialNumber.setText(documentSnapshot.getString("serial_number"));
            } else {
                // Fallback to SharedPreferences
                loadFromSharedPref();
            }
        }).addOnFailureListener(e -> {
            // Fallback to SharedPreferences
            loadFromSharedPref();
        });
    }

    private void loadFromSharedPref() {
        enterName.setText(sharedPref.getString("name", ""));
        enterNumber.setText(sharedPref.getString("number", ""));
        enterGPNumber.setText(sharedPref.getString("gp_number", ""));
        enterDate.setText(sharedPref.getString("date", ""));
        enterMakeName.setText(sharedPref.getString("make_name", ""));
        enterModelName.setText(sharedPref.getString("model_name", ""));
        enterHPrate.setText(sharedPref.getString("hp_rate", ""));
        enterSerialNumber.setText(sharedPref.getString("serial_number", ""));
    }

    // Called by AddDetailsActivity when "Next" is clicked
    public void saveToFirestore(String clientId, AddDetailsActivity.SaveCallback callback) {
        if (fillOneRef == null) {
            callback.onSaveComplete(false);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", enterName.getText().toString().trim());
        data.put("client_number", enterNumber.getText().toString().trim());
        data.put("gp_number", enterGPNumber.getText().toString().trim());
        data.put("gp_date", enterDate.getText().toString().trim());
        data.put("make_name", enterMakeName.getText().toString().trim());
        data.put("model_name", enterModelName.getText().toString().trim());
        data.put("hp_rate", enterHPrate.getText().toString().trim());
        data.put("serial_number", enterSerialNumber.getText().toString().trim());

        fillOneRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    saveToSharedPref(); // Backup
                    callback.onSaveComplete(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onSaveComplete(false);
                });
    }

    private void saveToSharedPref() {
        sharedPref.saveString("name", enterName.getText().toString());
        sharedPref.saveString("number", enterNumber.getText().toString());
        sharedPref.saveString("gp_number", enterGPNumber.getText().toString());
        sharedPref.saveString("date", enterDate.getText().toString());
        sharedPref.saveString("make_name", enterMakeName.getText().toString());
        sharedPref.saveString("model_name", enterModelName.getText().toString());
        sharedPref.saveString("hp_rate", enterHPrate.getText().toString());
        sharedPref.saveString("serial_number", enterSerialNumber.getText().toString());
    }

    @Override
    public void onStop() {
        super.onStop();
        saveToSharedPref(); // Always save locally
        if (clientId != null && fillOneRef != null) {
            // Auto-save to Firestore in background
            saveToFirestore(clientId, success -> {});
        }
    }

    public void anim() {
        RelativeLayout main_container1 = requireView().findViewById(R.id.main_container);
        main_container1.setAlpha(0f);
        main_container1.setTranslationY(50);
        main_container1.animate()
                .alpha(1f)
                .translationYBy(-50)
                .setDuration(1000)
                .start();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // Not used
    }
}