package com.qainsights.jmeter.themes;

import com.formdev.flatlaf.FlatPropertiesLaf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic FlatLaf subclass that loads theme-specific properties at runtime.
 * Works for both dark and light themes — the base class is chosen from the descriptor.
 */
public class ThemeLaf extends FlatPropertiesLaf {

    private final ThemeDescriptor descriptor;
    private static final Logger logger = LoggerFactory.getLogger(ThemeLaf.class);

    public ThemeLaf(ThemeDescriptor descriptor) {
        super(descriptor.getDisplayName(), loadProperties(descriptor));
        this.descriptor = descriptor;
    }

    /**
     * No-arg constructor required by JMeter's zoom functionality.
     * Re-instantiates the currently active theme from ThemeManager.
     */
    public ThemeLaf() {
        this(getActiveThemeDescriptor());
    }

    /**
     * Retrieves the currently active theme descriptor from ThemeManager.
     */
    private static ThemeDescriptor getActiveThemeDescriptor() {
        String activeThemeId = ThemeManager.getActiveThemeId();
        if ("default".equals(activeThemeId)) {
            throw new IllegalStateException("Cannot instantiate ThemeLaf when default JMeter theme is active");
        }
        ThemeDescriptor descriptor = ThemeManager.getTheme(activeThemeId);
        if (descriptor == null) {
            throw new IllegalStateException("Active theme not found: " + activeThemeId);
        }
        return descriptor;
    }

    @Override
    public String getName() {
        return descriptor.getDisplayName();
    }

    @Override
    public String getDescription() {
        return "Aura Theme: " + descriptor.getDisplayName();
    }

    @Override
    public boolean isDark() {
        return descriptor.isDark();
    }

    /**
     * Loads the .properties file for the given theme descriptor from classpath.
     */
    private static Properties loadProperties(ThemeDescriptor descriptor) {
        Properties props = new Properties();
        String path = descriptor.getPropertiesPath();
        if (path != null && !path.isEmpty()) {
            try (InputStream is = ThemeLaf.class.getClassLoader().getResourceAsStream(path)) {
                if (is != null) {
                    props.load(is);
                } else {
                    logger.error("[AuraTheme] Could not find theme properties: " + path);
                }
            } catch (IOException e) {
                logger.error("[AuraTheme] Error loading theme properties: " + e.getMessage());
            }
        }
        return props;
    }
}
