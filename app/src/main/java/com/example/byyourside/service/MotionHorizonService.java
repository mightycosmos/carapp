package com.example.byyourside.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener;

import com.example.byyourside.ui.MotionHorizonView;

public class MotionHorizonService extends Service {
    private static final String TAG = "MotionHorizonService";

    private WindowManager windowManager;
    private MotionHorizonView horizonView;
    private Vehicle vehicle;
    private SteeringWheelAngleListener steeringWheelAngleListener;
    private boolean listenerRegistered;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        horizonView = new MotionHorizonView(this);

        if (Settings.canDrawOverlays(this)) {
            if (!addHorizonOverlay()) {
                stopSelf();
                return;
            }
        } else {
            stopSelf();
            return;
        }

        connectVehicleSdk();
    }

    private boolean addHorizonOverlay() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;

        try {
            windowManager.addView(horizonView, layoutParams);
            return true;
        } catch (WindowManager.BadTokenException | SecurityException exception) {
            Log.e(TAG, "Unable to add the motion horizon overlay", exception);
            return false;
        }
    }

    private void connectVehicleSdk() {
        try {
            vehicle = new Vehicle(getApplicationContext());
            vehicle.initialize();

            steeringWheelAngleListener = new SteeringWheelAngleListener() {
                @Override
                public void onSteeringWheelAngleUpdated(Float angle) {
                    if (angle != null && horizonView != null) {
                        horizonView.setSteeringAngle(angle);
                    }
                }

                @Override
                public void onFailed(Exception exception) {
                    Log.w(TAG, "Unable to read the steering wheel angle", exception);
                }
            };
            vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringWheelAngleListener);
            listenerRegistered = true;
        } catch (RuntimeException exception) {
            Log.w(TAG, "Vehicle SDK is unavailable; showing a neutral horizon", exception);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (vehicle != null) {
            if (listenerRegistered && steeringWheelAngleListener != null) {
                try {
                    vehicle.getSteeringWheel().unregisterSteeringWheelAngle(
                            steeringWheelAngleListener);
                } catch (RuntimeException exception) {
                    Log.w(TAG, "Unable to unregister the steering angle listener", exception);
                }
            }
            vehicle.release();
            vehicle = null;
        }

        if (windowManager != null && horizonView != null && horizonView.isAttachedToWindow()) {
            windowManager.removeView(horizonView);
        }
        horizonView = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}