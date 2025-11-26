package com.example.temidummyapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Locale;
import android.content.res.Configuration;
import android.content.Context;
import android.content.SharedPreferences;

public class MainActivity extends BaseActivity implements OnGoToLocationStatusChangedListener, OnRobotDragStateChangedListener {

    private Robot robot;
    private AlertDialog navigatingDialog;
    private String currentDestination;
    private boolean wasDragged = false;
    private View adminPanel; // 포함된 관리자 패널 루트
    private View mapPanel; // 길찾기 맵 패널 루트
    private boolean debugOutline = false;
    private boolean mapBitmapLoaded = false;
    private ImageView character; // 챗봇 아이콘
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;
    private TextView btnWakeWord; // Wake Word 토글 버튼
    private boolean isWakeWordEnabled = false; // Wake Word 활성화 상태

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 레이아웃 설정
        setContentView(R.layout.activity_main);

        robot = Robot.getInstance();

        // 언어 버튼 설정
        setupLanguageButtons();
        
        // Wake Word 버튼 설정
        setupWakeWordButton();

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
            View backButton = mapPanel.findViewById(R.id.backButton);
            View mapTitle = mapPanel.findViewById(R.id.map_title);
            
            // 뒤로가기 버튼 클릭 리스너
            if (backButton != null) {
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mapPanel.setVisibility(View.GONE);
                        showCharacterIcon(); // 맵 패널 닫을 때 챗봇 아이콘 다시 표시
                    }
                });
            }
            
            if (mapTitle != null) {
                mapTitle.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        debugOutline = !debugOutline;
                        applyDebugOutline();
                        String message = debugOutline ? getString(R.string.debug_outline_on) : getString(R.string.debug_outline_off);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
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
        
        // 다이얼로그에서도 전체화면 모드 유지
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, 
                                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        
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
                    if (pin.equals(BuildConfig.ADMIN_PIN)) {
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
        
        // 다이얼로그 표시 후 전체화면 모드 적용
        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
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
                    Toast.makeText(MainActivity.this, getString(R.string.admin_location_not_set), Toast.LENGTH_SHORT).show();
                    return;
                }
                TtsRequest tts = TtsRequest.create(getString(R.string.navigation_start), false);
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
        AlertDialog locationDialog = new AlertDialog.Builder(this)
                .setTitle("이동할 위치 선택")
                .setItems(items, (dialog, which) -> {
                    String target = locations.get(which);
                    startNavigation(target);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        
        locationDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                if (locationDialog.getWindow() != null) {
                    View decor = locationDialog.getWindow().getDecorView();
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
                    }
                }
            }
        });
        
        locationDialog.show();
    }

    private void startNavigation(String target) {
        currentDestination = target;
        showNavigatingDialog();
        // 화면 안내 멘트 + 음성 안내
        TtsRequest tts = TtsRequest.create(getString(R.string.navigation_guiding), false);
        robot.speak(tts);
        robot.goTo(target);
    }

    private void showNavigatingDialog() {
        runOnUiThread(() -> {
            if (navigatingDialog == null) {
                navigatingDialog = new AlertDialog.Builder(MainActivity.this)
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
                    Toast.makeText(MainActivity.this, getString(R.string.navigation_arrived, location), Toast.LENGTH_SHORT).show()
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
                TtsRequest tts = TtsRequest.create(getString(R.string.navigation_restart), false);
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


    // ===== Wake Word 토글 기능 =====
    private void setupWakeWordButton() {
        btnWakeWord = findViewById(R.id.btn_wake_word);
        if (btnWakeWord == null) return;

        // 초기 상태는 비활성화
        isWakeWordEnabled = false;
        updateWakeWordButtonUI();

        btnWakeWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleWakeWord();
            }
        });
    }

    private void toggleWakeWord() {
        isWakeWordEnabled = !isWakeWordEnabled;
        updateWakeWordButtonUI();

        if (isWakeWordEnabled) {
            // Wake Word 활성화
            startWakeWordService();
            Toast.makeText(this, "테미야 감지 활성화됨", Toast.LENGTH_SHORT).show();
        } else {
            // Wake Word 비활성화
            stopWakeWordService();
            Toast.makeText(this, "테미야 감지 비활성화됨", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWakeWordButtonUI() {
        if (btnWakeWord == null) return;

        if (isWakeWordEnabled) {
            // 활성화: 파란색 배경, 흰색 글자
            btnWakeWord.setBackgroundColor(0xFF1976D2);
            btnWakeWord.setTextColor(0xFFFFFFFF);
        } else {
            // 비활성화: 회색 배경, 짙은 회색 글자 (언어 버튼 비활성 시와 동일)
            btnWakeWord.setBackgroundColor(0xFFDDE6F5);
            btnWakeWord.setTextColor(0xFF4A5A6A);
        }
    }

    private WakeWordService getWakeWordService() {
        if (getApplication() instanceof TemiApplication) {
            TemiApplication app = (TemiApplication) getApplication();
            return (app != null) ? app.getWakeWordService() : null;
        }
        return null;
    }

    private void startWakeWordService() {
        WakeWordService service = getWakeWordService();
        if (service != null && !service.isListening()) {
            service.startListening();
            Log.d("MainActivity", "Wake Word 감지 시작");
        }
    }

    private void stopWakeWordService() {
        WakeWordService service = getWakeWordService();
        if (service != null && service.isListening()) {
            service.stopListening();
            Log.d("MainActivity", "Wake Word 감지 중지");
        }
    }

    // ===== 언어 전환 기능 =====
    private void setupLanguageButtons() {
        TextView btnKor = findViewById(R.id.btn_kor);
        TextView btnEng = findViewById(R.id.btn_eng);

        if (btnKor == null || btnEng == null) return;

        // 현재 언어 확인
        String currentLang = getCurrentLanguage();
        updateLanguageButtonUI(currentLang);

        btnKor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLanguage("ko");
            }
        });

        btnEng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLanguage("en");
            }
        });
    }

    private void updateLanguageButtonUI(String currentLang) {
        TextView btnKor = findViewById(R.id.btn_kor);
        TextView btnEng = findViewById(R.id.btn_eng);

        if (btnKor == null || btnEng == null) return;

        if ("ko".equals(currentLang)) {
            // 한국어 선택됨
            btnKor.setBackgroundColor(0xFF1976D2);
            btnKor.setTextColor(0xFFFFFFFF);
            btnEng.setBackgroundColor(0xFFDDE6F5);
            btnEng.setTextColor(0xFF4A5A6A);
        } else {
            // 영어 선택됨
            btnKor.setBackgroundColor(0xFFDDE6F5);
            btnKor.setTextColor(0xFF4A5A6A);
            btnEng.setBackgroundColor(0xFF1976D2);
            btnEng.setTextColor(0xFFFFFFFF);
        }
    }

    private void changeLanguage(String languageCode) {
        String currentLang = getCurrentLanguage();
        if (currentLang.equals(languageCode)) {
            return; // 이미 선택된 언어
        }

        // 언어 설정 저장
        saveLanguagePreference(languageCode);

        // 로케일 변경
        setLocale(languageCode);

        // UI를 동적으로 갱신 (재시작 없이)
        updateUITexts();
        updateLanguageButtonUI(languageCode);
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private String getCurrentLanguage() {
        return getResources().getConfiguration().locale.getLanguage();
    }

    private void saveLanguagePreference(String languageCode) {
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", languageCode);
        editor.apply();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 시스템 언어 변경 시 UI 업데이트
        updateUITexts();
    }

    private void updateUITexts() {
        // 타이틀 텍스트 업데이트
        TextView title = findViewById(R.id.title);
        if (title != null) {
            title.setText(R.string.temi_title);
        }

        // we-meet 텍스트 업데이트
        TextView weMeetText = findViewById(R.id.textViewWeMeet);
        if (weMeetText != null) {
            weMeetText.setText(R.string.main_wemeet_text);
        }

        // 길 안내 카드 텍스트 업데이트
        TextView naviTitle = findViewById(R.id.tv_navi_title);
        TextView naviSubtitle = findViewById(R.id.tv_navi_subtitle);
        if (naviTitle != null) naviTitle.setText(R.string.main_navigation_title);
        if (naviSubtitle != null) naviSubtitle.setText(R.string.main_navigation_subtitle);

        // 주요 이벤트 카드 텍스트 업데이트
        TextView eventTitle = findViewById(R.id.tv_event_title);
        TextView eventSubtitle = findViewById(R.id.tv_event_subtitle);
        if (eventTitle != null) eventTitle.setText(R.string.main_event_title);
        if (eventSubtitle != null) eventSubtitle.setText(R.string.main_event_subtitle);

        // 사진 찍기 카드 텍스트 업데이트
        TextView photoTitle = findViewById(R.id.tv_photo_title);
        TextView photoSubtitle = findViewById(R.id.tv_photo_subtitle);
        if (photoTitle != null) photoTitle.setText(R.string.main_photo_title);
        if (photoSubtitle != null) photoSubtitle.setText(R.string.main_photo_subtitle);

        // 주요 부스 카드 텍스트 업데이트
        TextView boothTitle = findViewById(R.id.tv_booth_title);
        TextView boothSubtitle = findViewById(R.id.tv_booth_subtitle);
        if (boothTitle != null) boothTitle.setText(R.string.main_booth_title);
        if (boothSubtitle != null) boothSubtitle.setText(R.string.main_booth_subtitle);

        // 메뉴 추천 카드 텍스트 업데이트
        TextView menuTitle = findViewById(R.id.tv_menu_title);
        TextView menuSubtitle = findViewById(R.id.tv_menu_subtitle);
        if (menuTitle != null) menuTitle.setText(R.string.main_menu_title);
        if (menuSubtitle != null) menuSubtitle.setText(R.string.main_menu_subtitle);
    }
}