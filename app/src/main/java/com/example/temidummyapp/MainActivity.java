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
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

public class MainActivity extends BaseActivity implements OnGoToLocationStatusChangedListener, OnRobotDragStateChangedListener {

    private Robot robot;
    private AlertDialog navigatingDialog;
    private String currentDestination;
    private boolean wasDragged = false;
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
        CardView cardEvent = findViewById(R.id.card_event);
        if (cardEvent != null) {
            cardEvent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, EventQRActivity.class);
                    startActivity(intent);
                }
            });
        }

        // 말풍선 애니메이션 시작
        startMessageBoxAnimation();
    }

    /**
     * 말풍선이 위아래로 움직이는 애니메이션 (0dp ~ -10dp 범위)
     */
    private void startMessageBoxAnimation() {
        View messageBoxContainer = findViewById(R.id.message_box_container);
        if (messageBoxContainer != null) {
            // 0dp에서 -10dp까지 위로 이동 (translationY: 0 -> -10)
            ObjectAnimator animator = ObjectAnimator.ofFloat(
                    messageBoxContainer,
                    "translationY",
                    0f,
                    -10f
            );
            animator.setDuration(1500); // 1.5초 동안 이동
            animator.setRepeatCount(ValueAnimator.INFINITE); // 무한 반복
            animator.setRepeatMode(ValueAnimator.REVERSE); // 왕복 (0 -> -10 -> 0)
            animator.start();
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
                        // 관리자 Activity로 이동
                        Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                        startActivity(intent);
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
        Intent intent = new Intent(MainActivity.this, NavigatingActivity.class);
        intent.putExtra("destination", destination);
        startActivity(intent);
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
        // NavigatingActivity가 자체적으로 상태를 처리하므로
        // MainActivity에서는 도착 시에만 현재 목적지 초기화
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
        // 드래그가 해제되고, 이동 목적지가 있다면 같은 목적지로 재시도
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

        // 초기 상태는 비활성화 (사용자가 직접 ON 해야 함)
        isWakeWordEnabled = false;
        updateWakeWordButtonUI();

        btnWakeWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 마이크 권한 확인
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[] { android.Manifest.permission.RECORD_AUDIO },
                                PERMISSION_REQUEST_RECORD_AUDIO);
                        return;
                    }
                }
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

        // 현재 언어에 맞는 텍스트 사용
        btnWakeWord.setText(R.string.wake_word_button);

        if (isWakeWordEnabled) {
            // 활성화: 파란색 배경, 흰색 글자
            btnWakeWord.setBackgroundColor(0xFF1976D2);
            btnWakeWord.setTextColor(0xFFFFFFFF);
        } else {
            // 비활성화: 회색 배경, 짙은 회색 글자
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

    @Override
    protected void onPause() {
        super.onPause();
        // MainActivity를 떠날 때 Wake Word 중지
        if (isWakeWordEnabled) {
            stopWakeWordService();
            Log.d("MainActivity", "MainActivity 일시정지 - Wake Word 중지");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // MainActivity로 돌아올 때 버튼이 ON 상태였으면 다시 시작
        if (isWakeWordEnabled) {
            startWakeWordService();
            Log.d("MainActivity", "MainActivity 재개 - Wake Word 재시작");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 승인됨 - Wake Word 토글 시도
                toggleWakeWord();
            } else {
                // 권한 거부됨
                Toast.makeText(this, "Wake Word를 사용하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
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

        // Wake Word 버튼 텍스트 업데이트
        if (btnWakeWord != null) {
            btnWakeWord.setText(R.string.wake_word_button);
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