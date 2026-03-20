package com.qainsights.jmeter.themes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ThemeDescriptor}.
 */
class ThemeDescriptorTest {

    // ── No-arg constructor + setters ──────────────────────────────────────────

    @Test
    void testNoArgConstructorAndSetters() {
        ThemeDescriptor td = new ThemeDescriptor();
        td.setId("test-id");
        td.setDisplayName("Test Theme");
        td.setPropertiesPath("path/to/theme.properties");
        td.setIconDir("path/to/icons");
        td.setToolbarPropertiesPath("path/to/toolbar.properties");
        td.setTreeIconPropertiesPath("path/to/tree.properties");
        td.setDark(true);
        td.setPro(false);
        td.setLafClass("com.example.SomeLaf");

        assertEquals("test-id", td.getId());
        assertEquals("Test Theme", td.getDisplayName());
        assertEquals("path/to/theme.properties", td.getPropertiesPath());
        assertEquals("path/to/icons", td.getIconDir());
        assertEquals("path/to/toolbar.properties", td.getToolbarPropertiesPath());
        assertEquals("path/to/tree.properties", td.getTreeIconPropertiesPath());
        assertTrue(td.isDark());
        assertFalse(td.isPro());
        assertEquals("com.example.SomeLaf", td.getLafClass());
    }

    // ── Full-arg constructor ──────────────────────────────────────────────────

    @Test
    void testFullArgConstructor() {
        ThemeDescriptor td = new ThemeDescriptor(
                "aura-dark",
                "Aura Dark",
                "path/to/theme.properties",
                "path/to/icons",
                "path/to/toolbar.properties",
                "path/to/tree.properties",
                true,
                false
        );

        assertEquals("aura-dark", td.getId());
        assertEquals("Aura Dark", td.getDisplayName());
        assertEquals("path/to/theme.properties", td.getPropertiesPath());
        assertEquals("path/to/icons", td.getIconDir());
        assertEquals("path/to/toolbar.properties", td.getToolbarPropertiesPath());
        assertEquals("path/to/tree.properties", td.getTreeIconPropertiesPath());
        assertTrue(td.isDark());
        assertFalse(td.isPro());
        // lafClass is not part of positional ctor — should be null
        assertNull(td.getLafClass());
    }

    // ── isIJTheme ─────────────────────────────────────────────────────────────

    @Test
    void testIsIJTheme_whenLafClassSet() {
        ThemeDescriptor td = new ThemeDescriptor();
        td.setLafClass("com.formdev.flatlaf.intellijthemes.FlatNordIJTheme");

        assertTrue(td.isIJTheme());
    }

    @Test
    void testIsIJTheme_whenLafClassNull() {
        ThemeDescriptor td = new ThemeDescriptor();
        // lafClass is null by default after no-arg ctor
        assertFalse(td.isIJTheme());
    }

    @Test
    void testIsIJTheme_whenLafClassEmpty() {
        ThemeDescriptor td = new ThemeDescriptor();
        td.setLafClass("");

        assertFalse(td.isIJTheme());
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void testToString_containsKeyFields() {
        ThemeDescriptor td = new ThemeDescriptor();
        td.setId("aura-dark");
        td.setDisplayName("Aura Dark");
        td.setDark(true);
        td.setPro(false);
        td.setLafClass("com.example.MyLaf");

        String result = td.toString();

        assertTrue(result.contains("aura-dark"), "toString should contain id");
        assertTrue(result.contains("Aura Dark"), "toString should contain displayName");
        assertTrue(result.contains("true"), "toString should contain dark=true");
        assertTrue(result.contains("false"), "toString should contain pro=false");
        assertTrue(result.contains("com.example.MyLaf"), "toString should contain lafClass");
    }

    // ── Boolean flag toggles ──────────────────────────────────────────────────

    @Test
    void testProFlag_toggle() {
        ThemeDescriptor td = new ThemeDescriptor();
        assertFalse(td.isPro());

        td.setPro(true);
        assertTrue(td.isPro());

        td.setPro(false);
        assertFalse(td.isPro());
    }

    @Test
    void testDarkFlag_toggle() {
        ThemeDescriptor td = new ThemeDescriptor();
        assertFalse(td.isDark());

        td.setDark(true);
        assertTrue(td.isDark());

        td.setDark(false);
        assertFalse(td.isDark());
    }

    // ── Null-safety ───────────────────────────────────────────────────────────

    @Test
    void testSettersAcceptNull() {
        ThemeDescriptor td = new ThemeDescriptor();
        // Should not throw
        assertDoesNotThrow(() -> {
            td.setId(null);
            td.setDisplayName(null);
            td.setPropertiesPath(null);
            td.setIconDir(null);
            td.setToolbarPropertiesPath(null);
            td.setTreeIconPropertiesPath(null);
            td.setLafClass(null);
        });

        assertNull(td.getId());
        assertNull(td.getDisplayName());
        assertFalse(td.isIJTheme()); // null lafClass → not IJ theme
    }
}
