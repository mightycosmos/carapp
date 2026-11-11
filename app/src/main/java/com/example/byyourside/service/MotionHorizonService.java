package com.example.byyourside.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener;
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener;

import com.example.byyourside.ui.MotionHorizonView;

public class MotionHorizonService extends Service {
    private static final String TAG = "MotionHorizonService";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private MotionHorizonView horizonView;
    private Vehicle vehicle;
    private SteeringWheelAngleListener steeringWheelAngleListener;
    private BrakePedalStatusListener brakePedalStatusListener;
    private boolean steeringListenerRegistered;
    private boolean brakeListenerRegistered;
    private volatile boolean acceptingVehicleCallbacks;

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
            acceptingVehicleCallbacks = true;

            steeringWheelAngleListener = new SteeringWheelAngleListener() {
                @Override
                public void onSteeringWheelAngleUpdated(Float angle) {
                    if (angle != null) {
                        dispatchSteeringAngle(angle);
                    }
                }

                @Override
                public void onFailed(Exception exception) {
                    Log.w(TAG, "Unable to read the steering wheel angle", exception);
                }
            };
            vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringWheelAngleListener);
            steeringListenerRegistered = true;

            brakePedalStatusListener = new BrakePedalStatusListener() {
                @Override
                public void onBrakePedalStatusUpdated(Boolean isPressed) {
                    if (isPressed != null) {
                        dispatchBrakeStatus(isPressed);
                    }
                }

                @Override
                public void onFailed(Exception exception) {
                    Log.w(TAG, "Unable to read the brake pedal status", exception);
                }
            };
            vehicle.getBrake().registerBrakePedalStatus(brakePedalStatusListener);
            brakeListenerRegistered = true;
        } catch (RuntimeException exception) {
            Log.w(TAG, "Vehicle SDK is unavailable; showing a neutral horizon", exception);
        }
    }

    private void dispatchSteeringAngle(float angle) {
        MotionHorizonView targetView = horizonView;
        if (!acceptingVehicleCallbacks || targetView == null) {
            return;
        }
        mainHandler.post(() -> {
            if (acceptingVehicleCallbacks && horizonView == targetView) {
                targetView.setSteeringAngle(angle);
            }
        });
    }

    private void dispatchBrakeStatus(boolean isPressed) {
        MotionHorizonView targetView = horizonView;
        if (!acceptingVehicleCallbacks || targetView == null) {
            return;
        }
        mainHandler.post(() -> {
            if (acceptingVehicleCallbacks && horizonView == targetView) {
                targetView.setBrakeStatus(isPressed);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        acceptingVehicleCallbacks = false;
        mainHandler.removeCallbacksAndMessages(null);
        if (vehicle != null) {
            if (steeringListenerRegistered && steeringWheelAngleListener != null) {
                try {
                    vehicle.getSteeringWheel().unregisterSteeringWheelAngle(
                            steeringWheelAngleListener);
                } catch (RuntimeException exception) {
                    Log.w(TAG, "Unable to unregister the steering angle listener", exception);
                }
            }
            if (brakeListenerRegistered && brakePedalStatusListener != null) {
                try {
                    vehicle.getBrake().unregisterBrakePedalStatus(brakePedalStatusListener);
                } catch (RuntimeException exception) {
                    Log.w(TAG, "Unable to unregister the brake status listener", exception);
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