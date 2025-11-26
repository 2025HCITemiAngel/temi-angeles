package com.example.temidummyapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Button;
import android.app.Dialog;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private static final String ADMIN_PIN = "1234";
    private View adminPanel; // 포함된 관리자 패널 루트
    private View mapPanel; // 길찾기 맵 패널 루트
    private boolean debugOutline = false;
    private boolean mapBitmapLoaded = false;
    private ImageView character; // 챗봇 아이콘
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 화면 항상 켜짐 유지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 레이아웃 설정
        setContentView(R.layout.activity_main);

        // 상태바 & 네비게이션바 숨김
        applyImmersiveMode();

        // 오디오 녹음 권한 확인 및 요청
        checkAndRequestAudioPermission();

        robot = Robot.getInstance();

        // 텍스트 설정
        TextView title = findViewById(R.id.title);
        title.setText(R.string.temi_title);

        // include로 들어온 관리자 패널 루트
        adminPanel = findViewById(R.id.admin_panel);
        if (adminPanel == null) { // include 루트 id를 직접 찾을 수 없으면 include id로 대체 탐색
            View inc = findViewById(R.id.include_admin_panel);
            if (inc != null) {
                adminPanel = inc;
            }
        }
        if (adminPanel != null) {
            View close = adminPanel.findViewById(R.id.admin_close);
            if (close != null) {
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        adminPanel.setVisibility(View.GONE);
                        showCharacterIcon(); // 관리자 패널 닫을 때 챗봇 아이콘 다시 표시
                    }
                });
            }
            View save = adminPanel.findViewById(R.id.admin_save);
            if (save != null) {
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveAdminMappings();
                    }
                });
            }
        }

        // 맵 패널 루트
        mapPanel = findViewById(R.id.map_panel);
        if (mapPanel == null) {
            View incMap = findViewById(R.id.include_map_panel);
            if (incMap != null) {
                mapPanel = incMap;
            }
        }
        if (mapPanel != null) {
            View close = mapPanel.findViewById(R.id.map_close);
            View back = mapPanel.findViewById(R.id.map_back);
            View mapTitle = mapPanel.findViewById(R.id.map_title);
            View.OnClickListener hide = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mapPanel.setVisibility(View.GONE);
                    showCharacterIcon(); // 맵 패널 닫을 때 챗봇 아이콘 다시 표시
                }
            };
            if (close != null) close.setOnClickListener(hide);
            if (back != null) back.setOnClickListener(hide);
            if (mapTitle != null) {
                mapTitle.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        debugOutline = !debugOutline;
                        applyDebugOutline();
                        Toast.makeText(MainActivity.this, debugOutline ? "디버그 경계선 ON" : "디버그 경계선 OFF", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
            }
            wireMapButtons();
        }

        // 좌상단 빨간 점(관리자 진입)
        View adminDot = findViewById(R.id.btn_admin);
        if (adminDot != null) {
            adminDot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAdminPinDialog();
                }
            });
        }

        // 길 안내 카드: 위치 선택 → 이동
        CardView cardNavi = findViewById(R.id.card_navi);
        cardNavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DirectionsActivity.class);
                startActivity(intent);
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

        // 우하단 캐릭터(챗봇) 아이콘 클릭 → 채팅 화면 이동
        character = findViewById(R.id.character);
        if (character != null) {
            character.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    startActivity(intent);
                }
            });
        }

        // [1124] 하민용 임시 작업 textViewWeMeet 클릭 → QR 안내 화면 이동
        CardView cardEvent = findViewById(R.id.card_main_event);
        if (cardEvent != null) {
            cardEvent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, EventQRActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    private void showAdminPinDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_admin_pin);
        TextView display = dialog.findViewById(R.id.pin_display);
        // 원본 PIN 입력값은 display의 tag에 보관한다.
        display.setText("");
        display.setTag("");
        View.OnClickListener numClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(v instanceof Button)) return;
                Button b = (Button) v;
                String label = b.getText() != null ? b.getText().toString() : "";
                if ("삭제".equals(label)) {
                    String raw = display.getTag() != null ? display.getTag().toString() : "";
                    if (raw.length() > 0) {
                        raw = raw.substring(0, raw.length() - 1);
                        display.setTag(raw);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < raw.length(); i++) sb.append("•");
                        display.setText(sb.toString());
                    }
                    return;
                }
                if ("확인".equals(label)) {
                    String pin = display.getTag() != null ? display.getTag().toString() : "";
                    if (pin.equals(ADMIN_PIN)) {
                        dialog.dismiss();
                        showAdminOverlay();
                    } else {
                        Toast.makeText(MainActivity.this, "PIN이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                if (label.matches("\\d")) {
                    String raw = display.getTag() != null ? display.getTag().toString() : "";
                    if (raw.length() < 6) {
                        raw += label;
                        display.setTag(raw);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < raw.length(); i++) sb.append("•");
                        display.setText(sb.toString());
                    }
                }
            }
        };
        int[] ids = new int[]{R.id.key_0,R.id.key_1,R.id.key_2,R.id.key_3,R.id.key_4,R.id.key_5,R.id.key_6,R.id.key_7,R.id.key_8,R.id.key_9,R.id.key_del,R.id.key_ok};
        for (int id : ids) {
            View key = dialog.findViewById(id);
            if (key != null) key.setOnClickListener(numClick);
        }
        dialog.show();
    }

    private void showAdminOverlay() {
        if (adminPanel != null) {
            setupAdminRecycler();
            adminPanel.setVisibility(View.VISIBLE);
            hideCharacterIcon(); // 관리자 패널 표시 시 챗봇 아이콘 숨기기
        } else {
            Toast.makeText(this, "관리자 패널을 표시할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAdminRecycler() {
        android.view.View rvv = adminPanel.findViewById(R.id.recyclerMappings);
        if (!(rvv instanceof androidx.recyclerview.widget.RecyclerView)) return;
        androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) rvv;
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (int i = 1; i <= 21; i++) {
            ids.add(String.format(java.util.Locale.US, "btn_%02d", i));
        }
        java.util.List<String> temiLocations = getTemiLocationsSafe();
        java.util.Map<String,String> saved = AdminMappingStore.load(this);
        AdminMappingAdapter adapter = new AdminMappingAdapter(this, ids, saved, temiLocations);
        rv.setAdapter(adapter);
        rv.setTag(adapter);
    }

    private java.util.List<String> getTemiLocationsSafe() {
        try {
            java.util.List<String> list = robot != null ? robot.getLocations() : null;
            return list != null ? list : new java.util.ArrayList<String>();
        } catch (Exception e) {
            return new java.util.ArrayList<String>();
        }
    }

    private void saveAdminMappings() {
        android.view.View rvv = adminPanel.findViewById(R.id.recyclerMappings);
        if (!(rvv instanceof androidx.recyclerview.widget.RecyclerView)) return;
        androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) rvv;
        Object tag = rv.getTag();
        if (!(tag instanceof AdminMappingAdapter)) return;
        AdminMappingAdapter adapter = (AdminMappingAdapter) tag;
        java.util.HashMap<String,String> map = new java.util.HashMap<>();
        for (AdminMappingAdapter.Item it : adapter.getItems()) {
            if (it.location != null && it.location.length() > 0) {
                map.put(it.buttonId, it.location);
            }
        }
        AdminMappingStore.save(this, map);
        Toast.makeText(this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void showMapPanel() {
        if (mapPanel != null) {
            // 실행 시에는 기본적으로 투명하게 (윤곽 OFF)
            debugOutline = false;
            mapPanel.setVisibility(View.VISIBLE);
            applyDebugOutline();
            ensureMapBitmapLoaded();
            hideCharacterIcon(); // 맵 패널 표시 시 챗봇 아이콘 숨기기
        } else {
            // 폴백: 기존 위치 선택 다이얼로그
            showLocationPicker();
        }
    }

    private void wireMapButtons() {
        if (mapPanel == null) return;
        View container = mapPanel.findViewById(R.id.map_container);
        if (container == null) container = mapPanel;
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String resName = v.getResources().getResourceEntryName(v.getId());
                java.util.Map<String,String> map = AdminMappingStore.load(MainActivity.this);
                String location = map.get(resName);
                if (location == null || location.length() == 0) {
                    Toast.makeText(MainActivity.this, "관리자에서 위치를 설정해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                TtsRequest tts = TtsRequest.create("이동을 시작합니다.", false);
                robot.speak(tts);
                startNavigation(location);
            }
        };
        for (int i = 1; i <= 21; i++) {
            String idName = String.format(java.util.Locale.US, "btn_%02d", i);
            int id = getResources().getIdentifier(idName, "id", getPackageName());
            if (id != 0) {
                View b = container.findViewById(id);
                if (b != null) b.setOnClickListener(l);
            }
        }
    }

    private void applyDebugOutline() {
        if (mapPanel == null) return;
        int debugBg = debugOutline ? 0x3333AAFF : android.graphics.Color.TRANSPARENT;
        View container = mapPanel.findViewById(R.id.map_container);
        if (container == null) container = mapPanel;
        for (int i = 1; i <= 21; i++) {
            String idName = String.format(java.util.Locale.US, "btn_%02d", i);
            int id = getResources().getIdentifier(idName, "id", getPackageName());
            if (id != 0) {
                View b = container.findViewById(id);
                if (b != null) {
                    b.setBackgroundColor(debugBg);
                }
            }
        }
    }

    // ===== 큰 맵 이미지를 화면 크기에 맞게 다운샘플링해서 로드 =====
    private void ensureMapBitmapLoaded() {
        if (mapPanel == null || mapBitmapLoaded) return;
        final ImageView mapImage = mapPanel.findViewById(R.id.map_image);
        if (mapImage == null) return;
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

    // 챗봇 아이콘 숨기기
    private void hideCharacterIcon() {
        if (character != null) {
            character.setVisibility(View.GONE);
        }
    }

    // 챗봇 아이콘 표시하기
    private void showCharacterIcon() {
        if (character != null) {
            character.setVisibility(View.VISIBLE);
        }
    }

    // 오디오 녹음 권한 확인 및 요청
    private void checkAndRequestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        } else {
            // 권한이 이미 있으면 Wake Word 서비스 시작
            startWakeWordService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되면 Wake Word 서비스 시작
                startWakeWordService();
            } else {
                Toast.makeText(this, "오디오 녹음 권한이 필요합니다. Wake Word 기능을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startWakeWordService() {
        android.util.Log.d("MainActivity", "Starting Wake Word Service...");
        TemiApplication app = (TemiApplication) getApplication();
        if (app != null && app.getWakeWordService() != null) {
            app.getWakeWordService().startListening();
            android.util.Log.d("MainActivity", "Wake Word Service startListening() called");
        } else {
            android.util.Log.e("MainActivity", "Failed to get Wake Word Service");
            Toast.makeText(this, "Wake Word 서비스를 초기화할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}