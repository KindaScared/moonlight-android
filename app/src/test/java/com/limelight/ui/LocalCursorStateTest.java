package com.limelight.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalCursorStateTest {
    @Test
    public void setPosition_clampsToStreamBounds() {
        LocalCursorState state = new LocalCursorState();

        state.setPosition(-10f, 999f, 100, 200);

        assertEquals(0f, state.getX(), 0.0001f);
        assertEquals(200f, state.getY(), 0.0001f);
        assertTrue(state.isPositionValid());
    }

    @Test
    public void moveByDelta_appliesRelativeMovementToAbsoluteCoordinate() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(100f, 100f, 1000, 1000);

        state.moveByDelta(20f, -30f, 1000, 1000);

        assertEquals(120f, state.getX(), 0.0001f);
        assertEquals(70f, state.getY(), 0.0001f);
    }

    @Test
    public void moveByDelta_clampsAtEdges() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(990f, 10f, 1000, 1000);

        state.moveByDelta(100f, -100f, 1000, 1000);

        assertEquals(1000f, state.getX(), 0.0001f);
        assertEquals(0f, state.getY(), 0.0001f);
    }

    @Test
    public void moveByDelta_initializesCenterWhenPositionIsUnknown() {
        LocalCursorState state = new LocalCursorState();

        state.moveByDelta(10f, 5f, 100, 200);

        assertEquals(60f, state.getX(), 0.0001f);
        assertEquals(105f, state.getY(), 0.0001f);
        assertTrue(state.isPositionValid());
    }

    @Test
    public void invalidStreamSizeDoesNotMarkPositionValid() {
        LocalCursorState state = new LocalCursorState();

        state.setPosition(10f, 20f, 0, 0);

        assertFalse(state.isPositionValid());
    }

    @Test
    public void shortcutActivatesOnlyOnFirstKeyDown() {
        assertTrue(LocalCursorState.shouldActivateShortcut(true, 0));
        assertFalse(LocalCursorState.shouldActivateShortcut(true, 1));
        assertFalse(LocalCursorState.shouldActivateShortcut(false, 0));
    }

    @Test
    public void moveByDelta_appliesSpeedMultiplier() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(100f, 100f, 1000, 1000);

        state.moveByDelta(20f, -30f, 1000, 1000, 2.0f);

        assertEquals(140f, state.getX(), 0.0001f);
        assertEquals(40f, state.getY(), 0.0001f);
    }

    @Test
    public void moveByDelta_speedMultiplierStillClampsAtEdges() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(990f, 10f, 1000, 1000);

        state.moveByDelta(100f, -100f, 1000, 1000, 3.0f);

        assertEquals(1000f, state.getX(), 0.0001f);
        assertEquals(0f, state.getY(), 0.0001f);
    }

    @Test
    public void moveByDelta_multiplierOfOneMatchesFourArgOverload() {
        LocalCursorState a = new LocalCursorState();
        LocalCursorState b = new LocalCursorState();
        a.setPosition(100f, 100f, 1000, 1000);
        b.setPosition(100f, 100f, 1000, 1000);

        a.moveByDelta(37f, -13f, 1000, 1000);
        b.moveByDelta(37f, -13f, 1000, 1000, 1.0f);

        assertEquals(a.getX(), b.getX(), 0.0001f);
        assertEquals(a.getY(), b.getY(), 0.0001f);
    }
}
