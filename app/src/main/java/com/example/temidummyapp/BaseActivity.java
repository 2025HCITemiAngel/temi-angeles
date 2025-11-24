package com.example.temidummyapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

/**
 * 모든 Activity의 기본 클래스
 * Wake Word 감지를 위한 권한 체크를 공통으로 처리합니다.
 */
public class BaseActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestAudioPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Activity가 다시 활성화될 때 Wake Word 서비스가 실행 중인지 확인
        ensureWakeWordServiceRunning();
    }

    private void checkAndRequestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        } else {
            // 권한이 이미 있으면 Wake Word 서비스 시작
            ensureWakeWordServiceRunning();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되면 Wake Word 서비스 시작
                ensureWakeWordServiceRunning();
            }
        }
    }

    private void ensureWakeWordServiceRunning() {
        if (getApplication() instanceof TemiApplication) {
            TemiApplication app = (TemiApplication) getApplication();
            if (app != null && app.getWakeWordService() != null) {
                if (!app.getWakeWordService().isListening()) {
                    app.getWakeWordService().startListening();
                }
            }
        }
    }
}

