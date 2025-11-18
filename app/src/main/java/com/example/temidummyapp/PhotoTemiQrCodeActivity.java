package com.example.temidummyapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class PhotoTemiQrCodeActivity extends AppCompatActivity {

    private static final String TAG = "PhotoTemiQrCode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phototemi_qrcode);

        ImageView qrCodeImage = findViewById(R.id.qr_code_image);
        Button homeButton = findViewById(R.id.home_button);

        String viewUrl = getIntent().getStringExtra("viewUrl");

        if (viewUrl != null && !viewUrl.isEmpty()) {
            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap qrBitmap = barcodeEncoder.encodeBitmap(viewUrl, BarcodeFormat.QR_CODE, 400, 400);
                qrCodeImage.setImageBitmap(qrBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoTemiQrCodeActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
