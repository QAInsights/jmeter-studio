package com.qainsights.jmeter.themes;

import com.qainsights.jmeter.license.LicenseGuard;
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
    }

    private JMenu createThemeMenu() {
        themeMenu = new JMenu(THEME_PREFIX);
        ButtonGroup group = new ButtonGroup();
        String activeThemeId = ThemeManager.getActiveThemeId();

        JRadioButtonMenuItem restoreItem = new JRadioButtonMenuItem("Restore Default");
        restoreItem.setSelected("default".equals(activeThemeId));
        restoreItem.addActionListener(e -> onRestoreDefault());
        group.add(restoreItem);
        themeMenu.add(restoreItem);

        themeMenu.add(new JSeparator());

        for (ThemeDescriptor descriptor : ThemeManager.getAvailableThemes().values()) {
            String label = descriptor.getDisplayName();
            if (descriptor.isPro()) {
                label += " \u2605";
            }

            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
            item.setSelected(descriptor.getId().equals(activeThemeId));
            item.addActionListener(e -> onThemeSelected(descriptor));
            group.add(item);
            themeMenu.add(item);
        }

        themeMenu.add(new JSeparator());

        if (LicenseGuard.isProLicensed()) {
            JMenuItem licenseItem = new JMenuItem("License: Active \u2713");
            licenseItem.setEnabled(true);
            licenseItem.addActionListener(e -> onShowLicenseInfo());
            themeMenu.add(licenseItem);
        } else {
            JMenuItem activateItem = new JMenuItem("Activate Pro License...");
            activateItem.addActionListener(e -> onActivateLicense());
            themeMenu.add(activateItem);
        }

        return themeMenu;
    }

    private void onThemeSelected(ThemeDescriptor descriptor) {
        if (descriptor.isPro() && !LicenseGuard.isProLicensed()) {
            JOptionPane.showMessageDialog(null,
                    "'" + descriptor.getDisplayName() + "' is a premium theme.\n"
                    + "Please purchase a JMeter Studio Pro license to unlock it.\n"
                    + "Visit https://qainsights.com for more information.",
                    "JMeter Studio Pro",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean applied = ThemeManager.applyTheme(descriptor.getId());
        if (applied) {
            promptRestart(String.format("%s Theme '%s' has been applied.", THEME_PREFIX, descriptor.getDisplayName()));
        }
    }

 
    private void onShowLicenseInfo() {
        LicenseGuard.getLicenseInfo().ifPresent(info -> {
            String expiry = info.isLifetime() ? "Lifetime" : info.getExpiresAt()
                    .map(Object::toString).orElse("Unknown");
            JOptionPane.showMessageDialog(null,
                    "JMeter Studio Pro License\n\n"
                    + "Email: " + info.getEmail() + "\n"
                    + "Edition: " + info.getEdition() + "\n"
                    + "Expires: " + expiry + "\n"
                    + "License ID: " + info.getLicenseId(),
                    "JMeter Studio Pro",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void onActivateLicense() {
        JOptionPane.showMessageDialog(null,
                "To activate JMeter Studio Pro:\n\n"
                + "1. Purchase a license at https://qainsights.com\n"
                + "2. Download the jmeter-studio.license file\n"
                + "3. Place it in your JMeter bin/ directory\n"
                + "4. Restart JMeter\n\n"
                + "Already have a license file? Place it in:\n"
                + "  \u2022 JMETER_HOME/bin/jmeter-studio.license\n"
                + "  \u2022 ~/.jmeter-studio/jmeter-studio.license",
                "Activate JMeter Studio Pro",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void onRestoreDefault() {
        ThemeManager.restoreDefault();
        promptRestart("Default JMeter theme restored.");
    }

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
