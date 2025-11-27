package com.example.temidummyapp;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 채팅 메시지 RecyclerView Adapter
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    
    private List<ChatMessage> messages;
    
    public ChatAdapter() {
        this.messages = new ArrayList<>();
    }
    
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }
    
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == ChatMessage.TYPE_USER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_bot, parent, false);
        }
        return new ChatViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    /**
     * 새 메시지 추가
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    /**
     * 모든 메시지 삭제
     */
    public void clearMessages() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }
    
    /**
     * 메시지 리스트 가져오기
     */
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage;
        
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
        
        public void bind(ChatMessage message) {
            // 마크다운 볼드 처리 적용
            CharSequence formattedText = applyMarkdownFormatting(message.getMessage());
            tvMessage.setText(formattedText);
        }
        
        /**
         * 마크다운 스타일 포맷팅 적용
         * **텍스트** -> 볼드 처리
         * *텍스트* -> 이탤릭 처리
         * ~~텍스트~~ -> 취소선 처리
         */
        private CharSequence applyMarkdownFormatting(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            
            // 마크다운 패턴들 (순서 중요: 볼드 -> 취소선 -> 이탤릭)
            String[] patterns = {
                "\\*\\*(.+?)\\*\\*",  // 볼드: **텍스트**
                "~~(.+?)~~",          // 취소선: ~~텍스트~~
                "(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"  // 이탤릭: *텍스트* (단, **는 제외)
            };
            
            String workingText = text;
            List<FormatSpan> formatSpans = new ArrayList<>();
            
            // 각 패턴별로 처리
            for (int patternType = 0; patternType < patterns.length; patternType++) {
                Pattern pattern = Pattern.compile(patterns[patternType]);
                Matcher matcher = pattern.matcher(workingText);
                
                StringBuilder newText = new StringBuilder();
                int lastEnd = 0;
                List<FormatSpan> currentSpans = new ArrayList<>();
                
                while (matcher.find()) {
                    // 마크 앞의 텍스트 추가
                    newText.append(workingText.substring(lastEnd, matcher.start()));
                    
                    // 스타일 처리할 텍스트의 시작 위치 저장
                    int spanStart = newText.length();
                    
                    // 마크 없이 내용만 추가
                    String content = matcher.group(1);
                    newText.append(content);
                    
                    // 스타일 처리할 텍스트의 끝 위치 저장
                    int spanEnd = newText.length();
                    
                    currentSpans.add(new FormatSpan(spanStart, spanEnd, patternType));
                    
                    lastEnd = matcher.end();
                }
                
                // 매칭이 하나라도 있었다면
                if (!currentSpans.isEmpty()) {
                    // 나머지 텍스트 추가
                    newText.append(workingText.substring(lastEnd));
                    
                    // 다음 반복을 위해 텍스트 업데이트
                    workingText = newText.toString();
                    
                    // 스팬 정보 저장
                    formatSpans.addAll(currentSpans);
                }
            }
            
            // 포맷팅할 것이 없으면 원본 반환
            if (formatSpans.isEmpty()) {
                return text;
            }
            
            // 최종 SpannableString 생성
            SpannableString result = new SpannableString(workingText);
            
            // 스타일 적용
            for (FormatSpan span : formatSpans) {
                switch (span.type) {
                    case 0: // 볼드
                        result.setSpan(
                            new StyleSpan(Typeface.BOLD),
                            span.start,
                            span.end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        break;
                    case 1: // 취소선
                        result.setSpan(
                            new StrikethroughSpan(),
                            span.start,
                            span.end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        break;
                    case 2: // 이탤릭
                        result.setSpan(
                            new StyleSpan(Typeface.ITALIC),
                            span.start,
                            span.end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        break;
                }
            }
            
            return result;
        }
        
        /**
         * 포맷 스팬 위치 및 타입 저장용 헬퍼 클래스
         */
        private static class FormatSpan {
            int start;
            int end;
            int type; // 0: 볼드, 1: 취소선, 2: 이탤릭
            
            FormatSpan(int start, int end, int type) {
                this.start = start;
                this.end = end;
                this.type = type;
            }
        }
    }
}

