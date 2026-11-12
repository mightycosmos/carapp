package com.example.byyourside.ui;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
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
    private boolean serviceRequested;
    private boolean statusReceiverRegistered;
    private final BroadcastReceiver sessionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            serviceRequested = intent.getBooleanExtra(
                    MotionHorizonService.EXTRA_SESSION_RUNNING, false);
            updateOverlayState();
        }
    };

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
        serviceRequested = MotionHorizonService.isSessionRunning();
        registerSessionStateReceiver();
        updateOverlayState();
    }

    @Override
    protected void onPause() {
        if (statusReceiverRegistered) {
            unregisterReceiver(sessionStateReceiver);
            statusReceiverRegistered = false;
        }
        super.onPause();
    }

    private void registerSessionStateReceiver() {
        if (statusReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(MotionHorizonService.ACTION_SESSION_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sessionStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(sessionStateReceiver, filter);
        }
        statusReceiverRegistered = true;
    }

    private void onActionButtonClicked() {
        if (!serviceRequested && !Settings.canDrawOverlays(this)) {
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
            statusView.setText("오버레이 권한이 필요합니다. 권한을 허용한 뒤 세션을 시작하세요.");
            actionButton.setText("권한 허용");
        } else if (serviceRequested) {
            statusView.setText("활성 세션에서 수신된 차량 입력을 가장자리에 표시합니다.");
            actionButton.setText("세션 종료");
        } else {
            statusView.setText("세션이 종료되었습니다. 효과는 멀미 완화를 보장하지 않습니다.");
            actionButton.setText("세션 시작");
        }
    }
}