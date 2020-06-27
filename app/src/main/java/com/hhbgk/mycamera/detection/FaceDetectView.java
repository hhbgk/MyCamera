package com.hhbgk.mycamera.detection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class FaceDetectView extends AppCompatImageView {

    private Paint linePaint;
    private Camera.Face[] faces;
    private RectF rectF;

    private Matrix matrix;

    public FaceDetectView(Context context) {
        super(context);
        init();
    }

    public FaceDetectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceDetectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(getResources().getColor(android.R.color.white));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);

        matrix = new Matrix();
        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faces == null || faces.length <= 0) return;
        prepareMatrix(matrix, false, 90, getWidth(), getHeight());

        canvas.save();
//        matrix.postRotate(0); //Matrix.postRotate默认是顺时针
//        canvas.rotate(-0);   //Canvas.rotate()默认是逆时针

        for (int i = 0; i < faces.length; i++) {
            Camera.Face face = faces[i];
            rectF.set(face.rect);
            matrix.mapRect(rectF);
            canvas.drawRect(rectF, linePaint);
        }
        canvas.restore();
    }

    public void setFaces(Camera.Face[] faces) {
        this.faces = faces;
        invalidate();
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                                     int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }
}
