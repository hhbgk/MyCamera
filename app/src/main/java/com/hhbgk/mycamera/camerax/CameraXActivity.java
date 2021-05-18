package com.hhbgk.mycamera.camerax;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.hhbgk.mycamera.R;
import com.hhbgk.mycamera.util.BitmapUtils;
import com.hhbgk.mycamera.view.GraphicOverlay;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExperimentalGetImage
public class CameraXActivity extends AppCompatActivity {
    private final String tag = getClass().getSimpleName();
    private PreviewView pvCameraView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private FaceDetector mDetector;
    private GraphicOverlay ivPreview;
    private boolean needUpdateGraphicOverlayImageSourceInfo;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private ImageAnalysis imageAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_x);
        pvCameraView = findViewById(R.id.pv_preview);
        ivPreview = findViewById(R.id.iv_camera);
        startCamera();
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindCameraUseCase();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * 1.创建 Preview。
     * 2.指定所需的相机 LensFacing 选项。
     * 3.将所选相机和任意用例绑定到生命周期。
     * 4.将 Preview 连接到 PreviewView。
     */
    private void bindCameraUseCase() {
        // Unbind use cases before rebinding
        cameraProvider.unbindAll();

        if (cameraProvider == null) {
            Log.e(tag, "cameraProvider is null");
            return;
        }

        if (imageAnalyzer != null) {
            cameraProvider.unbind(imageAnalyzer);
        }

        if (mDetector != null) {
            mDetector.close();
        }
        // Real-time contour detection
        mDetector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build());

        int screenAspectRatio = getAspectRatio(pvCameraView.getWidth(), pvCameraView.getHeight());
        cameraExecutor = Executors.newSingleThreadExecutor();

//        Preview preview = new Preview.Builder().build();
//        preview.setSurfaceProvider(pvCameraView.getSurfaceProvider());

        final CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
//                .setTargetAspectRatio(screenAspectRatio)
//                .setTargetRotation(pvCameraView.getDisplay().getRotation())
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        needUpdateGraphicOverlayImageSourceInfo = true;
        imageAnalyzer.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
//                Log.e(tag, "image: w=" + imageProxy.getWidth() + ", h=" + imageProxy.getHeight() + ", f=" + imageProxy.getFormat());
                //图像格式
                int format = imageProxy.getFormat();
                if (format != ImageFormat.YUV_420_888) {
                    //抛出异常
                    imageProxy.close();
                    return;
                }

                if (needUpdateGraphicOverlayImageSourceInfo) {
                    boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                    Log.i(tag, "rotationDegrees " + rotationDegrees + ", isImageFlipped " + isImageFlipped);
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        ivPreview.setImageSourceInfo(isImageFlipped, imageProxy.getWidth(), imageProxy.getHeight());
                    } else {
                        ivPreview.setImageSourceInfo(isImageFlipped, imageProxy.getHeight(), imageProxy.getWidth());
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false;
                }

                final Bitmap bitmap;
                try {
//                    bitmap = ImageUtils.imageProxyToBitmap(image, image.getImageInfo().getRotationDegrees());
                    bitmap = BitmapUtils.getBitmap(imageProxy);
                    InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                    mDetector.process(inputImage)
                            .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                                @Override
                                public void onSuccess(@NonNull List<Face> faces) {
                                    if (faces.size() != 0) {
//                                        Log.e(tag, "onSuccess face size=" + faces.size() + "," + faces.get(0).getBoundingBox());
                                        ivPreview.updateFace(faces.get(0));
                                    } else {
                                        ivPreview.updateFace(null);
                                    }
                                    ivPreview.updateBitmap(bitmap);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(tag, "onFailure:" + e.getMessage());
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 释放资源
                imageProxy.close();
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer);
    }

    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double RATIO_16_9_VALUE = 16.0 / 9.0;
    private int getAspectRatio(int width, int height) {
        float previewRatio = (float) Math.max(width, height)/Math.min(width, height);
        if(Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)){
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    public void switchCamera(View view) {
        int newLensFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;
        Log.i(tag, "Set facing to " + newLensFacing);
        lensFacing = newLensFacing;
        bindCameraUseCase();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDetector != null) mDetector.close();
        if (cameraExecutor != null) cameraExecutor.shutdownNow();
    }
}