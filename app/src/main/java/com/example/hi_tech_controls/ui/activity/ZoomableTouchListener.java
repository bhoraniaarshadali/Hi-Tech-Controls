package com.example.hi_tech_controls.ui.activity;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

public class ZoomableTouchListener implements View.OnTouchListener {

    private final ImageView imageView;
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();

    // Touch states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // Movement tracking
    private final PointF start = new PointF();
    private final PointF mid = new PointF();
    private float oldDist = 1f;

    private final ScaleGestureDetector scaleDetector;

    public ZoomableTouchListener(ImageView imageView) {
        this.imageView = imageView;
        this.imageView.setScaleType(ImageView.ScaleType.MATRIX);
        
        // Pre-align image bounds to matrix so it doesn't jump on first touch
        if (imageView.getDrawable() != null) {
            int viewWidth = imageView.getWidth();
            int viewHeight = imageView.getHeight();
            int drawableWidth = imageView.getDrawable().getIntrinsicWidth();
            int drawableHeight = imageView.getDrawable().getIntrinsicHeight();
            
            float scale;
            float dx = 0;
            float dy = 0;
            
            if (drawableWidth * viewHeight > viewWidth * drawableHeight) {
                scale = (float) viewHeight / (float) drawableHeight;
                dx = (viewWidth - drawableWidth * scale) * 0.5f;
            } else {
                scale = (float) viewWidth / (float) drawableWidth;
                dy = (viewHeight - drawableHeight * scale) * 0.5f;
            }
            
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            imageView.setImageMatrix(matrix);
        }

        scaleDetector = new ScaleGestureDetector(imageView.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float[] values = new float[9];
                matrix.getValues(values);
                float currentScale = values[Matrix.MSCALE_X];
                
                // Limit scale between original aspect ratio scale and 5x
                if (currentScale * scaleFactor < 0.5f) {
                    scaleFactor = 0.5f / currentScale;
                } else if (currentScale * scaleFactor > 6.0f) {
                    scaleFactor = 6.0f / currentScale;
                }
                
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                imageView.setImageMatrix(matrix);
                return true;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        
        float[] values = new float[9];
        matrix.getValues(values);
        float currentScale = values[Matrix.MSCALE_X];

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;
                
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                    imageView.setImageMatrix(matrix);
                }
                break;
        }
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
