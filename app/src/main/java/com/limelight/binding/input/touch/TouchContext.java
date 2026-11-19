package com.limelight.binding.input.touch;

public interface TouchContext {
    /**
     * Reports the host-bound cursor position (in stream/video coordinates) so the
     * client-side overlay can stay in sync with absolute touch input.
     */
    interface CursorPositionListener {
        void onCursorPosition(int x, int y);

        /**
         * Called when a touch context is about to move the host cursor with a
         * relative delta (already scaled for the context's sensitivity). When
         * the client-side local cursor is enabled, the listener takes over the
         * movement entirely: it updates its authoritative absolute cursor model
         * and sends the matching absolute position packet itself. Returning
         * true tells the context NOT to send its own mouse move packet for
         * this delta, keeping the local model exactly in sync with the host so
         * touchscreen-to-mouse handoff cannot desynchronize.
         *
         * @return true if the delta was consumed by the local cursor path
         */
        boolean onCursorDelta(int deltaX, int deltaY);

        /**
         * Called when cursor-affecting touchscreen input begins (for example a
         * finger down in a trackpad emulation mode) so the client-side cursor
         * overlay can hide while the user touches the screen. This must never
         * change the enabled state or the tracked cursor position.
         */
        void onTouchActivity();
    }

    int getActionIndex();
    void setPointerCount(int pointerCount);
    boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger);
    boolean touchMoveEvent(int eventX, int eventY, long eventTime);
    void touchUpEvent(int eventX, int eventY, long eventTime);
    void cancelTouch();
    boolean isCancelled();

    /**
     * Aborts any pending self-scheduled touchscreen momentum cursor motion
     * (trackpad flick follow-up frames) without cancelling unrelated timers.
     * Called when physical mouse input takes over so stale momentum frames
     * cannot move the cursor after the handoff. A mouse button still held by
     * an active momentum drag is released.
     */
    void cancelMomentum();
}
