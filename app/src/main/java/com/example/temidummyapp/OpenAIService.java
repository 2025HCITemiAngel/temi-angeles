package com.example.temidummyapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OpenAI API 통신 서비스
 * RAG 구조를 위한 행사장 정보를 시스템 프롬프트에 포함
 */
public class OpenAIService {
    private static final String TAG = "OpenAIService";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    private String apiKey;
    
    // RAG: 행사장 정보 시스템 프롬프트
    private static final String SYSTEM_PROMPT = 
            "당신은 행사장 안내 도우미입니다. 방문객들에게 친절하고 정확한 정보를 제공해야 합니다.\n\n" +
            "=== 행사장 정보 ===\n" +
            "행사명: 2025 테크 엑스포\n" +
            "일시: 2025년 1월 15일 - 17일\n" +
            "장소: 서울 코엑스 그랜드볼룸\n\n" +
            "=== 주요 부스 정보 ===\n" +
            "1. A홀 - AI 및 로보틱스 존\n" +
            "   - A-01: 테미 로봇 체험존\n" +
            "   - A-05: 자율주행 기술 전시\n" +
            "   - A-10: AI 챗봇 데모\n\n" +
            "2. B홀 - 스마트 홈 & IoT 존\n" +
            "   - B-01: 스마트 가전 체험\n" +
            "   - B-07: IoT 센서 기술\n" +
            "   - B-12: 홈 오토메이션 솔루션\n\n" +
            "3. C홀 - 메타버스 & VR/AR 존\n" +
            "   - C-03: VR 게임 체험\n" +
            "   - C-08: AR 네비게이션\n" +
            "   - C-15: 메타버스 플랫폼\n\n" +
            "=== 주요 이벤트 일정 ===\n" +
            "- 1월 15일 10:00 - 개막식 (그랜드 홀)\n" +
            "- 1월 15일 14:00 - AI 미래 기술 세미나 (컨퍼런스룸 A)\n" +
            "- 1월 16일 11:00 - 로봇 댄스 퍼포먼스 (메인 스테이지)\n" +
            "- 1월 16일 15:00 - 스타트업 피칭 대회 (컨퍼런스룸 B)\n" +
            "- 1월 17일 13:00 - VR 체험 이벤트 (C홀)\n" +
            "- 1월 17일 16:00 - 폐막식 및 시상식 (그랜드 홀)\n\n" +
            "=== 편의시설 ===\n" +
            "- 푸드코트: 1층 동쪽 (한식, 양식, 카페)\n" +
            "- 휴게실: 각 홀마다 1개소\n" +
            "- 화장실: 각 층 양쪽 끝\n" +
            "- 안내 데스크: 메인 입구\n" +
            "- 주차장: 지하 1-3층\n\n" +
            "=== 운영 시간 ===\n" +
            "- 평일(월-금): 09:00 - 18:00\n" +
            "- 주말: 10:00 - 17:00\n\n" +
            "위 정보를 바탕으로 방문객의 질문에 답변해주세요. " +
            "모르는 정보는 솔직히 모른다고 말하고, 안내 데스크로 문의하도록 안내하세요. " +
            "항상 한국어로 친절하게 답변하세요.";
    
    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public interface StreamCallback {
        void onStream(String chunk);
        void onComplete();
        void onError(String error);
    }
    
    public OpenAIService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * API 키 설정
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * API 키가 설정되어 있는지 확인
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    /**
     * 채팅 완료 요청 (스트리밍 방식)
     * @param messages 대화 기록
     * @param callback 스트림 콜백
     */
    public void sendMessageStreaming(List<ChatMessage> messages, StreamCallback callback) {
        if (!hasApiKey()) {
            callback.onError("API 키가 설정되지 않았습니다.");
            return;
        }
        
        new Thread(() -> {
            try {
                JsonObject requestBody = buildRequestBody(messages, true);
                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                
                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();
                
                Response response = client.newCall(request).execute();
                
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.e(TAG, "API 오류 응답: " + errorBody);
                    mainHandler.post(() -> callback.onError("API 오류 (코드: " + response.code() + ")"));
                    return;
                }
                
                // SSE 스트림 읽기
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body().byteStream())
                );
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        
                        // 스트림 종료 신호
                        if (data.equals("[DONE]")) {
                            mainHandler.post(callback::onComplete);
                            break;
                        }
                        
                        try {
                            // 청크 파싱
                            String chunk = parseStreamChunk(data);
                            if (chunk != null && !chunk.isEmpty()) {
                                mainHandler.post(() -> callback.onStream(chunk));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "청크 파싱 오류: " + e.getMessage());
                        }
                    }
                }
                
                reader.close();
                
            } catch (Exception e) {
                Log.e(TAG, "스트리밍 오류", e);
                mainHandler.post(() -> callback.onError("스트리밍 오류: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 채팅 완료 요청 (일반 방식 - 호환성 유지)
     * @param messages 대화 기록
     * @param callback 응답 콜백
     */
    public void sendMessage(List<ChatMessage> messages, ChatCallback callback) {
        if (!hasApiKey()) {
            callback.onError("API 키가 설정되지 않았습니다.");
            return;
        }
        
        try {
            JsonObject requestBody = buildRequestBody(messages, false);
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API 호출 실패", e);
                    mainHandler.post(() -> callback.onError("네트워크 오류: " + e.getMessage()));
                }
                
                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String responseBody = response.body().string();
                        
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "API 오류 응답: " + responseBody);
                            mainHandler.post(() -> callback.onError("API 오류 (코드: " + response.code() + ")"));
                            return;
                        }
                        
                        String botMessage = parseResponse(responseBody);
                        mainHandler.post(() -> callback.onSuccess(botMessage));
                        
                    } catch (Exception e) {
                        Log.e(TAG, "응답 파싱 오류", e);
                        mainHandler.post(() -> callback.onError("응답 처리 오류: " + e.getMessage()));
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "요청 생성 오류", e);
            callback.onError("요청 생성 오류: " + e.getMessage());
        }
    }
    
    /**
     * OpenAI API 요청 본문 생성
     */
    private JsonObject buildRequestBody(List<ChatMessage> messages, boolean stream) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini"); // 비용 효율적인 모델
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 500);
        requestBody.addProperty("stream", stream); // 스트리밍 여부
        
        JsonArray messagesArray = new JsonArray();
        
        // 시스템 프롬프트 추가 (RAG)
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", SYSTEM_PROMPT);
        messagesArray.add(systemMessage);
        
        // 대화 기록 추가
        for (ChatMessage message : messages) {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", message.isUser() ? "user" : "assistant");
            msgObj.addProperty("content", message.getMessage());
            messagesArray.add(msgObj);
        }
        
        requestBody.add("messages", messagesArray);
        return requestBody;
    }
    
    /**
     * 스트림 청크 파싱
     */
    private String parseStreamChunk(String chunkData) {
        try {
            JsonObject json = gson.fromJson(chunkData, JsonObject.class);
            
            if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
                
                if (choice.has("delta")) {
                    JsonObject delta = choice.getAsJsonObject("delta");
                    
                    if (delta.has("content")) {
                        return delta.get("content").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "청크 파싱 실패: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * OpenAI API 응답 파싱
     */
    private String parseResponse(String responseBody) throws Exception {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        
        if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
            JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");
            return message.get("content").getAsString();
        }
        
        throw new Exception("응답에서 메시지를 찾을 수 없습니다.");
    }
    
    /**
     * 시스템 프롬프트 가져오기 (테스트/디버깅용)
     */
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}

