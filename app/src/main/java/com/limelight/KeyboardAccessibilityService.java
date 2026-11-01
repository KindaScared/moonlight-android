package com.limelight;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

public class KeyboardAccessibilityService extends AccessibilityService {

    //不屏蔽的按键列表
    private final static List BLACKLIST_KEYS = Arrays.asList(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_POWER
    );

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
//        Toast.makeText(getApplicationContext(),"scancode:"+event.getScanCode()+",code:"+event.getKeyCode(),Toast.LENGTH_LONG).show();
        //主要解决系统自带快捷键在pc端无法使用问题 home键 scancode=172 code- 3
        if (Game.instance != null && Game.instance.connected && !BLACKLIST_KEYS.contains(keyCode)) {

            if (action == KeyEvent.ACTION_DOWN) {
                Game.instance.handleKeyDown(normalizeEscape(event));
                return true;
            } else if (action == KeyEvent.ACTION_UP) {
                Game.instance.handleKeyUp(normalizeEscape(event));
                return true;
            }
        }

        return super.onKeyEvent(event);
    }

    // Normalizes a physical-keyboard Escape event before forwarding it to the stream.
    //
    // Android already reports a correctly-mapped Escape as KEYCODE_ESCAPE; such events are
    // returned untouched so their real device id, scan code, source and modifier state
    // survive. The previous implementation rebuilt every scan-code-1 event with the
    // two-argument KeyEvent constructor, which uses KeyCharacterMap.VIRTUAL_KEYBOARD
    // (device id -1) and zeroes the scan code/source. That stripped event is rejected by
    // Game.handleKeyDown()/handleKeyUp() when "Ignore synthetic events" is enabled (they
    // drop events with deviceId <= 0) and loses metadata in every case.
    //
    // The only rewrite performed here is the known Xiaomi-tablet mis-map where the Escape
    // key (Linux scan code 1 / KEY_ESC) is reported as KEYCODE_BACK. That rewrite is
    // restricted to keyboard-source events and preserves all other event metadata, so the
    // normalized event keeps a real device id (deviceId > 0) and is therefore not treated
    // as synthetic. Arbitrary key codes are never remapped to Escape.
    static KeyEvent normalizeEscape(KeyEvent event) {
        // Already a correct Escape: never replace it.
        if (event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
            return event;
        }

        // Known Xiaomi mis-map: Escape (Linux scan code 1) reported as KEYCODE_BACK.
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK &&
                event.getScanCode() == 1 &&
                (event.getSource() & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
            return new KeyEvent(
                    event.getDownTime(),
                    event.getEventTime(),
                    event.getAction(),
                    KeyEvent.KEYCODE_ESCAPE,
                    event.getRepeatCount(),
                    event.getMetaState(),
                    event.getDeviceId(),
                    event.getScanCode(),
                    event.getFlags(),
                    event.getSource());
        }

        // Any other key (including a legitimate physical Back with a non-1 scan code): unchanged.
        return event;
    }

    @Override
    public void onServiceConnected() {
        LimeLog.info("Keyboard service is connected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.packageNames = new String[] { BuildConfig.APPLICATION_ID };
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
//        LimeLog.info("onAccessibilityEvent:"+accessibilityEvent.toString());
    }
    @Override
    public void onInterrupt() {

    }

}