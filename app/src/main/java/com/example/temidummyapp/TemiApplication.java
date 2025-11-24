package com.example.temidummyapp;

import android.app.Application;
import android.util.Log;

public class TemiApplication extends Application {
    private static final String TAG = "TemiApplication";
    private WakeWordService wakeWordService;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Wake Word 서비스 초기화
        wakeWordService = new WakeWordService(this);
        
        // 앱 시작 시 Wake Word 감지 시작
        startWakeWordDetection();
    }
    
    private void startWakeWordDetection() {
        // 권한 체크는 MainActivity에서 수행하므로 여기서는 초기화만 수행
        // 실제 시작은 권한이 허용된 후 MainActivity에서 호출됨
        Log.d(TAG, "Wake word service created (will start after permission granted)");
    }
    
    public WakeWordService getWakeWordService() {
        return wakeWordService;
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        if (wakeWordService != null) {
            wakeWordService.release();
        }
    }
}

