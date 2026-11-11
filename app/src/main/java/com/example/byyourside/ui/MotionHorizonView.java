package com.example.byyourside.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;

public class MotionHorizonView extends View {
    private static final float MAX_TILT_DEGREES = 8f;
    private static final float EDGE_WIDTH_PX = 5f;

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
    private long previousFrameNanos;

    public MotionHorizonView(Context context) {
        super(context);
        choreographer = Choreographer.getInstance();
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setBackgroundColor(Color.TRANSPARENT);
        glowPaint.setColor(Color.rgb(82, 220, 232));
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setSteeringAngle(float steeringAngleDegrees) {
        float boundedAngle = Math.max(-540f, Math.min(540f, steeringAngleDegrees));
        post(() -> targetTiltDegrees = boundedAngle / 540f * MAX_TILT_DEGREES);
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

        float centerY = getHeight() / 2f;
        float edgeOffset = (float) (Math.tan(Math.toRadians(displayedTiltDegrees))
                * getWidth() / 2f);
        float leftY = centerY + edgeOffset;
        float rightY = centerY - edgeOffset;

        drawEdgeGlow(canvas, 0f, EDGE_WIDTH_PX, leftY, rightY);
        drawEdgeGlow(canvas, getWidth() - EDGE_WIDTH_PX, getWidth(), leftY, rightY);
    }

    private void drawEdgeGlow(Canvas canvas, float clipLeft, float clipRight,
                              float leftY, float rightY) {
        int saveCount = canvas.save();
        canvas.clipRect(clipLeft, 0f, clipRight, getHeight());

        glowPaint.setAlpha(36);
        glowPaint.setStrokeWidth(15f);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);

        glowPaint.setAlpha(76);
        glowPaint.setStrokeWidth(10f);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);

        glowPaint.setAlpha(190);
        glowPaint.setStrokeWidth(5f);
        canvas.drawLine(0f, leftY, getWidth(), rightY, glowPaint);
        canvas.restoreToCount(saveCount);
    }

    private void updateAnimation(long frameTimeNanos) {
        long elapsedNanos = Math.max(0L, frameTimeNanos - previousFrameNanos);
        previousFrameNanos = frameTimeNanos;
        float interpolation = 1f - (float) Math.exp(-elapsedNanos / 120_000_000f);
        displayedTiltDegrees += (targetTiltDegrees - displayedTiltDegrees) * interpolation;
        invalidate();
    }
}