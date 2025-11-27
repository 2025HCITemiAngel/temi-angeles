package com.example.temidummyapp;

/**
 * 음성 대화 메시지 모델
 */
public class VoiceChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    
    private String message;
    private int type;
    private boolean isActive; // 현재 말하고 있는 메시지인지 여부
    
    public VoiceChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
        this.isActive = false;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getType() {
        return type;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public boolean isUser() {
        return type == TYPE_USER;
    }
    
    public boolean isAI() {
        return type == TYPE_AI;
    }
}

