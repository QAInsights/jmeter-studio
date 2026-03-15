package com.qainsights.jmeter.themes;

import org.apache.jmeter.gui.plugin.MenuCreator;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.MenuElement;
import java.awt.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMeter plugin entry point. Implements {@link MenuCreator} to add theme items
 * to the Options menu.
 *
 * <p>On plugin load, this initializes the {@link ThemeManager}, registers all discovered
 * themes, and applies the saved theme. Menu items for theme switching are added under Options.</p>
 */
public class ThemePlugin implements MenuCreator {

    private static final Logger logger = LoggerFactory.getLogger(ThemePlugin.class);
    private static final String THEME_PREFIX = "JMeter Studio";
    private JMenu themeMenu;

    // Static initializer to set icon properties BEFORE JMeter loads icons
    static {
        ThemeManager.initialize();
        ThemeManager.setIconPropertiesEarly();
    }

    public ThemePlugin() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            ThemeManager.applyStartupTheme();
        });
    }

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if (location == MENU_LOCATION.OPTIONS) {
            return new JMenuItem[] { createThemeMenu() };
        }
        return new JMenuItem[0];
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menu) {
        return false;
    }

    @Override
    public void localeChanged() {
        // No locale-sensitive strings in theme names
    }

    /**
     * Creates the "JMeter Studio" submenu with a radio button for each available theme,
     * plus a "Restore Default" option.
     */
    private JMenu createThemeMenu() {
        themeMenu = new JMenu(THEME_PREFIX);
        ButtonGroup group = new ButtonGroup();
        String activeThemeId = ThemeManager.getActiveThemeId();

        // "Restore Default" option to switch back to JMeter's default theme
        JRadioButtonMenuItem restoreItem = new JRadioButtonMenuItem("Restore Default");
        restoreItem.setSelected("default".equals(activeThemeId));
        restoreItem.addActionListener(e -> onRestoreDefault());
        group.add(restoreItem);
        themeMenu.add(restoreItem);

        themeMenu.add(new JSeparator());

        // Add each registered theme
        for (ThemeDescriptor descriptor : ThemeManager.getAvailableThemes().values()) {
            String label = descriptor.getDisplayName();
            if (descriptor.isPro()) {
                label += " \u2605"; // Star marker for Pro themes
            }

            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
            item.setSelected(descriptor.getId().equals(activeThemeId));
            item.addActionListener(e -> onThemeSelected(descriptor));
            group.add(item);
            themeMenu.add(item);
        }

        return themeMenu;
    }

    /**
     * Handles theme selection from the menu.
     */
    private void onThemeSelected(ThemeDescriptor descriptor) {
        if (descriptor.isPro()) {
            JOptionPane.showMessageDialog(null,
                    "'" + descriptor.getDisplayName() + "' is a premium theme.\n"
                    + "Please install JMeter Studio Pro to unlock it.",
                    "JMeter Studio Pro",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean applied = ThemeManager.applyTheme(descriptor.getId());
        if (applied) {
            promptRestart(String.format("%s Theme '%s' has been applied.", THEME_PREFIX, descriptor.getDisplayName()));
        }
    }

    /**
     * Restores JMeter's default theme and clears Aura toolbar icons.
     */
    private void onRestoreDefault() {
        ThemeManager.restoreDefault();
        promptRestart("Default JMeter theme restored.");
    }

    /**
     * Prompts the user to restart JMeter for the best experience.
     */
    private void promptRestart(String message) {
        int result = JOptionPane.showConfirmDialog(null,
                message + "\n"
                + "For the best experience, we recommend restarting JMeter.\n\n"
                + "Restart JMeter now?",
                "JMeter Studio",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            triggerRestart();
        }
    }

    /**
     * Trigger JMeter restart via action system.
     */
    private void triggerRestart() {
        try {
            Class<?> actionRouter = Class.forName("org.apache.jmeter.gui.action.ActionRouter");
            Object router = actionRouter.getMethod("getInstance").invoke(null);

            Class<?> actionNames = Class.forName("org.apache.jmeter.gui.action.ActionNames");
            String restartAction = (String) actionNames.getField("RESTART").get(null);

            ActionEvent restartEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, restartAction);
            actionRouter.getMethod("doActionNow", ActionEvent.class).invoke(router, restartEvent);
        } catch (Exception ex) {
            logger.error("{} Could not trigger restart: {}", THEME_PREFIX, ex.getMessage());
        }
    }
}
