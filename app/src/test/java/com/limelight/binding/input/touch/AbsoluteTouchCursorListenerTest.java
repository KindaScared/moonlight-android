package com.limelight.binding.input.touch;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.nvstream.NvConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Verifies that absolute touchscreen input mirrors its positions into the
 * optional client-side cursor listener without generating duplicate host
 * position packets.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class AbsoluteTouchCursorListenerTest {

    private Context ctx;
    private NvConnection conn;
    private View targetView;
    private CursorPositionListener listener;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
        conn = Mockito.mock(NvConnection.class);

        targetView = new View(ctx);
        targetView.layout(0, 0, 1280, 720);

        listener = Mockito.mock(CursorPositionListener.class);
    }

    @Test
    public void touchDown_reportsPositionImmediately() {
        AbsoluteTouchContext tc =
                new AbsoluteTouchContext(conn, 0, targetView, false, listener);

        tc.touchDownEvent(100, 200, 1000, true);

        verify(listener, times(1)).onCursorPosition(100, 200);
    }

    @Test
    public void touchMove_reportsPositionAfterHostPacket_noDuplicatePackets() {
        AbsoluteTouchContext tc =
                new AbsoluteTouchContext(conn, 0, targetView, false, listener);

        tc.touchDownEvent(100, 200, 1000, true);
        reset(listener);

        // Move far enough to leave the touch-down deadzone: the context sends
        // its normal host packet and then reports the same coordinates.
        tc.touchMoveEvent(400, 500, 2000);

        verify(listener, atLeastOnce()).onCursorPosition(400, 500);

        // Exactly one host packet for the moved-to position (the listener must
        // not cause a second packet).
        verify(conn, times(1))
                .sendMousePosition((short) 400, (short) 500, (short) 1280, (short) 720);

        // Each reported (400,500) position corresponds to a preceding host send
        InOrder inOrder = Mockito.inOrder(conn, listener);
        inOrder.verify(conn)
                .sendMousePosition((short) 400, (short) 500, (short) 1280, (short) 720);
        inOrder.verify(listener, atLeastOnce()).onCursorPosition(400, 500);
    }

    @Test
    public void secondaryFingerScroll_neverReportsCursor() {
        AbsoluteTouchContext scrollCtx =
                new AbsoluteTouchContext(conn, 1, targetView, false, listener);

        scrollCtx.touchDownEvent(300, 300, 1000, true);
        scrollCtx.touchMoveEvent(300, 400, 2000);

        verify(listener, never()).onCursorPosition(anyInt(), anyInt());
    }

    @Test
    public void withoutListener_behaviourIsUnchanged() {
        // Legacy constructor still works and sends its normal host packet
        AbsoluteTouchContext tc = new AbsoluteTouchContext(conn, 0, targetView, false);

        tc.touchDownEvent(100, 200, 1000, true);
        tc.touchMoveEvent(400, 500, 2000);

        verify(conn, atLeastOnce())
                .sendMousePosition((short) 400, (short) 500, (short) 1280, (short) 720);
    }
}
