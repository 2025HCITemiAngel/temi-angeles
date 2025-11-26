package com.example.temidummyapp;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * OpenAI Realtime API를 사용한 실시간 STT 서비스
 * WebSocket을 통해 오디오를 스트리밍하면서 실시간으로 텍스트 변환
 */
public class RealtimeSTTService {
    private static final String TAG = "RealtimeSTTService";
    private static final String WS_URL = "wss://api.openai.com/v1/realtime?intent=transcription&model=gpt-4o-mini-transcribe";
    
    // 오디오 설정
    private static final int SAMPLE_RATE = 24000; // 24kHz (Realtime API 요구사항)
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_MULTIPLIER = 4;
    
    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;
    private String apiKey;
    
    private WebSocket webSocket;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    
    private RealtimeCallback callback;
    
    public interface RealtimeCallback {
        void onTextDelta(String deltaText); // 실시간 텍스트 조각
        void onTextComplete(String fullText); // 완성된 텍스트
        void onError(String error);
        void onConnectionReady(); // 연결 준비 완료
    }
    
    public RealtimeSTTService(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder().build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * WebSocket 연결 시작 및 실시간 녹음 시작
     */
    public void startRealtimeSTT(RealtimeCallback callback) {
        if (isRecording) {
            Log.w(TAG, "이미 녹음 중입니다");
            return;
        }
        
        this.callback = callback;
        
        Log.d(TAG, "WebSocket 연결 시작...");
        
        // WebSocket 연결
        Request request = new Request.Builder()
                .url(WS_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("OpenAI-Beta", "realtime=v1")
                .build();
        
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket 연결됨");
                
                // 세션 설정 전송
                sendSessionConfig(webSocket);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "WebSocket 메시지 수신: " + text);
                handleWebSocketMessage(text);
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket 실패", t);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("연결 실패: " + t.getMessage());
                    }
                });
                stopRealtimeSTT();
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 종료: " + reason);
                stopRealtimeSTT();
            }
        });
    }
    
    /**
     * 세션 설정 전송
     */
    private void sendSessionConfig(WebSocket webSocket) {
        try {
            JSONObject config = new JSONObject();
            config.put("type", "session.update");
            
            JSONObject session = new JSONObject();
            session.put("input_audio_format", "pcm16");
            session.put("input_audio_transcription", new JSONObject()
                    .put("model", "gpt-4o-mini-transcribe"));
            
            // VAD (Voice Activity Detection) 설정
            session.put("turn_detection", new JSONObject()
                    .put("type", "server_vad")
                    .put("threshold", 0.5)
                    .put("prefix_padding_ms", 300)
                    .put("silence_duration_ms", 500));
            
            config.put("session", session);
            
            String configJson = config.toString();
            Log.d(TAG, "세션 설정 전송: " + configJson);
            webSocket.send(configJson);
            
            // 설정 전송 후 녹음 시작
            startAudioRecording(webSocket);
            
        } catch (Exception e) {
            Log.e(TAG, "세션 설정 생성 실패", e);
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onError("설정 실패: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * 오디오 녹음 시작 및 WebSocket으로 스트리밍
     */
    private void startAudioRecording(WebSocket webSocket) {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) 
                    * BUFFER_SIZE_MULTIPLIER;
            
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("오디오 녹음을 초기화할 수 없습니다");
                    }
                });
                return;
            }
            
            audioRecord.startRecording();
            isRecording = true;
            
            Log.d(TAG, "오디오 녹음 시작됨");
            
            // 연결 준비 완료 알림
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onConnectionReady();
                }
            });
            
            // 녹음 스레드 시작
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                
                while (isRecording) {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0) {
                        // PCM16 데이터를 Base64로 인코딩
                        String base64Audio = Base64.encodeToString(buffer, 0, bytesRead, Base64.NO_WRAP);
                        
                        // WebSocket으로 오디오 전송
                        try {
                            JSONObject audioMessage = new JSONObject();
                            audioMessage.put("type", "input_audio_buffer.append");
                            audioMessage.put("audio", base64Audio);
                            
                            webSocket.send(audioMessage.toString());
                            
                        } catch (Exception e) {
                            Log.e(TAG, "오디오 전송 실패", e);
                        }
                    }
                }
                
                Log.d(TAG, "녹음 스레드 종료");
            });
            
            recordingThread.start();
            
        } catch (Exception e) {
            Log.e(TAG, "오디오 녹음 시작 실패", e);
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onError("녹음 시작 실패: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * WebSocket 메시지 처리
     */
    private void handleWebSocketMessage(String messageJson) {
        try {
            JSONObject message = new JSONObject(messageJson);
            String type = message.optString("type");
            
            Log.d(TAG, "메시지 타입: " + type);
            
            switch (type) {
                case "session.created":
                case "session.updated":
                    Log.d(TAG, "세션 준비 완료");
                    break;
                    
                case "input_audio_buffer.speech_started":
                    Log.d(TAG, "음성 감지 시작");
                    break;
                    
                case "input_audio_buffer.speech_stopped":
                    Log.d(TAG, "음성 감지 종료");
                    break;
                    
                case "conversation.item.input_audio_transcription.completed":
                    // 전체 텍스트 완성
                    String fullText = message.optString("transcript", "");
                    Log.d(TAG, "전체 텍스트 완성: " + fullText);
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onTextComplete(fullText);
                        }
                    });
                    break;
                    
                case "conversation.item.input_audio_transcription.delta":
                    // 실시간 텍스트 조각
                    String delta = message.optString("delta", "");
                    Log.d(TAG, "텍스트 델타: " + delta);
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onTextDelta(delta);
                        }
                    });
                    break;
                    
                case "error":
                    String error = message.optJSONObject("error").optString("message", "알 수 없는 오류");
                    Log.e(TAG, "서버 오류: " + error);
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onError(error);
                        }
                    });
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "메시지 처리 실패", e);
        }
    }
    
    /**
     * 녹음 중지 및 WebSocket 종료
     */
    public void stopRealtimeSTT() {
        Log.d(TAG, "실시간 STT 중지");
        
        isRecording = false;
        
        // AudioRecord 중지
        if (audioRecord != null) {
            try {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "AudioRecord 중지 실패", e);
            }
            audioRecord = null;
        }
        
        // 녹음 스레드 종료 대기
        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "녹음 스레드 종료 대기 실패", e);
            }
            recordingThread = null;
        }
        
        // WebSocket 종료
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
        }
    }
    
    /**
     * 녹음 중인지 확인
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * 리소스 정리
     */
    public void release() {
        stopRealtimeSTT();
    }
}

