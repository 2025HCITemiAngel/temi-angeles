package com.example.temidummyapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.temidummyapp.db.EventSearchHelper;
import com.example.temidummyapp.utils.CSVLoader;  // ✅ 변경됨

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerTarget;
    private CheckBox checkRecruit, checkField;
    private SeekBar seekTime;
    private TextView textTimeValue;
    private Button btnSearch;
    private RecyclerView recyclerEvents;
    private EventAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_program);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerTarget = findViewById(R.id.spinnerTarget);
        checkRecruit = findViewById(R.id.checkRecruit);
        checkField = findViewById(R.id.checkField);
        seekTime = findViewById(R.id.seekTime);
        textTimeValue = findViewById(R.id.textTimeValue);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerEvents = findViewById(R.id.recyclerEvents);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(new ArrayList<HashMap<String, String>>());
        recyclerEvents.setAdapter(adapter);

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

        setUpSpinners();

        seekTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                textTimeValue.setText("선택: " + value + "분");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSearch.setOnClickListener(v -> applyFilters());
    }

    private void setUpSpinners() {
        String[] categories = {"", "에너지신산업", "사물인터넷", "인공지능", "로봇", "3D프린팅"};
        String[] targets = {"", "전국민대상", "중학생 이상", "초등학생", "대학생", "일반인"};

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targets);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTarget.setAdapter(targetAdapter);
    }

    private void applyFilters() {
        String 분야 = spinnerCategory.getSelectedItem().toString();
        String 대상 = spinnerTarget.getSelectedItem().toString();
        int 시간 = seekTime.getProgress();

        String 모집 = null;
        if (checkRecruit.isChecked() && checkField.isChecked()) {
            모집 = null;
        } else if (checkRecruit.isChecked()) {
            모집 = "사전모집";
        } else if (checkField.isChecked()) {
            모집 = "현장접수";
        }

        EventSearchHelper dbHelper = new EventSearchHelper(this);
        List<HashMap<String, String>> results = dbHelper.search(분야, 모집, 대상, 시간);

        if (results.isEmpty()) {
            Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
        }

        adapter.updateData(results);
    }
}
