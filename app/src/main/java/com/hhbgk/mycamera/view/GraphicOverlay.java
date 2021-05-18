package com.hhbgk.mycamera.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;


public class GraphicOverlay extends View {
    private final String tag = getClass().getSimpleName();
    // Matrix for transforming from image coordinates to overlay view coordinates.
    private final Matrix transformationMatrix = new Matrix();

    private Bitmap mBitmap;
    private int imageWidth;
    private int imageHeight;
    // The factor of overlay View size to image size. Anything in the image coordinates need to be
    // scaled by this amount to fit with the area of overlay View.
    private float scaleFactor = 1.0f;
    // The number of horizontal pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private float postScaleWidthOffset;
    // The number of vertical pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private float postScaleHeightOffset;
    private boolean isImageFlipped;
    private boolean needUpdateTransformation = true;
    private volatile Face face;
    private Paint boxPaint;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        needUpdateTransformation = true);

        boxPaint = new Paint();
        boxPaint.setColor(Color.WHITE);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4.0f);
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }
    /**
     * Adjusts the x coordinate from the image's coordinate system to the view coordinate system.
     */
    public float translateX(float x) {
        if (isImageFlipped) {
            return getWidth() - (scale(x) - postScaleWidthOffset);
        } else {
            return scale(x) - postScaleWidthOffset;
        }
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    public float translateY(float y) {
        return scale(y) - postScaleHeightOffset;
    }

    /** Adjusts the supplied value from the image scale to the view scale. */
    public float scale(float imagePixel) {
        return imagePixel * scaleFactor;
    }

    private void updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
//            Log.e(tag, "error: imageWidth=" + imageWidth + ", imageHeight=" + imageHeight);
            return;
        }
        float viewAspectRatio = (float) getWidth() / getHeight();
        float imageAspectRatio = (float) imageWidth / imageHeight;
        postScaleWidthOffset = 0;
        postScaleHeightOffset = 0;
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = (float) getWidth() / imageWidth;
            postScaleHeightOffset = ((float) getWidth() / imageAspectRatio - getHeight()) / 2;
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = (float) getHeight() / imageHeight;
            postScaleWidthOffset = ((float) getHeight() * imageAspectRatio - getWidth()) / 2;
        }

        transformationMatrix.reset();
        transformationMatrix.setScale(scaleFactor, scaleFactor);
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset);

        if (isImageFlipped) {
            transformationMatrix.postScale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);
        }

        needUpdateTransformation = false;
    }

    public void setImageSourceInfo(boolean isImageFlipped, int imageWidth, int imageHeight) {
        this.isImageFlipped = isImageFlipped;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        needUpdateTransformation = true;
    }

    public void updateBitmap(Bitmap bitmap) {
        mBitmap = bitmap;

        postInvalidate();
    }

    public void updateFace(Face face) {
        this.face = face;
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Face face = this.face;

        updateTransformationIfNeeded();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, transformationMatrix, null);
        }

        if (face == null) {

            return;
        }
        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());
//        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

        // Calculate positions.
        float left = x - scale(face.getBoundingBox().width() / 2.0f);
        float top = y - scale(face.getBoundingBox().height() / 2.0f);
        float right = x + scale(face.getBoundingBox().width() / 2.0f);
        float bottom = y + scale(face.getBoundingBox().height() / 2.0f);
        canvas.drawRect(left, top, right, bottom, boxPaint);
    }
}
