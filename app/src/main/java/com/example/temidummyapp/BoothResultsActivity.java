package com.example.temidummyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.temidummyapp.db.EventSearchHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BoothResultsActivity extends AppCompatActivity {

    private RecyclerView recyclerBooths;
    private BoothCardAdapter adapter;
    private LinearLayout filterContainer;
    private Button btnBack;

    private String selectedTarget;
    private String selectedTime;
    private String selectedField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booth_results);

        // Intent에서 선택한 조건 받기
        Intent intent = getIntent();
        selectedTarget = intent.getStringExtra("target");
        selectedTime = intent.getStringExtra("time");
        selectedField = intent.getStringExtra("field");

        recyclerBooths = findViewById(R.id.recyclerBooths);
        filterContainer = findViewById(R.id.filterContainer);
        btnBack = findViewById(R.id.btnBack);

        // RecyclerView 설정 (가로 스크롤)
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerBooths.setLayoutManager(layoutManager);
        adapter = new BoothCardAdapter(new ArrayList<HashMap<String, String>>());
        recyclerBooths.setAdapter(adapter);

        // 필터 표시
        displayFilters();

        // 검색 실행
        searchBooths();

        // 뒤로 가기 버튼
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void displayFilters() {
        filterContainer.removeAllViews();

        if (selectedTarget != null && selectedTarget.length() > 0) {
            addFilterChip(selectedTarget);
        }
        if (selectedTime != null && selectedTime.length() > 0) {
            addFilterChip(selectedTime);
        }
        if (selectedField != null && selectedField.length() > 0) {
            addFilterChip(selectedField);
        }
    }

    private void addFilterChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setPadding(16, 8, 16, 8);
        chip.setTextColor(0xFF243B5A);
        chip.setTextSize(14);
        chip.setBackgroundResource(R.drawable.filter_chip);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 8, 0);
        chip.setLayoutParams(params);
        filterContainer.addView(chip);
    }

    private void searchBooths() {
        EventSearchHelper dbHelper = new EventSearchHelper(this);

        // 분야
        String 분야 = selectedField != null ? selectedField : "";

        // 참여대상 매핑
        String 대상 = "";
        if (selectedTarget != null) {
            if (selectedTarget.equals("전국민 대상")) {
                대상 = "전국민대상";
            } else if (selectedTarget.equals("초등학생 이상")) {
                대상 = "초등학생";
            } else if (selectedTarget.equals("중학생 이상")) {
                대상 = "중학생 이상";
            } else if (selectedTarget.equals("초·중·고 학생 및 일반 관람객")) {
                대상 = "초·중·고 학생 및 일반 관람객";
            } else if (selectedTarget.equals("누구나(제한없음)")) {
                대상 = "";
            }
        }

        // 소요시간 매핑
        int 최대시간 = 0;
        if (selectedTime != null) {
            if (selectedTime.contains("5분")) {
                최대시간 = 5;
            } else if (selectedTime.contains("10분")) {
                최대시간 = 10;
            } else if (selectedTime.contains("30분")) {
                최대시간 = 30;
            } else if (selectedTime.contains("60분")) {
                최대시간 = 60;
            } else if (selectedTime.contains("90분")) {
                최대시간 = 90;
            }
        }

        List<HashMap<String, String>> results = dbHelper.search(분야, null, 대상, 최대시간);

        if (results.isEmpty()) {
            Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
        }

        adapter.updateData(results);
    }
}

