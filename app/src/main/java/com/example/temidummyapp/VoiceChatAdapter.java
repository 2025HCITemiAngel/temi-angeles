package com.example.temidummyapp;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 음성 대화 프롬프터 어댑터
 * 현재 활성 메시지를 강조 표시
 */
public class VoiceChatAdapter extends RecyclerView.Adapter<VoiceChatAdapter.ViewHolder> {
    
    private List<VoiceChatMessage> messages;
    
    public VoiceChatAdapter() {
        this.messages = new ArrayList<>();
    }
    
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == VoiceChatMessage.TYPE_USER 
            ? R.layout.item_voice_chat_user 
            : R.layout.item_voice_chat_ai;
        
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoiceChatMessage message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    /**
     * 새 메시지 추가
     */
    public void addMessage(VoiceChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    /**
     * 마지막 메시지 업데이트 (실시간 스트리밍)
     */
    public void updateLastMessage(String text) {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            messages.get(lastIndex).setMessage(text);
            notifyItemChanged(lastIndex);
        }
    }
    
    /**
     * 특정 메시지를 활성 상태로 설정
     */
    public void setActiveMessage(int position) {
        // 모든 메시지 비활성화
        for (int i = 0; i < messages.size(); i++) {
            VoiceChatMessage msg = messages.get(i);
            if (msg.isActive()) {
                msg.setActive(false);
                notifyItemChanged(i);
            }
        }
        
        // 해당 메시지만 활성화
        if (position >= 0 && position < messages.size()) {
            messages.get(position).setActive(true);
            notifyItemChanged(position);
        }
    }
    
    /**
     * 모든 메시지 비활성화
     */
    public void clearActiveMessage() {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).isActive()) {
                messages.get(i).setActive(false);
                notifyItemChanged(i);
            }
        }
    }
    
    /**
     * 메시지 개수 반환
     */
    public int getMessageCount() {
        return messages.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
        
        public void bind(VoiceChatMessage message) {
            tvMessage.setText(message.getMessage());
            
            // 활성 메시지 강조 (볼드 + 흰색)
            if (message.isActive()) {
                tvMessage.setTypeface(null, Typeface.BOLD);
                tvMessage.setTextColor(0xFFFFFFFF); // 흰색
                tvMessage.setAlpha(1.0f);
                tvMessage.setTextSize(20f); // 약간 크게
            } else {
                // 지나간 메시지 (회색 처리)
                tvMessage.setTypeface(null, Typeface.NORMAL);
                tvMessage.setTextColor(0xFFB0B0B0); // 연한 회색
                tvMessage.setAlpha(0.7f); // 살짝 투명하게
                tvMessage.setTextSize(18f);
            }
        }
    }
}

