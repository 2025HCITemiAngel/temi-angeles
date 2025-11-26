package com.example.temidummyapp;

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
import android.content.DialogInterface;

public class DirectionsActivity extends AppCompatActivity implements OnGoToLocationStatusChangedListener, OnRobotDragStateChangedListener {

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

        // 화면 항상 켜짐 유지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 레이아웃 설정
        setContentView(R.layout.activity_directions);

        // 상태바 & 네비게이션바 숨김
        applyImmersiveMode();

        robot = Robot.getInstance();

        // 뷰 참조 가져오기
        mapContainer = findViewById(R.id.map_container);
        mapImage = findViewById(R.id.map_image);
        View mapTitle = findViewById(R.id.map_title);
        View mapClose = findViewById(R.id.map_close);
        View mapBack = findViewById(R.id.map_back);

        // 닫기 버튼 클릭 리스너
        if (mapClose != null) {
            mapClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // 뒤로 가기 버튼 클릭 리스너
        if (mapBack != null) {
            mapBack.setOnClickListener(new View.OnClickListener() {
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
                    Toast.makeText(DirectionsActivity.this, debugOutline ? "디버그 경계선 ON" : "디버그 경계선 OFF", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(DirectionsActivity.this, "관리자에서 위치를 설정해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                TtsRequest tts = TtsRequest.create("이동을 시작합니다.", false);
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
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
                navigatingDialog = new AlertDialog.Builder(DirectionsActivity.this)
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
        String s = status == null ? "" : status.toLowerCase();
        if (s.contains("start") || s.contains("going") || s.contains("calculate")) {
            showNavigatingDialog();
        } else if (s.contains("arrived") || s.contains("complete")) {
            dismissNavigatingDialog();
            if (currentDestination != null && currentDestination.equals(location)) {
                currentDestination = null;
            }
            runOnUiThread(() ->
                    Toast.makeText(DirectionsActivity.this, "도착했습니다: " + location, Toast.LENGTH_SHORT).show()
            );
        } else if (s.contains("abort") || s.contains("cancel")) {
            showNavigatingDialog();
        } else if (s.contains("obstacle")) {
            showNavigatingDialog();
        }
    }

    // 로봇 드래그(머리 만짐 등) 상태 콜백
    @Override
    public void onRobotDragStateChanged(boolean isDragged) {
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
