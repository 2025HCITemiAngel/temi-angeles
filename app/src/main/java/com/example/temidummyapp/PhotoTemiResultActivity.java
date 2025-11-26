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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;

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

    private static final TemplateFrame[] DEFAULT_TEMPLATE_FRAMES = {
            new TemplateFrame(0.09f, 0.1150f, 0.82f, 0.3256f),
            new TemplateFrame(0.09f, 0.4665f, 0.82f, 0.3256f)
    };

    private static final TemplateFrame[] BLACK_TEMPLATE_FRAMES = {
            new TemplateFrame(0.0872f, 0.0432f, 0.8256f, 0.3276f),
            new TemplateFrame(0.0872f, 0.3971f, 0.8256f, 0.3276f)
    };

    private static final TemplateFrame[] BLUE_TEMPLATE_FRAMES = {
            new TemplateFrame(0.0872f, 0.0432f, 0.8256f, 0.3276f),
            new TemplateFrame(0.0872f, 0.3971f, 0.8256f, 0.3276f)
    };

    private static final TemplateFrame[] DEVELOPER_TEMPLATE_FRAMES = {
            new TemplateFrame(0.0933f, 0.1062f, 0.82f, 0.3238f),
            new TemplateFrame(0.0933f, 0.4947f, 0.82f, 0.3238f)
    };

    private static final TemplateFrame[] BIT8_TEMPLATE_FRAMES = {
            new TemplateFrame(0.0872f, 0.0432f, 0.8256f, 0.3276f),
            new TemplateFrame(0.0872f, 0.3971f, 0.8256f, 0.3276f)
    };

    private static final TemplateFrame[] getTemplateFrames(String templateName) {
        switch (templateName) {
            case "Black Template":
                return BLACK_TEMPLATE_FRAMES;
            case "Blue Template":
                return BLUE_TEMPLATE_FRAMES;
            case "Developer Template":
                return DEVELOPER_TEMPLATE_FRAMES;
            case "8bit Template":
                return BIT8_TEMPLATE_FRAMES;
            default:
                return DEFAULT_TEMPLATE_FRAMES;
        }
    }

    private Bitmap finalImageBitmap;
    private ImageView resultImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phototemi_result);

        resultImage = findViewById(R.id.result_image);
        Button shareButton = findViewById(R.id.share_button);

        ArrayList<String> selectedImageUris = getIntent().getStringArrayListExtra("selected_images");
        String templateName = getIntent().getStringExtra("template");

        if (selectedImageUris != null && selectedImageUris.size() == 2 && templateName != null) {
            try {
                String resourceName = "photo_template_" + templateName.replace(" Template", "").toLowerCase().replace(" ", "_");
                int templateResourceId = getResources().getIdentifier(resourceName, "drawable", getPackageName());

                if (templateResourceId == 0) {
                    Toast.makeText(this, "Template not found: " + templateName, Toast.LENGTH_LONG).show();
                    return;
                }

                Bitmap templateBitmap = BitmapFactory.decodeResource(getResources(), templateResourceId);
                Bitmap resultBitmap = Bitmap.createBitmap(templateBitmap.getWidth(), templateBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(resultBitmap);

                Bitmap photo1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(selectedImageUris.get(0)));
                Bitmap photo2 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(selectedImageUris.get(1)));

                TemplateFrame[] templateFrames = getTemplateFrames(templateName);
                Rect[] targetRects = buildFrameRects(templateBitmap, templateFrames);
                ArrayList<Bitmap> photos = new ArrayList<>(Arrays.asList(photo1, photo2));
                drawPhotosOnTemplate(canvas, photos, targetRects);
                canvas.drawBitmap(templateBitmap, 0, 0, null);

                finalImageBitmap = resultBitmap;
                resultImage.setImageBitmap(finalImageBitmap);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error creating final image.", Toast.LENGTH_SHORT).show();
            }
        }

        shareButton.setOnClickListener(v -> {
            if (finalImageBitmap != null) {
                uploadImage(finalImageBitmap);
            } else {
                Toast.makeText(PhotoTemiResultActivity.this, "No image to share.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImage(Bitmap bitmap) {
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

                        Intent intent = new Intent(PhotoTemiResultActivity.this, PhotoTemiQrCodeActivity.class);
                        intent.putExtra("viewUrl", viewUrl);
                        startActivity(intent);

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: ", e);
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

    private Rect[] buildFrameRects(Bitmap templateBitmap, TemplateFrame[] templateFrames) {
        Rect[] rects = new Rect[templateFrames.length];
        int bitmapWidth = templateBitmap.getWidth();
        int bitmapHeight = templateBitmap.getHeight();

        for (int i = 0; i < templateFrames.length; i++) {
            TemplateFrame frame = templateFrames[i];
            int left = Math.round(frame.leftRatio * bitmapWidth);
            int top = Math.round(frame.topRatio * bitmapHeight);
            int right = Math.round((frame.leftRatio + frame.widthRatio) * bitmapWidth);
            int bottom = Math.round((frame.topRatio + frame.heightRatio) * bitmapHeight);
            rects[i] = new Rect(left, top, right, bottom);
        }
        return rects;
    }

    private void drawPhotosOnTemplate(Canvas canvas, ArrayList<Bitmap> photos, Rect[] rects) {
        for (int i = 0; i < photos.size() && i < rects.length; i++) {
            drawBitmapToCanvas(canvas, photos.get(i), rects[i]);
        }
    }

    private static class TemplateFrame {
        final float leftRatio;
        final float topRatio;
        final float widthRatio;
        final float heightRatio;

        TemplateFrame(float leftRatio, float topRatio, float widthRatio, float heightRatio) {
            this.leftRatio = leftRatio;
            this.topRatio = topRatio;
            this.widthRatio = widthRatio;
            this.heightRatio = heightRatio;
        }
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
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

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
