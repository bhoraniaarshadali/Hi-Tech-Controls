// ============================================
// LoadingDialog.java
// ============================================
package com.example.hi_tech_controls.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.ProgressBar;

import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.example.hi_tech_controls.R;

public class LoadingDialog {

    private static LoadingDialog instance;
    private AlertDialog dialog;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable showRunnable;

    private LoadingDialog() {
    }

    public static LoadingDialog getInstance() {
        if (instance == null) {
            instance = new LoadingDialog();
        }
        return instance;
    }

    public void show(Context context) {
        if (dialog != null && dialog.isShowing()) return;

        // Cancel any pending show request to avoid multiple triggers
        if (showRunnable != null) {
            handler.removeCallbacks(showRunnable);
        }

        // Create a runnable to show the dialog after a small delay (300ms)
        // If hide() is called before 300ms, the loader will never flicker on screen.
        showRunnable = () -> {
            ProgressBar progressBar = new ProgressBar(context);
            progressBar.setIndeterminate(true);
            progressBar.getIndeterminateDrawable()
                    .setColorFilter(ContextCompat.getColor(context, R.color.blue), android.graphics.PorterDuff.Mode.SRC_IN);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(progressBar);
            builder.setCancelable(false);

            dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setGravity(Gravity.CENTER);
                dialog.getWindow().setLayout(dpToPx(context, 60), dpToPx(context, 60));
            }
            try {
                dialog.show();
            } catch (Exception e) {
                // Activity might be finished
            }
        };

        handler.postDelayed(showRunnable, 300);
    }

    public void hide() {
        if (showRunnable != null) {
            handler.removeCallbacks(showRunnable);
            showRunnable = null;
        }

        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}