package com.limelight.ui;

/**
 * Pure coordinate state for the client-side cursor overlay.
 *
 * The overlay coordinate is intentionally stored as a float, but the value sent
 * to the host is truncated to short so it matches CursorOverlayView's drawing
 * behavior.
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

    // Sets the enabled state and returns true only when it actually changed. Used to run
    // activation side effects exactly once per transition and avoid duplicate init when
    // the requested state already matches the current state.
    public boolean setEnabledIfChanged(boolean enabled) {
        if (this.enabled == enabled) {
            return false;
        }
        this.enabled = enabled;
        return true;
    }

    public boolean isPositionValid() {
        return positionValid;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setPosition(float x, float y, int width, int height) {
        if (width <= 0 || height <= 0) {
            positionValid = false;
            return;
        }

        this.x = clamp(x, width);
        this.y = clamp(y, height);
        positionValid = true;
    }

    public void moveByDelta(float deltaX, float deltaY, int width, int height) {
        moveByDelta(deltaX, deltaY, width, height, 1.0f);
    }

    // Applies a pointer-speed multiplier (Android-style linear acceleration) to the
    // relative delta before moving the absolute coordinate. A multiplier of 1.0 keeps
    // the previous 1:1 behavior; values > 1 move the cursor further for the same delta.
    public void moveByDelta(float deltaX, float deltaY, int width, int height, float speedMultiplier) {
        if (width <= 0 || height <= 0) {
            return;
        }

        if (!positionValid) {
            initializeCenter(width, height);
        }

        setPosition(x + deltaX * speedMultiplier, y + deltaY * speedMultiplier, width, height);
    }

    public void initializeCenter(int width, int height) {
        setPosition(width / 2f, height / 2f, width, height);
    }

    public short getShortX() {
        return (short) x;
    }

    public short getShortY() {
        return (short) y;
    }

    public static boolean shouldActivateShortcut(boolean down, int repeatCount) {
        return down && repeatCount == 0;
    }

    private static float clamp(float value, int max) {
        return Math.min(Math.max(value, 0f), (float) max);
    }
}
