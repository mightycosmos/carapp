package com.example.byyourside.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.os.Build;
import android.os.Looper;
import android.view.Choreographer;
import android.view.WindowInsets;
import android.view.View;

public class MotionHorizonView extends View {
    private static final float MAX_STEERING_ANGLE = 540f;
    private static final float MAX_TILT_DEGREES = 15f;
    private static final float EDGE_WIDTH_PX = 5f;
    private static final float BRAKE_EDGE_WIDTH_PX = 8f;
    private static final float BRAKE_PITCH_SHIFT_PX = 5f;
    private static final float LOW_PASS_ALPHA = 0.12f;

    private final Choreographer choreographer;
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            updateAnimation();
            if (isAnimationNeeded()) {
                choreographer.postFrameCallback(this);
            } else {
                animationScheduled = false;
                invalidate();
            }
        }
    };

    private float targetTiltDegrees;
    private float displayedTiltDegrees;
    private float targetBrakeIntensity;
    private float displayedBrakeIntensity;
    private int safeInsetTop;
    private int safeInsetBottom;
    private boolean steeringFresh;
    private boolean brakeFresh;
    private boolean hasDisplayData;
    private boolean animationScheduled;

    public MotionHorizonView(Context context) {
        super(context);
        choreographer = Choreographer.getInstance();
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setBackgroundColor(Color.TRANSPARENT);
        glowPaint.setColor(Color.rgb(123, 245, 204));
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                safeInsetTop = systemBars.top;
                safeInsetBottom = systemBars.bottom;
            } else {
                safeInsetTop = insets.getSystemWindowInsetTop();
                safeInsetBottom = insets.getSystemWindowInsetBottom();
            }
            invalidate();
            return insets;
        });
    }

    public void setSteeringAngle(float steeringAngleDegrees) {
        if (!Float.isFinite(steeringAngleDegrees)) {
            return;
        }
        float boundedAngle = Math.max(-MAX_STEERING_ANGLE,
                Math.min(MAX_STEERING_ANGLE, steeringAngleDegrees));
        float targetTilt = boundedAngle / MAX_STEERING_ANGLE * MAX_TILT_DEGREES;
        runOnMainThread(() -> {
            targetTiltDegrees = targetTilt;
            steeringFresh = true;
            hasDisplayData = true;
            scheduleAnimation();
        });
    }

    public void setBrakeStatus(boolean isPressed) {
        runOnMainThread(() -> {
            targetBrakeIntensity = isPressed ? 1f : 0f;
            brakeFresh = true;
            hasDisplayData = true;
            scheduleAnimation();
        });
    }

    public void markSteeringStale() {
        runOnMainThread(() -> {
            targetTiltDegrees = 0f;
            steeringFresh = false;
            scheduleAnimation();
        });
    }

    public void markBrakeStale() {
        runOnMainThread(() -> {
            targetBrakeIntensity = 0f;
            brakeFresh = false;
            scheduleAnimation();
        });
    }

    private void runOnMainThread(Runnable update) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            update.run();
        } else {
            post(update);
        }
    }

    private void scheduleAnimation() {
        if (isAttachedToWindow() && !animationScheduled) {
            animationScheduled = true;
            choreographer.postFrameCallback(frameCallback);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestApplyInsets();
        scheduleAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        animationScheduled = false;
        choreographer.removeFrameCallback(frameCallback);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0 || !hasDisplayData) {
            return;
        }

        float safeTop = Math.min(safeInsetTop, getHeight() / 2f);
        float safeBottom = Math.max(safeTop, getHeight() - safeInsetBottom);
        float centerY = (safeTop + safeBottom) / 2f
                + displayedBrakeIntensity * BRAKE_PITCH_SHIFT_PX;
        float edgeOffset = (float) (Math.tan(Math.toRadians(displayedTiltDegrees))
                * getWidth() / 2f);
        float edgeWidth = EDGE_WIDTH_PX
                + (BRAKE_EDGE_WIDTH_PX - EDGE_WIDTH_PX) * displayedBrakeIntensity;
        float insetMargin = Math.min(edgeWidth + 5f, (safeBottom - safeTop) / 2f);
        safeTop += insetMargin;
        safeBottom -= insetMargin;
        float leftY = clamp(centerY + edgeOffset, safeTop, safeBottom);
        float rightY = clamp(centerY - edgeOffset, safeTop, safeBottom);

        drawEdgeGlow(canvas, 0f, edgeWidth, leftY, rightY, edgeWidth,
                displayedBrakeIntensity);
        drawEdgeGlow(canvas, getWidth() - edgeWidth, getWidth(), leftY, rightY,
                edgeWidth, displayedBrakeIntensity);
    }

    private void drawEdgeGlow(Canvas canvas, float clipLeft, float clipRight,
                              float leftY, float rightY, float edgeWidth, float brakeIntensity) {
        int saveCount = canvas.save();
        canvas.clipRect(clipLeft, 0f, clipRight, getHeight());

        glowPaint.setAlpha(Math.round(36f + 36f * brakeIntensity));
        glowPaint.setStrokeWidth(edgeWidth + 10f);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);

        glowPaint.setAlpha(Math.round(76f + 55f * brakeIntensity));
        glowPaint.setStrokeWidth(edgeWidth + 5f);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);

        glowPaint.setAlpha(Math.round(190f + 65f * brakeIntensity));
        glowPaint.setStrokeWidth(edgeWidth);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);
        canvas.restoreToCount(saveCount);
    }

    private void updateAnimation() {
        displayedTiltDegrees += (targetTiltDegrees - displayedTiltDegrees) * LOW_PASS_ALPHA;
        displayedBrakeIntensity += (targetBrakeIntensity - displayedBrakeIntensity)
                * LOW_PASS_ALPHA;
        if (!steeringFresh && !brakeFresh
                && Math.abs(displayedTiltDegrees) < 0.05f
                && displayedBrakeIntensity < 0.01f) {
            hasDisplayData = false;
        }
        invalidate();
    }

    private boolean isAnimationNeeded() {
        return Math.abs(targetTiltDegrees - displayedTiltDegrees) >= 0.01f
                || Math.abs(targetBrakeIntensity - displayedBrakeIntensity) >= 0.01f;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
