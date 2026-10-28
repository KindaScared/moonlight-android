package com.limelight.ui;

import android.content.Context;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class StreamContainerInputConnectionTest {
    private Context ctx;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void commitText_isForwarded_whenEnabled() {
        StreamContainer view = new StreamContainer(ctx, null);
        StreamContainer.InputCallbacks cb = Mockito.mock(StreamContainer.InputCallbacks.class);
        view.setInputCallbacks(cb);
        view.setCommitTextEnabled(true);

        EditorInfo ei = new EditorInfo();
        InputConnection ic = view.onCreateInputConnection(ei);
        assertNotNull("InputConnection should be created when commitText is enabled", ic);

        ic.commitText("hello", 1);

        verify(cb, times(1)).handleCommitText("hello");
    }

    @Test
    public void deleteSurroundingText_isForwardedAsBackspace_whenEnabled() {
        StreamContainer view = new StreamContainer(ctx, null);
        StreamContainer.InputCallbacks cb = Mockito.mock(StreamContainer.InputCallbacks.class);
        view.setInputCallbacks(cb);
        view.setCommitTextEnabled(true);

        InputConnection ic = view.onCreateInputConnection(new EditorInfo());
        assertNotNull(ic);

        ic.deleteSurroundingText(1, 0);

        verify(cb, times(1)).handleDeleteSurroundingText(1, 0);
    }

    @Test
    public void commitText_notForwarded_whenDisabled() {
        StreamContainer view = new StreamContainer(ctx, null);
        StreamContainer.InputCallbacks cb = Mockito.mock(StreamContainer.InputCallbacks.class);
        view.setInputCallbacks(cb);
        view.setCommitTextEnabled(false);

        InputConnection ic = view.onCreateInputConnection(new EditorInfo());
        // Falls back to the default SurfaceView connection (a plain BaseInputConnection
        // that does not forward to our callbacks).
        if (ic != null && !(ic instanceof BaseInputConnection)) {
            ic.commitText("hello", 1);
        }

        verify(cb, never()).handleCommitText(any());
        verify(cb, never()).handleDeleteSurroundingText(anyInt(), anyInt());
    }

    @Test
    public void setCommitTextEnabled_makesViewAnImeTarget() {
        StreamContainer view = new StreamContainer(ctx, null);

        // Before enabling, the view should not advertise itself as a text editor.
        assertFalse("View should not be a text editor before commitText is enabled",
                view.onCheckIsTextEditor());

        view.setCommitTextEnabled(true);

        // After enabling, the view must be focusable in touch mode and report itself as a
        // text editor so the system IME can target it (otherwise the soft keyboard never
        // appears). This mirrors StreamView.setCommitTextEnabled() behaviour.
        assertTrue("View must be focusable in touch mode when commitText is enabled",
                view.isFocusableInTouchMode());
        assertTrue("View must report itself as a text editor when commitText is enabled",
                view.onCheckIsTextEditor());
    }
}
