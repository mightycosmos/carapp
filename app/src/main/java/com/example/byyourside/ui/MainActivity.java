package com.example.byyourside.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.byyourside.service.MotionHorizonService;

public class MainActivity extends AppCompatActivity {
    private TextView statusView;
    private Button actionButton;
    private boolean serviceRequested = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        int padding = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        content.setPadding(padding, padding, padding, padding);

        TextView titleView = new TextView(this);
        titleView.setText("옆에서");
        titleView.setTextSize(28);
        content.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statusView = new TextView(this);
        statusView.setTextSize(16);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = padding / 2;
        content.addView(statusView, statusParams);

        actionButton = new Button(this);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = padding;
        content.addView(actionButton, buttonParams);
        actionButton.setOnClickListener(view -> onActionButtonClicked());

        setContentView(content);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOverlayState();
        if (serviceRequested && Settings.canDrawOverlays(this)) {
            startService(new Intent(this, MotionHorizonService.class));
        }
    }

    private void onActionButtonClicked() {
        if (!Settings.canDrawOverlays(this)) {
            Intent permissionIntent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(permissionIntent);
            return;
        }

        serviceRequested = !serviceRequested;
        if (serviceRequested) {
            startService(new Intent(this, MotionHorizonService.class));
        } else {
            stopService(new Intent(this, MotionHorizonService.class));
        }
        updateOverlayState();
    }

    private void updateOverlayState() {
        boolean permissionGranted = Settings.canDrawOverlays(this);
        if (!permissionGranted) {
            statusView.setText("오버레이 권한이 필요합니다.");
            actionButton.setText("권한 허용");
        } else if (serviceRequested) {
            statusView.setText("주행 정보를 가장자리 지평선에 표시합니다.");
            actionButton.setText("오버레이 중지");
        } else {
            statusView.setText("오버레이가 중지되었습니다.");
            actionButton.setText("오버레이 시작");
        }
    }
}