package com.example.temidummyapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private List<HashMap<String, String>> eventList;
    private Context context;

    public EventAdapter(List<HashMap<String, String>> eventList) {
        this.eventList = eventList;
    }

    public void updateData(List<HashMap<String, String>> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventAdapter.ViewHolder holder, int position) {
        HashMap<String, String> item = eventList.get(position);

        // 🔹 getOrDefault 사용 대신 null 체크로 대체
        String title = item.containsKey("대제목") ? item.get("대제목") : "";
        String category = item.containsKey("분야") ? item.get("분야") : "";
        String recruit = item.containsKey("사전모집여부") ? item.get("사전모집여부") : "";
        String time = item.containsKey("소요시간") ? item.get("소요시간") : "";
        final String url = item.containsKey("url") ? item.get("url") : "";

        holder.textTitle.setText(title);
        holder.textCategory.setText(category);
        holder.textRecruit.setText(recruit);
        if (time != null && time.length() > 0) {
            holder.textTime.setText(time + "분");
        } else {
            holder.textTime.setText("");
        }

        // 🔹 클릭 시 URL 열기
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (url != null && url.length() > 0) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return (eventList != null) ? eventList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textCategory, textTime, textRecruit;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textCategory = itemView.findViewById(R.id.textCategory);
            textTime = itemView.findViewById(R.id.textTime);
            textRecruit = itemView.findViewById(R.id.textRecruit);
        }
    }
}
