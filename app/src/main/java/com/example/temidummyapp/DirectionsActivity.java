package com.example.temidummyapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotDragStateChangedListener;

public class DirectionsActivity extends BaseActivity implements OnGoToLocationStatusChangedListener, OnRobotDragStateChangedListener {

    private Robot robot;
    private AlertDialog navigatingDialog;
    private String currentDestination;
    private boolean wasDragged = false;
    private boolean debugOutline = false;
    private boolean mapBitmapLoaded = false;
    private View mapContainer;
    private ImageView mapImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 레이아웃 설정
        setContentView(R.layout.activity_directions);

        robot = Robot.getInstance();

        // 뷰 참조 가져오기
        mapContainer = findViewById(R.id.map_container);
        mapImage = findViewById(R.id.map_image);
        View mapTitle = findViewById(R.id.map_title);
        View backButton = findViewById(R.id.backButton);

        // 뒤로가기 버튼 클릭 리스너
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // 타이틀 롱클릭으로 디버그 모드 전환
        if (mapTitle != null) {
            mapTitle.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    debugOutline = !debugOutline;
                    applyDebugOutline();
                    String message = debugOutline ? getString(R.string.debug_outline_on) : getString(R.string.debug_outline_off);
                    Toast.makeText(DirectionsActivity.this, message, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        // 맵 버튼들 와이어링
        wireMapButtons();

        // 맵 이미지 로드
        ensureMapBitmapLoaded();
    }

    private void wireMapButtons() {
        if (mapContainer == null) return;
        
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String resName = v.getResources().getResourceEntryName(v.getId());
                java.util.Map<String,String> map = AdminMappingStore.load(DirectionsActivity.this);
                String location = map.get(resName);
                if (location == null || location.length() == 0) {
                    Toast.makeText(DirectionsActivity.this, getString(R.string.admin_location_not_set), Toast.LENGTH_SHORT).show();
                    return;
                }
                TtsRequest tts = TtsRequest.create(getString(R.string.navigation_start), false);
                robot.speak(tts);
                startNavigation(location);
            }
        };
        
        // btn_01 ~ btn_21 버튼들에 클릭 리스너 설정
        for (int i = 1; i <= 21; i++) {
            String idName = String.format(java.util.Locale.US, "btn_%02d", i);
            int id = getResources().getIdentifier(idName, "id", getPackageName());
            if (id != 0) {
                View b = mapContainer.findViewById(id);
                if (b != null) b.setOnClickListener(l);
            }
        }
    }

    private void applyDebugOutline() {
        if (mapContainer == null) return;
        int debugBg = debugOutline ? 0x3333AAFF : android.graphics.Color.TRANSPARENT;
        
        for (int i = 1; i <= 21; i++) {
            String idName = String.format(java.util.Locale.US, "btn_%02d", i);
            int id = getResources().getIdentifier(idName, "id", getPackageName());
            if (id != 0) {
                View b = mapContainer.findViewById(id);
                if (b != null) {
                    b.setBackgroundColor(debugBg);
                }
            }
        }
    }

    // 큰 맵 이미지를 화면 크기에 맞게 다운샘플링해서 로드
    private void ensureMapBitmapLoaded() {
        if (mapImage == null || mapBitmapLoaded) return;
        
        mapImage.post(new Runnable() {
            @Override
            public void run() {
                if (mapBitmapLoaded) return;
                int targetW = mapImage.getWidth();
                int targetH = mapImage.getHeight();
                if (targetW <= 0 || targetH <= 0) return;
                android.graphics.Bitmap bitmap = decodeSampledBitmapFromResource(
                        getResources(), R.drawable.map, targetW, targetH);
                if (bitmap != null) {
                    mapImage.setImageBitmap(bitmap);
                    mapBitmapLoaded = true;
                }
            }
        });
    }

    private static android.graphics.Bitmap decodeSampledBitmapFromResource(android.content.res.Resources res, int resId, int reqWidth, int reqHeight) {
        final android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565; // 메모리 절약
        options.inJustDecodeBounds = false;
        try {
            return android.graphics.BitmapFactory.decodeResource(res, resId, options);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
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

    private void startNavigation(String target) {
        currentDestination = target;
        // 기존 다이얼로그 대신 NavigatingActivity로 이동
        startNavigatingActivity(target);
        // 화면 안내 멘트 + 음성 안내
        TtsRequest tts = TtsRequest.create(getString(R.string.navigation_guiding), false);
        robot.speak(tts);
        robot.goTo(target);
    }
    
    /**
     * NavigatingActivity 시작 (이동 중 얼굴 화면)
     */
    private void startNavigatingActivity(String destination) {
        Intent intent = new Intent(DirectionsActivity.this, NavigatingActivity.class);
        intent.putExtra("destination", destination);
        startActivity(intent);
    }

    private void showNavigatingDialog() {
        runOnUiThread(() -> {
            if (navigatingDialog == null) {
                navigatingDialog = new AlertDialog.Builder(DirectionsActivity.this)
                        .setTitle("이동 중")
                        .setMessage(getString(R.string.navigation_guiding))
                        .setCancelable(true)
                        .create();
                navigatingDialog.setCanceledOnTouchOutside(true);
                navigatingDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        if (navigatingDialog.getWindow() != null) {
                            View decor = navigatingDialog.getWindow().getDecorView();
                            if (decor != null) {
                                // 전체화면 모드 유지
                                decor.setSystemUiVisibility(
                                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                );
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
        // NavigatingActivity가 자체적으로 상태를 처리하므로
        // DirectionsActivity에서는 도착 시에만 현재 목적지 초기화
        String s = status == null ? "" : status.toLowerCase();
        if (s.contains("arrived") || s.contains("complete")) {
            if (currentDestination != null && currentDestination.equals(location)) {
                currentDestination = null;
            }
        }
    }

    // 로봇 드래그(머리 만짐 등) 상태 콜백
    @Override
    public void onRobotDragStateChanged(boolean isDragged) {
        boolean wasDraggedBefore = wasDragged;
        wasDragged = isDragged;
        if (wasDraggedBefore && !isDragged && currentDestination != null) {
            // 재시도 - NavigatingActivity로 다시 이동
            runOnUiThread(() -> {
                startNavigatingActivity(currentDestination);
                TtsRequest tts = TtsRequest.create(getString(R.string.navigation_restart), false);
                robot.speak(tts);
                robot.goTo(currentDestination);
            });
        }
    }
}
