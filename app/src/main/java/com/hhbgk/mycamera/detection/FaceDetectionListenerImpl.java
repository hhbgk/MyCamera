package com.hhbgk.mycamera.detection;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;

import com.hhbgk.mycamera.util.CommonUtil;

public class FaceDetectionListenerImpl implements Camera.FaceDetectionListener {
    private Handler handler;

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if (faces != null) {
            Message msg = new Message();
            msg.what = CommonUtil.MSG_FOUND_FACES;
            msg.obj = faces;
            handler.sendMessage(msg);
        }
    }
}
