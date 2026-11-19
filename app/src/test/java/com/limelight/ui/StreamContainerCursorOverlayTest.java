package com.limelight.ui;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.Game;
import com.limelight.preferences.PreferenceConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Verifies the client-side cursor overlay is inserted into the stream container
 * above the rendering surface and cannot intercept input or take focus.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class,
        com.limelight.shadows.ShadowGameManager.class})
public class StreamContainerCursorOverlayTest {

    private Context ctx;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void overlay_isAddedAfterTheSurfaceAndDrawsOnTop() {
        StreamContainer container = new StreamContainer(ctx, null);

        PreferenceConfiguration prefConfig = new PreferenceConfiguration();
        prefConfig.renderMode = 0; // MODE_2D

        container.init(Mockito.mock(Game.class), prefConfig);

        // Surface first, overlay second: in a FrameLayout the last child draws
        // on top of the (window-behind) video surface.
        assertEquals(2, container.getChildCount());
        assertTrue(container.getChildAt(0) instanceof android.view.SurfaceView);
        assertSame(container.getCursorOverlay(), container.getChildAt(1));
        assertTrue(container.getChildAt(1) instanceof CursorOverlayView);
    }

    @Test
    public void overlay_neverInterceptsInputOrTakesFocus() {
        StreamContainer container = new StreamContainer(ctx, null);
        PreferenceConfiguration prefConfig = new PreferenceConfiguration();
        prefConfig.renderMode = 0;

        container.init(Mockito.mock(Game.class), prefConfig);

        CursorOverlayView overlay = container.getCursorOverlay();
        assertNotNull(overlay);
        assertFalse(overlay.isClickable());
        assertFalse(overlay.isFocusable());
        assertFalse(overlay.isFocusableInTouchMode());

        // Hidden until explicitly enabled
        assertFalse(overlay.isOverlayEnabled());
        overlay.setOverlayEnabled(true);
        assertTrue(overlay.isOverlayEnabled());

        overlay.setCursorPosition(123f, 456f);
        assertEquals(123f, overlay.getCursorX(), 0.0001f);
        assertEquals(456f, overlay.getCursorY(), 0.0001f);
    }
}
