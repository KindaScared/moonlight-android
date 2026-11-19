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
    // Base sprite size in pixels. The user-configurable size is applied as a
    // percentage scale of this value (see setCursorSize).
    public static final int DEFAULT_CURSOR_SIZE = 32;
    private static final int MIN_CURSOR_SIZE = 8;
    private static final int MAX_CURSOR_SIZE = 512;

    private final Drawable cursorDrawable;

    private int cursorSize = DEFAULT_CURSOR_SIZE;
    private Bitmap cursorBitmap;

    private float cursorX = 0f;
    private float cursorY = 0f;
    private boolean enabled = false;

    public CursorOverlayView(Context context) {
        super(context);

        cursorDrawable = context.getDrawable(R.drawable.cursor_arrow);
        rasterizeCursor();

        // This view must never intercept input or take focus; it is purely a visual layer.
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    public int getCursorSize() {
        return cursorSize;
    }

    // Re-rasterizes the vector cursor at the requested size. The bitmap stays
    // square so the arrow is scaled uniformly (no distortion) and its hotspot
    // remains the top-left tip, which keeps pointer positioning and input
    // handling completely untouched. Must be called on the UI thread.
    public void setCursorSize(int sizePx) {
        if (sizePx < MIN_CURSOR_SIZE) {
            sizePx = MIN_CURSOR_SIZE;
        } else if (sizePx > MAX_CURSOR_SIZE) {
            sizePx = MAX_CURSOR_SIZE;
        }

        if (sizePx == cursorSize) {
            return;
        }

        cursorSize = sizePx;
        rasterizeCursor();
        invalidate();
    }

    // Rasterize the vector cursor into a bitmap (once per size change) so
    // onDraw never re-renders the vector on every invalidate (the zero-latency
    // overlay redraws very frequently).
    private void rasterizeCursor() {
        if (cursorDrawable == null) {
            cursorBitmap = null;
            return;
        }

        Bitmap rendered = Bitmap.createBitmap(cursorSize, cursorSize, Bitmap.Config.ARGB_8888);
        Canvas renderCanvas = new Canvas(rendered);
        cursorDrawable.setBounds(0, 0, cursorSize, cursorSize);
        cursorDrawable.draw(renderCanvas);
        cursorBitmap = rendered;
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
