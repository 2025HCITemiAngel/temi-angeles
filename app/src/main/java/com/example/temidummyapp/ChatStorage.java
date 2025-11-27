package com.example.temidummyapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 채팅 기록 저장 및 불러오기 클래스
 */
public class ChatStorage {
    private static final String PREFS_NAME = "chat_storage";
    private static final String KEY_MESSAGES = "messages";
    
    private final SharedPreferences prefs;
    private final Gson gson;
    
    public ChatStorage(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * 채팅 기록 저장
     */
    public void saveMessages(List<ChatMessage> messages) {
        String json = gson.toJson(messages);
        prefs.edit().putString(KEY_MESSAGES, json).apply();
    }
    
    /**
     * 채팅 기록 불러오기
     */
    public List<ChatMessage> loadMessages() {
        String json = prefs.getString(KEY_MESSAGES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type listType = new TypeToken<ArrayList<ChatMessage>>(){}.getType();
        List<ChatMessage> messages = gson.fromJson(json, listType);
        return messages != null ? messages : new ArrayList<>();
    }
    
    /**
     * 채팅 기록 삭제
     */
    public void clearMessages() {
        prefs.edit().remove(KEY_MESSAGES).apply();
    }
    
    /**
     * 저장된 채팅이 있는지 확인
     */
    public boolean hasMessages() {
        return prefs.contains(KEY_MESSAGES);
    }
}


