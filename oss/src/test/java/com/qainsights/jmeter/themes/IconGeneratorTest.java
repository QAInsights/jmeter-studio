package com.qainsights.jmeter.themes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IconGenerator}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Constructor colour-mode flag</li>
 *   <li>Scale factor helper</li>
 *   <li>Stroke minimum-width enforcement</li>
 *   <li>generateAll() actually writes expected PNG files (uses a temp dir)</li>
 *   <li>All individual {@code draw*()} methods at various sizes don't throw</li>
 * </ul>
 * </p>
 */
class IconGeneratorTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean getIsDark(IconGenerator gen) throws Exception {
        Field f = IconGenerator.class.getDeclaredField("isDark");
        f.setAccessible(true);
        return (boolean) f.get(gen);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    void testConstructor_darkMode_setsIsDarkTrue() throws Exception {
        IconGenerator gen = new IconGenerator(true);
        assertTrue(getIsDark(gen), "isDark should be true after new IconGenerator(true)");
    }

    @Test
    void testConstructor_lightMode_setsIsDarkFalse() throws Exception {
        IconGenerator gen = new IconGenerator(false);
        assertFalse(getIsDark(gen), "isDark should be false after new IconGenerator(false)");
    }

    // ── Scale factor helper ───────────────────────────────────────────────────

    @Test
    void testScaleFactor_48px_isOne() throws Exception {
        IconGenerator gen = new IconGenerator(true);
        Method s = IconGenerator.class.getDeclaredMethod("s", int.class);
        s.setAccessible(true);
        float result = (float) s.invoke(gen, 48);
        assertEquals(1.0f, result, 0.0001f);
    }

    @Test
    void testScaleFactor_24px_isHalf() throws Exception {
        IconGenerator gen = new IconGenerator(true);
        Method s = IconGenerator.class.getDeclaredMethod("s", int.class);
        s.setAccessible(true);
        float result = (float) s.invoke(gen, 24);
        assertEquals(0.5f, result, 0.0001f);
    }

    @Test
    void testScaleFactor_19px() throws Exception {
        IconGenerator gen = new IconGenerator(true);
        Method s = IconGenerator.class.getDeclaredMethod("s", int.class);
        s.setAccessible(true);
        float result = (float) s.invoke(gen, 19);
        assertEquals(19f / 48f, result, 0.0001f);
    }

    // ── Stroke minimum width ──────────────────────────────────────────────────

    @Test
    void testStroke_minimumWidthEnforced_forSmallIcon() throws Exception {
        IconGenerator gen = new IconGenerator(true);
        Method stroke = IconGenerator.class.getDeclaredMethod("stroke", int.class, float.class);
        stroke.setAccessible(true);
        // For size 22 (small), min effective width is 2.5f; requested width 1.0f
        java.awt.BasicStroke bs = (java.awt.BasicStroke) stroke.invoke(gen, 22, 1.0f);
        // effective line width = max(1.0f, 2.5f) * s(22)  =  2.5f * (22/48f)
        float expected = 2.5f * (22f / 48f);
        assertEquals(expected, bs.getLineWidth(), 0.001f,
                "Stroke width below minimum should be clamped to 2.5f * scale for size ≤ 22");
    }

    @Test
    void testStroke_minimumWidthEnforced_forLargeIcon() throws Exception {
        IconGenerator gen = new IconGenerator(true);
        Method stroke = IconGenerator.class.getDeclaredMethod("stroke", int.class, float.class);
        stroke.setAccessible(true);
        // For size 48 (large), min effective width is 2.0f; requested width 1.0f
        java.awt.BasicStroke bs = (java.awt.BasicStroke) stroke.invoke(gen, 48, 1.0f);
        float expected = 2.0f * 1.0f; // s(48) == 1.0f
        assertEquals(expected, bs.getLineWidth(), 0.001f);
    }

    @Test
    void testStroke_widthAboveMinimum_isNotClamped() throws Exception {
        IconGenerator gen = new IconGenerator(true);
        Method stroke = IconGenerator.class.getDeclaredMethod("stroke", int.class, float.class);
        stroke.setAccessible(true);
        // Requesting 3.0f on size 48 — no clamping needed
        java.awt.BasicStroke bs = (java.awt.BasicStroke) stroke.invoke(gen, 48, 3.0f);
        assertEquals(3.0f, bs.getLineWidth(), 0.001f);
    }

    // ── generateAll writes files ──────────────────────────────────────────────

    @Test
    void testGenerateAll_darkMode_writesToolbarPngs(@TempDir Path tempDir) throws Exception {
        IconGenerator gen = new IconGenerator(true);
        gen.generateAll(tempDir.toString());

        // Spot-check a few expected toolbar icons at 22x22
        List<String> expectedToolbar = List.of(
                "arrow-right.png", "document-new.png", "stop.png", "search.png"
        );
        for (String name : expectedToolbar) {
            File f = tempDir.resolve("toolbar/22x22/" + name).toFile();
            assertTrue(f.exists(), "Expected toolbar PNG not generated: " + f);
            assertTrue(f.length() > 0, "Generated PNG is empty: " + f);
        }
    }

    @Test
    void testGenerateAll_lightMode_writesTreePngs(@TempDir Path tempDir) throws Exception {
        IconGenerator gen = new IconGenerator(false);
        gen.generateAll(tempDir.toString());

        // Spot-check tree icons at 24x24
        List<String> expectedTree = List.of(
                "test-plan.png", "thread-group.png", "listener.png", "assertion.png"
        );
        for (String name : expectedTree) {
            File f = tempDir.resolve("tree/24x24/" + name).toFile();
            assertTrue(f.exists(), "Expected tree PNG not generated: " + f);
            assertTrue(f.length() > 0, "Generated PNG is empty: " + f);
        }
    }

    @Test
    void testGenerateAll_darkMode_writesStatusPngs(@TempDir Path tempDir) throws Exception {
        IconGenerator gen = new IconGenerator(true);
        gen.generateAll(tempDir.toString());

        List<String> expectedStatus = List.of(
                "error.png", "warning.png", "task-complete.png", "user-online.png"
        );
        for (String name : expectedStatus) {
            File f = tempDir.resolve("status/32x32/" + name).toFile();
            assertTrue(f.exists(), "Expected status PNG not generated: " + f);
        }
    }

    @Test
    void testGenerateAll_producesFilesForAllSizes(@TempDir Path tempDir) throws Exception {
        IconGenerator gen = new IconGenerator(true);
        gen.generateAll(tempDir.toString());

        // toolbar sizes: 22, 32, 48
        for (int size : new int[]{22, 32, 48}) {
            File dir = tempDir.resolve("toolbar/" + size + "x" + size).toFile();
            assertTrue(dir.exists() && dir.isDirectory(),
                    "Missing toolbar size directory: " + size + "x" + size);
            assertTrue(dir.list().length > 0,
                    "No icons generated in toolbar/" + size + "x" + size);
        }

        // tree sizes: 19, 24, 32, 48
        for (int size : new int[]{19, 24, 32, 48}) {
            File dir = tempDir.resolve("tree/" + size + "x" + size).toFile();
            assertTrue(dir.exists() && dir.isDirectory(),
                    "Missing tree size directory: " + size + "x" + size);
        }
    }

    // ── Individual draw methods don't throw ───────────────────────────────────

    /**
     * Creates a small Graphics2D context sufficient for exercising any draw* method.
     */
    private Graphics2D createG2D(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    @ParameterizedTest
    @ValueSource(ints = {19, 22, 32, 48})
    void testDrawTreeIcons_doNotThrow(int size) {
        IconGenerator gen = new IconGenerator(true);
        Graphics2D g = createG2D(size);
        assertDoesNotThrow(() -> {
            gen.drawTestPlan(g, size);
            gen.drawTimer(g, size);
            gen.drawThreadGroup(g, size);
            gen.drawListener(g, size);
            gen.drawConfig(g, size);
            gen.drawPreProcessor(g, size);
            gen.drawPostProcessor(g, size);
            gen.drawController(g, size);
            gen.drawSampler(g, size);
            gen.drawAssertion(g, size);
            gen.drawWorkbench(g, size);
        }, () -> "draw* call threw at size " + size);
        g.dispose();
    }

    @ParameterizedTest
    @ValueSource(ints = {22, 32, 48})
    void testDrawToolbarIcons_doNotThrow(int size) {
        IconGenerator gen = new IconGenerator(true);
        Graphics2D g = createG2D(size);
        assertDoesNotThrow(() -> {
            gen.drawNewDocument(g, size);
            gen.drawTemplates(g, size);
            gen.drawOpenDocument(g, size);
            gen.drawSaveDocument(g, size);
            gen.drawCut(g, size);
            gen.drawCopy(g, size);
            gen.drawPaste(g, size);
            gen.drawUndo(g, size);
            gen.drawRedo(g, size);
            gen.drawStart(g, size);
            gen.drawStartNoTimer(g, size);
            gen.drawStop(g, size);
            gen.drawShutdown(g, size);
            gen.drawClear(g, size);
            gen.drawClearAll(g, size);
            gen.drawExpand(g, size);
            gen.drawCollapse(g, size);
            gen.drawToggle(g, size);
            gen.drawSearch(g, size);
            gen.drawSearchReset(g, size);
            gen.drawFunctionHelper(g, size);
            gen.drawHelp(g, size);
        }, () -> "draw* call threw at size " + size);
        g.dispose();
    }

    @ParameterizedTest
    @ValueSource(ints = {22, 32, 48})
    void testDrawStatusIcons_doNotThrow(int size) {
        IconGenerator gen = new IconGenerator(true);
        Graphics2D g = createG2D(size);
        assertDoesNotThrow(() -> {
            gen.drawUserOnline(g, size);
            gen.drawUserOffline(g, size);
            gen.drawWarning(g, size);
            gen.drawError(g, size);
            gen.drawTaskComplete(g, size);
            gen.drawTaskRecurring(g, size);
        }, () -> "draw* call threw at size " + size);
        g.dispose();
    }

    @ParameterizedTest
    @ValueSource(ints = {22, 32, 48})
    void testDrawIcons_lightMode_doNotThrow(int size) {
        IconGenerator gen = new IconGenerator(false); // light mode
        Graphics2D g = createG2D(size);
        assertDoesNotThrow(() -> {
            gen.drawTestPlan(g, size);
            gen.drawStart(g, size);
            gen.drawStop(g, size);
            gen.drawError(g, size);
            gen.drawToggle(g, size);
            gen.drawSearchReset(g, size);
            gen.drawHelp(g, size);
        }, () -> "draw* call (light mode) threw at size " + size);
        g.dispose();
    }
}
