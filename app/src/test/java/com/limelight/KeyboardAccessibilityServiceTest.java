package com.limelight;

import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link KeyboardAccessibilityService#normalizeEscape(KeyEvent)}.
 *
 * These verify the targeted Escape fix:
 *  - correctly-mapped KEYCODE_ESCAPE events are passed through unchanged (so their real
 *    device id survives and the event is not treated as synthetic),
 *  - the known Xiaomi mis-map (Escape scan code 1 reported as KEYCODE_BACK) is rewritten to
 *    KEYCODE_ESCAPE while preserving device id, scan code, source and modifier state,
 *  - legitimate physical Back keys and arbitrary keys are never remapped,
 *  - key-down and key-up are normalized identically.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class KeyboardAccessibilityServiceTest {

    private static final long DOWN_TIME = 1000L;
    private static final long EVENT_TIME = 1100L;
    private static final int USB_DEVICE_ID = 7; // a real USB keyboard device id (> 0)

    private static KeyEvent event(int action, int keyCode, int scanCode, int metaState, int source) {
        return new KeyEvent(
                DOWN_TIME, EVENT_TIME, action, keyCode, 0, metaState,
                USB_DEVICE_ID, scanCode, 0, source);
    }

    @Test
    public void normalUsbEscape_isPassedThroughUnchanged() {
        KeyEvent esc = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE, 1, 0, InputDevice.SOURCE_KEYBOARD);

        KeyEvent out = KeyboardAccessibilityService.normalizeEscape(esc);

        assertSame("Already-correct Escape must not be replaced", esc, out);
        assertEquals(KeyEvent.KEYCODE_ESCAPE, out.getKeyCode());
        assertEquals(USB_DEVICE_ID, out.getDeviceId());
        assertEquals(1, out.getScanCode());
    }

    @Test
    public void misMappedXiaomiEscape_isNormalizedToEscapePreservingMetadata() {
        KeyEvent back = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 1, 0, InputDevice.SOURCE_KEYBOARD);

        KeyEvent out = KeyboardAccessibilityService.normalizeEscape(back);

        assertEquals(KeyEvent.KEYCODE_ESCAPE, out.getKeyCode());
        // Metadata that keeps the event from being dropped by the synthetic-event filter.
        assertEquals(USB_DEVICE_ID, out.getDeviceId());
        assertEquals(1, out.getScanCode());
        assertEquals(KeyEvent.ACTION_DOWN, out.getAction());
        assertEquals(DOWN_TIME, out.getDownTime());
        assertEquals(EVENT_TIME, out.getEventTime());
        assertTrue((out.getSource() & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD);
    }

    @Test
    public void normalNonEscapeKey_isPassedThroughUnchanged() {
        KeyEvent keyA = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, 30, 0, InputDevice.SOURCE_KEYBOARD);

        KeyEvent out = KeyboardAccessibilityService.normalizeEscape(keyA);

        assertSame(keyA, out);
        assertEquals(KeyEvent.KEYCODE_A, out.getKeyCode());
    }

    @Test
    public void legitimateBackKey_isNotRemapped() {
        // A real physical Back key has Linux scan code 158, not 1.
        KeyEvent back = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 158, 0, InputDevice.SOURCE_KEYBOARD);

        KeyEvent out = KeyboardAccessibilityService.normalizeEscape(back);

        assertSame("A legitimate Back key must NOT be turned into Escape", back, out);
        assertEquals(KeyEvent.KEYCODE_BACK, out.getKeyCode());
    }

    @Test
    public void misMappedBackFromNonKeyboardSource_isNotRemapped() {
        // KEYCODE_BACK + scan code 1 but NOT a keyboard source: do not assume it is Escape.
        KeyEvent back = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 1, 0, InputDevice.SOURCE_GAMEPAD);

        KeyEvent out = KeyboardAccessibilityService.normalizeEscape(back);

        assertSame(back, out);
        assertEquals(KeyEvent.KEYCODE_BACK, out.getKeyCode());
    }

    @Test
    public void downAndUpAreNormalizedIdentically() {
        KeyEvent down = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 1, 0, InputDevice.SOURCE_KEYBOARD);
        KeyEvent up = event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 1, 0, InputDevice.SOURCE_KEYBOARD);

        KeyEvent outDown = KeyboardAccessibilityService.normalizeEscape(down);
        KeyEvent outUp = KeyboardAccessibilityService.normalizeEscape(up);

        assertEquals(KeyEvent.KEYCODE_ESCAPE, outDown.getKeyCode());
        assertEquals(KeyEvent.KEYCODE_ESCAPE, outUp.getKeyCode());
        assertEquals(KeyEvent.ACTION_DOWN, outDown.getAction());
        assertEquals(KeyEvent.ACTION_UP, outUp.getAction());
        assertEquals(outDown.getDeviceId(), outUp.getDeviceId());
        assertEquals(outDown.getScanCode(), outUp.getScanCode());
    }

    @Test
    public void shiftModifier_isPreservedThroughNormalization() {
        KeyEvent shiftEsc = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 1,
                KeyEvent.META_SHIFT_ON, InputDevice.SOURCE_KEYBOARD);

        KeyEvent out = KeyboardAccessibilityService.normalizeEscape(shiftEsc);

        assertEquals(KeyEvent.KEYCODE_ESCAPE, out.getKeyCode());
        assertEquals(KeyEvent.META_SHIFT_ON, out.getMetaState() & KeyEvent.META_SHIFT_ON);
        assertTrue(out.isShiftPressed());
    }

    @Test
    public void ctrlModifier_isPreservedThroughNormalization() {
        KeyEvent ctrlEsc = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 1,
                KeyEvent.META_CTRL_ON, InputDevice.SOURCE_KEYBOARD);

        KeyEvent out = KeyboardAccessibilityService.normalizeEscape(ctrlEsc);

        assertEquals(KeyEvent.KEYCODE_ESCAPE, out.getKeyCode());
        assertEquals(KeyEvent.META_CTRL_ON, out.getMetaState() & KeyEvent.META_CTRL_ON);
        assertTrue(out.isCtrlPressed());
    }

    @Test
    public void normalizedEvent_survivesSyntheticEventFilter() {
        // Game.handleKeyDown()/handleKeyUp() drop events when ignoreSynthEvents is enabled and
        // deviceId <= 0. The normalized event must keep a real (positive) device id.
        KeyEvent back = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 1, 0, InputDevice.SOURCE_KEYBOARD);
        KeyEvent out = KeyboardAccessibilityService.normalizeEscape(back);

        assertTrue("Normalized Escape must have deviceId > 0 to survive the synthetic filter",
                out.getDeviceId() > 0);

        // Document the regression the fix removes: the old two-argument constructor uses
        // KeyCharacterMap.VIRTUAL_KEYBOARD (device id -1), which the filter would reject.
        KeyEvent legacySynthetic = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE);
        assertEquals(KeyCharacterMap.VIRTUAL_KEYBOARD, legacySynthetic.getDeviceId());
        assertTrue("Old synthetic Escape had deviceId <= 0", legacySynthetic.getDeviceId() <= 0);
    }
}
