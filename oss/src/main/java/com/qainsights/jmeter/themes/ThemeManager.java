package com.qainsights.jmeter.themes;

import com.formdev.flatlaf.FlatLaf;
import com.google.gson.Gson;
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
                            logger.info("[AuraTheme] Registered theme: " + desc.getDisplayName());
                        }
                    }
                } catch (IOException e) {
                    logger.error("[AuraTheme] Error reading themes manifest: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("[AuraTheme] Error scanning for theme manifests: " + e.getMessage());
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
            // 1. Apply FlatLaf theme (IntelliJ-pack or custom properties)
            if (descriptor.isIJTheme()) {
                applyIJTheme(descriptor);
            } else {
                ThemeLaf laf = new ThemeLaf(descriptor);
                FlatLaf.setup(laf);
            }

            // 2. Manage JMeter icon properties.
            //    Aura themes supply their own icons; IJ themes must clear the properties so JMeter
            //    falls back to its own default icons.
            if (descriptor.getToolbarPropertiesPath() != null) {
                setJMeterProperty(JMETER_TOOLBAR_ICONS_PROP, descriptor.getToolbarPropertiesPath());
                logger.info("[AuraTheme] Set toolbar icons: " + descriptor.getToolbarPropertiesPath());
            } else {
                clearJMeterProperty(JMETER_TOOLBAR_ICONS_PROP);
                logger.info("[AuraTheme] Cleared toolbar icons (using JMeter defaults)");
            }

            // 3. Set JMeter tree icons property so tree uses our icons
            if (descriptor.getTreeIconPropertiesPath() != null) {
                setJMeterProperty(JMETER_TREE_ICONS_PROP, descriptor.getTreeIconPropertiesPath());
                logger.info("[AuraTheme] Set tree icons: " + descriptor.getTreeIconPropertiesPath());
            } else {
                clearJMeterProperty(JMETER_TREE_ICONS_PROP);
                logger.info("[AuraTheme] Cleared tree icons (using JMeter defaults)");
            }

            // 4. Reset explicit component style overrides so the new theme's UIManager defaults apply cleanly,
            //    then update all windows.
            for (Window window : Window.getWindows()) {
                resetComponentStyles(window);
                SwingUtilities.updateComponentTreeUI(window);
                window.repaint();
                // For IJ themes, updateComponentTreeUI is sufficient — they are well-behaved FlatLaf themes.
                // For custom properties-based Aura themes, we still need the manual background-forcing pass.
                if (!descriptor.isIJTheme()) {
                    repaintAll(window);
                }
            }

            // 5. Trigger toolbar rebuild
            rebuildToolbar();

            // 6. Persist the selection and flush immediately so it survives process exit
            PREFS.put(PREF_KEY, themeId);
            try {
                PREFS.flush();
            } catch (java.util.prefs.BackingStoreException bse) {
                logger.warn("[AuraTheme] Warning: could not flush preferences: " + bse.getMessage());
            }

            logger.info("[AuraTheme] Applied theme: " + descriptor.getDisplayName());
            return true;
        } catch (Exception e) {
            logger.error("[AuraTheme] Failed to apply theme '" + themeId + "': " + e.getMessage());
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

        // Persist "default" as active theme and flush immediately
        PREFS.put(PREF_KEY, DEFAULT_THEME);
        try {
            PREFS.flush();
        } catch (java.util.prefs.BackingStoreException bse) {
            logger.warn("[AuraTheme] Warning: could not flush preferences: " + bse.getMessage());
        }

        logger.info("[AuraTheme] Restored default JMeter theme. Restart required.");
    }

    /**
     * Apply the user's saved theme preference. Called during startup.
     */
    public static void applyStartupTheme() {
        String savedTheme = getActiveThemeId();
        if (DEFAULT_THEME.equals(savedTheme)) {
            // User chose default — don't apply any Aura theme, don't set icon property
            logger.info("[AuraTheme] Default theme selected, skipping Aura theme application.");
            return;
        }
        if (themes.containsKey(savedTheme)) {
            ThemeDescriptor descriptor = themes.get(savedTheme);

            try {
                if (descriptor.isIJTheme()) {
                    applyIJTheme(descriptor);
                } else {
                    // Apply the FlatLaf properties-based theme
                    ThemeLaf laf = new ThemeLaf(descriptor);
                    FlatLaf.setup(laf);
                }

                // Update all existing windows and components
                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                    window.repaint();
                    // IJ themes are well-behaved FlatLaf themes — no extra color-forcing needed.
                    // Only run the Aura-specific repaintAll pass for custom properties themes.
                    if (!descriptor.isIJTheme()) {
                        repaintAll(window);
                    }
                }

                logger.info("[AuraTheme] Startup theme applied: " + descriptor.getDisplayName());

                // Schedule a delayed recolor pass — JMeter's log viewer populates AFTER
                // the startup theme is applied, so our immediate repaintAll ran on an empty document.
                if (descriptor.isDark() && !descriptor.isIJTheme()) {
                    scheduleDelayedRecolor();
                }
            } catch (Exception e) {
                logger.error("[AuraTheme] Failed to apply startup theme: " + e.getMessage());
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
                logger.info("[AuraTheme] Early set toolbar icons: " + descriptor.getToolbarPropertiesPath());
            }

            // Set tree icons property BEFORE JMeter loads tree
            if (descriptor.getTreeIconPropertiesPath() != null) {
                setJMeterProperty(JMETER_TREE_ICONS_PROP, descriptor.getTreeIconPropertiesPath());
                logger.info("[AuraTheme] Early set tree icons: " + descriptor.getTreeIconPropertiesPath());
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
            logger.info("[AuraTheme] Set JMeter property " + key + " = " + value);
        } catch (Exception e) {
            // Fallback: set system property
            System.setProperty(key, value);
            logger.error("[AuraTheme] JMeterUtils not available, set system property: " + key);
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
            logger.info("[AuraTheme] Cleared JMeter property: " + key);
        } catch (Exception e) {
            System.clearProperty(key);
        }
    }

    /**
     * Apply a FlatLaf IntelliJ-pack theme by reflectively calling its {@code setup()} static method.
     * This avoids a compile-time dependency on the specific theme subclass.
     *
     * @param descriptor theme descriptor with a non-null {@code lafClass}
     * @throws Exception if the class cannot be found or {@code setup()} fails
     */
    private static void applyIJTheme(ThemeDescriptor descriptor) throws Exception {
        String className = descriptor.getLafClass();
        Class<?> themeClass = Class.forName(className);
        themeClass.getMethod("setup").invoke(null);
        logger.info("[AuraTheme] Applied IJ theme via: " + className);
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
            logger.error("[AuraTheme] Toolbar will update on restart: " + e.getMessage());
        }
    }

    /**
     * Recursively clear explicitly-set background, foreground, and opaque flags on all components
     * in a container so that the incoming theme's UIManager defaults take full effect.
     *
     * <p>When a previous Aura theme called {@code setBackground(color)} directly on components,
     * those values are stored as client state and survive {@link SwingUtilities#updateComponentTreeUI}.
     * Nulling them out restores normal LAF inheritance before we apply the new theme.</p>
     */
    private static void resetComponentStyles(java.awt.Container container) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof javax.swing.JComponent) {
                javax.swing.JComponent jcomp = (javax.swing.JComponent) comp;
                // Remove explicit background/foreground so UIManager defaults take over
                jcomp.setBackground(null);
                jcomp.setForeground(null);
                // NOTE: we deliberately do NOT touch setOpaque() here.
                // The opaque flag is a "user-set" property in Swing — if we set it,
                // LAF installDefaults() won't restore it and panels would become transparent.
            }
            if (comp instanceof java.awt.Container) {
                resetComponentStyles((java.awt.Container) comp);
            }
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
                    logger.info("[AuraTheme] Set background on JEditorPane: " + editorPane.getName());
                } else if (comp instanceof javax.swing.JTextPane) {
                    javax.swing.JTextPane textPane = (javax.swing.JTextPane) comp;
                    java.awt.Color textPaneBg = javax.swing.UIManager.getColor("TextPane.background");
                    java.awt.Color textPaneFg = javax.swing.UIManager.getColor("TextPane.foreground");
                    if (textPaneBg != null) {
                        textPane.setBackground(textPaneBg);
                    }
                    if (textPaneFg != null) {
                        textPane.setForeground(textPaneFg);
                        // Recolor existing styled text
                        recolorStyledDocument(textPane, textPaneBg, textPaneFg);
                        // Install a listener to recolor new text as it arrives
                        installRecolorListener(textPane, textPaneBg, textPaneFg);
                    }
                    logger.info("[AuraTheme] Set colors on JTextPane: " + comp.getName());
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
                    logger.info("[AuraTheme] Set background on JScrollPane: " + comp.getName());
                } else if (comp instanceof javax.swing.JViewport) {
                    java.awt.Color viewportBg = javax.swing.UIManager.getColor("Viewport.background");
                    if (viewportBg != null) {
                        jcomp.setBackground(viewportBg);
                    }
                    logger.info("[AuraTheme] Set background on JViewport: " + comp.getName());
                } else if (comp instanceof javax.swing.JSplitPane) {
                    java.awt.Color splitBg = javax.swing.UIManager.getColor("SplitPane.background");
                    if (splitBg != null) {
                        jcomp.setBackground(splitBg);
                    }
                    logger.info("[AuraTheme] Set background on JSplitPane: " + comp.getName());
                } else {
                    java.awt.Color panelBg = javax.swing.UIManager.getColor("Panel.background");
                    if (panelBg != null) {
                        jcomp.setBackground(panelBg);
                    }
                    logger.info("[AuraTheme] Set background on " + comp.getClass().getSimpleName() + ": " + comp.getName());
                }
            }
            comp.repaint();
            if (comp instanceof java.awt.Container) {
                repaintAll((java.awt.Container) comp);
            }
        }
    }

    /** Client property key used to avoid installing duplicate recolor listeners. */
    private static final String RECOLOR_LISTENER_KEY = "aura.recolor.installed";

    /**
     * Schedule a delayed recoloring pass. JMeter's log viewer ({@code LoggerPanel}) uses a
     * {@code JSyntaxTextArea} (extends RSyntaxTextArea extends JTextArea) that is created and
     * populated AFTER the startup theme is applied. This timer fires after 3 seconds, by which
     * time the log entries should be present, and sets background/foreground on all JTextArea
     * instances found in the component hierarchy.
     */
    private static void scheduleDelayedRecolor() {
        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> {
            java.awt.Color bg = javax.swing.UIManager.getColor("TextArea.background");
            java.awt.Color fg = javax.swing.UIManager.getColor("TextArea.foreground");
            if (bg == null || fg == null) return;

            for (Window window : Window.getWindows()) {
                recolorTextAreasInContainer(window, bg, fg);
            }
            logger.info("[AuraTheme] Delayed recolor pass complete.");
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
            // JViewport.getView() may return a component not listed by getComponents()
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

    /**
     * Apply colors to a JTextArea, with special handling for RSyntaxTextArea instances.
     * RSyntaxTextArea uses a {@code SyntaxScheme} that overrides {@code setForeground()},
     * so we need to use reflection to update each token type's style directly.
     */
    private static void applyTextAreaColors(javax.swing.JTextArea textArea,
                                             java.awt.Color bg, java.awt.Color fg) {
        textArea.setBackground(bg);
        textArea.setForeground(fg);

        // Check if this is an RSyntaxTextArea by class name (no compile-time dependency)
        String className = textArea.getClass().getName();
        if (className.contains("RSyntaxTextArea") || className.contains("JSyntaxTextArea")) {
            try {
                // Get the SyntaxScheme: RSyntaxTextArea.getSyntaxScheme()
                java.lang.reflect.Method getScheme = findMethod(textArea.getClass(), "getSyntaxScheme");
                if (getScheme != null) {
                    Object scheme = getScheme.invoke(textArea);
                    // SyntaxScheme.getStyles() returns Style[]
                    java.lang.reflect.Method getStyles = findMethod(scheme.getClass(), "getStyles");
                    if (getStyles != null) {
                        Object[] styles = (Object[]) getStyles.invoke(scheme);
                        // Each Style has a public 'foreground' field
                        for (Object style : styles) {
                            if (style != null) {
                                try {
                                    java.lang.reflect.Field fgField = style.getClass().getField("foreground");
                                    java.awt.Color currentFg = (java.awt.Color) fgField.get(style);
                                    if (currentFg != null) {
                                        // Replace dark foreground colors with theme's light foreground
                                        double lum = 0.2126 * currentFg.getRed()
                                                   + 0.7152 * currentFg.getGreen()
                                                   + 0.0722 * currentFg.getBlue();
                                        if (lum < 128) {
                                            fgField.set(style, fg);
                                        }
                                    } else {
                                        // No foreground set = uses component default, which is dark
                                        fgField.set(style, fg);
                                    }
                                } catch (NoSuchFieldException nsfe) {
                                    // Style doesn't have foreground field; skip
                                }
                            }
                        }
                    }
                    // Also set caret color
                    textArea.setCaretColor(fg);
                    textArea.repaint();
                    logger.info("[AuraTheme] Recolored RSyntaxTextArea SyntaxScheme: " + className);
                }
            } catch (Exception ex) {
                logger.error("[AuraTheme] Failed to recolor RSyntaxTextArea: " + ex.getMessage());
            }
        } else {
            logger.info("[AuraTheme] Recolored JTextArea: " + className);
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



    /**
     * Install a {@link javax.swing.event.DocumentListener} on a JTextPane so that new text
     * inserted with dark foreground colors (e.g. JMeter log entries) is automatically recolored
     * to the theme's foreground.
     *
     * <p>Guarded by a client property to avoid installing duplicate listeners on the same pane.</p>
     */
    private static void installRecolorListener(javax.swing.JTextPane textPane,
                                                java.awt.Color bgColor,
                                                java.awt.Color fgColor) {
        if (bgColor == null || fgColor == null) return;
        // Only act on dark backgrounds
        double bgLum = 0.2126 * bgColor.getRed() + 0.7152 * bgColor.getGreen() + 0.0722 * bgColor.getBlue();
        if (bgLum >= 128) return;

        // Check if we already installed a listener
        if (Boolean.TRUE.equals(textPane.getClientProperty(RECOLOR_LISTENER_KEY))) {
            return;
        }
        textPane.putClientProperty(RECOLOR_LISTENER_KEY, Boolean.TRUE);

        final java.awt.Color themeFg = fgColor;
        textPane.getStyledDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                // Must defer document modification to avoid IllegalStateException
                SwingUtilities.invokeLater(() -> {
                    javax.swing.text.StyledDocument doc = textPane.getStyledDocument();
                    int offset = e.getOffset();
                    int length = e.getLength();
                    // Recolor the just-inserted range
                    javax.swing.text.SimpleAttributeSet attrs = new javax.swing.text.SimpleAttributeSet();
                    javax.swing.text.StyleConstants.setForeground(attrs, themeFg);
                    doc.setCharacterAttributes(offset, length, attrs, false);
                });
            }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) {}
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
    }

    /**
     * Recolor the styled text of a {@link javax.swing.JTextPane} so that dark-colored text

     * (e.g. {@code Color.black} used by the JMeter log viewer for INFO messages) becomes readable
     * on a dark theme background.
     *
     * <p>JMeter's log panel appends entries via {@link javax.swing.text.StyledDocument} character
     * attributes with hard-coded colors. Setting {@code setForeground()} on the component itself
     * does not affect these per-run attributes, so we walk the document and replace any foreground
     * that is dark against the (now dark) background with the theme's foreground color.</p>
     */
    private static void recolorStyledDocument(javax.swing.JTextPane textPane,
                                               java.awt.Color bgColor,
                                               java.awt.Color fgColor) {
        if (bgColor == null || fgColor == null) return;

        // Only act when the background is dark (luminance < 128 on 0-255 scale)
        double bgLum = 0.2126 * bgColor.getRed() + 0.7152 * bgColor.getGreen() + 0.0722 * bgColor.getBlue();
        if (bgLum >= 128) return;

        javax.swing.text.StyledDocument doc = textPane.getStyledDocument();
        if (doc == null || doc.getLength() == 0) return;

        try {
            recolorDocumentElement(doc.getDefaultRootElement(), doc, fgColor);
        } catch (Exception e) {
            logger.error("[AuraTheme] Error recoloring StyledDocument: " + e.getMessage());
        }
    }

    /**
     * Recursively walk a document element tree and replace dark foreground attributes with
     * {@code themeFg}.
     */
    private static void recolorDocumentElement(javax.swing.text.Element elem,
                                                javax.swing.text.StyledDocument doc,
                                                java.awt.Color themeFg) {
        if (elem.isLeaf()) {
            javax.swing.text.AttributeSet attrs = elem.getAttributes();
            // Check if this run has an explicit foreground color set
            if (attrs.isDefined(javax.swing.text.StyleConstants.Foreground)) {
                java.awt.Color fg = javax.swing.text.StyleConstants.getForeground(attrs);
                // Replace if the foreground is dark (luminance < 128)
                double fgLum = 0.2126 * fg.getRed() + 0.7152 * fg.getGreen() + 0.0722 * fg.getBlue();
                if (fgLum < 128) {
                    javax.swing.text.SimpleAttributeSet replacement = new javax.swing.text.SimpleAttributeSet();
                    javax.swing.text.StyleConstants.setForeground(replacement, themeFg);
                    doc.setCharacterAttributes(elem.getStartOffset(),
                            elem.getEndOffset() - elem.getStartOffset(),
                            replacement, false);
                }
            }
            return;
        }
        for (int i = 0; i < elem.getElementCount(); i++) {
            recolorDocumentElement(elem.getElement(i), doc, themeFg);
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
            logger.error("[AuraTheme] Error injecting CSS: " + e.getMessage());
        }
    }

    /**
     * Convert a Color to hex string.
     */
    private static String colorToHex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
