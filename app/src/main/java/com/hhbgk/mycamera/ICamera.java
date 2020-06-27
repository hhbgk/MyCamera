package com.hhbgk.mycamera;

import android.content.Context;
import android.hardware.Camera;

public interface ICamera {
    void init(Context context);
    void open(int cameraId);
    void close();
    void startPreview();
    void stopPreview();
    void startFaceDetection(Camera.FaceDetectionListener listener);
    void switchCamera();
    void takePicture();
    void startRecording();
    void stopRecording();
    void release();
}
