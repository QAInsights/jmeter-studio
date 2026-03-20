package com.qainsights.jmeter.themes;

import com.formdev.flatlaf.FlatPropertiesLaf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ThemeLaf extends FlatPropertiesLaf {

    private final ThemeDescriptor descriptor;
    private static final String THEME_PREFIX = "JMeter Studio";
    private static final Logger logger = LoggerFactory.getLogger(ThemeLaf.class);

    public ThemeLaf(ThemeDescriptor descriptor) {
        super(descriptor.getDisplayName(), loadProperties(descriptor));
        this.descriptor = descriptor;
    }

    public ThemeLaf() {
        this(getActiveThemeDescriptor());
    }

    private static ThemeDescriptor getActiveThemeDescriptor() {
        String activeThemeId = ThemeManager.getActiveThemeId();
        if ("default".equals(activeThemeId)) {
            throw new IllegalStateException(THEME_PREFIX + " Cannot instantiate ThemeLaf when default JMeter theme is active");
        }
        ThemeDescriptor descriptor = ThemeManager.getTheme(activeThemeId);
        if (descriptor == null) {
            throw new IllegalStateException(String.format("%s Active theme not found: %s", THEME_PREFIX, activeThemeId));
        }
        return descriptor;
    }

    @Override
    public String getName() {
        return descriptor.getDisplayName();
    }

    @Override
    public String getDescription() {
        return THEME_PREFIX + ": " + descriptor.getDisplayName();
    }

    @Override
    public boolean isDark() {
        return descriptor.isDark();
    }

    private static Properties loadProperties(ThemeDescriptor descriptor) {
        Properties props = new Properties();
        String path = descriptor.getPropertiesPath();
        if (path != null && !path.isEmpty()) {
            try (InputStream is = ThemeLaf.class.getClassLoader().getResourceAsStream(path)) {
                if (is != null) {
                    props.load(is);
                } else {
                    logger.error("{} Could not find theme properties: {}", THEME_PREFIX, path);
                }
            } catch (IOException e) {
                logger.error("{} Error loading theme properties: {}", THEME_PREFIX, e.getMessage());
            }
        }
        return props;
    }
}
