package com.limelight.binding.input;

/**
 * Holds the logical state of the client-side (local) cursor overlay.
 *
 * <p>This class intentionally has no dependencies on Android framework classes,
 * Views or Activities so that the cursor position maths can be unit tested in
 * isolation. Coordinates are stored as floats in the displayed video coordinate
 * space and are only converted to the host protocol's short representation when
 * a position packet is submitted.</p>
 */
public final class LocalCursorState {

    private boolean enabled;
    private boolean positionValid;

    private float x;
    private float y;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets the enabled state, returning true only if the state actually changed.
     * Callers use the return value to make state-transition side effects idempotent.
     */
    public boolean setEnabledIfChanged(boolean enabled) {
        if (this.enabled == enabled) {
            return false;
        }

        this.enabled = enabled;
        return true;
    }

    /**
     * @return true if this state currently contains a usable cursor position.
     *         Note that a valid position does NOT imply the position has been
     *         submitted to the currently active host connection.
     */
    public boolean isPositionValid() {
        return positionValid;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    /**
     * Sets and clamps the cursor position to the given video bounds.
     * If the provided bounds are invalid, the position is marked invalid and
     * the stored coordinates are left untouched.
     */
    public void setPosition(float x, float y, int width, int height) {
        if (width <= 0 || height <= 0) {
            positionValid = false;
            return;
        }

        this.x = clamp(x, width);
        this.y = clamp(y, height);

        positionValid = true;
    }

    public void moveByDelta(float dx, float dy, int width, int height) {
        moveByDelta(dx, dy, width, height, 1.0f);
    }

    /**
     * Applies a relative movement scaled by a constant linear speed multiplier.
     * If no valid position exists yet, the cursor is first initialized at the
     * center of the video. Invalid dimensions are ignored entirely.
     */
    public void moveByDelta(float dx, float dy, int width, int height, float speedMultiplier) {
        if (width <= 0 || height <= 0) {
            return;
        }

        if (!positionValid) {
            initializeCenter(width, height);
        }

        setPosition(
                x + dx * speedMultiplier,
                y + dy * speedMultiplier,
                width,
                height
        );
    }

    public void initializeCenter(int width, int height) {
        setPosition(
                width / 2f,
                height / 2f,
                width,
                height
        );
    }

    public short getShortX() {
        return (short) x;
    }

    public short getShortY() {
        return (short) y;
    }

    /**
     * The Ctrl+Alt+Shift+V shortcut must activate once per physical key press:
     * only on the initial key-down event, never on key-up or automatic key repeat.
     */
    public static boolean shouldActivateShortcut(boolean keyDown, int repeatCount) {
        return keyDown && repeatCount == 0;
    }

    private static float clamp(float value, int max) {
        return Math.min(
                Math.max(value, 0f),
                (float) max
        );
    }
}
