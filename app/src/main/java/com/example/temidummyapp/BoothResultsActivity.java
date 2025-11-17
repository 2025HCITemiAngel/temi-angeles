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

    private List<String> selectedTargets;
    private List<String> selectedTimes;
    private List<String> selectedFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booth_results);

        // Intent에서 선택한 조건 받기 (쉼표로 구분된 문자열)
        Intent intent = getIntent();
        String targetsStr = intent.getStringExtra("targets");
        String timesStr = intent.getStringExtra("times");
        String fieldsStr = intent.getStringExtra("fields");
        
        // 쉼표로 구분된 문자열을 리스트로 변환
        selectedTargets = new ArrayList<>();
        if (targetsStr != null && !targetsStr.isEmpty()) {
            String[] targets = targetsStr.split(",");
            for (String target : targets) {
                if (target != null && !target.trim().isEmpty()) {
                    selectedTargets.add(target.trim());
                }
            }
        }
        
        selectedTimes = new ArrayList<>();
        if (timesStr != null && !timesStr.isEmpty()) {
            String[] times = timesStr.split(",");
            for (String time : times) {
                if (time != null && !time.trim().isEmpty()) {
                    selectedTimes.add(time.trim());
                }
            }
        }
        
        selectedFields = new ArrayList<>();
        if (fieldsStr != null && !fieldsStr.isEmpty()) {
            String[] fields = fieldsStr.split(",");
            for (String field : fields) {
                if (field != null && !field.trim().isEmpty()) {
                    selectedFields.add(field.trim());
                }
            }
        }

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

        // 검색 실행 (예외 처리)
        try {
            searchBooths();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "검색 중 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

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

        if (selectedTargets != null) {
            for (String target : selectedTargets) {
                addFilterChip(target);
            }
        }
        if (selectedTimes != null) {
            for (String time : selectedTimes) {
                addFilterChip(time);
            }
        }
        if (selectedFields != null) {
            for (String field : selectedFields) {
                addFilterChip(field);
            }
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
        try {
            EventSearchHelper dbHelper = new EventSearchHelper(this);

            // 분야 리스트 (null 체크)
            List<String> 분야목록 = (selectedFields != null) ? selectedFields : new ArrayList<String>();

            // 참여대상 리스트 (null 체크)
            List<String> 대상목록 = (selectedTargets != null) ? selectedTargets : new ArrayList<String>();

            // 소요시간 매핑: 텍스트를 숫자로 변환
            List<Integer> 최대시간목록 = new ArrayList<Integer>();
            if (selectedTimes != null) {
                for (String time : selectedTimes) {
                    if (time != null) {
                        int 최대시간 = 0;
                        if (time.contains("5분")) {
                            최대시간 = 5;
                        } else if (time.contains("10분")) {
                            최대시간 = 10;
                        } else if (time.contains("30분")) {
                            최대시간 = 30;
                        } else if (time.contains("60분")) {
                            최대시간 = 60;
                        } else if (time.contains("90분")) {
                            최대시간 = 90;
                        }
                        if (최대시간 > 0) {
                            최대시간목록.add(최대시간);
                        }
                    }
                }
            }

            List<HashMap<String, String>> results = dbHelper.search(분야목록, null, 대상목록, 최대시간목록);

            if (results == null) {
                results = new ArrayList<HashMap<String, String>>();
            }

            if (results.isEmpty()) {
                Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
            }

            adapter.updateData(results);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}

