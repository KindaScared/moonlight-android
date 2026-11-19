package com.limelight.preferences;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.profiles.ProfilesManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

/**
 * Loading/clamping behaviour of the new client-side cursor preferences. Uses
 * the explicit-SharedPreferences overload of readPreferences so the test is
 * independent of stored application state.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class,
        com.limelight.shadows.ShadowGameManager.class})
public class LocalCursorPreferencesTest {

    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        try {
            java.lang.reflect.Field instanceField = ProfilesManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore reflection errors
        }

        context = ApplicationProvider.getApplicationContext();
        deleteRecursively(new File(context.getFilesDir(), "profiles"));

        prefs = context.getSharedPreferences("LocalCursorPrefsTest", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    @Test
    public void speedDefaultsToOneHundred() {
        PreferenceConfiguration config =
                PreferenceConfiguration.readPreferences(context, prefs);

        assertEquals(100,
                prefs.getInt(PreferenceConfiguration.SEEKBAR_LOCAL_CURSOR_SPEED,
                        PreferenceConfiguration.LOCAL_CURSOR_SPEED_DEFAULT), 100);
        assertEquals(1.0f, config.localCursorSpeedMultiplier, 0.0001f);
    }

    @Test
    public void speedIsLoadedAndConvertedToMultiplier() {
        prefs.edit().putInt(PreferenceConfiguration.SEEKBAR_LOCAL_CURSOR_SPEED, 250).commit();

        PreferenceConfiguration config =
                PreferenceConfiguration.readPreferences(context, prefs);

        assertEquals(2.5f, config.localCursorSpeedMultiplier, 0.0001f);
    }

    @Test
    public void speedIsClampedToValidRange() {
        prefs.edit().putInt(PreferenceConfiguration.SEEKBAR_LOCAL_CURSOR_SPEED, -75).commit();
        assertEquals(0.2f,
                PreferenceConfiguration.readPreferences(context, prefs)
                        .localCursorSpeedMultiplier, 0.0001f);

        prefs.edit().putInt(PreferenceConfiguration.SEEKBAR_LOCAL_CURSOR_SPEED, 5000).commit();
        assertEquals(3.0f,
                PreferenceConfiguration.readPreferences(context, prefs)
                        .localCursorSpeedMultiplier, 0.0001f);
    }

    @Test
    public void speedPersistsAcrossReads() {
        prefs.edit().putInt(PreferenceConfiguration.SEEKBAR_LOCAL_CURSOR_SPEED, 150).commit();

        assertEquals(1.5f,
                PreferenceConfiguration.readPreferences(context, prefs)
                        .localCursorSpeedMultiplier, 0.0001f);
        assertEquals(1.5f,
                PreferenceConfiguration.readPreferences(context, prefs)
                        .localCursorSpeedMultiplier, 0.0001f);
    }

    @Test
    public void autoEnableDefaultsToFalse() {
        PreferenceConfiguration config =
                PreferenceConfiguration.readPreferences(context, prefs);

        assertFalse(config.autoEnableLocalCursor);
    }

    @Test
    public void autoEnableIsReloadedCorrectly() {
        prefs.edit().putBoolean(
                PreferenceConfiguration.CHECKBOX_AUTO_ENABLE_LOCAL_CURSOR, true).commit();

        assertTrue(PreferenceConfiguration.readPreferences(context, prefs)
                .autoEnableLocalCursor);

        prefs.edit().putBoolean(
                PreferenceConfiguration.CHECKBOX_AUTO_ENABLE_LOCAL_CURSOR, false).commit();

        assertFalse(PreferenceConfiguration.readPreferences(context, prefs)
                .autoEnableLocalCursor);
    }
}
