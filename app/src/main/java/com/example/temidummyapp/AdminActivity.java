package com.example.temidummyapp;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.robotemi.sdk.Robot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends BaseActivity {

    private Robot robot;
    private Map<String, String> currentMappings = new HashMap<>();
    private boolean mapBitmapLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        robot = Robot.getInstance();

        // 저장된 매핑 불러오기
        currentMappings.putAll(AdminMappingStore.load(this));

        // 뒤로가기 버튼
        View backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // 저장 버튼
        Button saveBtn = findViewById(R.id.admin_save);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveAllMappings();
                }
            });
        }

        // 지도 이미지 로드
        ensureMapBitmapLoaded();

        // 모든 버튼에 클릭 리스너 설정
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // btn_01 ~ btn_21까지 반복
        for (int i = 1; i <= 21; i++) {
            String buttonId = String.format(java.util.Locale.US, "btn_%02d", i);
            int resId = getResources().getIdentifier(buttonId, "id", getPackageName());
            
            if (resId != 0) {
                Button button = findViewById(resId);
                if (button != null) {
                    final String finalButtonId = buttonId;
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showLocationPickerDialog(finalButtonId, (Button) v);
                        }
                    });
                }
            }
        }
    }

    private void showLocationPickerDialog(final String buttonId, final Button button) {
        // Temi 위치 목록 가져오기
        final List<String> locations = getTemiLocationsSafe();
        
        if (locations.isEmpty()) {
            Toast.makeText(this, "테미에 등록된 위치가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 다이얼로그 생성
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_location_picker);
        dialog.setTitle("위치 선택");

        TextView dialogTitle = dialog.findViewById(R.id.dialog_title);
        if (dialogTitle != null) {
            dialogTitle.setText(buttonId + " 위치 매핑");
        }

        ListView listView = dialog.findViewById(R.id.location_list);
        if (listView != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, 
                android.R.layout.simple_list_item_1, 
                locations
            );
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selectedLocation = locations.get(position);
                    
                    // 다이얼로그 즉시 닫기
                    dialog.dismiss();
                    
                    // 매핑 저장 (메모리에만, 실제 저장은 저장 버튼 클릭 시)
                    currentMappings.put(buttonId, selectedLocation);
                    
                    // 버튼 깜빡임 애니메이션
                    animateButtonBlink(button);
                    
                    // 토스트 메시지
                    Toast.makeText(AdminActivity.this, 
                        selectedLocation + " 위치 저장 성공!", 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 취소 버튼
        Button cancelBtn = dialog.findViewById(R.id.btn_cancel);
        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private void animateButtonBlink(Button button) {
        // 깜빡임 애니메이션 (3번 반복)
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(200); // 200ms
        animation.setStartOffset(0);
        animation.setRepeatMode(AlphaAnimation.REVERSE);
        animation.setRepeatCount(5); // 총 3번 깜빡임
        button.startAnimation(animation);
    }

    private List<String> getTemiLocationsSafe() {
        try {
            List<String> list = robot != null ? robot.getLocations() : null;
            return list != null ? list : new ArrayList<String>();
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    private void saveAllMappings() {
        AdminMappingStore.save(this, currentMappings);
        Toast.makeText(this, "모든 매핑이 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    // 큰 맵 이미지를 화면 크기에 맞게 다운샘플링해서 로드
    private void ensureMapBitmapLoaded() {
        if (mapBitmapLoaded) return;
        
        final ImageView mapImage = findViewById(R.id.map_image);
        if (mapImage == null) return;
        
        mapImage.post(new Runnable() {
            @Override
            public void run() {
                if (mapBitmapLoaded) return;
                int targetW = mapImage.getWidth();
                int targetH = mapImage.getHeight();
                if (targetW <= 0 || targetH <= 0) return;
                
                Bitmap bitmap = decodeSampledBitmapFromResource(
                    getResources(), R.drawable.map, targetW, targetH);
                if (bitmap != null) {
                    mapImage.setImageBitmap(bitmap);
                    mapBitmapLoaded = true;
                }
            }
        });
    }

    private static Bitmap decodeSampledBitmapFromResource(
            android.content.res.Resources res, int resId, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inPreferredConfig = Bitmap.Config.RGB_565; // 메모리 절약
        options.inJustDecodeBounds = false;
        try {
            return BitmapFactory.decodeResource(res, resId, options);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && 
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
