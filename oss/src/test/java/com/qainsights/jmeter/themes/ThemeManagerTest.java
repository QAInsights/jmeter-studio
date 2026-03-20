package com.qainsights.jmeter.themes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ThemeManager}.
 *
 * <p>Only headless-safe functionality is tested. Methods that touch Swing windows,
 * FlatLaf installation, or JMeter runtime (applyTheme, applyStartupTheme,
 * restoreDefault) are intentionally not covered here — they require a display
 * and JMeter bootstrap that do not exist in the test environment.</p>
 */
class ThemeManagerTest {

    /**
     * Reset ThemeManager's internal state between tests by reflection so that
     * each test starts from a known, fresh baseline.
     */
    @BeforeEach
    void resetThemeManagerState() throws Exception {
        // Reset 'initialized' flag
        Field initializedField = ThemeManager.class.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        initializedField.set(null, false);

        // Clear the themes map
        Field themesField = ThemeManager.class.getDeclaredField("themes");
        themesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ThemeDescriptor> themes = (Map<String, ThemeDescriptor>) themesField.get(null);
        themes.clear();
    }

    // ── initialize() ─────────────────────────────────────────────────────────

    @Test
    void testInitialize_loadsThemesFromClasspath() {
        ThemeManager.initialize();

        Map<String, ThemeDescriptor> themes = ThemeManager.getAvailableThemes();
        assertFalse(themes.isEmpty(), "At least one theme should be loaded from themes.json on the classpath");
    }

    @Test
    void testInitialize_registersAuraDark() {
        ThemeManager.initialize();

        ThemeDescriptor auraDark = ThemeManager.getTheme("aura-dark");
        assertNotNull(auraDark, "aura-dark theme should be registered");
        assertEquals("Aura Dark", auraDark.getDisplayName());
        assertTrue(auraDark.isDark());
        assertFalse(auraDark.isPro());
    }

    @Test
    void testInitialize_loadsAllOssThemes() {
        ThemeManager.initialize();

        Map<String, ThemeDescriptor> themes = ThemeManager.getAvailableThemes();
        // themes.json ships 17 OSS themes
        assertTrue(themes.size() >= 17,
                "Expected at least 17 OSS themes but got: " + themes.size());
    }

    @Test
    void testInitialize_isIdempotent() {
        ThemeManager.initialize();
        int firstCount = ThemeManager.getAvailableThemes().size();

        ThemeManager.initialize(); // second call — must be a no-op
        int secondCount = ThemeManager.getAvailableThemes().size();

        assertEquals(firstCount, secondCount,
                "Calling initialize() twice must not duplicate theme registrations");
    }

    // ── getTheme() ───────────────────────────────────────────────────────────

    @Test
    void testGetTheme_returnsCorrectDescriptor() {
        ThemeManager.initialize();

        ThemeDescriptor td = ThemeManager.getTheme("aura-dark");
        assertNotNull(td);
        assertEquals("aura-dark", td.getId());
    }

    @Test
    void testGetTheme_unknownId_returnsNull() {
        ThemeManager.initialize();

        assertNull(ThemeManager.getTheme("this-theme-does-not-exist"));
    }

    @Test
    void testGetTheme_beforeInitialize_returnsNull() {
        // themes map is empty after the @BeforeEach reset
        assertNull(ThemeManager.getTheme("aura-dark"));
    }

    // ── getActiveThemeId() ───────────────────────────────────────────────────

    @Test
    void testGetActiveThemeId_returnsDefault_whenNoPreferenceSet() {
        // Use Java Preferences directly to clear the key before testing
        java.util.prefs.Preferences prefs =
                java.util.prefs.Preferences.userNodeForPackage(ThemeManager.class);
        prefs.remove("jmeter.studio.theme.active");

        String active = ThemeManager.getActiveThemeId();
        assertEquals("default", active,
                "Without a stored preference the active theme id should be 'default'");
    }

    // ── isStudioThemeActive() ─────────────────────────────────────────────────

    @Test
    void testIsStudioThemeActive_falseForDefault() {
        java.util.prefs.Preferences prefs =
                java.util.prefs.Preferences.userNodeForPackage(ThemeManager.class);
        prefs.remove("jmeter.studio.theme.active");

        ThemeManager.initialize();
        assertFalse(ThemeManager.isStudioThemeActive(),
                "Default theme should not count as a Studio theme being active");
    }

    @Test
    void testIsStudioThemeActive_falseForUnknownId() {
        java.util.prefs.Preferences prefs =
                java.util.prefs.Preferences.userNodeForPackage(ThemeManager.class);
        prefs.put("jmeter.studio.theme.active", "totally-made-up-theme");

        ThemeManager.initialize();
        assertFalse(ThemeManager.isStudioThemeActive(),
                "An unknown theme id should not be reported as active");

        // Cleanup
        prefs.remove("jmeter.studio.theme.active");
    }

    @Test
    void testIsStudioThemeActive_trueForKnownNonDefaultTheme() {
        ThemeManager.initialize();

        java.util.prefs.Preferences prefs =
                java.util.prefs.Preferences.userNodeForPackage(ThemeManager.class);
        prefs.put("jmeter.studio.theme.active", "aura-dark");

        assertTrue(ThemeManager.isStudioThemeActive());

        // Cleanup
        prefs.remove("jmeter.studio.theme.active");
    }

    // ── getAvailableThemes() — unmodifiability ───────────────────────────────

    @Test
    void testGetAvailableThemes_isUnmodifiable() {
        ThemeManager.initialize();

        Map<String, ThemeDescriptor> themes = ThemeManager.getAvailableThemes();
        assertThrows(UnsupportedOperationException.class,
                () -> themes.put("injected", new ThemeDescriptor()),
                "getAvailableThemes() should return an unmodifiable view");
    }

    // ── IJ theme detection via descriptor ────────────────────────────────────

    @Test
    void testNordTheme_isIJTheme() {
        ThemeManager.initialize();

        ThemeDescriptor nord = ThemeManager.getTheme("nord");
        assertNotNull(nord);
        assertTrue(nord.isIJTheme(), "Nord should be recognised as an IJ theme (lafClass is set)");
        assertFalse(nord.isPro());
    }

    @Test
    void testAuraDark_isNotIJTheme() {
        ThemeManager.initialize();

        ThemeDescriptor auraDark = ThemeManager.getTheme("aura-dark");
        assertNotNull(auraDark);
        assertFalse(auraDark.isIJTheme(),
                "Aura Dark uses properties-based LAF, not an IJ class");
    }

    // ── Pro flag ─────────────────────────────────────────────────────────────

    @Test
    void testAllOssThemes_areNotPro() {
        ThemeManager.initialize();

        for (ThemeDescriptor td : ThemeManager.getAvailableThemes().values()) {
            assertFalse(td.isPro(),
                    "OSS theme '" + td.getDisplayName() + "' must not be marked pro");
        }
    }
}
