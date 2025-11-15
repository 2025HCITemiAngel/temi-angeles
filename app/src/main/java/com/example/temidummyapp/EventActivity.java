package com.example.temidummyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.temidummyapp.utils.CSVLoader;

public class EventActivity extends AppCompatActivity {

    private Button btnTarget1, btnTarget2, btnTarget3, btnTarget4, btnTarget5;
    private Button btnTime1, btnTime2, btnTime3, btnTime4, btnTime5;
    private Button btnField1, btnField2, btnField3, btnField4, btnField5, btnField6, btnField7, btnField8, btnField9, btnField10;
    private Button btnField11, btnField12, btnField13, btnField14, btnField15, btnField16, btnField17, btnField18;
    private Button btnSearch;

    private String selectedTarget = "";
    private String selectedTime = "";
    private String selectedField = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booth_search);

        // 참여대상 버튼
        btnTarget1 = findViewById(R.id.btnTarget1);
        btnTarget2 = findViewById(R.id.btnTarget2);
        btnTarget3 = findViewById(R.id.btnTarget3);
        btnTarget4 = findViewById(R.id.btnTarget4);
        btnTarget5 = findViewById(R.id.btnTarget5);

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

        // 참여대상 버튼 클릭 리스너
        View.OnClickListener targetListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTargetButtons();
                Button btn = (Button) v;
                btn.setBackgroundResource(R.drawable.button_selected);
                btn.setBackgroundTintList(null);
                btn.setTextColor(getResources().getColor(android.R.color.white));
                selectedTarget = btn.getText().toString();
            }
        };

        btnTarget1.setOnClickListener(targetListener);
        btnTarget2.setOnClickListener(targetListener);
        btnTarget3.setOnClickListener(targetListener);
        btnTarget4.setOnClickListener(targetListener);
        btnTarget5.setOnClickListener(targetListener);

        // 소요시간 버튼 클릭 리스너
        View.OnClickListener timeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimeButtons();
                Button btn = (Button) v;
                btn.setBackgroundResource(R.drawable.button_selected);
                btn.setBackgroundTintList(null);
                btn.setTextColor(getResources().getColor(android.R.color.white));
                selectedTime = btn.getText().toString();
            }
        };

        btnTime1.setOnClickListener(timeListener);
        btnTime2.setOnClickListener(timeListener);
        btnTime3.setOnClickListener(timeListener);
        btnTime4.setOnClickListener(timeListener);
        btnTime5.setOnClickListener(timeListener);

        // 분야 버튼 클릭 리스너
        View.OnClickListener fieldListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFieldButtons();
                Button btn = (Button) v;
                btn.setBackgroundResource(R.drawable.button_selected);
                btn.setBackgroundTintList(null);
                btn.setTextColor(getResources().getColor(android.R.color.white));
                selectedField = btn.getText().toString();
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
                if (selectedTarget.isEmpty() && selectedTime.isEmpty() && selectedField.isEmpty()) {
                    Toast.makeText(EventActivity.this, "최소 하나의 조건을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(EventActivity.this, BoothResultsActivity.class);
                intent.putExtra("target", selectedTarget);
                intent.putExtra("time", selectedTime);
                intent.putExtra("field", selectedField);
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
        btnTarget5.setBackgroundTintList(null);

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

    private void resetTargetButtons() {
        btnTarget1.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTarget1.setBackgroundTintList(null);
        btnTarget1.setTextColor(0xFF2B87F4);
        btnTarget2.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTarget2.setBackgroundTintList(null);
        btnTarget2.setTextColor(0xFF2B87F4);
        btnTarget3.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTarget3.setBackgroundTintList(null);
        btnTarget3.setTextColor(0xFF2B87F4);
        btnTarget4.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTarget4.setBackgroundTintList(null);
        btnTarget4.setTextColor(0xFF2B87F4);
        btnTarget5.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTarget5.setBackgroundTintList(null);
        btnTarget5.setTextColor(0xFF2B87F4);
    }

    private void resetTimeButtons() {
        btnTime1.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTime1.setBackgroundTintList(null);
        btnTime1.setTextColor(0xFF2B87F4);
        btnTime2.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTime2.setBackgroundTintList(null);
        btnTime2.setTextColor(0xFF2B87F4);
        btnTime3.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTime3.setBackgroundTintList(null);
        btnTime3.setTextColor(0xFF2B87F4);
        btnTime4.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTime4.setBackgroundTintList(null);
        btnTime4.setTextColor(0xFF2B87F4);
        btnTime5.setBackgroundResource(R.drawable.button_unselected_selector);
        btnTime5.setBackgroundTintList(null);
        btnTime5.setTextColor(0xFF2B87F4);
    }

    private void resetFieldButtons() {
        btnField1.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField1.setBackgroundTintList(null);
        btnField1.setTextColor(0xFF2B87F4);
        btnField2.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField2.setBackgroundTintList(null);
        btnField2.setTextColor(0xFF2B87F4);
        btnField3.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField3.setBackgroundTintList(null);
        btnField3.setTextColor(0xFF2B87F4);
        btnField4.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField4.setBackgroundTintList(null);
        btnField4.setTextColor(0xFF2B87F4);
        btnField5.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField5.setBackgroundTintList(null);
        btnField5.setTextColor(0xFF2B87F4);
        btnField6.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField6.setBackgroundTintList(null);
        btnField6.setTextColor(0xFF2B87F4);
        btnField7.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField7.setBackgroundTintList(null);
        btnField7.setTextColor(0xFF2B87F4);
        btnField8.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField8.setBackgroundTintList(null);
        btnField8.setTextColor(0xFF2B87F4);
        btnField9.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField9.setBackgroundTintList(null);
        btnField9.setTextColor(0xFF2B87F4);
        btnField10.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField10.setBackgroundTintList(null);
        btnField10.setTextColor(0xFF2B87F4);
        btnField11.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField11.setBackgroundTintList(null);
        btnField11.setTextColor(0xFF2B87F4);
        btnField12.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField12.setBackgroundTintList(null);
        btnField12.setTextColor(0xFF2B87F4);
        btnField13.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField13.setBackgroundTintList(null);
        btnField13.setTextColor(0xFF2B87F4);
        btnField14.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField14.setBackgroundTintList(null);
        btnField14.setTextColor(0xFF2B87F4);
        btnField15.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField15.setBackgroundTintList(null);
        btnField15.setTextColor(0xFF2B87F4);
        btnField16.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField16.setBackgroundTintList(null);
        btnField16.setTextColor(0xFF2B87F4);
        btnField17.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField17.setBackgroundTintList(null);
        btnField17.setTextColor(0xFF2B87F4);
        btnField18.setBackgroundResource(R.drawable.button_unselected_selector);
        btnField18.setBackgroundTintList(null);
        btnField18.setTextColor(0xFF2B87F4);
    }
}
