package com.example.temidummyapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;

public class PhotoTemiPictureSelectActivity extends AppCompatActivity {

    private GridView pictureGrid;
    private ArrayList<String> imageUris;
    private ImageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phototemi_picture_select);

        pictureGrid = findViewById(R.id.picture_grid);
        Button retakeButton = findViewById(R.id.retake_button);
        Button doneButton = findViewById(R.id.done_button);

        imageUris = getIntent().getStringArrayListExtra("captured_images");

        if (imageUris != null) {
            adapter = new ImageAdapter(this, imageUris);
            pictureGrid.setAdapter(adapter);
        }

        pictureGrid.setOnItemClickListener((parent, view, position, id) -> {
            adapter.toggleSelection(position);
        });

        retakeButton.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoTemiPictureSelectActivity.this, PhotoTemi.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        doneButton.setOnClickListener(v -> {
            // 완료 버튼 클릭 시 로직 (예: 메인 화면으로 이동)
            Intent intent = new Intent(PhotoTemiPictureSelectActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    public class ImageAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<String> imageUris;
        private final ArrayList<Boolean> selectedPositions;

        public ImageAdapter(Context context, ArrayList<String> imageUris) {
            this.context = context;
            this.imageUris = imageUris;
            this.selectedPositions = new ArrayList<>(Collections.nCopies(imageUris.size(), false));
        }

        public void toggleSelection(int position) {
            selectedPositions.set(position, !selectedPositions.get(position));
            notifyDataSetChanged();
        }

        public int getCount() {
            return imageUris.size();
        }



        public Object getItem(int position) {
            return imageUris.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 900));
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageURI(Uri.parse(imageUris.get(position)));

            if (selectedPositions.get(position)) {
                imageView.setBackground(ContextCompat.getDrawable(context, R.drawable.blue_border));
            } else {
                imageView.setBackground(null);
            }

            return imageView;
        }
    }
}
