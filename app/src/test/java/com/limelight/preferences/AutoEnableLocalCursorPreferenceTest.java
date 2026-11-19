package com.limelight.preferences;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.TestLogSuppressor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Config(sdk = 33, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class AutoEnableLocalCursorPreferenceTest {
    private Context context;

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
    }

    @Test
    public void autoEnableLocalCursor_defaultsToFalse() {
        assertFalse(PreferenceConfiguration.readPreferences(context).autoEnableLocalCursor);
    }

    @Test
    public void autoEnableLocalCursor_isPersistedAndLoaded() {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("checkbox_auto_enable_local_cursor", true)
                .commit();

        assertTrue(PreferenceConfiguration.readPreferences(context).autoEnableLocalCursor);
    }

    @Test
    public void localCursorSize_defaultsTo100Percent() {
        assertEquals(PreferenceConfiguration.LOCAL_CURSOR_SIZE_DEFAULT,
                PreferenceConfiguration.readPreferences(context).localCursorSizePercent);
    }

    @Test
    public void localCursorSize_isPersistedAndLoaded() {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt("seekbar_local_cursor_size", 150)
                .commit();

        assertEquals(150, PreferenceConfiguration.readPreferences(context).localCursorSizePercent);
    }
}