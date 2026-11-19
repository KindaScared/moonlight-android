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

    @Test
    public void setEnabledIfChanged_onlyReportsChangeOnce() {
        LocalCursorState state = new LocalCursorState();

        // First transition reports a change (activation side effects should run).
        assertTrue(state.setEnabledIfChanged(true));
        assertTrue(state.isEnabled());
        // Re-applying the same state reports no change (no duplicate init).
        assertFalse(state.setEnabledIfChanged(true));
        assertTrue(state.isEnabled());

        assertTrue(state.setEnabledIfChanged(false));
        assertFalse(state.isEnabled());
        assertFalse(state.setEnabledIfChanged(false));
        assertFalse(state.isEnabled());
    }

    @Test
    public void autoInitialize_enablesCursorWhenPreferenceTrue() {
        LocalCursorState state = new LocalCursorState();

        // Stream start with the auto-enable preference ON.
        assertTrue(state.setEnabledIfChanged(true));
        assertTrue(state.isEnabled());
    }

    @Test
    public void autoInitialize_keepsCursorDisabledWhenPreferenceFalse() {
        LocalCursorState state = new LocalCursorState();

        // Stream start with the auto-enable preference OFF: already disabled, no change.
        assertFalse(state.setEnabledIfChanged(false));
        assertFalse(state.isEnabled());
    }

    @Test
    public void manualToggleWorksRegardlessOfAutoEnablePreference() {
        // Preference ON: auto-enable, then a manual Ctrl+Alt+Shift+V toggle turns it OFF.
        LocalCursorState onState = new LocalCursorState();
        onState.setEnabledIfChanged(true);
        onState.setEnabledIfChanged(!onState.isEnabled());
        assertFalse(onState.isEnabled());

        // Preference OFF: a manual toggle still turns it ON.
        LocalCursorState offState = new LocalCursorState();
        offState.setEnabledIfChanged(false);
        offState.setEnabledIfChanged(!offState.isEnabled());
        assertTrue(offState.isEnabled());
    }

    @Test
    public void autoEnableReappliedOnResumeOverridesManualToggle() {
        LocalCursorState state = new LocalCursorState();

        // Stream starts with preference ON.
        state.setEnabledIfChanged(true);
        // User manually toggles OFF for the remainder of the session.
        state.setEnabledIfChanged(!state.isEnabled());
        assertFalse(state.isEnabled());

        // Stream resumes with preference ON -> cursor enabled again.
        assertTrue(state.setEnabledIfChanged(true));
        assertTrue(state.isEnabled());
    }

    @Test
    public void autoDisableReappliedOnResumeOverridesManualEnable() {
        LocalCursorState state = new LocalCursorState();

        // Stream starts with preference OFF.
        state.setEnabledIfChanged(false);
        // User manually toggles ON for the remainder of the session.
        state.setEnabledIfChanged(!state.isEnabled());
        assertTrue(state.isEnabled());

        // Stream resumes with preference OFF -> cursor disabled again.
        assertTrue(state.setEnabledIfChanged(false));
        assertFalse(state.isEnabled());
    }
}
