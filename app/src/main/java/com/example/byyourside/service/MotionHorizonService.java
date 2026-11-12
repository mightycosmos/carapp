package com.example.byyourside.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
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
    private static final long DATA_STALE_AFTER_MS = 3_000L;
    private static final long DATA_CHECK_INTERVAL_MS = 1_000L;
    public static final String ACTION_SESSION_STATE =
            "com.example.byyourside.action.SESSION_STATE";
    public static final String EXTRA_SESSION_RUNNING = "session_running";

    private static volatile boolean sessionRunning;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable staleDataCheck = new Runnable() {
        @Override
        public void run() {
            if (!acceptingVehicleCallbacks || horizonView == null) {
                return;
            }

            long now = SystemClock.elapsedRealtime();
            if (lastSteeringUpdateMs != 0L
                    && now - lastSteeringUpdateMs > DATA_STALE_AFTER_MS) {
                lastSteeringUpdateMs = 0L;
                horizonView.markSteeringStale();
            }
            if (lastBrakeUpdateMs != 0L && now - lastBrakeUpdateMs > DATA_STALE_AFTER_MS) {
                lastBrakeUpdateMs = 0L;
                horizonView.markBrakeStale();
            }
            mainHandler.postDelayed(this, DATA_CHECK_INTERVAL_MS);
        }
    };
    private WindowManager windowManager;
    private MotionHorizonView horizonView;
    private Vehicle vehicle;
    private SteeringWheelAngleListener steeringWheelAngleListener;
    private BrakePedalStatusListener brakePedalStatusListener;
    private boolean steeringListenerRegistered;
    private boolean brakeListenerRegistered;
    private volatile boolean acceptingVehicleCallbacks;
    private volatile long lastSteeringUpdateMs;
    private volatile long lastBrakeUpdateMs;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        horizonView = new MotionHorizonView(this);

        if (!Settings.canDrawOverlays(this) || !addHorizonOverlay()) {
            stopSelf();
            return;
        }

        sessionRunning = true;
        notifySessionState(true);
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
            mainHandler.postDelayed(staleDataCheck, DATA_CHECK_INTERVAL_MS);

            steeringWheelAngleListener = new SteeringWheelAngleListener() {
                @Override
                public void onSteeringWheelAngleUpdated(Float angle) {
                    if (angle != null && Float.isFinite(angle)) {
                        lastSteeringUpdateMs = SystemClock.elapsedRealtime();
                        dispatchSteeringAngle(angle);
                    }
                }

                @Override
                public void onFailed(Exception exception) {
                    handleVehicleFailure("Unable to read the steering wheel angle", exception);
                }
            };
            vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringWheelAngleListener);
            steeringListenerRegistered = true;

            brakePedalStatusListener = new BrakePedalStatusListener() {
                @Override
                public void onBrakePedalStatusUpdated(Boolean isPressed) {
                    if (isPressed != null) {
                        lastBrakeUpdateMs = SystemClock.elapsedRealtime();
                        dispatchBrakeStatus(isPressed);
                    }
                }

                @Override
                public void onFailed(Exception exception) {
                    handleVehicleFailure("Unable to read the brake pedal status", exception);
                }
            };
            vehicle.getBrake().registerBrakePedalStatus(brakePedalStatusListener);
            brakeListenerRegistered = true;
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to start the Vehicle SDK session", exception);
            acceptingVehicleCallbacks = false;
            stopSelf();
        }
    }

    private void handleVehicleFailure(String message, Exception exception) {
        Log.e(TAG, message + "; ending the session", exception);
        acceptingVehicleCallbacks = false;
        stopSelf();
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
            try {
                vehicle.release();
            } catch (RuntimeException exception) {
                Log.w(TAG, "Unable to release the Vehicle SDK", exception);
            }
            vehicle = null;
        }

        if (windowManager != null && horizonView != null && horizonView.isAttachedToWindow()) {
            try {
                windowManager.removeView(horizonView);
            } catch (IllegalArgumentException | SecurityException exception) {
                Log.w(TAG, "Unable to remove the motion horizon overlay", exception);
            }
        }
        horizonView = null;
        sessionRunning = false;
        notifySessionState(false);
        super.onDestroy();
    }

    private void notifySessionState(boolean isRunning) {
        Intent stateIntent = new Intent(ACTION_SESSION_STATE)
                .setPackage(getPackageName())
                .putExtra(EXTRA_SESSION_RUNNING, isRunning);
        sendBroadcast(stateIntent);
    }

    public static boolean isSessionRunning() {
        return sessionRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}