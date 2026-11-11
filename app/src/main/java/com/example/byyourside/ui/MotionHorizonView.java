package com.example.byyourside.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;

public class MotionHorizonView extends View {
    private static final float MAX_STEERING_ANGLE = 540f;
    private static final float MAX_TILT_DEGREES = 15f;
    private static final float EDGE_WIDTH_PX = 5f;
    private static final float BRAKE_EDGE_WIDTH_PX = 8f;
    private static final float BRAKE_PITCH_SHIFT_PX = 5f;
    private static final float LOW_PASS_ALPHA = 0.12f;
    private static final long BRAKE_PULSE_DURATION_NANOS = 420_000_000L;

    private final Choreographer choreographer;
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            updateAnimation(frameTimeNanos);
            choreographer.postFrameCallback(this);
        }
    };

    private float targetTiltDegrees;
    private float displayedTiltDegrees;
    private float targetPitchShift;
    private float displayedPitchShift;
    private boolean brakeActive;
    private long brakePulseStartNanos;
    private long previousFrameNanos;

    public MotionHorizonView(Context context) {
        super(context);
        choreographer = Choreographer.getInstance();
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setBackgroundColor(Color.TRANSPARENT);
        glowPaint.setColor(Color.rgb(123, 245, 204));
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setSteeringAngle(float steeringAngleDegrees) {
        float boundedAngle = Math.max(-MAX_STEERING_ANGLE,
                Math.min(MAX_STEERING_ANGLE, steeringAngleDegrees));
        float targetTilt = boundedAngle / MAX_STEERING_ANGLE * MAX_TILT_DEGREES;
        Runnable update = () -> targetTiltDegrees = Math.max(-MAX_TILT_DEGREES,
            Math.min(MAX_TILT_DEGREES, targetTilt));
        if (Looper.myLooper() == Looper.getMainLooper()) {
            update.run();
        } else {
            post(update);
        }
    }

    public void setBrakeStatus(boolean isPressed) {
        Runnable update = () -> {
            if (isPressed && !brakeActive) {
                brakePulseStartNanos = SystemClock.elapsedRealtimeNanos();
            }
            brakeActive = isPressed;
            targetPitchShift = isPressed ? BRAKE_PITCH_SHIFT_PX : 0f;
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            update.run();
        } else {
            post(update);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        previousFrameNanos = SystemClock.elapsedRealtimeNanos();
        choreographer.postFrameCallback(frameCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        choreographer.removeFrameCallback(frameCallback);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        float centerY = getHeight() / 2f + displayedPitchShift;
        float edgeOffset = (float) (Math.tan(Math.toRadians(displayedTiltDegrees))
                * getWidth() / 2f);
        float leftY = centerY + edgeOffset;
        float rightY = centerY - edgeOffset;
        float pulse = getBrakePulse(SystemClock.elapsedRealtimeNanos());
        float edgeWidth = EDGE_WIDTH_PX
                + (BRAKE_EDGE_WIDTH_PX - EDGE_WIDTH_PX) * pulse;

        drawEdgeGlow(canvas, 0f, edgeWidth, leftY, rightY, edgeWidth, pulse);
        drawEdgeGlow(canvas, getWidth() - edgeWidth, getWidth(), leftY, rightY,
                edgeWidth, pulse);
    }

    private void drawEdgeGlow(Canvas canvas, float clipLeft, float clipRight,
                              float leftY, float rightY, float edgeWidth, float pulse) {
        int saveCount = canvas.save();
        canvas.clipRect(clipLeft, 0f, clipRight, getHeight());

        glowPaint.setAlpha(Math.round(36f + 36f * pulse));
        glowPaint.setStrokeWidth(edgeWidth + 10f);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);

        glowPaint.setAlpha(Math.round(76f + 55f * pulse));
        glowPaint.setStrokeWidth(edgeWidth + 5f);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);

        glowPaint.setAlpha(Math.round(190f + 65f * pulse));
        glowPaint.setStrokeWidth(edgeWidth);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);
        canvas.restoreToCount(saveCount);
    }

    private void updateAnimation(long frameTimeNanos) {
        previousFrameNanos = frameTimeNanos;
        displayedTiltDegrees += (targetTiltDegrees - displayedTiltDegrees) * LOW_PASS_ALPHA;
        displayedPitchShift += (targetPitchShift - displayedPitchShift) * LOW_PASS_ALPHA;
        invalidate();
    }

    private float getBrakePulse(long frameTimeNanos) {
        if (brakeActive) {
            return 0.65f + 0.35f
                    * (float) Math.sin(frameTimeNanos / 90_000_000f);
        }
        if (brakePulseStartNanos == 0L) {
            return 0f;
        }

        long elapsedNanos = frameTimeNanos - brakePulseStartNanos;
        if (elapsedNanos >= BRAKE_PULSE_DURATION_NANOS) {
            brakePulseStartNanos = 0L;
            return 0f;
        }
        float remaining = 1f - (float) elapsedNanos / BRAKE_PULSE_DURATION_NANOS;
        return remaining * (0.5f + 0.5f
                * (float) Math.sin(frameTimeNanos / 90_000_000f));
    }
}