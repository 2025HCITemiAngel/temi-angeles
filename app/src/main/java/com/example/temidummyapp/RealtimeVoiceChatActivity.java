package com.example.temidummyapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

/**
 * OpenAI Realtime API를 활용한 실시간 음성 대화 Activity
 * 사용자와 AI가 자연스럽게 음성으로 대화
 */
public class RealtimeVoiceChatActivity extends BaseActivity {
    private static final String TAG = "RealtimeVoiceChat";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 2001;

    private AnimatedCircleView animatedCircle;
    private TextView instructionText;
    private ImageButton btnClose;
    private OpenAIRealtimeService realtimeService;
    private boolean isConnected = false;
    private boolean isRecording = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime_voice_chat);

        // 전체화면 모드
        setupImmersiveMode();

        // UI 초기화
        initializeViews();

        // Realtime 서비스 초기화
        setupRealtimeService();

        // 권한 확인 및 연결 시작
        checkPermissionAndStart();
    }

    private void setupImmersiveMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void initializeViews() {
        animatedCircle = findViewById(R.id.animated_circle);
        instructionText = findViewById(R.id.instruction_text);
        btnClose = findViewById(R.id.btn_close);

        btnClose.setOnClickListener(v -> {
            // 버튼 비활성화 (중복 클릭 방지)
            btnClose.setEnabled(false);
            instructionText.setText("종료 중...");
            
            // 순차적으로 종료
            stopVoiceChatGracefully();
        });
    }

    private void setupRealtimeService() {
        String apiKey = BuildConfig.OPENAI_API_KEY;
        realtimeService = new OpenAIRealtimeService(apiKey);

        realtimeService.setCallback(new OpenAIRealtimeService.RealtimeCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "Realtime API 연결됨");
                isConnected = true;
                runOnUiThread(() -> {
                    instructionText.setText("연결되었습니다. 말씀해주세요!");
                    animatedCircle.setIdleMode();
                });
                startRecording();
            }

            @Override
            public void onAudioLevelChanged(float level) {
                // 음압 레벨에 따라 원 크기 및 애니메이션 속도 조정
                runOnUiThread(() -> {
                    animatedCircle.setAudioLevel(level);
                });
            }

            @Override
            public void onTranscriptReceived(String transcript) {
                Log.d(TAG, "사용자 음성: " + transcript);
                runOnUiThread(() -> {
                    instructionText.setText("사용자: " + transcript);
                });
            }

            @Override
            public void onResponseStarted() {
                Log.d(TAG, "AI 응답 시작");
                
                // 🔇 마이크 일시 중지 (에코 방지)
                realtimeService.pauseMicrophone();
                
                runOnUiThread(() -> {
                    instructionText.setText("AI가 응답하고 있습니다...");
                    animatedCircle.setSpeakingMode();
                });
            }

            @Override
            public void onResponseReceived(String response) {
                Log.d(TAG, "AI 응답: " + response);
                runOnUiThread(() -> {
                    instructionText.setText("AI: " + response);
                });
            }

            @Override
            public void onResponseComplete() {
                Log.d(TAG, "AI 응답 완료");
                
                // 🎤 마이크 재개 (사용자 입력 대기)
                realtimeService.resumeMicrophone();
                
                runOnUiThread(() -> {
                    animatedCircle.setListeningMode();
                    instructionText.setText("말씀해주세요");
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "오류: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(RealtimeVoiceChatActivity.this, "오류: " + error, Toast.LENGTH_SHORT)
                            .show();
                    instructionText.setText("오류가 발생했습니다. 다시 시도해주세요.");
                });
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "연결 종료됨");
                isConnected = false;
                runOnUiThread(() -> {
                    instructionText.setText("연결이 종료되었습니다.");
                });
            }
        });
    }

    private void checkPermissionAndStart() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { android.Manifest.permission.RECORD_AUDIO },
                        PERMISSION_REQUEST_RECORD_AUDIO);
                return;
            }
        }

        startVoiceChat();
    }

    private void startVoiceChat() {
        instructionText.setText("연결 중...");
        animatedCircle.setConnectingMode();

        // Realtime API 연결
        realtimeService.connect();
    }

    private void startRecording() {
        if (!isRecording && isConnected) {
            isRecording = true;
            realtimeService.startAudioStreaming();
            animatedCircle.setListeningMode();
            Log.d(TAG, "음성 녹음 시작");
        }
    }

    /**
     * 순차적으로 안전하게 음성 대화 종료
     */
    private void stopVoiceChatGracefully() {
        new Thread(() -> {
            try {
                Log.d(TAG, "=== 음성 대화 종료 시작 ===");

                // 1단계: 오디오 스트리밍 중지 (녹음 및 재생)
                if (isRecording || isConnected) {
                    runOnUiThread(() -> instructionText.setText("오디오 중지 중..."));
                    realtimeService.stopAudioStreaming();
                    isRecording = false;
                    Thread.sleep(300); // 오디오 리소스 해제 대기
                    Log.d(TAG, "1단계: 오디오 스트리밍 중지 완료");
                }

                // 2단계: WebSocket 연결 종료
                if (isConnected) {
                    runOnUiThread(() -> instructionText.setText("연결 종료 중..."));
                    realtimeService.disconnect();
                    isConnected = false;
                    Thread.sleep(200); // WebSocket 종료 대기
                    Log.d(TAG, "2단계: WebSocket 연결 종료 완료");
                }

                // 3단계: 리소스 정리 완료
                runOnUiThread(() -> {
                    instructionText.setText("종료 완료");
                    Log.d(TAG, "=== 음성 대화 종료 완료 ===");
                    
                    // Activity 종료
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "종료 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(RealtimeVoiceChatActivity.this, 
                        "종료 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    /**
     * 즉시 종료 (백그라운드로 이동 시)
     */
    private void stopVoiceChat() {
        if (isRecording) {
            realtimeService.stopAudioStreaming();
            isRecording = false;
        }

        if (isConnected) {
            realtimeService.disconnect();
            isConnected = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy 호출됨");
        stopVoiceChat();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause 호출됨 - 리소스 일시 정지");
        // 백그라운드로 가면 즉시 중지
        if (isRecording) {
            realtimeService.stopAudioStreaming();
            isRecording = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceChat();
            } else {
                Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}

