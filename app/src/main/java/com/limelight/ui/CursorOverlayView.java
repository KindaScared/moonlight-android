package com.limelight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.limelight.R;

/**
 * A pure visual overlay that renders the client-side (local) cursor sprite on
 * top of the streamed video. It never processes mouse movement, never manages
 * host communication and never owns the authoritative cursor coordinates; the
 * owning activity (Game) updates it from LocalCursorState.
 *
 * <p>The bitmap's top-left pixel corresponds to the cursor hotspot, so the
 * sprite is drawn with its tip exactly at the tracked cursor coordinate.</p>
 */
public class CursorOverlayView extends View {

    /** Rasterised size of the cursor sprite in pixels. */
    public static final int CURSOR_SIZE_PX = 32;

    private final Bitmap cursorBitmap;

    private float cursorX;
    private float cursorY;
    private boolean enabled;

    public CursorOverlayView(Context context) {
        super(context);

        // This is a visual layer only. It must never intercept touchscreen or
        // mouse input and must never take keyboard focus.
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);

        cursorBitmap = rasterizeCursor(context);
    }

    private Bitmap rasterizeCursor(Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_local_cursor);
        if (drawable == null) {
            return null;
        }

        // Rasterise the 24x24 vector once into a 32x32 ARGB bitmap. onDraw()
        // must never repeat this work.
        Bitmap bitmap = Bitmap.createBitmap(CURSOR_SIZE_PX, CURSOR_SIZE_PX, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, CURSOR_SIZE_PX, CURSOR_SIZE_PX);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Sets the hotspot position of the cursor sprite in container coordinates.
     */
    public void setCursorPosition(float x, float y) {
        cursorX = x;
        cursorY = y;
        invalidate();
    }

    public float getCursorX() {
        return cursorX;
    }

    public float getCursorY() {
        return cursorY;
    }

    public void setOverlayEnabled(boolean enabled) {
        this.enabled = enabled;
        invalidate();
    }

    public boolean isOverlayEnabled() {
        return enabled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!enabled || cursorBitmap == null) {
            return;
        }

        canvas.drawBitmap(cursorBitmap, cursorX, cursorY, null);
    }
}
