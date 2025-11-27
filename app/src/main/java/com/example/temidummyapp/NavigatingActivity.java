package com.example.temidummyapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;

/**
 * 테미 이동 중 표시되는 얼굴 화면 Activity
 * - 화면을 터치하면 이전 페이지로 돌아감
 * - 도착하면 도착 상태로 전환 (터치 대기)
 */
public class NavigatingActivity extends AppCompatActivity implements OnGoToLocationStatusChangedListener {
    
    private static final String TAG = "NavigatingActivity";
    private Robot robot;
    private TextView statusText;
    private TextView guideText;
    private View progressBar;
    private ImageView backgroundImage;
    private View backgroundOverlay;
    private String destination;
    private boolean hasArrived = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigating);
        
        // 전체화면 설정
        setupFullscreen();
        
        // Robot 인스턴스 가져오기
        robot = Robot.getInstance();
        
        // 목적지 정보 받기
        destination = getIntent().getStringExtra("destination");
        
        // UI 초기화
        statusText = findViewById(R.id.status_text);
        guideText = findViewById(R.id.guide_text);
        progressBar = findViewById(R.id.progress_bar);
        backgroundImage = findViewById(R.id.background_image);
        backgroundOverlay = findViewById(R.id.background_overlay);
        
        if (destination != null) {
            statusText.setText("'" + destination + "' 으로 이동 중입니다");
        } else {
            statusText.setText("이동 안내 중입니다");
        }
        
        // 이동 시작 시 배경 이미지 표시
        showNavigatingBackground();
        
        // 화면 터치 시 이전 페이지로 돌아가기
        View rootView = findViewById(R.id.root_layout);
        rootView.setOnClickListener(v -> {
            finish(); // Activity 종료 = 이전 화면으로 복귀
        });
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if (robot != null) {
            robot.addOnGoToLocationStatusChangedListener(this);
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (robot != null) {
            robot.removeOnGoToLocationStatusChangedListener(this);
        }
    }
    
    @Override
    public void onGoToLocationStatusChanged(String location, String status, int descriptionId, String description) {
        String s = status == null ? "" : status.toLowerCase();
        
        // 도착하면 얼굴만 표시 (텍스트 모두 숨김)
        if (s.contains("arrived") || s.contains("complete")) {
            hasArrived = true;
            runOnUiThread(() -> {
                // 모든 텍스트와 로딩 바 숨기기 - 얼굴만 표시
                statusText.setVisibility(View.GONE);
                guideText.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                
                // 도착 시 배경 이미지 숨기기
                hideNavigatingBackground();
                
                // Toast로만 도착 알림 (잠깐 표시되었다 사라짐)
                Toast.makeText(this, "도착했습니다: " + location, Toast.LENGTH_SHORT).show();
                
                // finish() 호출 안 함 → 사용자 터치 대기 (얼굴만 보이는 상태)
            });
        }
        // 이동 중 상태 메시지 업데이트
        else {
            runOnUiThread(() -> {
                if (s.contains("obstacle")) {
                    statusText.setText("장애물을 피하고 있습니다...");
                } else if (s.contains("abort") || s.contains("cancel")) {
                    statusText.setText("이동이 중단되었습니다");
                    // 중단 시 배경 이미지 숨기기
                    hideNavigatingBackground();
                } else if (s.contains("calculate")) {
                    statusText.setText("경로를 계산하고 있습니다...");
                } else if (s.contains("going") || s.contains("start")) {
                    // 이동 중 상태로 복원
                    if (hasArrived) {
                        // 도착했다가 다시 이동하는 경우 (드래그 재시작 등)
                        hasArrived = false;
                        statusText.setVisibility(View.VISIBLE);
                        statusText.setTextColor(getResources().getColor(android.R.color.white));
                        guideText.setVisibility(View.VISIBLE);
                        guideText.setText("잠시만 길을 내어주세요\n(화면을 터치하면 이전 화면으로 돌아갑니다)");
                        progressBar.setVisibility(View.VISIBLE);
                        
                        // 재이동 시 배경 이미지 다시 표시
                        showNavigatingBackground();
                    }
                    
                    if (destination != null) {
                        statusText.setText("'" + destination + "' 으로 이동 중입니다");
                    } else {
                        statusText.setText("이동 중입니다");
                    }
                }
            });
        }
    }
    
    /**
     * 이동 중 배경 이미지를 표시합니다.
     * 이 메서드에서 원하는 이미지 리소스를 설정하세요.
     */
    private void showNavigatingBackground() {
        if (backgroundImage != null) {
            // TODO: 원하는 이미지 리소스로 변경하세요
            // 예시: backgroundImage.setImageResource(R.drawable.your_navigation_background);
            backgroundImage.setImageResource(R.drawable.ic_temi_menu);
            backgroundImage.setVisibility(View.VISIBLE);
        }
        if (backgroundOverlay != null) {
            backgroundOverlay.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 이동 중 배경 이미지를 숨깁니다.
     */
    private void hideNavigatingBackground() {
        if (backgroundImage != null) {
            backgroundImage.setVisibility(View.GONE);
        }
        if (backgroundOverlay != null) {
            backgroundOverlay.setVisibility(View.GONE);
        }
    }
    
    /**
     * 전체화면 모드 설정 (상태바, 네비게이션바 숨김)
     */
    private void setupFullscreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupFullscreen(); // 포커스 돌아올 때마다 전체화면 유지
        }
    }
}

