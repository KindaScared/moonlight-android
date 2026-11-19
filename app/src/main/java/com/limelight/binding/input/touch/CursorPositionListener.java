package com.limelight.binding.input.touch;

/**
 * Optional listener used by absolute touch input to mirror the positions it has
 * already submitted to the host into the client-side (local) cursor state and
 * overlay. The listener is visual/state mirroring only; it must never cause a
 * second host position packet to be sent.
 */
public interface CursorPositionListener {
    void onCursorPosition(int x, int y);
}
