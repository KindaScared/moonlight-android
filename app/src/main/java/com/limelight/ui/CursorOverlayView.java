package com.limelight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.limelight.R;

/**
 * A lightweight overlay View that draws a client-side cursor sprite on top of the
 * streamed video, entirely on the client device (zero round-trip latency).
 *
 * This is NOT the native Android cursor. It is added as the topmost child of
 * StreamContainer (the SurfaceView content renders behind other views), so it draws
 * over the video without invoking the OS pointer machinery (which would trigger
 * Android system UI such as the navigation bars).
 *
 * The cursor bitmap's hotspot is its top-left origin, so the drawable is placed with
 * its top-left at the tracked pointer position.
 */
public class CursorOverlayView extends View {
    private final int cursorSize;
    private final Bitmap cursorBitmap;

    private float cursorX = 0f;
    private float cursorY = 0f;
    private boolean enabled = false;

    public CursorOverlayView(Context context) {
        super(context);

        cursorSize = 32;

        // Rasterize the vector cursor ONCE into a bitmap so onDraw never re-renders the
        // vector on every invalidate (the zero-latency overlay redraws very frequently).
        Drawable cursorDrawable = context.getDrawable(R.drawable.cursor_arrow);
        Bitmap rendered = null;
        if (cursorDrawable != null) {
            rendered = Bitmap.createBitmap(cursorSize, cursorSize, Bitmap.Config.ARGB_8888);
            Canvas renderCanvas = new Canvas(rendered);
            cursorDrawable.setBounds(0, 0, cursorSize, cursorSize);
            cursorDrawable.draw(renderCanvas);
        }
        cursorBitmap = rendered;

        // This view must never intercept input or take focus; it is purely a visual layer.
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    public void setCursorPosition(float x, float y) {
        cursorX = x;
        cursorY = y;
    }

    public float getCursorX() {
        return cursorX;
    }

    public float getCursorY() {
        return cursorY;
    }

    public boolean isOverlayEnabled() {
        return enabled;
    }

    public void setOverlayEnabled(boolean enabled) {
        this.enabled = enabled;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!enabled || cursorBitmap == null) {
            return;
        }

        // The arrow hotspot is its top-left tip, so draw the cached bitmap with its
        // top-left corner exactly at the tracked pointer position.
        canvas.drawBitmap(cursorBitmap, cursorX, cursorY, null);
    }
}
