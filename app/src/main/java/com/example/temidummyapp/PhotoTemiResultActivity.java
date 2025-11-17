package com.example.temidummyapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PhotoTemiResultActivity extends AppCompatActivity {

    private static final String TAG = "PhotoTemiResult";
    private static final String UPLOAD_URL = "https://phototemi.kwidea.com/api/upload";

    private Bitmap finalImageBitmap;
    private ImageView resultImage;
    private ImageView qrCodeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phototemi_result);

        resultImage = findViewById(R.id.result_image);
        qrCodeImage = findViewById(R.id.qr_code_image);
        Button homeButton = findViewById(R.id.home_button);
        Button shareButton = findViewById(R.id.share_button);

        ArrayList<String> selectedImageUris = getIntent().getStringArrayListExtra("selected_images");

        if (selectedImageUris != null && selectedImageUris.size() == 2) {
            try {
                Bitmap templateBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo_template_black);
                Bitmap mutableBitmap = templateBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);

                final int frameWidth = 250;
                final int frameHeight = 188;
                final int frame1_left = 24;
                final int frame1_top = 68;
                Rect destRect1 = new Rect(frame1_left, frame1_top, frame1_left + frameWidth, frame1_top + frameHeight);
                final int frame2_left = 24;
                final int frame2_top = destRect1.bottom + 10;
                Rect destRect2 = new Rect(frame2_left, frame2_top, frame2_left + frameWidth, frame2_top + frameHeight);

                Bitmap photo1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(selectedImageUris.get(0)));
                Bitmap photo2 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(selectedImageUris.get(1)));

                drawBitmapToCanvas(canvas, photo1, destRect1);
                drawBitmapToCanvas(canvas, photo2, destRect2);

                finalImageBitmap = mutableBitmap;
                resultImage.setImageBitmap(finalImageBitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoTemiResultActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        shareButton.setOnClickListener(v -> {
            if (finalImageBitmap != null) {
                uploadImageAndGenerateQrCode(finalImageBitmap);
            }
        });
    }

    private void uploadImageAndGenerateQrCode(Bitmap bitmap) {
        // Bitmap을 byte[]로 변환
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        OkHttpClient client = getUnsafeOkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.jpg", RequestBody.create(byteArray, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Upload failed: ", e);
                runOnUiThread(() -> Toast.makeText(PhotoTemiResultActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseBody = response.body().string();
                    Log.d(TAG, "Upload successful: " + responseBody);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String viewUrl = json.getString("viewUrl");

                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        Bitmap qrBitmap = barcodeEncoder.encodeBitmap(viewUrl, BarcodeFormat.QR_CODE, 400, 400);

                        runOnUiThread(() -> {
                            qrCodeImage.setImageBitmap(qrBitmap);
                            resultImage.setVisibility(View.GONE);
                            qrCodeImage.setVisibility(View.VISIBLE);
                        });

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: ", e);
                    } catch (Exception e) {
                        Log.e(TAG, "QR code generation error: ", e);
                    }
                } else {
                    final String responseBody = response.body().string();
                    Log.e(TAG, "Upload failed with code: " + response.code() + " and body: " + responseBody);
                    runOnUiThread(() -> Toast.makeText(PhotoTemiResultActivity.this, "Upload failed: " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void drawBitmapToCanvas(Canvas canvas, Bitmap bitmap, Rect destination) {
        Matrix matrix = new Matrix();
        float scale;
        float dx = 0;
        float dy = 0;

        if (bitmap.getWidth() * destination.height() > destination.width() * bitmap.getHeight()) {
            scale = (float) destination.height() / (float) bitmap.getHeight();
            dx = (destination.width() - bitmap.getWidth() * scale) * 0.5f;
        } else {
            scale = (float) destination.width() / (float) bitmap.getWidth();
            dy = (destination.height() - bitmap.getHeight() * scale) * 0.5f;
        }

        matrix.setScale(scale, scale);
        matrix.postTranslate((int) (dx + 0.5f) + destination.left, (int) (dy + 0.5f) + destination.top);
        canvas.drawBitmap(bitmap, matrix, null);
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
