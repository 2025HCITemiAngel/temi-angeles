package com.example.temidummyapp;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;

import java.util.Random;

public class MenuActivity extends AppCompatActivity {

    private static final long INITIAL_SPIN_INTERVAL = 70L;
    private static final long MAX_SPIN_INTERVAL = 220L;
    private static final long SPIN_DURATION_MS = 2200L;

    private final String[] menus = {
            "국밥", "밀면", "떡볶이", "피자", "치킨", "햄버거", "비빔밥", "돈까스", "떡갈비", 
    };

    private TextView menuPrev2;
    private TextView menuPrev1;
    private TextView currentMenu;
    private TextView menuNext1;
    private TextView menuNext2;
    private TextView resultText;

    private Button recommendButton;
    private ImageButton backButton;

    private final Handler handler = new Handler();
    private final Random random = new Random();

    private Robot robot;

    private boolean isSpinning = false;
    private boolean stopRequested = false;
    private int currentIndex = 0;
    private int targetIndex = -1;
    private long currentInterval = INITIAL_SPIN_INTERVAL;

    private final Runnable spinRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSpinning) {
                return;
            }

            stepForward();

            if (stopRequested && currentIndex == targetIndex) {
                finishSpin();
                return;
            }

            if (stopRequested && currentInterval < MAX_SPIN_INTERVAL) {
                currentInterval += 20L;
            }

            handler.postDelayed(this, currentInterval);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        bindViews();
        robot = Robot.getInstance();

        updateMenuSlot();
        resultText.setText(getString(R.string.menu_result_placeholder));

        recommendButton.setOnClickListener(v -> startSpin());
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void bindViews() {
        menuPrev2 = findViewById(R.id.menuPrev2);
        menuPrev1 = findViewById(R.id.menuPrev1);
        currentMenu = findViewById(R.id.currentMenu);
        menuNext1 = findViewById(R.id.menuNext1);
        menuNext2 = findViewById(R.id.menuNext2);
        resultText = findViewById(R.id.resultText);

        recommendButton = findViewById(R.id.recommendButton);
        backButton = findViewById(R.id.backButton);
    }

    private void startSpin() {
        if (isSpinning) {
            return;
        }

        isSpinning = true;
        stopRequested = false;
        targetIndex = -1;
        currentInterval = INITIAL_SPIN_INTERVAL;
        recommendButton.setEnabled(false);
        resultText.setText(getString(R.string.menu_result_placeholder));

        handler.post(spinRunnable);
        handler.postDelayed(this::requestStop, SPIN_DURATION_MS);
    }

    private void requestStop() {
        if (!isSpinning || stopRequested) {
            return;
        }
        stopRequested = true;
        targetIndex = random.nextInt(menus.length);
    }

    private void finishSpin() {
        isSpinning = false;
        stopRequested = false;
        handler.removeCallbacks(spinRunnable);

        String selectedMenu = menus[currentIndex];
        String message = getString(R.string.menu_result_format, selectedMenu);
        resultText.setText(message);

        recommendButton.setEnabled(true);

        TtsRequest ttsRequest = TtsRequest.create(message, false);
        robot.speak(ttsRequest);
    }

    private void stepForward() {
        currentIndex = (currentIndex + 1) % menus.length;
        updateMenuSlot();
    }

    private void updateMenuSlot() {
        menuPrev2.setText(getMenuForOffset(-2));
        menuPrev1.setText(getMenuForOffset(-1));
        currentMenu.setText(getMenuForOffset(0));
        menuNext1.setText(getMenuForOffset(1));
        menuNext2.setText(getMenuForOffset(2));
        animateMenuSlot();
    }

    private String getMenuForOffset(int offset) {
        int index = (currentIndex + offset + menus.length) % menus.length;
        return menus[index];
    }

    private void animateMenuSlot() {
        View slot = findViewById(R.id.menuSlot);
        slot.setTranslationY(-18f);
        slot.animate()
                .translationY(0f)
                .setDuration(120L)
                .setStartDelay(0L)
                .start();
    }
}
