package com.example.temidummyapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;

public class BoothCardAdapter extends RecyclerView.Adapter<BoothCardAdapter.ViewHolder> {

    private List<HashMap<String, String>> boothList;
    private Context context;

    public BoothCardAdapter(List<HashMap<String, String>> boothList) {
        this.boothList = boothList;
    }

    public void updateData(List<HashMap<String, String>> newList) {
        this.boothList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_booth_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (boothList == null || position < 0 || position >= boothList.size()) {
                return;
            }
            
            HashMap<String, String> item = boothList.get(position);
            if (item == null) {
                return;
            }

        // 데이터 추출
        String title = item.containsKey("대제목") ? item.get("대제목") : "";
        String description = item.containsKey("한줄소개") ? item.get("한줄소개") : "";
        String target = item.containsKey("참여대상") ? item.get("참여대상") : "";
        String recruit = item.containsKey("사전모집여부") ? item.get("사전모집여부") : "";
        String timeOriginal = item.containsKey("소요시간_원본") ? item.get("소요시간_원본") : "";
        String time = item.containsKey("소요시간") ? item.get("소요시간") : "";
        String period = item.containsKey("체험기간") ? item.get("체험기간") : "";
        String experienceTime = item.containsKey("체험시간") ? item.get("체험시간") : "";
        final String url = item.containsKey("url") ? item.get("url") : "";

        // 데이터 검증: 참여대상 필드에 "현장접수"나 "사전모집" 같은 값이 들어있으면 제거
        // (이것은 사전모집여부 필드에 들어가야 할 값)
        if (target != null && (target.contains("현장접수") || target.contains("사전모집"))) {
            target = "";
        }
        
        // 참여대상이 비어있고 사전모집여부가 있으면, 사전모집여부 값을 참여대상으로 사용하지 않음
        // (데이터가 잘못 매핑된 경우를 방지)

        // 날짜/시간 포맷팅 (두 줄로 표시)
        String dateTime = "";
        if (period != null && period.length() > 0) {
            dateTime = period;
            if (experienceTime != null && experienceTime.length() > 0) {
                dateTime += "\n" + experienceTime;
            }
        } else if (experienceTime != null && experienceTime.length() > 0) {
            dateTime = experienceTime;
        }

        // UI 설정
        holder.textTitle.setText(title != null ? title : "");
        holder.textDescription.setText(description != null ? description : "");
        
        // 참여대상 표시 (비어있지 않을 때만)
        if (target != null && target.length() > 0 && !target.trim().isEmpty()) {
            holder.textTarget.setText("참여대상 : " + target);
            holder.textTarget.setVisibility(View.VISIBLE);
        } else {
            holder.textTarget.setVisibility(View.GONE);
        }
        
        // 소요시간 표시: 원본 문자열이 있으면 그대로 사용, 없으면 숫자에 "분" 붙이기
        String timeDisplay = "";
        if (timeOriginal != null && timeOriginal.length() > 0 && !timeOriginal.trim().isEmpty()) {
            // 원본 문자열 사용 (예: "10~20분", "5분")
            timeDisplay = timeOriginal;
            // "분"이 없으면 추가
            if (!timeDisplay.contains("분")) {
                timeDisplay += "분";
            }
        } else if (time != null && time.length() > 0 && !time.trim().isEmpty()) {
            // 원본이 없으면 숫자에 "분" 붙이기
            timeDisplay = time + "분";
        }
        
        if (timeDisplay.length() > 0) {
            holder.textTime.setText("소요시간: " + timeDisplay);
        } else {
            holder.textTime.setText("소요시간: -");
        }

        if (dateTime.length() > 0) {
            holder.textDateTime.setText(dateTime);
            holder.textDateTime.setVisibility(View.VISIBLE);
        } else {
            holder.textDateTime.setVisibility(View.GONE);
        }

        // 이미지는 기본적으로 표시 (나중에 이미지 로딩 추가 가능)
        holder.imageBooth.setImageResource(R.drawable.ic_temibot);

        // 클릭 시 URL 열기
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (url != null && url.length() > 0) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                }
            }
        });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return (boothList != null) ? boothList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageBooth;
        TextView textDateTime;
        TextView textTitle;
        TextView textDescription;
        TextView textTarget;
        TextView textTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageBooth = itemView.findViewById(R.id.imageBooth);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
            textTarget = itemView.findViewById(R.id.textTarget);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}

