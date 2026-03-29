package com.qainsights.jmeter.themes;

import com.formdev.flatlaf.FlatLaf;
import com.google.gson.Gson;
import com.qainsights.jmeter.license.LicenseGuard;
import com.google.gson.reflect.TypeToken;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private static final String THEME_PREFIX = "JMeter Studio";
    private static final String THEMES_MANIFEST = "com/qainsights/jmeter/themes/themes.json";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_KEY = "jmeter.studio.theme.active";
    private static final String DEFAULT_THEME = "default";

    private static final String JMETER_TOOLBAR_ICONS_PROP = "jmeter.toolbar.icons";

    private static final String JMETER_TREE_ICONS_PROP = "jmeter.icons";

    private static final Map<String, ThemeDescriptor> themes = new LinkedHashMap<>();

    private static boolean initialized = false;

    private static String originalLafClassName = null;

    private ThemeManager() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (originalLafClassName == null) {
            javax.swing.LookAndFeel currentLaf = javax.swing.UIManager.getLookAndFeel();
            if (currentLaf != null) {
                originalLafClassName = currentLaf.getClass().getName();
                logger.info("{} Captured original LAF: {}", THEME_PREFIX, originalLafClassName);
            }
        }

        Gson gson = new Gson();
        Type listType = new TypeToken<List<ThemeDescriptor>>() {}.getType();

        try {
            ClassLoader cl = ThemeManager.class.getClassLoader();
            Enumeration<URL> resources = cl.getResources(THEMES_MANIFEST);
            if (!resources.hasMoreElements()) {
                cl = Thread.currentThread().getContextClassLoader();
                if (cl != null) {
                    resources = cl.getResources(THEMES_MANIFEST);
                }
            }
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream is = url.openStream();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    List<ThemeDescriptor> descriptors = gson.fromJson(reader, listType);
                    if (descriptors != null) {
                        for (ThemeDescriptor desc : descriptors) {
                            themes.put(desc.getId(), desc);
                            logger.info("{} Registered theme: {}", THEME_PREFIX, desc.getDisplayName());
                        }
                    }
                } catch (IOException e) {
                    logger.error("{} Error reading themes manifest: {}", THEME_PREFIX, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("{} Error scanning for theme manifests: {}", THEME_PREFIX, e.getMessage());
        }
    }

    public static Map<String, ThemeDescriptor> getAvailableThemes() {
        return Collections.unmodifiableMap(themes);
    }

    public static ThemeDescriptor getTheme(String id) {
        return themes.get(id);
    }

    public static String getActiveThemeId() {
        return PREFS.get(PREF_KEY, DEFAULT_THEME);
    }

    public static boolean isStudioThemeActive() {
        String activeId = getActiveThemeId();
        return !DEFAULT_THEME.equals(activeId) && themes.containsKey(activeId);
    }
    public static boolean applyTheme(String themeId) {
        ThemeDescriptor descriptor = themes.get(themeId);
        if (descriptor == null) {
            logger.error("{} Theme not found: {}", THEME_PREFIX, themeId);
            return false;
        }

        try {
            if (descriptor.isIJTheme()) {
                applyIJTheme(descriptor);
            } else {
                ThemeLaf laf = new ThemeLaf(descriptor);
                FlatLaf.setup(laf);
            }

            if (descriptor.getToolbarPropertiesPath() != null) {
                setJMeterProperty(JMETER_TOOLBAR_ICONS_PROP, descriptor.getToolbarPropertiesPath());
                logger.info("{} Set toolbar icons: {}", THEME_PREFIX, descriptor.getToolbarPropertiesPath());
            } else {
                clearJMeterProperty(JMETER_TOOLBAR_ICONS_PROP);
                logger.info("{} Cleared toolbar icons (using JMeter defaults)", THEME_PREFIX);
            }

            if (descriptor.getTreeIconPropertiesPath() != null) {
                setJMeterProperty(JMETER_TREE_ICONS_PROP, descriptor.getTreeIconPropertiesPath());
                logger.info("{} Set tree icons: {}", THEME_PREFIX, descriptor.getTreeIconPropertiesPath());
            } else {
                clearJMeterProperty(JMETER_TREE_ICONS_PROP);
                logger.info("{} Cleared tree icons (using JMeter defaults)", THEME_PREFIX);
            }

            for (Window window : Window.getWindows()) {
                resetComponentStyles(window);
                SwingUtilities.updateComponentTreeUI(window);
                window.repaint();
                if (!descriptor.isIJTheme()) {
                    repaintAll(window);
                }
            }
            rebuildToolbar();

            PREFS.put(PREF_KEY, themeId);
            try {
                PREFS.flush();
            } catch (java.util.prefs.BackingStoreException bse) {
                logger.warn("{} Warning: could not flush preferences: {}", THEME_PREFIX, bse.getMessage());
            }

            logger.info("{} Applied theme: {}", THEME_PREFIX, descriptor.getDisplayName());
            return true;
        } catch (Exception e) {
            logger.error("{} Failed to apply theme '{}': {}", THEME_PREFIX, themeId, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void restoreDefault() {
        clearJMeterProperty(JMETER_TOOLBAR_ICONS_PROP);

        clearJMeterProperty(JMETER_TREE_ICONS_PROP);
        if (originalLafClassName != null) {
            try {
                javax.swing.UIManager.setLookAndFeel(originalLafClassName);
                for (Window window : Window.getWindows()) {
                    resetComponentStyles(window);
                    SwingUtilities.updateComponentTreeUI(window);
                    window.repaint();
                }
                rebuildToolbar();
                logger.info("{} Reinstalled original LAF: {}", THEME_PREFIX, originalLafClassName);
            } catch (Exception e) {
                logger.error("{} Failed to reinstall original LAF '{}': {}",
                        THEME_PREFIX, originalLafClassName, e.getMessage());
            }
        } else {
            logger.warn("{} Original LAF was not captured; UI may need a restart to fully revert.",
                    THEME_PREFIX);
        }

        PREFS.put(PREF_KEY, DEFAULT_THEME);
        try {
            PREFS.flush();
        } catch (java.util.prefs.BackingStoreException bse) {
            logger.warn("{} Warning: could not flush preferences: {}", THEME_PREFIX, bse.getMessage());
        }

        logger.info("{} Restored default JMeter theme.", THEME_PREFIX);
    }

    public static void applyStartupTheme() {
        String savedTheme = getActiveThemeId();
        if (DEFAULT_THEME.equals(savedTheme)) {
            logger.info("{} Default theme selected, skipping Aura theme application.", THEME_PREFIX);
            return;
        }
        if (themes.containsKey(savedTheme)) {
            ThemeDescriptor descriptor = themes.get(savedTheme);

            if (descriptor.isPro() && !LicenseGuard.isProLicensed()) {
                logger.warn("{} Saved theme '{}' is pro but no valid license found. Falling back to default.",
                        THEME_PREFIX, descriptor.getDisplayName());
                PREFS.put(PREF_KEY, DEFAULT_THEME);
                return;
            }

            try {
                if (descriptor.isIJTheme()) {
                    applyIJTheme(descriptor);
                } else {
                    ThemeLaf laf = new ThemeLaf(descriptor);
                    FlatLaf.setup(laf);
                }

                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                    window.repaint();
                    if (!descriptor.isIJTheme()) {
                        repaintAll(window);
                    }
                }

                logger.info("{} Startup theme applied: {}", THEME_PREFIX, descriptor.getDisplayName());

                if (descriptor.isDark() && !descriptor.isIJTheme()) {
                    scheduleDelayedRecolor();
                }
            } catch (Exception e) {
                logger.error("{} Failed to apply startup theme: {}", THEME_PREFIX, e.getMessage());
            }
        }
    }

    public static void setIconPropertiesEarly() {
        String savedTheme = getActiveThemeId();
        if (DEFAULT_THEME.equals(savedTheme)) {
            return;
        }
        if (themes.containsKey(savedTheme)) {
            ThemeDescriptor descriptor = themes.get(savedTheme);

            if (descriptor.getToolbarPropertiesPath() != null) {
                setJMeterProperty(JMETER_TOOLBAR_ICONS_PROP, descriptor.getToolbarPropertiesPath());
                logger.info("{} Early set toolbar icons: {}", THEME_PREFIX, descriptor.getToolbarPropertiesPath());
            }

            if (descriptor.getTreeIconPropertiesPath() != null) {
                setJMeterProperty(JMETER_TREE_ICONS_PROP, descriptor.getTreeIconPropertiesPath());
                logger.info("{} Early set tree icons: {}", THEME_PREFIX, descriptor.getTreeIconPropertiesPath());
            }
        }
    }

    private static void setJMeterProperty(String key, String value) {
        try {
            Class<?> jmeterUtils = Class.forName("org.apache.jmeter.util.JMeterUtils");
            jmeterUtils.getMethod("setProperty", String.class, String.class).invoke(null, key, value);
        } catch (Exception e) {
            // Fallback: set system property
            System.setProperty(key, value);
        }
    }

    private static void clearJMeterProperty(String key) {
        try {
            Class<?> jmeterUtils = Class.forName("org.apache.jmeter.util.JMeterUtils");
            java.lang.reflect.Method getPropMethod = jmeterUtils.getMethod("getJMeterProperties");
            Object props = getPropMethod.invoke(null);
            if (props instanceof java.util.Properties) {
                ((java.util.Properties) props).remove(key);
            }
        } catch (Exception e) {
            System.clearProperty(key);
        }
    }
    private static void applyIJTheme(ThemeDescriptor descriptor) throws Exception {
        String className = descriptor.getLafClass();
        Class<?> themeClass = Class.forName(className);
        themeClass.getMethod("setup").invoke(null);
        logger.info("{} Applied IJ theme via: {}", THEME_PREFIX, className);
    }

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
            logger.error("{} Toolbar will update on restart: {}", THEME_PREFIX, e.getMessage());
        }
    }

    private static void resetComponentStyles(java.awt.Container container) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof javax.swing.JComponent) {
                javax.swing.JComponent jcomp = (javax.swing.JComponent) comp;
                jcomp.setBackground(null);
                jcomp.setForeground(null);
            }
            if (comp instanceof java.awt.Container) {
                resetComponentStyles((java.awt.Container) comp);
            }
        }
    }

    private static void repaintAll(java.awt.Container container) {

        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof javax.swing.JComponent) {
                javax.swing.JComponent jcomp = (javax.swing.JComponent) comp;
                jcomp.setOpaque(true);

                if (comp instanceof javax.swing.JEditorPane) {
                    javax.swing.JEditorPane editorPane = (javax.swing.JEditorPane) comp;
                    java.awt.Color editorBg = javax.swing.UIManager.getColor("EditorPane.background");
                    java.awt.Color editorFg = javax.swing.UIManager.getColor("EditorPane.foreground");
                    if (editorBg != null) {
                        jcomp.setBackground(editorBg);
                    }

                    String contentType = editorPane.getContentType();
                    if ("text/html".equals(contentType)) {
                        injectDarkThemeCSS(editorPane, editorBg, editorFg);
                    }
                } else if (comp instanceof javax.swing.JTextPane) {
                    java.awt.Color textPaneBg = javax.swing.UIManager.getColor("TextPane.background");
                    java.awt.Color textPaneFg = javax.swing.UIManager.getColor("TextPane.foreground");
                    if (textPaneBg != null) {
                        jcomp.setBackground(textPaneBg);
                    }
                    if (textPaneFg != null) {
                        jcomp.setForeground(textPaneFg);
                    }
                } else if (comp instanceof javax.swing.JTextArea || comp instanceof javax.swing.JTextField) {
                    java.awt.Color textAreaBg = javax.swing.UIManager.getColor("TextArea.background");
                    java.awt.Color textAreaFg = javax.swing.UIManager.getColor("TextArea.foreground");
                    if (textAreaBg != null) {
                        jcomp.setBackground(textAreaBg);
                    }
                    if (textAreaFg != null) {
                        jcomp.setForeground(textAreaFg);
                    }
                } else if (comp instanceof javax.swing.JScrollPane) {
                    java.awt.Color scrollBg = javax.swing.UIManager.getColor("ScrollPane.background");
                    if (scrollBg != null) {
                        jcomp.setBackground(scrollBg);
                    }
                } else if (comp instanceof javax.swing.JViewport) {
                    java.awt.Color viewportBg = javax.swing.UIManager.getColor("Viewport.background");
                    if (viewportBg != null) {
                        jcomp.setBackground(viewportBg);
                    }
                } else if (comp instanceof javax.swing.JSplitPane) {
                    java.awt.Color splitBg = javax.swing.UIManager.getColor("SplitPane.background");
                    if (splitBg != null) {
                        jcomp.setBackground(splitBg);
                    }
                } else {
                    java.awt.Color panelBg = javax.swing.UIManager.getColor("Panel.background");
                    if (panelBg != null) {
                        jcomp.setBackground(panelBg);
                    }
                }
            }
            comp.repaint();
            if (comp instanceof java.awt.Container) {
                repaintAll((java.awt.Container) comp);
            }
        }
    }



    private static void scheduleDelayedRecolor() {
        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> {
            java.awt.Color bg = javax.swing.UIManager.getColor("TextArea.background");
            java.awt.Color fg = javax.swing.UIManager.getColor("TextArea.foreground");
            if (bg == null || fg == null) return;

            for (Window window : Window.getWindows()) {
                recolorTextAreasInContainer(window, bg, fg);
            }
            logger.info("{} Delayed recolor pass complete.", THEME_PREFIX);
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Recursively find all {@link javax.swing.JTextArea} instances (including RSyntaxTextArea
     * subclasses) in a container and set their foreground/background. Also checks
     * {@link javax.swing.JViewport#getView()} to reach components that
     * {@code getComponents()} may not return (e.g. inside RTextScrollPane).
     *
     * <p>For RSyntaxTextArea instances, we additionally update the {@code SyntaxScheme}
     * via reflection since RSyntaxTextArea ignores {@code setForeground()} and uses its
     * own per-token-type styling instead.</p>
     */
    private static void recolorTextAreasInContainer(java.awt.Container container,
                                                     java.awt.Color bg, java.awt.Color fg) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof javax.swing.JTextArea) {
                applyTextAreaColors((javax.swing.JTextArea) comp, bg, fg);
            }
            if (comp instanceof javax.swing.JViewport) {
                java.awt.Component view = ((javax.swing.JViewport) comp).getView();
                if (view instanceof javax.swing.JTextArea) {
                    applyTextAreaColors((javax.swing.JTextArea) view, bg, fg);
                }
            }
            if (comp instanceof java.awt.Container) {
                recolorTextAreasInContainer((java.awt.Container) comp, bg, fg);
            }
        }
    }

    private static void applyTextAreaColors(javax.swing.JTextArea textArea,
                                             java.awt.Color bg, java.awt.Color fg) {
        textArea.setBackground(bg);
        textArea.setForeground(fg);

        String className = textArea.getClass().getName();
        if (className.contains("RSyntaxTextArea") || className.contains("JSyntaxTextArea")) {
            try {
                java.lang.reflect.Method getScheme = findMethod(textArea.getClass(), "getSyntaxScheme");
                if (getScheme != null) {
                    Object scheme = getScheme.invoke(textArea);
                    java.lang.reflect.Method getStyles = findMethod(scheme.getClass(), "getStyles");
                    if (getStyles != null) {
                        Object[] styles = (Object[]) getStyles.invoke(scheme);
                        for (Object style : styles) {
                            if (style != null) {
                                try {
                                    java.lang.reflect.Field fgField = style.getClass().getField("foreground");
                                    java.awt.Color currentFg = (java.awt.Color) fgField.get(style);
                                    if (currentFg != null) {
                                        double lum = 0.2126 * currentFg.getRed()
                                                   + 0.7152 * currentFg.getGreen()
                                                   + 0.0722 * currentFg.getBlue();
                                        if (lum < 128) {
                                            fgField.set(style, fg);
                                        }
                                    } else {
                                        fgField.set(style, fg);
                                    }
                                } catch (NoSuchFieldException nsfe) {
                                }
                            }
                        }
                    }
                    textArea.setCaretColor(fg);
                    textArea.repaint();
                }
            } catch (Exception ex) {
                logger.error("{} Failed to recolor RSyntaxTextArea: {}", THEME_PREFIX, ex.getMessage());
            }
        }
    }

    /** Find a public no-arg method by name, returning null if not found. */
    private static java.lang.reflect.Method findMethod(Class<?> clazz, String name) {
        try {
            return clazz.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }


    private static void injectDarkThemeCSS(javax.swing.JEditorPane editorPane, java.awt.Color bgColor, java.awt.Color fgColor) {

        try {
            String html = editorPane.getText();
            if (html != null && !html.isEmpty()) {
                if (!html.contains("/* JMeter Studio Dark Theme CSS */")) {
                    String bgHex = colorToHex(bgColor != null ? bgColor : java.awt.Color.decode("#231d35"));
                    String fgHex = colorToHex(fgColor != null ? fgColor : java.awt.Color.decode("#e8e0f0"));

                    String darkCSS = "<style type=\"text/css\">\n" +
                            "/* JMeter Studio Dark Theme CSS */\n" +
                            "body { background-color: " + bgHex + " !important; color: " + fgHex + " !important; }\n" +
                            "table { background-color: " + bgHex + " !important; }\n" +
                            "td, th { background-color: " + bgHex + " !important; color: " + fgHex + " !important; }\n" +
                            "</style>\n";

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
            logger.error("{} Error injecting CSS: {}", THEME_PREFIX, e.getMessage());
        }
    }

    private static String colorToHex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
