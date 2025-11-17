package com.example.temidummyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.temidummyapp.utils.CSVLoader;

import java.util.ArrayList;
import java.util.List;

public class EventActivity extends AppCompatActivity {

    private Button btnTarget1, btnTarget2, btnTarget3, btnTarget4;
    private Button btnTime1, btnTime2, btnTime3, btnTime4, btnTime5;
    private Button btnField1, btnField2, btnField3, btnField4, btnField5, btnField6, btnField7, btnField8, btnField9, btnField10;
    private Button btnField11, btnField12, btnField13, btnField14, btnField15, btnField16, btnField17, btnField18;
    private Button btnSearch;

    private List<String> selectedTargets = new ArrayList<>();
    private List<String> selectedTimes = new ArrayList<>();
    private List<String> selectedFields = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booth_search);

        // 참여대상 버튼
        btnTarget1 = findViewById(R.id.btnTarget1);
        btnTarget2 = findViewById(R.id.btnTarget2);
        btnTarget3 = findViewById(R.id.btnTarget3);
        btnTarget4 = findViewById(R.id.btnTarget4);

        // 소요시간 버튼
        btnTime1 = findViewById(R.id.btnTime1);
        btnTime2 = findViewById(R.id.btnTime2);
        btnTime3 = findViewById(R.id.btnTime3);
        btnTime4 = findViewById(R.id.btnTime4);
        btnTime5 = findViewById(R.id.btnTime5);

        // 분야 버튼
        btnField1 = findViewById(R.id.btnField1);
        btnField2 = findViewById(R.id.btnField2);
        btnField3 = findViewById(R.id.btnField3);
        btnField4 = findViewById(R.id.btnField4);
        btnField5 = findViewById(R.id.btnField5);
        btnField6 = findViewById(R.id.btnField6);
        btnField7 = findViewById(R.id.btnField7);
        btnField8 = findViewById(R.id.btnField8);
        btnField9 = findViewById(R.id.btnField9);
        btnField10 = findViewById(R.id.btnField10);
        btnField11 = findViewById(R.id.btnField11);
        btnField12 = findViewById(R.id.btnField12);
        btnField13 = findViewById(R.id.btnField13);
        btnField14 = findViewById(R.id.btnField14);
        btnField15 = findViewById(R.id.btnField15);
        btnField16 = findViewById(R.id.btnField16);
        btnField17 = findViewById(R.id.btnField17);
        btnField18 = findViewById(R.id.btnField18);

        btnSearch = findViewById(R.id.btnSearch);

        // 참여대상 버튼 클릭 리스너 (중복 클릭 가능)
        View.OnClickListener targetListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                String text = btn.getText().toString();
                
                // 토글 방식: 이미 선택되어 있으면 해제, 아니면 선택
                if (selectedTargets.contains(text)) {
                    // 해제
                    selectedTargets.remove(text);
                    btn.setBackgroundResource(R.drawable.button_unselected_selector);
                    btn.setBackgroundTintList(null);
                    btn.setTextColor(0xFF2B87F4);
                } else {
                    // 선택
                    selectedTargets.add(text);
                    btn.setBackgroundResource(R.drawable.button_selected);
                    btn.setBackgroundTintList(null);
                    btn.setTextColor(0xFFFFFFFF); // 하얀색
                }
            }
        };

        btnTarget1.setOnClickListener(targetListener);
        btnTarget2.setOnClickListener(targetListener);
        btnTarget3.setOnClickListener(targetListener);
        btnTarget4.setOnClickListener(targetListener);

        // 소요시간 버튼 클릭 리스너 (중복 클릭 가능)
        View.OnClickListener timeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                String text = btn.getText().toString();
                
                // 토글 방식: 이미 선택되어 있으면 해제, 아니면 선택
                if (selectedTimes.contains(text)) {
                    // 해제
                    selectedTimes.remove(text);
                    btn.setBackgroundResource(R.drawable.button_unselected_selector);
                    btn.setBackgroundTintList(null);
                    btn.setTextColor(0xFF2B87F4);
                } else {
                    // 선택
                    selectedTimes.add(text);
                    btn.setBackgroundResource(R.drawable.button_selected);
                    btn.setBackgroundTintList(null);
                    btn.setTextColor(0xFFFFFFFF); // 하얀색
                }
            }
        };

        btnTime1.setOnClickListener(timeListener);
        btnTime2.setOnClickListener(timeListener);
        btnTime3.setOnClickListener(timeListener);
        btnTime4.setOnClickListener(timeListener);
        btnTime5.setOnClickListener(timeListener);

        // 분야 버튼 클릭 리스너 (중복 클릭 가능)
        View.OnClickListener fieldListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                String text = btn.getText().toString();
                
                // 토글 방식: 이미 선택되어 있으면 해제, 아니면 선택
                if (selectedFields.contains(text)) {
                    // 해제
                    selectedFields.remove(text);
                    btn.setBackgroundResource(R.drawable.button_unselected_selector);
                    btn.setBackgroundTintList(null);
                    btn.setTextColor(0xFF2B87F4);
                } else {
                    // 선택
                    selectedFields.add(text);
                    btn.setBackgroundResource(R.drawable.button_selected);
                    btn.setBackgroundTintList(null);
                    btn.setTextColor(0xFFFFFFFF); // 하얀색
                }
            }
        };

        btnField1.setOnClickListener(fieldListener);
        btnField2.setOnClickListener(fieldListener);
        btnField3.setOnClickListener(fieldListener);
        btnField4.setOnClickListener(fieldListener);
        btnField5.setOnClickListener(fieldListener);
        btnField6.setOnClickListener(fieldListener);
        btnField7.setOnClickListener(fieldListener);
        btnField8.setOnClickListener(fieldListener);
        btnField9.setOnClickListener(fieldListener);
        btnField10.setOnClickListener(fieldListener);
        btnField11.setOnClickListener(fieldListener);
        btnField12.setOnClickListener(fieldListener);
        btnField13.setOnClickListener(fieldListener);
        btnField14.setOnClickListener(fieldListener);
        btnField15.setOnClickListener(fieldListener);
        btnField16.setOnClickListener(fieldListener);
        btnField17.setOnClickListener(fieldListener);
        btnField18.setOnClickListener(fieldListener);

        // 검색 버튼
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedTargets.isEmpty() && selectedTimes.isEmpty() && selectedFields.isEmpty()) {
                    Toast.makeText(EventActivity.this, "최소 하나의 조건을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(EventActivity.this, BoothResultsActivity.class);
                // 리스트를 쉼표로 구분된 문자열로 변환 (하위 호환성)
                intent.putExtra("targets", joinList(selectedTargets));
                intent.putExtra("times", joinList(selectedTimes));
                intent.putExtra("fields", joinList(selectedFields));
                startActivity(intent);
            }
        });

        // 초기 버튼 tint 제거
        initializeButtons();

        // ✅ CSV → SQLite 로드 (한 번만 실행)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CSVLoader.loadCSVToDB(EventActivity.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initializeButtons() {
        // 참여대상 버튼 초기화
        btnTarget1.setBackgroundTintList(null);
        btnTarget2.setBackgroundTintList(null);
        btnTarget3.setBackgroundTintList(null);
        btnTarget4.setBackgroundTintList(null);

        // 소요시간 버튼 초기화
        btnTime1.setBackgroundTintList(null);
        btnTime2.setBackgroundTintList(null);
        btnTime3.setBackgroundTintList(null);
        btnTime4.setBackgroundTintList(null);
        btnTime5.setBackgroundTintList(null);

        // 분야 버튼 초기화
        btnField1.setBackgroundTintList(null);
        btnField2.setBackgroundTintList(null);
        btnField3.setBackgroundTintList(null);
        btnField4.setBackgroundTintList(null);
        btnField5.setBackgroundTintList(null);
        btnField6.setBackgroundTintList(null);
        btnField7.setBackgroundTintList(null);
        btnField8.setBackgroundTintList(null);
        btnField9.setBackgroundTintList(null);
        btnField10.setBackgroundTintList(null);
        btnField11.setBackgroundTintList(null);
        btnField12.setBackgroundTintList(null);
        btnField13.setBackgroundTintList(null);
        btnField14.setBackgroundTintList(null);
        btnField15.setBackgroundTintList(null);
        btnField16.setBackgroundTintList(null);
        btnField17.setBackgroundTintList(null);
        btnField18.setBackgroundTintList(null);
    }

    // 리스트를 쉼표로 구분된 문자열로 변환 (하위 호환성)
    private String joinList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

}
