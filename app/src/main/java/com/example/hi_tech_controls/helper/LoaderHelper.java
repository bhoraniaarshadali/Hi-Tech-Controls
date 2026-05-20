package com.example.hi_tech_controls.helper;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ProgressBar;
import androidx.core.content.ContextCompat;
import com.example.hi_tech_controls.R;

public class LoaderHelper {
    
    /**
     * Globally applies the custom iOS spoke spinner drawable to a ProgressBar.
     * Keeps any existing color tinting if present.
     * 
     * @param progressBar The ProgressBar to style
     */
    public static void applyIOSLoader(ProgressBar progressBar) {
        if (progressBar == null) return;
        Context context = progressBar.getContext();
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ios_loading_progress);
        if (drawable != null) {
            progressBar.setIndeterminateDrawable(drawable);
        }
    }
}
