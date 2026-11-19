package com.limelight.binding.input;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Behaviour tests for the pure {@link LocalCursorState} logic.
 */
public class LocalCursorStateTest {

    private static final float EPS = 0.0001f;

    @Test
    public void initialState_isDisabledWithInvalidPosition() {
        LocalCursorState state = new LocalCursorState();

        assertFalse(state.isEnabled());
        assertFalse(state.isPositionValid());
        assertEquals(0f, state.getX(), EPS);
        assertEquals(0f, state.getY(), EPS);
    }

    @Test
    public void setEnabled_togglesState() {
        LocalCursorState state = new LocalCursorState();

        state.setEnabled(true);
        assertTrue(state.isEnabled());

        state.setEnabled(false);
        assertFalse(state.isEnabled());
    }

    @Test
    public void setEnabledIfChanged_reportsTransitionsOnly() {
        LocalCursorState state = new LocalCursorState();

        assertFalse(state.setEnabledIfChanged(false)); // no change (already disabled)
        assertTrue(state.setEnabledIfChanged(true));   // disabled -> enabled
        assertFalse(state.setEnabledIfChanged(true));  // no change
        assertTrue(state.setEnabledIfChanged(false));  // enabled -> disabled
    }

    @Test
    public void setPosition_clampsToVideoBounds() {
        LocalCursorState state = new LocalCursorState();

        state.setPosition(100f, 200f, 1280, 720);
        assertEquals(100f, state.getX(), EPS);
        assertEquals(200f, state.getY(), EPS);
        assertTrue(state.isPositionValid());

        state.setPosition(-50f, 10000f, 1280, 720);
        assertEquals(0f, state.getX(), EPS);
        assertEquals(720f, state.getY(), EPS);
    }

    @Test
    public void setPosition_invalidDimensionsMarkPositionInvalid() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(10f, 20f, 1280, 720);
        assertTrue(state.isPositionValid());

        state.setPosition(40f, 50f, 0, 720);
        assertFalse(state.isPositionValid());
        // Stored coordinates are left untouched on invalid dimensions
        assertEquals(10f, state.getX(), EPS);
        assertEquals(20f, state.getY(), EPS);

        state.setPosition(40f, 50f, 1280, -5);
        assertFalse(state.isPositionValid());
    }

    @Test
    public void moveByDelta_appliesRelativeMovement() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(100f, 100f, 1280, 720);

        state.moveByDelta(40f, -30f, 1280, 720);
        assertEquals(140f, state.getX(), EPS);
        assertEquals(70f, state.getY(), EPS);
    }

    @Test
    public void moveByDelta_negativeMovement_clampsAtOrigin() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(10f, 10f, 1280, 720);

        state.moveByDelta(-9999f, -9999f, 1280, 720);
        assertEquals(0f, state.getX(), EPS);
        assertEquals(0f, state.getY(), EPS);
    }

    @Test
    public void moveByDelta_clampsAtFarBoundary() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(1270f, 710f, 1280, 720);

        state.moveByDelta(10000f, 10000f, 1280, 720);
        assertEquals(1280f, state.getX(), EPS);
        assertEquals(720f, state.getY(), EPS);
    }

    @Test
    public void moveByDelta_unknownPosition_initializesAtCenter() {
        LocalCursorState state = new LocalCursorState();

        // First movement with no known position starts from the video center
        state.moveByDelta(10f, 20f, 1280, 720);
        assertEquals(640f + 10f, state.getX(), EPS);
        assertEquals(360f + 20f, state.getY(), EPS);
        assertTrue(state.isPositionValid());
    }

    @Test
    public void moveByDelta_zeroSizedDimensions_isIgnored() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(50f, 60f, 1280, 720);

        state.moveByDelta(100f, 100f, 0, 0);
        assertEquals(50f, state.getX(), EPS);
        assertEquals(60f, state.getY(), EPS);
    }

    @Test
    public void moveByDelta_preservesFractionalAccumulation() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(100f, 100f, 1280, 720);

        // Ten fractional deltas must accumulate without integer rounding loss
        for (int i = 0; i < 10; i++) {
            state.moveByDelta(0.4f, -0.4f, 1280, 720);
        }

        assertEquals(104f, state.getX(), 0.01f);
        assertEquals(96f, state.getY(), 0.01f);
    }

    @Test
    public void moveByDelta_defaultMultiplierIsOne() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(0f, 0f, 1280, 720);

        state.moveByDelta(13f, 17f, 1280, 720);
        assertEquals(13f, state.getX(), EPS);
        assertEquals(17f, state.getY(), EPS);
    }

    @Test
    public void moveByDelta_appliesSpeedMultiplier() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(100f, 100f, 1280, 720);

        state.moveByDelta(10f, -20f, 1280, 720, 2.5f);
        assertEquals(125f, state.getX(), EPS);
        assertEquals(50f, state.getY(), EPS);

        state.moveByDelta(10f, 10f, 1280, 720, 0.5f);
        assertEquals(130f, state.getX(), EPS);
        assertEquals(55f, state.getY(), EPS);
    }

    @Test
    public void initializeCenter_placesCursorAtVideoMiddle() {
        LocalCursorState state = new LocalCursorState();

        state.initializeCenter(1920, 1080);
        assertEquals(960f, state.getX(), EPS);
        assertEquals(540f, state.getY(), EPS);
        assertTrue(state.isPositionValid());
    }

    @Test
    public void shortConvertions_truncateForHostProtocol() {
        LocalCursorState state = new LocalCursorState();
        state.setPosition(100.9f, 719.5f, 1280, 720);

        assertEquals(100, state.getShortX());
        assertEquals(719, state.getShortY());
    }

    @Test
    public void shouldActivateShortcut_initialKeyDownOnly() {
        assertTrue(LocalCursorState.shouldActivateShortcut(true, 0));
        assertFalse(LocalCursorState.shouldActivateShortcut(false, 0)); // key up
        assertFalse(LocalCursorState.shouldActivateShortcut(true, 1));  // auto-repeat
        assertFalse(LocalCursorState.shouldActivateShortcut(true, 15)); // auto-repeat
    }
}
