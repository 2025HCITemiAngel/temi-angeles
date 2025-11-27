package com.example.temidummyapp;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * OpenAI Realtime API 서비스
 * WebSocket을 통한 실시간 양방향 음성 대화
 */
public class OpenAIRealtimeService {
    private static final String TAG = "OpenAIRealtimeService";
    private static final String REALTIME_API_URL = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-10-01";

    // 오디오 설정
    private static final int SAMPLE_RATE = 24000; // OpenAI Realtime API 요구사항
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    private final String apiKey;

    private WebSocket webSocket;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isStreaming = false;
    private boolean isMicrophonePaused = false; // 마이크 일시 중지 상태
    private RealtimeCallback callback;

    // 음압 감지
    private float currentAudioLevel = 0.0f;

    public interface RealtimeCallback {
        void onConnected();

        void onAudioLevelChanged(float level); // 0.0 ~ 1.0

        void onTranscriptReceived(String transcript);

        void onResponseStarted();

        void onResponseReceived(String response);

        void onResponseComplete();

        void onError(String error);

        void onDisconnected();
    }

    public OpenAIRealtimeService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setCallback(RealtimeCallback callback) {
        this.callback = callback;
    }

    /**
     * Realtime API 연결
     */
    public void connect() {
        Request request = new Request.Builder()
                .url(REALTIME_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("OpenAI-Beta", "realtime=v1")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket 연결됨");

                // 세션 설정 전송
                sendSessionUpdate();

                if (callback != null) {
                    mainHandler.post(callback::onConnected);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleTextMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                handleBinaryMessage(bytes);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket 오류", t);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(t.getMessage()));
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 종료됨: " + reason);
                if (callback != null) {
                    mainHandler.post(callback::onDisconnected);
                }
            }
        });
    }

    /**
     * 세션 설정 전송 (RAG 시스템 프롬프트 포함)
     */
    private void sendSessionUpdate() {
        // OpenAIService의 SYSTEM_PROMPT 재사용
        String systemPrompt = getSystemPrompt();

        JsonObject sessionUpdate = new JsonObject();
        sessionUpdate.addProperty("type", "session.update");

        JsonObject session = new JsonObject();

        // 모달리티 설정 (텍스트 + 오디오) - 배열 형식으로 전송
        JsonArray modalities = new JsonArray();
        modalities.add("text");
        modalities.add("audio");
        session.add("modalities", modalities);

        // 음성 설정
        session.addProperty("voice", "alloy"); // alloy, echo, fable, onyx, nova, shimmer

        // 지시사항 (RAG 시스템 프롬프트)
        session.addProperty("instructions", systemPrompt);

        // 입력 오디오 형식 - 문자열로 전송
        session.addProperty("input_audio_format", "pcm16");

        // 출력 오디오 형식 - 문자열로 전송
        session.addProperty("output_audio_format", "pcm16");

        // 입력 음성 전사 활성화 (사용자 음성을 텍스트로 변환)
        JsonObject inputAudioTranscription = new JsonObject();
        inputAudioTranscription.addProperty("model", "whisper-1");
        session.add("input_audio_transcription", inputAudioTranscription);

        // VAD (Voice Activity Detection) 설정
        // threshold를 높여서 확실한 음성만 감지 (0.5 → 0.8)
        // silence_duration을 늘려서 사용자가 말할 시간 확보 (500ms → 2000ms)
        JsonObject turnDetection = new JsonObject();
        turnDetection.addProperty("type", "server_vad");
        turnDetection.addProperty("threshold", 0.8); // 높은 민감도 (0.5 → 0.8)
        turnDetection.addProperty("prefix_padding_ms", 500); // 발화 시작 전 패딩 증가
        turnDetection.addProperty("silence_duration_ms", 2000); // 2초 침묵 후 턴 종료 (500ms → 2000ms)
        session.add("turn_detection", turnDetection);

        sessionUpdate.add("session", session);

        String message = gson.toJson(sessionUpdate);
        webSocket.send(message);
        Log.d(TAG, "세션 설정 전송 완료 (RAG 포함)");
    }

    /**
     * OpenAIService의 SYSTEM_PROMPT 가져오기
     */
    private String getSystemPrompt() {
        // OpenAIService 인스턴스를 통해 가져오기
        OpenAIService openAIService = new OpenAIService();
        return openAIService.getSystemPrompt();
    }

    /**
     * 텍스트 메시지 처리
     */
    private void handleTextMessage(String text) {
        try {
            JsonObject json = gson.fromJson(text, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "";

            Log.d(TAG, "수신 메시지 타입: " + type);

            switch (type) {
                case "session.created":
                case "session.updated":
                    Log.d(TAG, "세션 준비됨");
                    break;

                case "conversation.item.input_audio_transcription.completed":
                    // 사용자 음성 인식 결과
                    if (json.has("transcript")) {
                        String transcript = json.get("transcript").getAsString();
                        if (callback != null) {
                            mainHandler.post(() -> callback.onTranscriptReceived(transcript));
                        }
                    }
                    break;

                case "response.audio_transcript.delta":
                    // AI 응답 텍스트 스트림
                    if (json.has("delta")) {
                        String delta = json.get("delta").getAsString();
                        if (callback != null) {
                            mainHandler.post(() -> callback.onResponseReceived(delta));
                        }
                    }
                    break;

                case "response.audio.delta":
                    // AI 음성 스트림 (Base64 인코딩된 PCM16 데이터)
                    if (json.has("delta")) {
                        String audioBase64 = json.get("delta").getAsString();
                        playAudioChunk(audioBase64);
                    }
                    break;

                case "response.done":
                    // AI 응답 완료
                    if (callback != null) {
                        mainHandler.post(callback::onResponseComplete);
                    }
                    break;

                case "error":
                    // 오류 발생
                    String error = json.has("error") ? json.get("error").toString() : "Unknown error";
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(error));
                    }
                    break;

                default:
                    Log.d(TAG, "처리되지 않은 메시지 타입: " + type);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "메시지 파싱 오류", e);
        }
    }

    /**
     * 바이너리 메시지 처리
     */
    private void handleBinaryMessage(ByteString bytes) {
        // 오디오 데이터 처리
        Log.d(TAG, "바이너리 데이터 수신: " + bytes.size() + " bytes");
    }

    /**
     * 오디오 스트리밍 시작
     */
    public void startAudioStreaming() {
        if (isStreaming) {
            return;
        }

        isStreaming = true;

        new Thread(() -> {
            try {
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG,
                        AUDIO_FORMAT, bufferSize);

                audioRecord.startRecording();
                Log.d(TAG, "오디오 녹음 시작");

                byte[] buffer = new byte[bufferSize];

                while (isStreaming) {
                    // 마이크가 일시 중지 상태면 데이터 읽지만 전송하지 않음
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                    if (bytesRead > 0 && !isMicrophonePaused) {
                        // 음압 계산
                        calculateAudioLevel(buffer, bytesRead);

                        // Base64 인코딩
                        String audioBase64 = Base64.encodeToString(buffer, 0, bytesRead, Base64.NO_WRAP);

                        // WebSocket으로 전송 (마이크 일시 중지 상태가 아닐 때만)
                        JsonObject audioAppend = new JsonObject();
                        audioAppend.addProperty("type", "input_audio_buffer.append");
                        audioAppend.addProperty("audio", audioBase64);

                        webSocket.send(gson.toJson(audioAppend));
                    } else if (isMicrophonePaused) {
                        // 일시 중지 중에는 음압 레벨 0으로 설정
                        currentAudioLevel = 0.0f;
                        if (callback != null) {
                            mainHandler.post(() -> callback.onAudioLevelChanged(0.0f));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "오디오 스트리밍 오류", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("오디오 스트리밍 오류: " + e.getMessage()));
                }
            }
        }).start();
    }

    /**
     * 음압 계산 (0.0 ~ 1.0)
     */
    private void calculateAudioLevel(byte[] buffer, int length) {
        if (length == 0) {
            currentAudioLevel = 0.0f;
            return;
        }

        // PCM16 데이터를 short로 변환하여 RMS 계산
        long sum = 0;
        int sampleCount = length / 2;

        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sum += sample * sample;
        }

        double rms = Math.sqrt((double) sum / sampleCount);
        double db = 20 * Math.log10(rms / 32768.0); // -60dB ~ 0dB 범위

        // 0.0 ~ 1.0으로 정규화 (-60dB ~ 0dB -> 0.0 ~ 1.0)
        float normalizedLevel = (float) Math.max(0.0, Math.min(1.0, (db + 60) / 60));

        // 부드러운 전환
        currentAudioLevel = currentAudioLevel * 0.7f + normalizedLevel * 0.3f;

        if (callback != null) {
            mainHandler.post(() -> callback.onAudioLevelChanged(currentAudioLevel));
        }
    }

    /**
     * AI 음성 재생
     */
    private void playAudioChunk(String audioBase64) {
        try {
            byte[] audioData = Base64.decode(audioBase64, Base64.NO_WRAP);

            if (audioTrack == null) {
                int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AUDIO_FORMAT);

                audioTrack = new AudioTrack(
                        android.media.AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AUDIO_FORMAT,
                        bufferSize,
                        AudioTrack.MODE_STREAM);

                audioTrack.play();
            }

            audioTrack.write(audioData, 0, audioData.length);
        } catch (Exception e) {
            Log.e(TAG, "오디오 재생 오류", e);
        }
    }

    /**
     * 오디오 스트리밍 중지 (순차적 종료)
     */
    public void stopAudioStreaming() {
        Log.d(TAG, "오디오 스트리밍 중지 시작");
        
        // 1. 스트리밍 플래그 끄기 (녹음 루프 중단)
        isStreaming = false;

        // 2. 오디오 재생 중지 (진행 중인 출력 즉시 중단)
        if (audioTrack != null) {
            try {
                Log.d(TAG, "AudioTrack 중지 시작");
                
                // 재생 중인 오디오 즉시 플러시
                audioTrack.pause();
                audioTrack.flush();
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
                
                Log.d(TAG, "AudioTrack 중지 완료");
            } catch (IllegalStateException e) {
                Log.w(TAG, "AudioTrack이 이미 해제됨", e);
            } catch (Exception e) {
                Log.e(TAG, "AudioTrack 중지 오류", e);
            }
        }

        // 3. 오디오 녹음 중지
        if (audioRecord != null) {
            try {
                Log.d(TAG, "AudioRecord 중지 시작");
                
                // 녹음 상태 확인 후 중지
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
                
                Log.d(TAG, "AudioRecord 중지 완료");
            } catch (IllegalStateException e) {
                Log.w(TAG, "AudioRecord가 이미 해제됨", e);
            } catch (Exception e) {
                Log.e(TAG, "AudioRecord 중지 오류", e);
            }
        }

        // 4. 음압 레벨 초기화
        currentAudioLevel = 0.0f;
        
        Log.d(TAG, "오디오 스트리밍 중지 완료");
    }

    /**
     * WebSocket 연결 종료 (순차적 종료)
     */
    public void disconnect() {
        Log.d(TAG, "WebSocket 연결 종료 시작");
        
        // 1. 오디오 먼저 중지
        stopAudioStreaming();

        // 2. WebSocket 종료
        if (webSocket != null) {
            try {
                Log.d(TAG, "WebSocket close 호출");
                webSocket.close(1000, "정상 종료");
                webSocket = null;
                Log.d(TAG, "WebSocket 종료 완료");
            } catch (Exception e) {
                Log.e(TAG, "WebSocket 종료 오류", e);
                webSocket = null; // 오류 발생 시에도 null로 설정
            }
        }

        // 3. OkHttp 리소스 정리
        try {
            client.dispatcher().executorService().shutdown();
            Log.d(TAG, "OkHttp 리소스 정리 완료");
        } catch (Exception e) {
            Log.e(TAG, "OkHttp 리소스 정리 오류", e);
        }
        
        Log.d(TAG, "WebSocket 연결 종료 완료");
    }

    /**
     * 마이크 일시 중지 (AI가 말할 때 - 에코 방지)
     */
    public void pauseMicrophone() {
        if (!isMicrophonePaused && isStreaming) {
            isMicrophonePaused = true;
            Log.d(TAG, "🔇 마이크 일시 중지 (AI 응답 중 - 에코 방지)");
        }
    }

    /**
     * 마이크 재개 (AI가 말 끝났을 때)
     */
    public void resumeMicrophone() {
        if (isMicrophonePaused && isStreaming) {
            isMicrophonePaused = false;
            Log.d(TAG, "🎤 마이크 재개 (사용자 입력 대기)");
        }
    }
}

