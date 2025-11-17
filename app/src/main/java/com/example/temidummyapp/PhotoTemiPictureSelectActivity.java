package com.example.temidummyapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class PhotoTemiPictureSelectActivity extends AppCompatActivity {

    private GridView pictureGrid;
    private ArrayList<String> imageUris;
    private ImageAdapter adapter;
    private Button doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phototemi_picture_select);

        pictureGrid = findViewById(R.id.picture_grid);
        doneButton = findViewById(R.id.done_button);

        imageUris = getIntent().getStringArrayListExtra("captured_images");

        if (imageUris != null) {
            adapter = new ImageAdapter(this, imageUris);
            pictureGrid.setAdapter(adapter);
        }

        doneButton.setEnabled(false);
        doneButton.setText("사진 보러 가기");

        pictureGrid.setOnItemClickListener((parent, view, position, id) -> {
            adapter.toggleSelection(position);
            doneButton.setEnabled(adapter.getSelectedItemCount() == 2);
        });

        doneButton.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoTemiPictureSelectActivity.this, PhotoTemiResultActivity.class);
            intent.putStringArrayListExtra("selected_images", adapter.getSelectedItems());
            startActivity(intent);
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
            if (getSelectedItemCount() >= 2 && !selectedPositions.get(position)) {
                Toast.makeText(context, "2개까지만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedPositions.set(position, !selectedPositions.get(position));
            notifyDataSetChanged();
        }

        public int getSelectedItemCount() {
            int count = 0;
            for (boolean selected : selectedPositions) {
                if (selected) {
                    count++;
                }
            }
            return count;
        }

        public ArrayList<String> getSelectedItems() {
            ArrayList<String> selectedItems = new ArrayList<>();
            for (int i = 0; i < selectedPositions.size(); i++) {
                if (selectedPositions.get(i)) {
                    selectedItems.add(imageUris.get(i));
                }
            }
            return selectedItems;
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
                imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600));
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                imageView = (ImageView) convertView;
            }

            Uri uri = Uri.parse(imageUris.get(position));

            if (selectedPositions.get(position)) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    Drawable imageDrawable = Drawable.createFromStream(inputStream, uri.toString());
                    Drawable borderDrawable = ContextCompat.getDrawable(context, R.drawable.blue_border);

                    if (imageDrawable != null && borderDrawable != null) {
                        Drawable[] layers = {borderDrawable, imageDrawable};
                        LayerDrawable layerDrawable = new LayerDrawable(layers);

                        int borderWidth = (int) (4 * context.getResources().getDisplayMetrics().density); // 4dp border
                        layerDrawable.setLayerInset(1, borderWidth, borderWidth, borderWidth, borderWidth);

                        imageView.setBackground(null);
                        imageView.setPadding(0, 0, 0, 0);
                        imageView.setImageDrawable(layerDrawable);
                    } else {
                        imageView.setImageURI(uri);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    imageView.setImageURI(uri);
                }
            } else {
                imageView.setImageDrawable(null);
                imageView.setImageURI(uri);
                imageView.setBackground(null);
                imageView.setPadding(0, 0, 0, 0);
            }

            return imageView;
        }
    }
}
