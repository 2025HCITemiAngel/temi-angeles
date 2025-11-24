package com.example.temidummyapp;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

public class WakeWordService {
    private static final String TAG = "WakeWordService";
    private static final String KEYWORD_FILE = "테미야안녕_ko_android_v3_0_0.ppn";
    // 한국어 모델 파일 - Porcupine GitHub에서 다운로드 필요
    // 다운로드: https://github.com/Picovoice/porcupine/tree/master/lib/common
    // 파일명: porcupine_params_ko.pv
    private static final String MODEL_FILE = "porcupine_params_ko.pv";
    
    private PorcupineManager porcupineManager;
    private Context context;
    private boolean isListening = false;
    
    // AccessKey는 실제 Picovoice Console에서 발급받은 키로 교체해야 합니다
    // TODO: 실제 AccessKey로 교체하세요. Picovoice Console (https://console.picovoice.ai/)에서 발급받을 수 있습니다.
    private static final String ACCESS_KEY = "VO54Vp97/81vAL8ZiaWGi0idMIdthF3X2DRw/FLKwpIbgCwjLAkjQw==";
    
    public WakeWordService(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public void startListening() {
        if (isListening) {
            Log.w(TAG, "Wake word detection is already running");
            showToast("Wake Word 감지가 이미 실행 중입니다.");
            return;
        }
        
        try {
            Log.i(TAG, "=== Wake Word Service 초기화 시작 ===");
            Log.i(TAG, "Keyword file: " + KEYWORD_FILE);
            Log.i(TAG, "AccessKey: " + ACCESS_KEY.substring(0, Math.min(10, ACCESS_KEY.length())) + "...");
            
            // assets 폴더의 모든 파일 목록 확인 (디버깅용)
            try {
                String[] assetFiles = context.getAssets().list("");
                Log.i(TAG, "📁 Assets folder contents (" + (assetFiles != null ? assetFiles.length : 0) + " items):");
                if (assetFiles != null) {
                    for (String file : assetFiles) {
                        Log.i(TAG, "   - " + file);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not list assets: " + e.getMessage());
            }
            
            // assets 폴더에 파일이 있는지 확인
            boolean fileFound = false;
            try {
                java.io.InputStream is = context.getAssets().open(KEYWORD_FILE);
                is.close();
                fileFound = true;
                Log.i(TAG, "✅ Keyword file found in assets: " + KEYWORD_FILE);
            } catch (java.io.IOException e) {
                Log.e(TAG, "❌ Keyword file NOT found in assets: " + KEYWORD_FILE);
                Log.e(TAG, "   Error: " + e.getMessage());
                Log.e(TAG, "   Please ensure the file is in: app/src/main/assets/");
                Log.e(TAG, "   File must be copied to assets folder and project must be rebuilt");
                
                // 파일을 찾을 수 없으면 초기화 중단
                isListening = false;
                showToast("❌ 키워드 파일을 찾을 수 없습니다: " + KEYWORD_FILE);
                return;
            }
            
            PorcupineManagerCallback callback = new PorcupineManagerCallback() {
                @Override
                public void invoke(int keywordIndex) {
                    Log.i(TAG, "🎯 Wake word detected! Keyword index: " + keywordIndex);
                    onWakeWordDetected();
                }
            };
            
            Log.i(TAG, "Building PorcupineManager...");
            PorcupineManager.Builder builder = new PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPaths(new String[]{KEYWORD_FILE});
            
            // 한국어 모델 파일이 있는 경우 사용
            try {
                java.io.InputStream testStream = context.getAssets().open(MODEL_FILE);
                testStream.close();
                builder.setModelPath(MODEL_FILE);
                Log.i(TAG, "✅ Using Korean model file: " + MODEL_FILE);
            } catch (java.io.IOException e) {
                Log.w(TAG, "⚠️ Korean model file not found: " + MODEL_FILE);
                Log.w(TAG, "   Download from: https://github.com/Picovoice/porcupine/tree/master/lib/common");
                Log.w(TAG, "   File name: porcupine_params_ko.pv");
                Log.w(TAG, "   Place it in: app/src/main/assets/");
                Log.w(TAG, "   Continuing without model file (may not work for Korean keywords)");
            }
            
            porcupineManager = builder.build(context, callback);
            
            Log.i(TAG, "Starting PorcupineManager...");
            porcupineManager.start();
            isListening = true;
            Log.i(TAG, "✅ Wake word detection started successfully!");
            Log.i(TAG, "📢 Now listening for: '테미야안녕'");
            showToast("🎤 '테미야안녕' 감지 시작됨");
            
        } catch (PorcupineException e) {
            Log.e(TAG, "❌ Failed to initialize Porcupine: " + e.getMessage(), e);
            Log.e(TAG, "   Error details: " + e.toString());
            isListening = false;
            showToast("❌ Wake Word 초기화 실패: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Unexpected error: " + e.getMessage(), e);
            Log.e(TAG, "   Error type: " + e.getClass().getName());
            isListening = false;
            showToast("❌ 오류 발생: " + e.getMessage());
        }
    }
    
    public void stopListening() {
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
                porcupineManager = null;
                isListening = false;
                Log.i(TAG, "Wake word detection stopped");
                showToast("Wake Word 감지 중지됨");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping Porcupine: " + e.getMessage(), e);
                showToast("Wake Word 중지 중 오류: " + e.getMessage());
            }
        }
    }
    
    private void onWakeWordDetected() {
        Log.i(TAG, "🎯 Wake word '테미야안녕' detected! Moving to ChatActivity...");
        Log.i(TAG, "   Current context: " + context.getClass().getSimpleName());
        
        // UI 스레드에서 Toast 표시 및 Activity 이동
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    // 감지 성공 메시지 표시
                    Toast.makeText(context, "✅ '테미야안녕' 감지됨! 챗봇으로 이동합니다.", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "   Toast shown, starting ChatActivity...");
                    
                    // ChatActivity로 이동 (어떤 Activity에서든 작동하도록 FLAG 설정)
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);
                    Log.i(TAG, "   ✅ ChatActivity started successfully!");
                } catch (Exception e) {
                    Log.e(TAG, "   ❌ Error starting ChatActivity: " + e.getMessage(), e);
                    Toast.makeText(context, "❌ 챗봇 페이지로 이동 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public boolean isListening() {
        return isListening;
    }
    
    public void release() {
        stopListening();
    }
}

