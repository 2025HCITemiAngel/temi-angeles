package com.example.temidummyapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 음압에 따라 크기가 변하고 그라데이션 애니메이션이 있는 원형 뷰
 */
public class AnimatedCircleView extends View {
    private static final float MIN_RADIUS_RATIO = 0.15f; // 최소 크기 (화면 대비)
    private static final float MAX_RADIUS_RATIO = 0.35f; // 최대 크기 (화면 대비)
    private static final float DEFAULT_RADIUS_RATIO = 0.20f; // 기본 크기

    private Paint circlePaint;
    private float currentRadius;
    private float targetRadius;
    private float baseRadius;
    private float audioLevel = 0.0f; // 0.0 ~ 1.0

    // 그라데이션 애니메이션
    private ValueAnimator gradientAnimator;
    private float gradientOffset = 0.0f;
    private float animationSpeed = 1.0f; // 애니메이션 속도 배율

    // 색상 배열 (그라데이션)
    private final int[] gradientColors = {
            Color.parseColor("#FF6B9D"), // 핑크
            Color.parseColor("#C86DD7"), // 보라
            Color.parseColor("#3023AE"), // 진한 보라
            Color.parseColor("#53A0FD"), // 파랑
            Color.parseColor("#F18F01"), // 주황
            Color.parseColor("#FF6B9D")  // 핑크 (순환)
    };

    // 모드
    private enum Mode {
        IDLE, // 대기
        LISTENING, // 듣기
        SPEAKING, // 말하기
        CONNECTING // 연결 중
    }

    private Mode currentMode = Mode.CONNECTING;

    public AnimatedCircleView(Context context) {
        super(context);
        init();
    }

    public AnimatedCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);

        // 기본 반지름 설정 (화면 크기 기준, 나중에 onSizeChanged에서 재계산)
        currentRadius = 200f;
        targetRadius = 200f;
        baseRadius = 200f;

        startGradientAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 화면 크기에 따라 반지름 재계산
        float minDimension = Math.min(w, h);
        baseRadius = minDimension * DEFAULT_RADIUS_RATIO;
        currentRadius = baseRadius;
        targetRadius = baseRadius;
    }

    /**
     * 그라데이션 애니메이션 시작
     */
    private void startGradientAnimation() {
        gradientAnimator = ValueAnimator.ofFloat(0f, 360f);
        gradientAnimator.setDuration(3000); // 기본 3초
        gradientAnimator.setRepeatCount(ValueAnimator.INFINITE);
        gradientAnimator.setInterpolator(new LinearInterpolator());
        gradientAnimator.addUpdateListener(animation -> {
            gradientOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
        gradientAnimator.start();
    }

    /**
     * 음압 레벨 설정 (0.0 ~ 1.0)
     */
    public void setAudioLevel(float level) {
        this.audioLevel = Math.max(0.0f, Math.min(1.0f, level));

        // 음압에 따라 원 크기 조정
        float minDimension = Math.min(getWidth(), getHeight());
        float minRadius = minDimension * MIN_RADIUS_RATIO;
        float maxRadius = minDimension * MAX_RADIUS_RATIO;
        targetRadius = minRadius + (maxRadius - minRadius) * audioLevel;

        // 음압에 따라 애니메이션 속도 조정
        updateAnimationSpeed();
    }

    /**
     * 애니메이션 속도 업데이트
     */
    private void updateAnimationSpeed() {
        if (gradientAnimator != null) {
            // 음압이 높으면 빠르게 (500ms), 낮으면 느리게 (5000ms)
            long duration = (long) (5000 - (4500 * audioLevel));
            gradientAnimator.setDuration(duration);
        }
    }

    /**
     * 대기 모드
     */
    public void setIdleMode() {
        currentMode = Mode.IDLE;
        audioLevel = 0.1f;
        setAudioLevel(0.1f);
    }

    /**
     * 듣기 모드
     */
    public void setListeningMode() {
        currentMode = Mode.LISTENING;
        audioLevel = 0.2f;
        setAudioLevel(0.2f);
    }

    /**
     * 말하기 모드 (AI 응답 중)
     */
    public void setSpeakingMode() {
        currentMode = Mode.SPEAKING;
        audioLevel = 0.5f;
        setAudioLevel(0.5f);
    }

    /**
     * 연결 중 모드
     */
    public void setConnectingMode() {
        currentMode = Mode.CONNECTING;
        audioLevel = 0.3f;
        setAudioLevel(0.3f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // 부드러운 크기 전환 (easing)
        currentRadius += (targetRadius - currentRadius) * 0.15f;

        // 그라데이션 생성 (회전 효과)
        float angle = (float) Math.toRadians(gradientOffset);
        float gradientX = centerX + (float) Math.cos(angle) * currentRadius;
        float gradientY = centerY + (float) Math.sin(angle) * currentRadius;

        LinearGradient gradient = new LinearGradient(
                centerX - currentRadius, centerY - currentRadius,
                gradientX, gradientY,
                gradientColors,
                null,
                Shader.TileMode.CLAMP);

        circlePaint.setShader(gradient);

        // 원 그리기
        canvas.drawCircle(centerX, centerY, currentRadius, circlePaint);

        // 부드러운 외곽선 (글로우 효과)
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(8f);
        glowPaint.setColor(Color.parseColor("#80FFFFFF")); // 반투명 흰색
        canvas.drawCircle(centerX, centerY, currentRadius + 4f, glowPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (gradientAnimator != null) {
            gradientAnimator.cancel();
        }
    }
}

