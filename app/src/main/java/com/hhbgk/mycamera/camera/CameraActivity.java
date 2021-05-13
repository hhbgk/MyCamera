package com.hhbgk.mycamera.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.hhbgk.mycamera.R;
import com.hhbgk.mycamera.core.BaseCamera;
import com.hhbgk.mycamera.detection.FaceDetectView;
import com.hhbgk.mycamera.detection.FaceDetectionListenerImpl;
import com.hhbgk.mycamera.util.CommonUtil;

import java.lang.ref.WeakReference;

public class CameraActivity extends Activity {
    private String tag = getClass().getSimpleName();
    private BaseCamera camera;
    private FaceDetectView faceDetectView;
    private MainHandler mainHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        faceDetectView = findViewById(R.id.face_detect_view);
        Button btnSwitch = findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera != null) camera.switchCamera();
            }
        });
        SurfaceView surfaceView = findViewById(R.id.surfaceView);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.i(tag, "surfaceCreated:");
                camera = new Camera1Imp(holder);

                camera.init(getApplicationContext());
                camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                camera.startPreview();
                FaceDetectionListenerImpl listener = new FaceDetectionListenerImpl();
                mainHandler = new MainHandler(faceDetectView);
                listener.setHandler(mainHandler);
                camera.startFaceDetection(listener);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(tag, "surfaceChanged: format=" + format + ",w=" + width + ",h=" + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.i(tag, "surfaceDestroyed:");
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (camera != null) {
            camera.release();
        }
    }

    private final Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
//            Log.e(tag, "Face size=" + faces.length);
            faceDetectView.setFaces(faces);
        }
    };

    private static class MainHandler extends Handler {
        private final WeakReference<FaceDetectView> mFaceViewWeakReference;

        public MainHandler(FaceDetectView faceView) {
            mFaceViewWeakReference = new WeakReference<>(faceView);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CommonUtil.MSG_FOUND_FACES) {
                Camera.Face[] faces = (Camera.Face[]) msg.obj;
                mFaceViewWeakReference.get().setFaces(faces);
            }
            super.handleMessage(msg);
        }
    }
}
