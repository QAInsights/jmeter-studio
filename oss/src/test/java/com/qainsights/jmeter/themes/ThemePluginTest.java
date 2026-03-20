package com.qainsights.jmeter.themes;

import org.apache.jmeter.gui.plugin.MenuCreator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ThemePlugin} (MenuCreator interface contract).
 *
 * <p>All tests run with {@code java.awt.headless=true}. Construction of
 * ThemePlugin involves a static initializer that calls
 * {@code ThemeManager.initialize()} — safe in headless mode because it only
 * reads classpath resources. The constructor schedules
 * {@code applyStartupTheme()} via {@code invokeLater}; that call is harmless
 * in headless mode (it will either no-op or log an error).</p>
 */
class ThemePluginTest {

    private static ThemePlugin plugin;

    @BeforeAll
    static void createPlugin() throws Exception {
        // Force headless to avoid display requirement
        System.setProperty("java.awt.headless", "true");

        // Create the plugin on the EDT to satisfy any Swing requirements
        AtomicReference<ThemePlugin> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new ThemePlugin()));
        plugin = ref.get();
    }

    // ── getMenuItemsAtLocation ────────────────────────────────────────────────

    @Test
    void testGetMenuItemsAtLocation_options_returnsOneItem() throws Exception {
        AtomicReference<JMenuItem[]> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
                result.set(plugin.getMenuItemsAtLocation(MenuCreator.MENU_LOCATION.OPTIONS)));

        JMenuItem[] items = result.get();
        assertNotNull(items);
        assertEquals(1, items.length,
                "OPTIONS location should return exactly one menu item");
    }

    @Test
    void testGetMenuItemsAtLocation_options_itemIsJMenu() throws Exception {
        AtomicReference<JMenuItem[]> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
                result.set(plugin.getMenuItemsAtLocation(MenuCreator.MENU_LOCATION.OPTIONS)));

        JMenuItem item = result.get()[0];
        assertInstanceOf(JMenu.class, item,
                "The item returned for OPTIONS should be a JMenu (submenu)");
    }

    @Test
    void testGetMenuItemsAtLocation_options_menuLabelIsJMeterStudio() throws Exception {
        AtomicReference<JMenuItem[]> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
                result.set(plugin.getMenuItemsAtLocation(MenuCreator.MENU_LOCATION.OPTIONS)));

        JMenuItem item = result.get()[0];
        assertEquals("JMeter Studio", item.getText());
    }

    @Test
    void testGetMenuItemsAtLocation_edit_returnsEmpty() throws Exception {
        AtomicReference<JMenuItem[]> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
                result.set(plugin.getMenuItemsAtLocation(MenuCreator.MENU_LOCATION.EDIT)));

        assertEquals(0, result.get().length,
                "Non-OPTIONS location should return empty array");
    }

    @Test
    void testGetMenuItemsAtLocation_run_returnsEmpty() throws Exception {
        AtomicReference<JMenuItem[]> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
                result.set(plugin.getMenuItemsAtLocation(MenuCreator.MENU_LOCATION.RUN)));

        assertEquals(0, result.get().length);
    }

    // ── getTopLevelMenus ─────────────────────────────────────────────────────

    @Test
    void testGetTopLevelMenus_returnsEmptyArray() throws Exception {
        AtomicReference<JMenu[]> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
                result.set(plugin.getTopLevelMenus()));

        assertNotNull(result.get());
        assertEquals(0, result.get().length,
                "ThemePlugin should not register any top-level menus");
    }

    // ── localeChanged ────────────────────────────────────────────────────────

    @Test
    void testLocaleChanged_menuElement_returnsFalse() throws Exception {
        AtomicReference<Boolean> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            // Pass a stub MenuElement (the menu itself)
            MenuElement stub = new JMenu("stub");
            result.set(plugin.localeChanged(stub));
        });

        assertFalse(result.get(),
                "localeChanged(MenuElement) should always return false");
    }

    @Test
    void testLocaleChanged_noArg_doesNotThrow() {
        assertDoesNotThrow(() ->
                SwingUtilities.invokeAndWait(() -> plugin.localeChanged()),
                "localeChanged() should not throw");
    }

    // ── Theme menu contents ───────────────────────────────────────────────────

    @Test
    void testThemeMenu_containsRestoreDefaultItem() throws Exception {
        AtomicReference<JMenu> menuRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JMenuItem[] items = plugin.getMenuItemsAtLocation(MenuCreator.MENU_LOCATION.OPTIONS);
            menuRef.set((JMenu) items[0]);
        });

        JMenu menu = menuRef.get();
        boolean hasRestoreDefault = false;
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && "Restore Default".equals(item.getText())) {
                hasRestoreDefault = true;
                break;
            }
        }
        assertTrue(hasRestoreDefault, "Theme menu should contain a 'Restore Default' item");
    }

    @Test
    void testThemeMenu_hasMoreThanOneItem() throws Exception {
        AtomicReference<JMenu> menuRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JMenuItem[] items = plugin.getMenuItemsAtLocation(MenuCreator.MENU_LOCATION.OPTIONS);
            menuRef.set((JMenu) items[0]);
        });

        // Must have at least: Restore Default + separator + at least one theme
        assertTrue(menuRef.get().getItemCount() > 2,
                "Theme menu must contain Restore Default, a separator, and at least one theme entry");
    }
}
