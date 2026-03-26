package com.limelight.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Lightweight client-side cursor overlay drawn above the video surface.
 */
public class LocalCursorOverlayView extends View {
    private static final float CURSOR_SIZE_DP = 20f;
    private static final float CURSOR_STROKE_DP = 1.75f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cursorPath = new Path();

    private float cursorX;
    private float cursorY;
    private float cursorSizePx;

    public LocalCursorOverlayView(Context context) {
        this(context, null);
    }

    public LocalCursorOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        float density = getResources().getDisplayMetrics().density;
        cursorSizePx = CURSOR_SIZE_DP * density;

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xFFFFFFFF);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(CURSOR_STROKE_DP * density);
        strokePaint.setColor(0xFF000000);

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(0x66000000);

        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setCursorPosition(float x, float y) {
        if (cursorX == x && cursorY == y) {
            return;
        }

        cursorX = x;
        cursorY = y;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getVisibility() != VISIBLE) {
            return;
        }

        float tipX = cursorX;
        float tipY = cursorY;
        float size = cursorSizePx;

        float rightX = tipX + size * 0.55f;
        float downY = tipY + size;
        float innerX = tipX + size * 0.34f;
        float innerY = tipY + size * 0.62f;

        cursorPath.reset();
        cursorPath.moveTo(tipX, tipY);
        cursorPath.lineTo(rightX, innerY);
        cursorPath.lineTo(innerX, innerY);
        cursorPath.lineTo(innerX, downY);
        cursorPath.close();

        canvas.save();
        canvas.translate(getResources().getDisplayMetrics().density, getResources().getDisplayMetrics().density);
        canvas.drawPath(cursorPath, shadowPaint);
        canvas.restore();

        canvas.drawPath(cursorPath, fillPaint);
        canvas.drawPath(cursorPath, strokePaint);
    }
}