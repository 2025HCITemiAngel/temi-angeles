package com.example.temidummyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import androidx.cardview.widget.CardView;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotDragStateChangedListener;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnGoToLocationStatusChangedListener, OnRobotDragStateChangedListener {

    private Robot robot;
    private AlertDialog navigatingDialog;
    private String currentDestination;
    private boolean wasDragged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 화면 항상 켜짐 유지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 레이아웃 설정
        setContentView(R.layout.activity_main);

        // 상태바 & 네비게이션바 숨김
        applyImmersiveMode();

        robot = Robot.getInstance();

        // 텍스트 설정
        TextView title = findViewById(R.id.title);
        title.setText(R.string.temi_title);

        // 길 안내 카드: 위치 선택 → 이동
        CardView cardNavi = findViewById(R.id.card_navi);
        cardNavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLocationPicker();
            }
        });

        // 사진 찍기 카드 클릭 리스너 설정
        CardView cardPhoto = findViewById(R.id.card_photo);
        cardPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PhotoTemi.class);
                startActivity(intent);
            }
        });

        // 메뉴 추천 카드 클릭 리스너 설정
        CardView cardMenu = findViewById(R.id.card_menu);
        cardMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                startActivity(intent);
            }
        });

        // 주요 부스 카드 클릭 리스너 설정
        CardView cardBooth = findViewById(R.id.card_booth);
        cardBooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EventActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode(); // 포커스 복귀 시 다시 풀스크린 적용
    }

    private void applyImmersiveMode() {
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
    protected void onStart() {
        super.onStart();
        if (robot != null) {
            robot.addOnGoToLocationStatusChangedListener(this);
            robot.addOnRobotDragStateChangedListener(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (robot != null) {
            robot.removeOnGoToLocationStatusChangedListener(this);
            robot.removeOnRobotDragStateChangedListener(this);
        }
        dismissNavigatingDialog();
    }

    private void showLocationPicker() {
        List<String> locations = robot.getLocations();
        if (locations == null || locations.isEmpty()) {
            Toast.makeText(this, "저장된 위치가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] items = locations.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
                .setTitle("이동할 위치 선택")
                .setItems(items, (dialog, which) -> {
                    String target = locations.get(which);
                    startNavigation(target);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startNavigation(String target) {
        currentDestination = target;
        showNavigatingDialog();
        // 화면 안내 멘트 + 음성 안내
        TtsRequest tts = TtsRequest.create("이동 안내 중입니다! 잠시만 길을 내어주세요.", false);
        robot.speak(tts);
        robot.goTo(target);
    }

    private void showNavigatingDialog() {
        runOnUiThread(() -> {
            if (navigatingDialog == null) {
                navigatingDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("이동 중")
                        .setMessage("이동 안내 중입니다! 잠시만 길을 내어주세요.")
                        .setCancelable(true)
                        .create();
                navigatingDialog.setCanceledOnTouchOutside(true);
                navigatingDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        if (navigatingDialog.getWindow() != null) {
                            View decor = navigatingDialog.getWindow().getDecorView();
                            if (decor != null) {
                                decor.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        dismissNavigatingDialog();
                                    }
                                });
                            }
                        }
                    }
                });
            }
            if (!navigatingDialog.isShowing()) {
                navigatingDialog.show();
            }
        });
    }

    private void dismissNavigatingDialog() {
        runOnUiThread(() -> {
            if (navigatingDialog != null && navigatingDialog.isShowing()) {
                navigatingDialog.dismiss();
            }
        });
    }

    // 내비 상태 콜백
    @Override
    public void onGoToLocationStatusChanged(String location, String status, int descriptionId, String description) {
        // status 문자열은 SDK 버전에 따라 "start", "going", "arrived/complete", "abort/cancelled", "obstacle" 등
        String s = status == null ? "" : status.toLowerCase();
        if (s.contains("start") || s.contains("going") || s.contains("calculate")) {
            showNavigatingDialog();
        } else if (s.contains("arrived") || s.contains("complete")) {
            dismissNavigatingDialog();
            if (currentDestination != null && currentDestination.equals(location)) {
                currentDestination = null;
            }
            runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "도착했습니다: " + location, Toast.LENGTH_SHORT).show()
            );
        } else if (s.contains("abort") || s.contains("cancel")) {
            // 이동이 중지됨 → 끌어당김(drags) 해제 후 자동 재시도 플로우에 의해 복구됨
            showNavigatingDialog();
        } else if (s.contains("obstacle")) {
            showNavigatingDialog();
        }
    }

    // 로봇 드래그(머리 만짐 등) 상태 콜백
    @Override
    public void onRobotDragStateChanged(boolean isDragged) {
        // 드래그가 해제되고, 이동 목적지가 있다면 같은 목적지로 재시도
        boolean wasDraggedBefore = wasDragged;
        wasDragged = isDragged;
        if (wasDraggedBefore && !isDragged && currentDestination != null) {
            // 재시도
            runOnUiThread(() -> {
                showNavigatingDialog();
                TtsRequest tts = TtsRequest.create("이동을 다시 시작합니다.", false);
                robot.speak(tts);
                robot.goTo(currentDestination);
            });
        }
    }
}