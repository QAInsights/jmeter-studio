package com.qainsights.jmeter.themes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ThemeLaf}.
 *
 * <p>These tests exercise ThemeLaf using a descriptor with a {@code null}
 * propertiesPath (so no classpath resource is needed) and assert the
 * delegating behaviour of the overridden methods. No display or JMeter
 * runtime is required.</p>
 */
class ThemeLafTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ThemeDescriptor darkDescriptor() {
        ThemeDescriptor td = new ThemeDescriptor();
        td.setId("test-dark");
        td.setDisplayName("Test Dark");
        td.setDark(true);
        td.setPro(false);
        // null propertiesPath → loadProperties() returns an empty Properties object
        return td;
    }

    private ThemeDescriptor lightDescriptor() {
        ThemeDescriptor td = new ThemeDescriptor();
        td.setId("test-light");
        td.setDisplayName("Test Light");
        td.setDark(false);
        td.setPro(false);
        return td;
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    void testConstructor_withNullPropertiesPath_doesNotThrow() {
        assertDoesNotThrow(() -> new ThemeLaf(darkDescriptor()),
                "Constructing ThemeLaf with null propertiesPath should not throw");
    }

    @Test
    void testConstructor_withEmptyPropertiesPath_doesNotThrow() {
        ThemeDescriptor td = darkDescriptor();
        td.setPropertiesPath("");

        assertDoesNotThrow(() -> new ThemeLaf(td));
    }

    // ── getName() ─────────────────────────────────────────────────────────────

    @Test
    void testGetName_returnsDarkDisplayName() {
        ThemeLaf laf = new ThemeLaf(darkDescriptor());
        assertEquals("Test Dark", laf.getName());
    }

    @Test
    void testGetName_returnsLightDisplayName() {
        ThemeLaf laf = new ThemeLaf(lightDescriptor());
        assertEquals("Test Light", laf.getName());
    }

    // ── getDescription() ─────────────────────────────────────────────────────

    @Test
    void testGetDescription_startsWithPrefix() {
        ThemeLaf laf = new ThemeLaf(darkDescriptor());
        assertTrue(laf.getDescription().startsWith("JMeter Studio"),
                "Description should start with 'JMeter Studio'");
    }

    @Test
    void testGetDescription_includesDisplayName() {
        ThemeLaf laf = new ThemeLaf(darkDescriptor());
        assertTrue(laf.getDescription().contains("Test Dark"),
                "Description should include the theme's display name");
    }

    // ── isDark() ─────────────────────────────────────────────────────────────

    @Test
    void testIsDark_propagatesFromDescriptor_true() {
        ThemeLaf laf = new ThemeLaf(darkDescriptor());
        assertTrue(laf.isDark(), "ThemeLaf.isDark() should return true for a dark descriptor");
    }

    @Test
    void testIsDark_propagatesFromDescriptor_false() {
        ThemeLaf laf = new ThemeLaf(lightDescriptor());
        assertFalse(laf.isDark(), "ThemeLaf.isDark() should return false for a light descriptor");
    }

    // ── Descriptor with a missing-but-non-null propertiesPath ─────────────────

    @Test
    void testConstructor_withNonExistentPropertiesPath_doesNotThrow() {
        // If the resource is absent, loadProperties() logs an error but returns
        // an empty Properties — the constructor must not throw.
        ThemeDescriptor td = darkDescriptor();
        td.setPropertiesPath("does/not/exist.properties");

        assertDoesNotThrow(() -> new ThemeLaf(td),
                "Missing properties file should be gracefully handled, not throw");
    }
}
