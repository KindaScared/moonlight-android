package com.limelight.binding.input.touch;

public interface TouchContext {
    /**
     * Reports the host-bound cursor position (in stream/video coordinates) so the
     * client-side overlay can stay in sync with absolute touch input.
     */
    interface CursorPositionListener {
        void onCursorPosition(int x, int y);
    }

    int getActionIndex();
    void setPointerCount(int pointerCount);
    boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger);
    boolean touchMoveEvent(int eventX, int eventY, long eventTime);
    void touchUpEvent(int eventX, int eventY, long eventTime);
    void cancelTouch();
    boolean isCancelled();
}
