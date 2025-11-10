package com.example.temidummyapp;  // ✅ 실제 패키지명으로 수정

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Random;
import com.robotemi.sdk.Robot; // ✅ Temi SDK import

public class MenuActivity extends AppCompatActivity {

    private TextView menuText;
    private Button startButton;
    private String[] menus = {"국밥", "밀면", "떡볶이", "피자", "치킨", "햄버거"};

    private Handler handler = new Handler();
    private Random random = new Random();
    private boolean isSpinning = false;
    private int currentIndex = 0;

    private Robot robot; // ✅ Temi 로봇 인스턴스

    private Runnable spinRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSpinning) {
                currentIndex = (currentIndex + 1) % menus.length;
                menuText.setText(menus[currentIndex]);
                handler.postDelayed(this, 100);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        menuText = findViewById(R.id.menuText);
        startButton = findViewById(R.id.startButton);
        robot = Robot.getInstance(); // ✅ Temi SDK 초기화

        menuText.setVisibility(View.INVISIBLE);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSpinning) {
                    startSlot();
                }
            }
        });
    }

    private void startSlot() {
        isSpinning = true;
        menuText.setVisibility(View.VISIBLE);
        handler.post(spinRunnable);

        // 2초 뒤 멈춤
        handler.postDelayed(() -> stopSlot(), 2000);
    }

    private void stopSlot() {
        isSpinning = false;
        handler.removeCallbacks(spinRunnable);

        int index = random.nextInt(menus.length);
        String selectedMenu = menus[index];
        String message = "오늘의 메뉴는 " + selectedMenu + "입니다!";

        menuText.setText(message);

        // ✅ Temi 음성 출력 (TtsRequest 사용)
        com.robotemi.sdk.TtsRequest ttsRequest =
                com.robotemi.sdk.TtsRequest.create(message, false);
        robot.speak(ttsRequest);
    }
}
