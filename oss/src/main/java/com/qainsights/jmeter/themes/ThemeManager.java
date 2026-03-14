package com.qainsights.jmeter.themes;

import com.formdev.flatlaf.FlatLaf;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Core theme engine — discovers themes from classpath, manages switching, persists preferences.
 *
 * <p>Themes are defined as data in {@code themes.json} files on the classpath (contributed by
 * both OSS and Pro JARs). The engine builds a registry and exposes methods to apply themes
 * at runtime.</p>
 */
public final class ThemeManager {

    private static final String THEMES_MANIFEST = "com/qainsights/jmeter/themes/themes.json";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_KEY = "aura.theme.active";
    private static final String DEFAULT_THEME = "default";

    /** JMeter property key for custom toolbar icons */
    private static final String JMETER_TOOLBAR_ICONS_PROP = "jmeter.toolbar.icons";

    /** JMeter property key for custom tree icons */
    private static final String JMETER_TREE_ICONS_PROP = "jmeter.icons";

    /** All discovered themes, keyed by id */
    private static final Map<String, ThemeDescriptor> themes = new LinkedHashMap<>();

    private static boolean initialized = false;

    private ThemeManager() {
    }

    /**
     * Discover and register all themes from {@code themes.json} manifests on the classpath.
     * Called once during plugin initialization.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        Gson gson = new Gson();
        Type listType = new TypeToken<List<ThemeDescriptor>>() {}.getType();

        try {
            Enumeration<URL> resources = ThemeManager.class.getClassLoader().getResources(THEMES_MANIFEST);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream is = url.openStream();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    List<ThemeDescriptor> descriptors = gson.fromJson(reader, listType);
                    if (descriptors != null) {
                        for (ThemeDescriptor desc : descriptors) {
                            themes.put(desc.getId(), desc);
                            System.out.println("[AuraTheme] Registered theme: " + desc.getDisplayName());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[AuraTheme] Error reading themes manifest: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[AuraTheme] Error scanning for theme manifests: " + e.getMessage());
        }
    }

    /**
     * Returns all available themes, both OSS and Pro.
     */
    public static Map<String, ThemeDescriptor> getAvailableThemes() {
        return Collections.unmodifiableMap(themes);
    }

    /**
     * Returns the theme descriptor by id, or null if not found.
     */
    public static ThemeDescriptor getTheme(String id) {
        return themes.get(id);
    }

    /**
     * Returns the id of the currently saved/active theme.
     */
    public static String getActiveThemeId() {
        return PREFS.get(PREF_KEY, DEFAULT_THEME);
    }

    /**
     * Returns true if an Aura theme is currently active (not "default").
     */
    public static boolean isAuraThemeActive() {
        String activeId = getActiveThemeId();
        return !DEFAULT_THEME.equals(activeId) && themes.containsKey(activeId);
    }

    /**
     * Apply a theme by id. Sets the FlatLaf properties, installs the LaF,
     * updates toolbar icons, and refreshes all windows.
     *
     * @param themeId the theme id to apply
     * @return true if the theme was applied successfully
     */
    public static boolean applyTheme(String themeId) {
        ThemeDescriptor descriptor = themes.get(themeId);
        if (descriptor == null) {
            System.err.println("[AuraTheme] Theme not found: " + themeId);
            return false;
        }

        try {
            // 1. Apply FlatLaf theme
            ThemeLaf laf = new ThemeLaf(descriptor);
            FlatLaf.setup(laf);

            // 2. Set JMeter toolbar icons property so toolbar uses our icons
            if (descriptor.getToolbarPropertiesPath() != null) {
                setJMeterProperty(JMETER_TOOLBAR_ICONS_PROP, descriptor.getToolbarPropertiesPath());
                System.out.println("[AuraTheme] Set toolbar icons: " + descriptor.getToolbarPropertiesPath());
            }

            // 3. Set JMeter tree icons property so tree uses our icons
            if (descriptor.getTreeIconPropertiesPath() != null) {
                setJMeterProperty(JMETER_TREE_ICONS_PROP, descriptor.getTreeIconPropertiesPath());
                System.out.println("[AuraTheme] Set tree icons: " + descriptor.getTreeIconPropertiesPath());
            }

            // 4. Update all existing windows
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
                // Force a complete repaint
                window.repaint();
                // Recursively repaint all children
                repaintAll(window);
            }

            // 5. Trigger toolbar rebuild
            rebuildToolbar();

            // 6. Persist the selection
            PREFS.put(PREF_KEY, themeId);

            System.out.println("[AuraTheme] Applied theme: " + descriptor.getDisplayName());
            return true;
        } catch (Exception e) {
            System.err.println("[AuraTheme] Failed to apply theme '" + themeId + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Restore JMeter's default theme and clear Aura toolbar icons.
     */
    public static void restoreDefault() {
        // Clear the toolbar icons property so JMeter uses its default icons
        clearJMeterProperty(JMETER_TOOLBAR_ICONS_PROP);

        // Clear the tree icons property so JMeter uses its default icons
        clearJMeterProperty(JMETER_TREE_ICONS_PROP);

        // Persist "default" as active theme
        PREFS.put(PREF_KEY, DEFAULT_THEME);

        System.out.println("[AuraTheme] Restored default JMeter theme. Restart required.");
    }

    /**
     * Apply the user's saved theme preference. Called during startup.
     */
    public static void applyStartupTheme() {
        String savedTheme = getActiveThemeId();
        if (DEFAULT_THEME.equals(savedTheme)) {
            // User chose default — don't apply any Aura theme, don't set icon property
            System.out.println("[AuraTheme] Default theme selected, skipping Aura theme application.");
            return;
        }
        if (themes.containsKey(savedTheme)) {
            ThemeDescriptor descriptor = themes.get(savedTheme);

            // Apply the FlatLaf theme
            try {
                ThemeLaf laf = new ThemeLaf(descriptor);
                FlatLaf.setup(laf);

                // Update all existing windows and components
                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                    window.repaint();
                    repaintAll(window);
                }

                System.out.println("[AuraTheme] Startup theme applied: " + descriptor.getDisplayName());
            } catch (Exception e) {
                System.err.println("[AuraTheme] Failed to apply startup theme: " + e.getMessage());
            }
        }
    }

    /**
     * Set icon properties early, before JMeter loads icons.
     * Called from static initializer in ThemePlugin.
     */
    public static void setIconPropertiesEarly() {
        String savedTheme = getActiveThemeId();
        if (DEFAULT_THEME.equals(savedTheme)) {
            return;
        }
        if (themes.containsKey(savedTheme)) {
            ThemeDescriptor descriptor = themes.get(savedTheme);

            // Set toolbar icons property BEFORE JMeter loads toolbar
            if (descriptor.getToolbarPropertiesPath() != null) {
                setJMeterProperty(JMETER_TOOLBAR_ICONS_PROP, descriptor.getToolbarPropertiesPath());
                System.out.println("[AuraTheme] Early set toolbar icons: " + descriptor.getToolbarPropertiesPath());
            }

            // Set tree icons property BEFORE JMeter loads tree
            if (descriptor.getTreeIconPropertiesPath() != null) {
                setJMeterProperty(JMETER_TREE_ICONS_PROP, descriptor.getTreeIconPropertiesPath());
                System.out.println("[AuraTheme] Early set tree icons: " + descriptor.getTreeIconPropertiesPath());
            }
        }
    }

    /**
     * Set a JMeter property using reflection (to avoid compile-time dependency on JMeterUtils).
     */
    private static void setJMeterProperty(String key, String value) {
        try {
            Class<?> jmeterUtils = Class.forName("org.apache.jmeter.util.JMeterUtils");
            jmeterUtils.getMethod("setProperty", String.class, String.class).invoke(null, key, value);
            System.out.println("[AuraTheme] Set JMeter property " + key + " = " + value);
        } catch (Exception e) {
            // Fallback: set system property
            System.setProperty(key, value);
            System.err.println("[AuraTheme] JMeterUtils not available, set system property: " + key);
        }
    }

    /**
     * Clear a JMeter property so the default is used.
     */
    private static void clearJMeterProperty(String key) {
        try {
            Class<?> jmeterUtils = Class.forName("org.apache.jmeter.util.JMeterUtils");
            // JMeterUtils stores properties in a Properties object — remove the key
            java.lang.reflect.Method getPropMethod = jmeterUtils.getMethod("getJMeterProperties");
            Object props = getPropMethod.invoke(null);
            if (props instanceof java.util.Properties) {
                ((java.util.Properties) props).remove(key);
            }
            System.out.println("[AuraTheme] Cleared JMeter property: " + key);
        } catch (Exception e) {
            System.clearProperty(key);
        }
    }

    /**
     * Trigger a toolbar rebuild by firing a locale change event.
     */
    private static void rebuildToolbar() {
        try {
            Class<?> jmeterUtils = Class.forName("org.apache.jmeter.util.JMeterUtils");
            java.lang.reflect.Method fireMethod = null;
            for (java.lang.reflect.Method m : jmeterUtils.getMethods()) {
                if (m.getName().equals("refreshUI")) {
                    fireMethod = m;
                    break;
                }
            }
            if (fireMethod != null) {
                fireMethod.invoke(null);
            }
        } catch (Exception e) {
            System.out.println("[AuraTheme] Toolbar will update on restart: " + e.getMessage());
        }
    }

    /**
     * Recursively repaint all components in a container and force background colors.
     */
    private static void repaintAll(java.awt.Container container) {
        for (java.awt.Component comp : container.getComponents()) {
            // Force opaque and background for all JComponents
            if (comp instanceof javax.swing.JComponent) {
                javax.swing.JComponent jcomp = (javax.swing.JComponent) comp;
                jcomp.setOpaque(true);

                // Set appropriate background based on component type
                if (comp instanceof javax.swing.JEditorPane) {
                    javax.swing.JEditorPane editorPane = (javax.swing.JEditorPane) comp;
                    // Use the theme's EditorPane.background color
                    java.awt.Color editorBg = javax.swing.UIManager.getColor("EditorPane.background");
                    java.awt.Color editorFg = javax.swing.UIManager.getColor("EditorPane.foreground");
                    if (editorBg != null) {
                        jcomp.setBackground(editorBg);
                    }

                    // If displaying HTML, inject dark theme CSS
                    String contentType = editorPane.getContentType();
                    if ("text/html".equals(contentType)) {
                        injectDarkThemeCSS(editorPane, editorBg, editorFg);
                    }
                    System.out.println("[AuraTheme] Set background on JEditorPane: " + editorPane.getName());
                } else if (comp instanceof javax.swing.JTextPane) {
                    java.awt.Color textPaneBg = javax.swing.UIManager.getColor("TextPane.background");
                    if (textPaneBg != null) {
                        jcomp.setBackground(textPaneBg);
                    }
                    System.out.println("[AuraTheme] Set background on JTextPane: " + comp.getName());
                } else if (comp instanceof javax.swing.JTextArea || comp instanceof javax.swing.JTextField) {
                    java.awt.Color textAreaBg = javax.swing.UIManager.getColor("TextArea.background");
                    if (textAreaBg != null) {
                        jcomp.setBackground(textAreaBg);
                    }
                } else if (comp instanceof javax.swing.JScrollPane) {
                    java.awt.Color scrollBg = javax.swing.UIManager.getColor("ScrollPane.background");
                    if (scrollBg != null) {
                        jcomp.setBackground(scrollBg);
                    }
                    System.out.println("[AuraTheme] Set background on JScrollPane: " + comp.getName());
                } else if (comp instanceof javax.swing.JViewport) {
                    java.awt.Color viewportBg = javax.swing.UIManager.getColor("Viewport.background");
                    if (viewportBg != null) {
                        jcomp.setBackground(viewportBg);
                    }
                    System.out.println("[AuraTheme] Set background on JViewport: " + comp.getName());
                } else if (comp instanceof javax.swing.JSplitPane) {
                    java.awt.Color splitBg = javax.swing.UIManager.getColor("SplitPane.background");
                    if (splitBg != null) {
                        jcomp.setBackground(splitBg);
                    }
                    System.out.println("[AuraTheme] Set background on JSplitPane: " + comp.getName());
                } else {
                    java.awt.Color panelBg = javax.swing.UIManager.getColor("Panel.background");
                    if (panelBg != null) {
                        jcomp.setBackground(panelBg);
                    }
                    System.out.println("[AuraTheme] Set background on " + comp.getClass().getSimpleName() + ": " + comp.getName());
                }
            }
            comp.repaint();
            if (comp instanceof java.awt.Container) {
                repaintAll((java.awt.Container) comp);
            }
        }
    }

    /**
     * Inject dark theme CSS into an HTML editor pane.
     */
    private static void injectDarkThemeCSS(javax.swing.JEditorPane editorPane, java.awt.Color bgColor, java.awt.Color fgColor) {
        try {
            String html = editorPane.getText();
            if (html != null && !html.isEmpty()) {
                // Check if we've already injected our CSS
                if (!html.contains("/* Aura Dark Theme CSS */")) {
                    // Convert colors to hex
                    String bgHex = colorToHex(bgColor != null ? bgColor : java.awt.Color.decode("#231d35"));
                    String fgHex = colorToHex(fgColor != null ? fgColor : java.awt.Color.decode("#e8e0f0"));

                    String darkCSS = "<style type=\"text/css\">\n" +
                            "/* Aura Dark Theme CSS */\n" +
                            "body { background-color: " + bgHex + " !important; color: " + fgHex + " !important; }\n" +
                            "table { background-color: " + bgHex + " !important; }\n" +
                            "td, th { background-color: " + bgHex + " !important; color: " + fgHex + " !important; }\n" +
                            "</style>\n";

                    // Insert CSS after <head> tag
                    if (html.contains("<head>")) {
                        html = html.replace("<head>", "<head>" + darkCSS);
                        editorPane.setText(html);
                    } else if (html.contains("<html>")) {
                        html = html.replace("<html>", "<html><head>" + darkCSS + "</head>");
                        editorPane.setText(html);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors during CSS injection
            System.err.println("[AuraTheme] Error injecting CSS: " + e.getMessage());
        }
    }

    /**
     * Convert a Color to hex string.
     */
    private static String colorToHex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
