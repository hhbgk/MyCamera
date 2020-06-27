package com.hhbgk.mycamera.core;

import android.hardware.Camera;

import com.hhbgk.mycamera.ICamera;

public abstract class BaseCamera implements ICamera {
    protected String tag = getClass().getSimpleName();

    protected Camera camera;
    protected int currentCameraId;
    protected int faceFrontCameraId;
    protected int faceBackCameraId;
    protected int numberOfCameras = 0;
    protected int faceFrontCameraOrientation;
    protected int faceBackCameraOrientation;
}
