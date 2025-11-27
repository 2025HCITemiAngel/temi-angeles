package com.example.temidummyapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventDetailAdapter extends RecyclerView.Adapter<EventDetailAdapter.ViewHolder> {

    private List<EventData> eventList;

    public EventDetailAdapter(List<EventData> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventData event = eventList.get(position);
        
        // 왼쪽 빨간 섹션 - 이미지만 표시
        if (event.getImageResId() != 0 && holder.eventImage != null) {
            holder.eventImage.setImageResource(event.getImageResId());
        }
        
        // 오른쪽 흰 섹션 - 통짜 string 하나만 설정
        if (event.getContent() != null && !event.getContent().isEmpty()) {
            holder.eventContent.setText(event.getContent());
        } else {
            holder.eventContent.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // 왼쪽 빨간 섹션
        ImageView eventImage;
        
        // 오른쪽 흰 섹션
        TextView eventContent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.eventImage);
            eventContent = itemView.findViewById(R.id.eventContent);
        }
    }
}

