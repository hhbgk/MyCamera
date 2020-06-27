package com.hhbgk.mycamera.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.hhbgk.mycamera.core.BaseCamera;

import java.io.IOException;
import java.util.List;

public class Camera1Imp extends BaseCamera {
    private static final int MAX_UNSPECIFIED = -1;
    protected int mMaxHeight;
    protected int mMaxWidth;
    private Context context;
    private SurfaceHolder holder;

    public Camera1Imp(SurfaceHolder holder) {
        this.holder = holder;
    }

    @Override
    public void init(Context context) {
        this.context = context;
        int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                faceBackCameraId = i;
                faceBackCameraOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                faceFrontCameraId = i;
                faceFrontCameraOrientation = cameraInfo.orientation;
            }
        }
    }

    @Override
    public void open(int cameraId) {
        currentCameraId = cameraId;
        camera = Camera.open(cameraId);
    }

    @Override
    public void close() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    public void startPreview() {
        if (camera == null) {
            throw new NullPointerException("camera object is null");
        }
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        if (sizes == null) {
            return;
        }
        Size frameSize = calculateCameraFrameSize(sizes, 960, 1280);

        Log.e(tag, "w=" + frameSize.getWidth() +", h="+ frameSize.getHeight());
//        parameters.setPreviewSize(frameSize.getWidth(), frameSize.getHeight());
//        parameters.setPreviewSize(960, 1280);

        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCameraId, cameraInfo);
        int cameraRotationOffset = cameraInfo.orientation;
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }

        int displayRotation;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayRotation = (cameraRotationOffset + degrees) % 360;
            displayRotation = (360 - displayRotation) % 360; // compensate
        } else {
            displayRotation = (cameraRotationOffset - degrees + 360) % 360;
        }
        this.camera.setDisplayOrientation(displayRotation);

//        camera.setPreviewTexture();
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }


        camera.startPreview();

    }

    @Override
    public void stopPreview() {
        if (camera != null) {
            camera.stopFaceDetection();
            camera.stopPreview();
        }
    }

    @Override
    public void switchCamera() {

    }

    @Override
    public void takePicture() {

    }

    @Override
    public void startRecording() {

    }

    @Override
    public void stopRecording() {

    }

    @Override
    public void release() {

    }

    @Override
    public void startFaceDetection(Camera.FaceDetectionListener listener) {
        camera.setFaceDetectionListener(listener);
        camera.startFaceDetection();
    }

    protected Size calculateCameraFrameSize(List<Camera.Size> supportedSizes, int surfaceWidth, int surfaceHeight) {
        int calcWidth = 0;
        int calcHeight = 0;

        int maxAllowedWidth = (mMaxWidth != MAX_UNSPECIFIED && mMaxWidth < surfaceWidth)? mMaxWidth : surfaceWidth;
        int maxAllowedHeight = (mMaxHeight != MAX_UNSPECIFIED && mMaxHeight < surfaceHeight)? mMaxHeight : surfaceHeight;

        for (Camera.Size size : supportedSizes) {
            int width = size.width;
            int height = size.height;

            Log.e(tag, "w=" + width +", h=" + height);
            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = width;
                    calcHeight = height;
                }
            }
        }
        Log.e(tag, "calcWidth=" + calcWidth +", calcHeight=" + calcHeight);
        return new Size(calcWidth, calcHeight);
    }
}
