package com.example.temidummyapp;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * OpenAI Whisper API를 사용한 음성 인식 서비스
 */
public class SpeechToTextService {
    private static final String TAG = "SpeechToTextService";
    private static final String API_URL = "https://api.openai.com/v1/audio/transcriptions";
    
    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;
    private String apiKey;
    
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    
    public interface TranscriptionCallback {
        void onTranscriptionComplete(String transcribedText);
        void onError(String error);
    }
    
    public SpeechToTextService(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * API 키 설정
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * 음성 녹음 시작
     */
    public boolean startRecording() {
        if (isRecording) {
            Log.w(TAG, "이미 녹음 중입니다");
            return false;
        }
        
        try {
            // 임시 파일 생성
            audioFile = File.createTempFile("audio_", ".m4a", context.getCacheDir());
            Log.d(TAG, "녹음 파일 경로: " + audioFile.getAbsolutePath());
            
            // MediaRecorder 초기화
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            Log.d(TAG, "녹음 시작됨");
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "녹음 시작 실패", e);
            releaseMediaRecorder();
            return false;
        }
    }
    
    /**
     * 음성 녹음 중지 및 STT 요청
     */
    public void stopRecordingAndTranscribe(TranscriptionCallback callback) {
        if (!isRecording) {
            callback.onError("녹음 중이 아닙니다");
            return;
        }
        
        try {
            // 녹음 중지
            mediaRecorder.stop();
            isRecording = false;
            Log.d(TAG, "녹음 중지됨");
            
            // 파일 크기 확인
            long fileSize = audioFile.length();
            Log.d(TAG, "녹음 파일 크기: " + fileSize + " bytes");
            
            if (fileSize == 0) {
                mainHandler.post(() -> callback.onError("녹음된 음성이 없습니다"));
                return;
            }
            
            // STT 요청
            transcribeAudio(audioFile, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "녹음 중지 실패", e);
            mainHandler.post(() -> callback.onError("녹음 중지 중 오류 발생: " + e.getMessage()));
        } finally {
            releaseMediaRecorder();
        }
    }
    
    /**
     * OpenAI Whisper API로 음성 파일 전송 및 텍스트 변환
     */
    private void transcribeAudio(File audioFile, TranscriptionCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            mainHandler.post(() -> callback.onError("API 키가 설정되지 않았습니다"));
            return;
        }
        
        Log.d(TAG, "STT 요청 시작...");
        
        // Multipart 요청 생성
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("language", "ko") // 한국어 설정
                .addFormDataPart("response_format", "text") // 텍스트 형식으로 응답
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.parse("audio/m4a")))
                .build();
        
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "STT API 호출 실패", e);
                mainHandler.post(() -> callback.onError("네트워크 오류: " + e.getMessage()));
                cleanupAudioFile();
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String responseBody = response.body().string();
                    
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "STT API 오류 응답: " + responseBody);
                        mainHandler.post(() -> callback.onError("음성 인식 실패 (코드: " + response.code() + ")"));
                        return;
                    }
                    
                    // response_format이 "text"이므로 응답이 바로 텍스트입니다
                    String transcribedText = responseBody.trim();
                    Log.d(TAG, "STT 결과: " + transcribedText);
                    
                    if (transcribedText.isEmpty()) {
                        mainHandler.post(() -> callback.onError("음성을 인식할 수 없습니다"));
                    } else {
                        mainHandler.post(() -> callback.onTranscriptionComplete(transcribedText));
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "STT 응답 처리 오류", e);
                    mainHandler.post(() -> callback.onError("응답 처리 오류: " + e.getMessage()));
                } finally {
                    cleanupAudioFile();
                }
            }
        });
    }
    
    /**
     * MediaRecorder 리소스 해제
     */
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                if (isRecording) {
                    mediaRecorder.stop();
                    isRecording = false;
                }
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "MediaRecorder 해제 중 오류", e);
            }
            mediaRecorder = null;
        }
    }
    
    /**
     * 임시 오디오 파일 삭제
     */
    private void cleanupAudioFile() {
        if (audioFile != null && audioFile.exists()) {
            boolean deleted = audioFile.delete();
            Log.d(TAG, "녹음 파일 삭제: " + deleted);
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
        releaseMediaRecorder();
        cleanupAudioFile();
    }
}

