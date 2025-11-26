package com.example.temidummyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;

public class PhotoTemi extends AppCompatActivity {

    private View selectedBorder;
    private String selectedTemplateName;
    private Robot robot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phototemi);

        final ImageView templateDefault = findViewById(R.id.template_default);
        final View templateDefaultBorder = findViewById(R.id.template_default_border);
        final ImageView templateBlack = findViewById(R.id.template_black);
        final View templateBlackBorder = findViewById(R.id.template_black_border);
        final ImageView templateBlue = findViewById(R.id.template_blue);
        final View templateBlueBorder = findViewById(R.id.template_blue_border);
        final ImageView templateDeveloper = findViewById(R.id.template_developer);
        final View templateDeveloperBorder = findViewById(R.id.template_developer_border);
        final ImageView template8bit = findViewById(R.id.template_8bit);
        final View template8bitBorder = findViewById(R.id.template_8bit_border);

        // Set default selection
        selectedBorder = templateDefaultBorder;
        selectedBorder.setVisibility(View.VISIBLE);
        selectedTemplateName = "Default Template";

        View.OnClickListener templateClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear previous selection
                if (selectedBorder != null) {
                    selectedBorder.setVisibility(View.GONE);
                }

                // Set new selection
                int id = v.getId();
                if (id == R.id.template_default) {
                    selectedBorder = templateDefaultBorder;
                    selectedTemplateName = "Default Template";
                } else if (id == R.id.template_black) {
                    selectedBorder = templateBlackBorder;
                    selectedTemplateName = "Black Template";
                } else if (id == R.id.template_blue) {
                    selectedBorder = templateBlueBorder;
                    selectedTemplateName = "Blue Template";
                } else if (id == R.id.template_developer) {
                    selectedBorder = templateDeveloperBorder;
                    selectedTemplateName = "Developer Template";
                } else if (id == R.id.template_8bit) {
                    selectedBorder = template8bitBorder;
                    selectedTemplateName = "8bit Template";
                }
                selectedBorder.setVisibility(View.VISIBLE);
            }
        };

        templateDefault.setOnClickListener(templateClickListener);
        templateBlack.setOnClickListener(templateClickListener);
        templateBlue.setOnClickListener(templateClickListener);
        templateDeveloper.setOnClickListener(templateClickListener);
        template8bit.setOnClickListener(templateClickListener);

        robot = Robot.getInstance();
        Button actionButton = findViewById(R.id.action_button);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (robot != null) {
                    TtsRequest tts = TtsRequest.create("촬영을 시작합니다.", false);
                    robot.speak(tts);
                }
                Intent intent = new Intent(PhotoTemi.this, PhotoTemiFilmingActivity.class);
                intent.putExtra("template", selectedTemplateName);
                startActivity(intent);
            }
        });
    }
}
