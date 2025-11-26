package com.example.temidummyapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener;

import java.util.List;

public class DirectionsActivity extends AppCompatActivity implements OnLocationsUpdatedListener {

    private Robot robot;
    private TextView tvMapLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions);

        robot = Robot.getInstance();

        tvMapLocations = findViewById(R.id.tv_map_locations);
        Button btnGetMap = findViewById(R.id.btn_get_map);

        btnGetMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // robot.getLocations()를 호출하여 저장된 위치 목록을 가져옵니다.
                // 결과는 onLocationsUpdated() 콜백으로 전달됩니다.
                robot.getLocations();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 위치 업데이트를 수신하기 위해 리스너를 추가합니다.
        robot.addOnLocationsUpdatedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 메모리 누수를 방지하기 위해 리스너를 제거합니다.
        // FIXME: SDK 버전에 맞는 정확한 리스너 제거 메소드 확인 필요
        // robot.removeOnLocationsUpdatedListener(this);
    }

    @Override
    public void onLocationsUpdated(List<String> locations) {
        // 이 콜백은 위치 목록이 업데이트될 때 호출됩니다.
        // robot.getLocations()를 호출하거나 위치가 저장/삭제될 때 호출될 수 있습니다.
        if (locations.isEmpty()) {
            tvMapLocations.setText("저장된 위치가 없습니다.");
            return;
        }

        StringBuilder locationsText = new StringBuilder("저장된 위치:\n");
        for (String location : locations) {
            locationsText.append("- ").append(location).append("\n");
        }
        tvMapLocations.setText(locationsText.toString());
    }
}
