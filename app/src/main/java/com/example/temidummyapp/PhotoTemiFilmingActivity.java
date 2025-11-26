package com.example.temidummyapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class PhotoTemiFilmingActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "PhotoTemiFilming";
    private static final long CAPTURE_TIMEOUT = 10000; // 10 seconds timeout
    private static final long INITIAL_DELAY_MS = 5000;

    private PreviewView previewView;
    private TextView countdownText;
    private TextView repetitionText;
    private ImageCapture imageCapture;

    private int countdownCount = 4;
    private final int totalRepetitions = 4;
    private final ArrayList<String> capturedImagePaths = new ArrayList<>();
    private String templateName;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable captureTimeoutRunnable;
    private CountDownTimer initialDelayTimer;

    private static final String[] REQUIRED_PERMISSIONS;
    static {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            REQUIRED_PERMISSIONS = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        } else {
            REQUIRED_PERMISSIONS = new String[] {Manifest.permission.CAMERA};
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phototemi_filming);

        templateName = getIntent().getStringExtra("template");
        if (templateName == null) {
            templateName = "Default Template";
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        previewView = findViewById(R.id.camera_preview);
        countdownText = findViewById(R.id.countdown_text);
        repetitionText = findViewById(R.id.repetition_text);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private void startCamera() {
        Log.i(TAG, "startCamera() - CameraX 초기화 시작");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        Log.i(TAG, "startCamera() - ProcessCameraProvider 인스턴스 요청됨.");

        cameraProviderFuture.addListener(() -> {
            Log.i(TAG, "startCamera() - CameraProvider 리스너 실행됨.");
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Log.i(TAG, "startCamera() - CameraProvider 가져오기 성공.");

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetResolution(new Size(640, 480)) // 해상도 명시적 설정
                        .build();
                Log.d(TAG, "startCamera() - ImageCapture: 해상도 640x480으로 설정됨");

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();

                try {
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                    Log.i(TAG, "startCamera() - CameraX use case 바인딩 성공.");
                } catch (Exception e) {
                    Log.e(TAG, "startCamera() - use case 바인딩 실패.", e);
                    return;
                }

                startInitialDelay();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "startCamera() - CameraProvider 가져오기 실패.", e);
            } catch (Exception e) {
                Log.e(TAG, "startCamera() - CameraX 초기화 중 알 수 없는 오류 발생.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        Log.d(TAG, "takePhoto() called");
        if (imageCapture == null) {
            Log.e(TAG, "imageCapture is null, cannot take photo.");
            handlePhotoResult(false);
            return;
        }

        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoTemi");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues)
                .build();

        captureTimeoutRunnable = () -> {
            Log.e(TAG, "Photo capture timed out after " + CAPTURE_TIMEOUT + "ms");
            handlePhotoResult(false);
        };
        timeoutHandler.postDelayed(captureTimeoutRunnable, CAPTURE_TIMEOUT);

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        timeoutHandler.removeCallbacks(captureTimeoutRunnable);
                        String msg = "사진 촬영 성공: " + output.getSavedUri();
                        Log.d(TAG, msg);
                        if (output.getSavedUri() != null) {
                            capturedImagePaths.add(output.getSavedUri().toString());
                        }
                        handlePhotoResult(true);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        timeoutHandler.removeCallbacks(captureTimeoutRunnable);
                        Log.e(TAG, "사진 촬영 실패: ", exc);
                        handlePhotoResult(false);
                    }
                }
        );
    }

    private void handlePhotoResult(boolean success) {
        Log.d(TAG, "handlePhotoResult() called with success: " + success);
        countdownCount--;
        if (countdownCount > 0) {
            startNextCountdown();
        } else {
            Log.d(TAG, "Finishing filming, starting PictureSelectActivity.");
            Intent intent = new Intent(PhotoTemiFilmingActivity.this, PhotoTemiPictureSelectActivity.class);
            intent.putStringArrayListExtra("captured_images", capturedImagePaths);
            intent.putExtra("template", templateName);
            startActivity(intent);
            finish();
        }
    }

    private void startInitialDelay() {
        countdownText.setVisibility(View.INVISIBLE);
        initialDelayTimer = new CountDownTimer(INITIAL_DELAY_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                initialDelayTimer = null;
                countdownText.setVisibility(View.VISIBLE);
                startNextCountdown();
            }
        }.start();
    }

    private void startNextCountdown() {
        repetitionText.setText(String.format(Locale.getDefault(), "%d/%d", totalRepetitions - countdownCount + 1, totalRepetitions));
        new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                countdownText.setText(String.valueOf(millisUntilFinished / 1000));
            }

            public void onFinish() {
                takePhoto();
            }
        }.start();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "필요한 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (captureTimeoutRunnable != null) {
            timeoutHandler.removeCallbacks(captureTimeoutRunnable);
        }
        if (initialDelayTimer != null) {
            initialDelayTimer.cancel();
            initialDelayTimer = null;
        }
    }
}
