package com.qainsights.jmeter.themes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class IconGenerator {

    private Color PRIMARY;      
    private Color SECONDARY;    
    private Color ACCENT;       
    private Color SUCCESS;      
    private Color DANGER;       
    private Color WARNING;      
    private Color MUTED;        
    private Color INFO;         

    private Color CARD_FILL;
    private Color CARD_BORDER;

    private boolean isDark;

    @FunctionalInterface
    interface IconPainter {
        void paint(Graphics2D g, int size);
    }

    public IconGenerator(boolean dark) {
        this.isDark = dark;
        if (dark) {
            PRIMARY     = new Color(0xCD, 0xCE, 0xCF);  
            SECONDARY   = new Color(0x9b, 0x8f, 0xb8);  
            ACCENT      = new Color(0x7c, 0x3a, 0xed);  
            SUCCESS     = new Color(0x44, 0xAD, 0x54);  
            DANGER      = new Color(0xEF, 0x44, 0x44);  
            WARNING     = new Color(0xFB, 0xBF, 0x24);  
            MUTED       = new Color(0x6B, 0x6F, 0x73);  
            INFO        = new Color(0x60, 0xA5, 0xFA);  
            CARD_FILL   = new Color(0x2d, 0x25, 0x45, 220);  
            CARD_BORDER = new Color(0x3d, 0x35, 0x55, 240);  
        } else {
            PRIMARY     = new Color(0x4A, 0x3A, 0x63);  
            SECONDARY   = new Color(0x6b, 0x60, 0x80);  
            ACCENT      = new Color(0x7c, 0x3a, 0xed);  
            SUCCESS     = new Color(0x16, 0xA3, 0x4A);  
            DANGER      = new Color(0xDC, 0x26, 0x26);  
            WARNING     = new Color(0xD9, 0x77, 0x06);  
            MUTED       = new Color(0x94, 0xA3, 0xB8);  
            INFO        = new Color(0x25, 0x63, 0xEB);  
            CARD_FILL   = new Color(255, 255, 255, 180);  
            CARD_BORDER = new Color(0, 0, 0, 40);         
        }
    }

    public static void main(String[] args) throws IOException {
        String baseDir = args.length > 0 ? args[0] :
                "oss/src/main/resources/com/qainsights/jmeter/themes/aura-dark";
        String mode = args.length > 1 ? args[1] : "dark";
        boolean dark = "dark".equalsIgnoreCase(mode);

        IconGenerator gen = new IconGenerator(dark);
        gen.generateAll(baseDir);
    }

    public void generateAll(String baseDir) throws IOException {
        // Tree icons
        Map<String, IconPainter> treeIcons = new LinkedHashMap<>();
        treeIcons.put("test-plan", this::drawTestPlan);
        treeIcons.put("timer", this::drawTimer);
        treeIcons.put("thread-group", this::drawThreadGroup);
        treeIcons.put("listener", this::drawListener);
        treeIcons.put("config", this::drawConfig);
        treeIcons.put("pre-processor", this::drawPreProcessor);
        treeIcons.put("post-processor", this::drawPostProcessor);
        treeIcons.put("controller", this::drawController);
        treeIcons.put("sampler", this::drawSampler);
        treeIcons.put("assertion", this::drawAssertion);
        treeIcons.put("workbench", this::drawWorkbench);

        int[] treeSizes = {19, 24, 32, 48};
        for (int size : treeSizes) {
            for (Map.Entry<String, IconPainter> entry : treeIcons.entrySet()) {
                generateIcon(baseDir + "/tree/" + size + "x" + size,
                        entry.getKey() + ".png", size, entry.getValue());
            }
        }

        // Toolbar icons
        Map<String, IconPainter> toolbarIcons = new LinkedHashMap<>();
        toolbarIcons.put("document-new", this::drawNewDocument);
        toolbarIcons.put("templates", this::drawTemplates);
        toolbarIcons.put("document-open", this::drawOpenDocument);
        toolbarIcons.put("document-save", this::drawSaveDocument);
        toolbarIcons.put("edit-cut", this::drawCut);
        toolbarIcons.put("edit-copy", this::drawCopy);
        toolbarIcons.put("edit-paste", this::drawPaste);
        toolbarIcons.put("edit-undo", this::drawUndo);
        toolbarIcons.put("edit-redo", this::drawRedo);
        toolbarIcons.put("arrow-right", this::drawStart);
        toolbarIcons.put("arrow-right-notimer", this::drawStartNoTimer);
        toolbarIcons.put("stop", this::drawStop);
        toolbarIcons.put("shutdown", this::drawShutdown);
        toolbarIcons.put("clear", this::drawClear);
        toolbarIcons.put("clear-all", this::drawClearAll);
        toolbarIcons.put("expand", this::drawExpand);
        toolbarIcons.put("collapse", this::drawCollapse);
        toolbarIcons.put("toggle", this::drawToggle);
        toolbarIcons.put("search", this::drawSearch);
        toolbarIcons.put("search-reset", this::drawSearchReset);
        toolbarIcons.put("function-helper", this::drawFunctionHelper);
        toolbarIcons.put("help", this::drawHelp);

        int[] toolbarSizes = {22, 32, 48};
        for (int size : toolbarSizes) {
            for (Map.Entry<String, IconPainter> entry : toolbarIcons.entrySet()) {
                generateIcon(baseDir + "/toolbar/" + size + "x" + size,
                        entry.getKey() + ".png", size, entry.getValue());
            }
        }

        // Status icons
        Map<String, IconPainter> statusIcons = new LinkedHashMap<>();
        statusIcons.put("user-online", this::drawUserOnline);
        statusIcons.put("user-offline", this::drawUserOffline);
        statusIcons.put("warning", this::drawWarning);
        statusIcons.put("error", this::drawError);
        statusIcons.put("task-complete", this::drawTaskComplete);
        statusIcons.put("task-recurring", this::drawTaskRecurring);

        int[] statusSizes = {22, 32, 48};
        for (int size : statusSizes) {
            for (Map.Entry<String, IconPainter> entry : statusIcons.entrySet()) {
                generateIcon(baseDir + "/status/" + size + "x" + size,
                        entry.getKey() + ".png", size, entry.getValue());
            }
        }
    }

    private void generateIcon(String dir, String filename, int size, IconPainter painter) throws IOException {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(AlphaComposite.SrcOver);
        paintBackgroundCard(g, size);
        painter.paint(g, size);
        g.dispose();

        File outFile = new File(dir, filename);
        outFile.getParentFile().mkdirs();
        ImageIO.write(img, "png", outFile);
    }

    /**
     * Paints a rounded rectangle background card behind each icon.
     */
    private void paintBackgroundCard(Graphics2D g, int size) {
        float sc = s(size);
        float margin = 1 * sc;
        float cardW = size - 2 * margin;
        float arc = 4 * sc;

        g.setColor(CARD_FILL);
        g.fill(new RoundRectangle2D.Float(margin, margin, cardW, cardW, arc, arc));

        g.setColor(CARD_BORDER);
        float strokeW = isDark ? 0.75f : 0.5f;
        g.setStroke(new BasicStroke(strokeW * sc, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new RoundRectangle2D.Float(margin, margin, cardW, cardW, arc, arc));
    }

    private float s(int size) {
        return size / 48f;
    }

    /**
     * Returns stroke with minimum width per the skill rules.
     * Minimum 2.0f for most sizes, 2.5f for emphasis.
     */
    private BasicStroke stroke(int size, float width) {
        float minWidth = size <= 22 ? 2.5f : 2.0f;
        float effectiveWidth = Math.max(width, minWidth);
        return new BasicStroke(effectiveWidth * s(size), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /**
     * Fill a shape and draw an outline (Rule 4).
     */
    private void fillWithOutline(Graphics2D g, Shape shape, Color fill, Color outline, int size) {
        g.setColor(fill);
        g.fill(shape);
        g.setColor(outline);
        g.setStroke(new BasicStroke(1.5f * s(size), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    void drawTestPlan(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f;
        g.setColor(ACCENT);
        g.setStroke(stroke(size, 2.5f));
        Path2D flask = new Path2D.Float();
        flask.moveTo(cx - 8 * sc, 6 * sc);
        flask.lineTo(cx - 8 * sc, 18 * sc);
        flask.lineTo(cx - 16 * sc, 36 * sc);
        flask.quadTo(cx - 16 * sc, 42 * sc, cx - 8 * sc, 42 * sc);
        flask.lineTo(cx + 8 * sc, 42 * sc);
        flask.quadTo(cx + 16 * sc, 42 * sc, cx + 16 * sc, 36 * sc);
        flask.lineTo(cx + 8 * sc, 18 * sc);
        flask.lineTo(cx + 8 * sc, 6 * sc);
        flask.closePath();
        g.draw(flask);
        Path2D liquid = new Path2D.Float();
        liquid.moveTo(cx - 12 * sc, 30 * sc);
        liquid.lineTo(cx - 16 * sc, 36 * sc);
        liquid.quadTo(cx - 16 * sc, 42 * sc, cx - 8 * sc, 42 * sc);
        liquid.lineTo(cx + 8 * sc, 42 * sc);
        liquid.quadTo(cx + 16 * sc, 42 * sc, cx + 16 * sc, 36 * sc);
        liquid.lineTo(cx + 12 * sc, 30 * sc);
        liquid.closePath();
        Color liquidFill = new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 100);
        fillWithOutline(g, liquid, liquidFill, ACCENT, size);
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2f));
        g.draw(new Line2D.Float(cx - 10 * sc, 6 * sc, cx + 10 * sc, 6 * sc));
    }

    void drawTimer(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        float r = 16 * sc;
        g.draw(new Ellipse2D.Float(cx - r, cy - r + 2 * sc, r * 2, r * 2));
        g.setColor(ACCENT);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Line2D.Float(cx, cy + 2 * sc, cx, cy - 6 * sc + 2 * sc));
        g.draw(new Line2D.Float(cx, cy + 2 * sc, cx + 6 * sc, cy + 2 * sc));
        g.setColor(PRIMARY);
        g.draw(new Line2D.Float(cx - 3 * sc, 4 * sc, cx + 3 * sc, 4 * sc));
    }

    void drawThreadGroup(Graphics2D g, int size) {
        float sc = s(size);
        g.setStroke(stroke(size, 2.5f));
        float[] ys = {14 * sc, 24 * sc, 34 * sc};
        Color[] colors = {ACCENT, INFO, PRIMARY};
        for (int i = 0; i < 3; i++) {
            g.setColor(colors[i]);
            float y = ys[i];
            g.draw(new Line2D.Float(8 * sc, y, 32 * sc, y));
            g.draw(new Line2D.Float(32 * sc, y, 26 * sc, y - 4 * sc));
            g.draw(new Line2D.Float(32 * sc, y, 26 * sc, y + 4 * sc));
        }
        g.setColor(SECONDARY);
        g.draw(new Line2D.Float(8 * sc, 14 * sc, 8 * sc, 34 * sc));
    }

    void drawListener(Graphics2D g, int size) {
        float sc = s(size);
        g.setStroke(stroke(size, 2f));
        float[] heights = {20, 32, 26, 38, 22};
        float barW = 5 * sc;
        float gap = 2 * sc;
        float baseY = 42 * sc;
        float startX = 6 * sc;
        for (int i = 0; i < heights.length; i++) {
            float h = heights[i] * sc;
            float x = startX + i * (barW + gap);
            RoundRectangle2D bar = new RoundRectangle2D.Float(
                    x, baseY - h, barW, h, 2 * sc, 2 * sc);
            Color barFill = new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(),
                    100 + i * 30);
            fillWithOutline(g, bar, i == 3 ? ACCENT : barFill, ACCENT, size);
        }
    }

    void drawConfig(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        float outerR = 16 * sc, innerR = 10 * sc;
        int teeth = 8;
        Path2D gear = new Path2D.Float();
        for (int i = 0; i < teeth * 2; i++) {
            float angle = (float) (i * Math.PI / teeth);
            float r = (i % 2 == 0) ? outerR : innerR;
            float x = cx + r * (float) Math.cos(angle);
            float y = cy + r * (float) Math.sin(angle);
            if (i == 0) gear.moveTo(x, y);
            else gear.lineTo(x, y);
        }
        gear.closePath();
        Color gearFill = new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 30);
        fillWithOutline(g, gear, gearFill, PRIMARY, size);
        // Center circle
        g.setColor(ACCENT);
        float cr = 5 * sc;
        Ellipse2D center = new Ellipse2D.Float(cx - cr, cy - cr, cr * 2, cr * 2);
        fillWithOutline(g, center, ACCENT, ACCENT.darker(), size);
    }

    void drawPreProcessor(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f;
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        Path2D funnel = new Path2D.Float();
        funnel.moveTo(6 * sc, 8 * sc);
        funnel.lineTo(42 * sc, 8 * sc);
        funnel.lineTo(cx + 4 * sc, 28 * sc);
        funnel.lineTo(cx + 4 * sc, 42 * sc);
        funnel.lineTo(cx - 4 * sc, 42 * sc);
        funnel.lineTo(cx - 4 * sc, 28 * sc);
        funnel.closePath();
        g.draw(funnel);
        // Arrow pointing down
        g.setColor(ACCENT);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Line2D.Float(cx, 14 * sc, cx, 22 * sc));
        g.draw(new Line2D.Float(cx - 3 * sc, 19 * sc, cx, 22 * sc));
        g.draw(new Line2D.Float(cx + 3 * sc, 19 * sc, cx, 22 * sc));
    }

    void drawPostProcessor(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        float r = 12 * sc;
        g.draw(new Ellipse2D.Float(cx - r - 4 * sc, cy - r - 4 * sc, r * 2, r * 2));
        g.draw(new Line2D.Float(cx + 4 * sc, cy + 4 * sc, cx + 16 * sc, cy + 16 * sc));
        // Code brackets inside
        g.setColor(ACCENT);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Line2D.Float(cx - 9 * sc, cy - 8 * sc, cx - 13 * sc, cy - 4 * sc));
        g.draw(new Line2D.Float(cx - 13 * sc, cy - 4 * sc, cx - 9 * sc, cy));
    }

    void drawController(Graphics2D g, int size) {
        float sc = s(size);
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        float startX = 10 * sc;
        g.draw(new Line2D.Float(startX, 8 * sc, startX, 40 * sc));
        // Branches
        g.setColor(ACCENT);
        g.draw(new Line2D.Float(startX, 14 * sc, 38 * sc, 14 * sc));
        g.setColor(INFO);
        g.draw(new Line2D.Float(startX, 24 * sc, 32 * sc, 24 * sc));
        g.setColor(PRIMARY);
        g.draw(new Line2D.Float(startX, 34 * sc, 38 * sc, 34 * sc));
        // Dots at branch ends
        float dotR = 3 * sc;
        fillWithOutline(g, new Ellipse2D.Float(38 * sc - dotR, 14 * sc - dotR, dotR * 2, dotR * 2),
                ACCENT, ACCENT.darker(), size);
        fillWithOutline(g, new Ellipse2D.Float(32 * sc - dotR, 24 * sc - dotR, dotR * 2, dotR * 2),
                INFO, INFO.darker(), size);
        fillWithOutline(g, new Ellipse2D.Float(38 * sc - dotR, 34 * sc - dotR, dotR * 2, dotR * 2),
                PRIMARY, PRIMARY.darker(), size);
    }

    void drawSampler(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        // Lightning bolt
        Path2D bolt = new Path2D.Float();
        bolt.moveTo(cx + 4 * sc, 4 * sc);
        bolt.lineTo(cx - 8 * sc, cy + 2 * sc);
        bolt.lineTo(cx + 2 * sc, cy + 2 * sc);
        bolt.lineTo(cx - 4 * sc, 44 * sc);
        bolt.lineTo(cx + 12 * sc, cy - 2 * sc);
        bolt.lineTo(cx + 2 * sc, cy - 2 * sc);
        bolt.closePath();
        Color boltFill = new Color(WARNING.getRed(), WARNING.getGreen(), WARNING.getBlue(), 80);
        fillWithOutline(g, bolt, boltFill, WARNING, size);
    }

    void drawAssertion(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        // Shield
        Path2D shield = new Path2D.Float();
        shield.moveTo(cx, 6 * sc);
        shield.lineTo(cx + 16 * sc, 12 * sc);
        shield.lineTo(cx + 16 * sc, 26 * sc);
        shield.quadTo(cx + 16 * sc, 40 * sc, cx, 44 * sc);
        shield.quadTo(cx - 16 * sc, 40 * sc, cx - 16 * sc, 26 * sc);
        shield.lineTo(cx - 16 * sc, 12 * sc);
        shield.closePath();
        Color shieldFill = new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 50);
        fillWithOutline(g, shield, shieldFill, ACCENT, size);
        // Checkmark
        g.setColor(SUCCESS);
        g.setStroke(stroke(size, 3f));
        g.draw(new Line2D.Float(cx - 7 * sc, cy + 2 * sc, cx - 2 * sc, cy + 8 * sc));
        g.draw(new Line2D.Float(cx - 2 * sc, cy + 8 * sc, cx + 8 * sc, cy - 6 * sc));
    }

    void drawWorkbench(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Line2D.Float(cx - 10 * sc, cy + 10 * sc, cx + 6 * sc, cy - 6 * sc));
        g.draw(new Arc2D.Float(cx + 2 * sc, cy - 16 * sc, 16 * sc, 16 * sc, 30, 300, Arc2D.OPEN));
        // Dot
        float dotR = 3 * sc;
        fillWithOutline(g, new Ellipse2D.Float(cx - 10 * sc - dotR, cy + 10 * sc - dotR, dotR * 2, dotR * 2),
                ACCENT, ACCENT.darker(), size);
    }

    // ==================== TOOLBAR ICONS ====================

    void drawNewDocument(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f;
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2f));
        Path2D page = new Path2D.Float();
        page.moveTo(10 * sc, 4 * sc);
        page.lineTo(30 * sc, 4 * sc);
        page.lineTo(38 * sc, 12 * sc);
        page.lineTo(38 * sc, 44 * sc);
        page.lineTo(10 * sc, 44 * sc);
        page.closePath();
        g.draw(page);
        g.draw(new Line2D.Float(30 * sc, 4 * sc, 30 * sc, 12 * sc));
        g.draw(new Line2D.Float(30 * sc, 12 * sc, 38 * sc, 12 * sc));
        // Plus sign
        g.setColor(SUCCESS);
        g.setStroke(stroke(size, 2.5f));
        float cy = size / 2f + 4 * sc;
        g.draw(new Line2D.Float(cx - 6 * sc, cy, cx + 6 * sc, cy));
        g.draw(new Line2D.Float(cx, cy - 6 * sc, cx, cy + 6 * sc));
    }

    void drawTemplates(Graphics2D g, int size) {
        float sc = s(size);
        g.setStroke(stroke(size, 2f));
        float gap = 3 * sc;
        float boxW = 15 * sc;
        float startX = 7 * sc, startY = 7 * sc;
        // 4 boxes with different colors
        fillWithOutline(g, new RoundRectangle2D.Float(startX, startY, boxW, boxW, 3 * sc, 3 * sc),
                new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 30), ACCENT, size);
        fillWithOutline(g, new RoundRectangle2D.Float(startX + boxW + gap, startY, boxW, boxW, 3 * sc, 3 * sc),
                new Color(INFO.getRed(), INFO.getGreen(), INFO.getBlue(), 30), INFO, size);
        fillWithOutline(g, new RoundRectangle2D.Float(startX, startY + boxW + gap, boxW, boxW, 3 * sc, 3 * sc),
                new Color(SUCCESS.getRed(), SUCCESS.getGreen(), SUCCESS.getBlue(), 30), SUCCESS, size);
        fillWithOutline(g, new RoundRectangle2D.Float(startX + boxW + gap, startY + boxW + gap, boxW, boxW, 3 * sc, 3 * sc),
                new Color(WARNING.getRed(), WARNING.getGreen(), WARNING.getBlue(), 30), WARNING, size);
    }

    void drawOpenDocument(Graphics2D g, int size) {
        float sc = s(size);
        g.setStroke(stroke(size, 2f));
        Path2D folder = new Path2D.Float();
        folder.moveTo(6 * sc, 14 * sc);
        folder.lineTo(6 * sc, 40 * sc);
        folder.lineTo(42 * sc, 40 * sc);
        folder.lineTo(42 * sc, 18 * sc);
        folder.lineTo(26 * sc, 18 * sc);
        folder.lineTo(22 * sc, 14 * sc);
        folder.closePath();
        Color folderFill = new Color(WARNING.getRed(), WARNING.getGreen(), WARNING.getBlue(), 50);
        fillWithOutline(g, folder, folderFill, WARNING, size);
        // Tab line
        g.setColor(WARNING.darker());
        g.setStroke(stroke(size, 2.0f));
        g.draw(new Line2D.Float(6 * sc, 22 * sc, 42 * sc, 22 * sc));
    }

    void drawSaveDocument(Graphics2D g, int size) {
        float sc = s(size);
        g.setStroke(stroke(size, 2f));
        RoundRectangle2D body = new RoundRectangle2D.Float(6 * sc, 6 * sc, 36 * sc, 36 * sc, 4 * sc, 4 * sc);
        Color bodyFill = new Color(INFO.getRed(), INFO.getGreen(), INFO.getBlue(), 30);
        fillWithOutline(g, body, bodyFill, PRIMARY, size);
        // Top tab
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2f));
        g.draw(new RoundRectangle2D.Float(14 * sc, 6 * sc, 16 * sc, 14 * sc, 2 * sc, 2 * sc));
        // Bottom label
        fillWithOutline(g,
                new RoundRectangle2D.Float(12 * sc, 28 * sc, 24 * sc, 14 * sc, 3 * sc, 3 * sc),
                new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 40), ACCENT, size);
    }

    void drawCut(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        // Scissor blades
        g.draw(new Line2D.Float(cx, cy, cx - 12 * sc, 8 * sc));
        g.draw(new Line2D.Float(cx, cy, cx + 12 * sc, 8 * sc));
        // Circles at bottom with fill
        float r = 5 * sc;
        fillWithOutline(g, new Ellipse2D.Float(cx - 8 * sc - r, 36 * sc - r, r * 2, r * 2),
                new Color(DANGER.getRed(), DANGER.getGreen(), DANGER.getBlue(), 40), DANGER, size);
        fillWithOutline(g, new Ellipse2D.Float(cx + 8 * sc - r, 36 * sc - r, r * 2, r * 2),
                new Color(DANGER.getRed(), DANGER.getGreen(), DANGER.getBlue(), 40), DANGER, size);
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Line2D.Float(cx, cy, cx - 8 * sc, 36 * sc));
        g.draw(new Line2D.Float(cx, cy, cx + 8 * sc, 36 * sc));
    }

    void drawCopy(Graphics2D g, int size) {
        float sc = s(size);
        g.setStroke(stroke(size, 2f));
        // Back page
        Color pageFill = new Color(SECONDARY.getRed(), SECONDARY.getGreen(), SECONDARY.getBlue(), 30);
        fillWithOutline(g, new RoundRectangle2D.Float(12 * sc, 4 * sc, 28 * sc, 32 * sc, 3 * sc, 3 * sc),
                pageFill, SECONDARY, size);
        // Front page
        Color frontFill = new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 20);
        fillWithOutline(g, new RoundRectangle2D.Float(6 * sc, 12 * sc, 28 * sc, 32 * sc, 3 * sc, 3 * sc),
                frontFill, PRIMARY, size);
    }

    void drawPaste(Graphics2D g, int size) {
        float sc = s(size);
        g.setStroke(stroke(size, 2f));
        // Clipboard
        Color clipFill = new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 15);
        fillWithOutline(g, new RoundRectangle2D.Float(8 * sc, 10 * sc, 32 * sc, 34 * sc, 3 * sc, 3 * sc),
                clipFill, PRIMARY, size);
        // Clip at top
        fillWithOutline(g, new RoundRectangle2D.Float(16 * sc, 6 * sc, 16 * sc, 8 * sc, 3 * sc, 3 * sc),
                ACCENT, ACCENT.darker(), size);
        // Lines
        g.setColor(SECONDARY);
        g.setStroke(stroke(size, 2f));
        g.draw(new Line2D.Float(14 * sc, 24 * sc, 34 * sc, 24 * sc));
        g.draw(new Line2D.Float(14 * sc, 30 * sc, 30 * sc, 30 * sc));
        g.draw(new Line2D.Float(14 * sc, 36 * sc, 32 * sc, 36 * sc));
    }

    void drawUndo(Graphics2D g, int size) {
        float sc = s(size);
        g.setColor(ACCENT);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Arc2D.Float(10 * sc, 12 * sc, 28 * sc, 28 * sc, 180, -180, Arc2D.OPEN));
        g.draw(new Line2D.Float(10 * sc, 26 * sc, 10 * sc, 18 * sc));
        g.draw(new Line2D.Float(10 * sc, 26 * sc, 18 * sc, 26 * sc));
    }

    void drawRedo(Graphics2D g, int size) {
        float sc = s(size);
        g.setColor(ACCENT);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Arc2D.Float(10 * sc, 12 * sc, 28 * sc, 28 * sc, 0, 180, Arc2D.OPEN));
        g.draw(new Line2D.Float(38 * sc, 26 * sc, 38 * sc, 18 * sc));
        g.draw(new Line2D.Float(38 * sc, 26 * sc, 30 * sc, 26 * sc));
    }

    void drawStart(Graphics2D g, int size) {
        float sc = s(size);
        Path2D tri = new Path2D.Float();
        tri.moveTo(14 * sc, 8 * sc);
        tri.lineTo(38 * sc, size / 2f);
        tri.lineTo(14 * sc, 40 * sc);
        tri.closePath();
        fillWithOutline(g, tri, SUCCESS, SUCCESS.darker(), size);
    }

    void drawStartNoTimer(Graphics2D g, int size) {
        drawStart(g, size);
        float sc = s(size);
        // "No timer" badge
        float cx = 34 * sc, cy = 10 * sc;
        Ellipse2D badge = new Ellipse2D.Float(cx - 6 * sc, cy - 6 * sc, 12 * sc, 12 * sc);
        fillWithOutline(g, badge, WARNING, WARNING.darker(), size);
        g.setColor(isDark ? Color.WHITE : Color.WHITE);
        g.setStroke(stroke(size, 2f));
        g.draw(new Line2D.Float(cx - 3 * sc, cy + 3 * sc, cx + 3 * sc, cy - 3 * sc));
    }

    void drawStop(Graphics2D g, int size) {
        float sc = s(size);
        float pad = 10 * sc;
        RoundRectangle2D sq = new RoundRectangle2D.Float(pad, pad,
                size - pad * 2, size - pad * 2, 4 * sc, 4 * sc);
        fillWithOutline(g, sq, DANGER, DANGER.darker(), size);
    }

    void drawShutdown(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        g.setColor(DANGER);
        g.setStroke(stroke(size, 3f));
        g.draw(new Arc2D.Float(cx - 14 * sc, cy - 10 * sc, 28 * sc, 28 * sc, 40, 280, Arc2D.OPEN));
        g.draw(new Line2D.Float(cx, cy - 16 * sc, cx, cy));
    }

    void drawClear(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Line2D.Float(cx - 10 * sc, cy + 12 * sc, cx + 10 * sc, cy - 12 * sc));
        // Brush head with fill
        Path2D brush = new Path2D.Float();
        brush.moveTo(cx + 6 * sc, cy - 8 * sc);
        brush.lineTo(cx + 18 * sc, cy - 14 * sc);
        brush.lineTo(cx + 14 * sc, cy - 20 * sc);
        brush.lineTo(cx + 2 * sc, cy - 14 * sc);
        brush.closePath();
        fillWithOutline(g, brush, ACCENT, ACCENT.darker(), size);
    }

    void drawClearAll(Graphics2D g, int size) {
        drawClear(g, size);
        float sc = s(size);
        g.setColor(WARNING);
        g.setStroke(stroke(size, 2f));
        g.draw(new Line2D.Float(6 * sc, 40 * sc, 42 * sc, 40 * sc));
        g.draw(new Line2D.Float(10 * sc, 44 * sc, 38 * sc, 44 * sc));
    }

    void drawExpand(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        float r = 14 * sc;
        Ellipse2D circle = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
        Color circleFill = new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 20);
        fillWithOutline(g, circle, circleFill, PRIMARY, size);
        g.setColor(SUCCESS);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Line2D.Float(cx - 8 * sc, cy, cx + 8 * sc, cy));
        g.draw(new Line2D.Float(cx, cy - 8 * sc, cx, cy + 8 * sc));
    }

    void drawCollapse(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        float r = 14 * sc;
        Ellipse2D circle = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
        Color circleFill = new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 20);
        fillWithOutline(g, circle, circleFill, PRIMARY, size);
        g.setColor(DANGER);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Line2D.Float(cx - 8 * sc, cy, cx + 8 * sc, cy));
    }

    void drawToggle(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        // Toggle track
        RoundRectangle2D track = new RoundRectangle2D.Float(
                8 * sc, cy - 8 * sc, 32 * sc, 16 * sc, 16 * sc, 16 * sc);
        Color trackFill = new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 80);
        fillWithOutline(g, track, trackFill, ACCENT, size);
        // Knob
        float knobR = 6 * sc;
        Ellipse2D knob = new Ellipse2D.Float(28 * sc - knobR, cy - knobR, knobR * 2, knobR * 2);
        fillWithOutline(g, knob, isDark ? new Color(0xCD, 0xCE, 0xCF) : Color.WHITE,
                ACCENT, size);
    }

    void drawSearch(Graphics2D g, int size) {
        float sc = s(size);
        float r = 12 * sc;
        float cx = size / 2f - 4 * sc, cy = size / 2f - 4 * sc;
        Ellipse2D lens = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
        Color lensFill = new Color(INFO.getRed(), INFO.getGreen(), INFO.getBlue(), 20);
        fillWithOutline(g, lens, lensFill, PRIMARY, size);
        // Handle
        g.setColor(PRIMARY);
        g.setStroke(stroke(size, 3f));
        g.draw(new Line2D.Float(cx + r * 0.7f, cy + r * 0.7f,
                cx + r * 0.7f + 10 * sc, cy + r * 0.7f + 10 * sc));
    }

    void drawSearchReset(Graphics2D g, int size) {
        drawSearch(g, size);
        float sc = s(size);
        // X badge
        float bx = 36 * sc, by = 12 * sc;
        Ellipse2D badge = new Ellipse2D.Float(bx - 5 * sc, by - 5 * sc, 10 * sc, 10 * sc);
        fillWithOutline(g, badge, DANGER, DANGER.darker(), size);
        g.setColor(isDark ? new Color(0xCD, 0xCE, 0xCF) : Color.WHITE);
        g.setStroke(stroke(size, 2f));
        g.draw(new Line2D.Float(bx - 3 * sc, by - 3 * sc, bx + 3 * sc, by + 3 * sc));
        g.draw(new Line2D.Float(bx + 3 * sc, by - 3 * sc, bx - 3 * sc, by + 3 * sc));
    }

    void drawFunctionHelper(Graphics2D g, int size) {
        float sc = s(size);
        g.setStroke(stroke(size, 2f));
        // Document
        Color docFill = new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 15);
        fillWithOutline(g, new RoundRectangle2D.Float(8 * sc, 4 * sc, 32 * sc, 40 * sc, 4 * sc, 4 * sc),
                docFill, PRIMARY, size);
        // f(x) text
        g.setColor(ACCENT);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (14 * sc)));
        FontMetrics fm = g.getFontMetrics();
        String text = "f(x)";
        int tw = fm.stringWidth(text);
        g.drawString(text, (size - tw) / 2f, size / 2f + fm.getAscent() / 3f);
    }

    void drawHelp(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        float r = 16 * sc;
        Ellipse2D circle = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
        Color circleFill = new Color(INFO.getRed(), INFO.getGreen(), INFO.getBlue(), 30);
        fillWithOutline(g, circle, circleFill, INFO, size);
        // Question mark
        g.setColor(isDark ? new Color(0xCD, 0xCE, 0xCF) : new Color(0x3C, 0x3C, 0x3C));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (22 * sc)));
        FontMetrics fm = g.getFontMetrics();
        String text = "?";
        int tw = fm.stringWidth(text);
        g.drawString(text, (size - tw) / 2f, cy + fm.getAscent() / 3f);
    }

    // ==================== STATUS ICONS ====================

    void drawUserOnline(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f;
        float hr = 7 * sc;
        Ellipse2D head = new Ellipse2D.Float(cx - hr, 6 * sc, hr * 2, hr * 2);
        fillWithOutline(g, head, SUCCESS, SUCCESS.darker(), size);
        Path2D body = new Path2D.Float();
        body.moveTo(cx - 14 * sc, 40 * sc);
        body.quadTo(cx - 14 * sc, 22 * sc, cx, 22 * sc);
        body.quadTo(cx + 14 * sc, 22 * sc, cx + 14 * sc, 40 * sc);
        body.closePath();
        fillWithOutline(g, body, SUCCESS, SUCCESS.darker(), size);
    }

    void drawUserOffline(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f;
        float hr = 7 * sc;
        Ellipse2D head = new Ellipse2D.Float(cx - hr, 6 * sc, hr * 2, hr * 2);
        fillWithOutline(g, head, MUTED, MUTED.darker(), size);
        Path2D body = new Path2D.Float();
        body.moveTo(cx - 14 * sc, 40 * sc);
        body.quadTo(cx - 14 * sc, 22 * sc, cx, 22 * sc);
        body.quadTo(cx + 14 * sc, 22 * sc, cx + 14 * sc, 40 * sc);
        body.closePath();
        fillWithOutline(g, body, MUTED, MUTED.darker(), size);
    }

    void drawWarning(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f;
        Path2D tri = new Path2D.Float();
        tri.moveTo(cx, 6 * sc);
        tri.lineTo(42 * sc, 42 * sc);
        tri.lineTo(6 * sc, 42 * sc);
        tri.closePath();
        Color triFill = new Color(WARNING.getRed(), WARNING.getGreen(), WARNING.getBlue(), 60);
        fillWithOutline(g, tri, triFill, WARNING, size);
        // Exclamation
        g.setColor(WARNING);
        g.setStroke(stroke(size, 3f));
        g.draw(new Line2D.Float(cx, 20 * sc, cx, 32 * sc));
        fillWithOutline(g, new Ellipse2D.Float(cx - 2 * sc, 36 * sc, 4 * sc, 4 * sc),
                WARNING, WARNING.darker(), size);
    }

    void drawError(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        float r = 16 * sc;
        Ellipse2D circle = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
        Color circleFill = new Color(DANGER.getRed(), DANGER.getGreen(), DANGER.getBlue(), 50);
        fillWithOutline(g, circle, circleFill, DANGER, size);
        g.setColor(DANGER);
        g.setStroke(stroke(size, 3f));
        g.draw(new Line2D.Float(cx - 8 * sc, cy - 8 * sc, cx + 8 * sc, cy + 8 * sc));
        g.draw(new Line2D.Float(cx + 8 * sc, cy - 8 * sc, cx - 8 * sc, cy + 8 * sc));
    }

    void drawTaskComplete(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        float r = 16 * sc;
        Ellipse2D circle = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
        Color circleFill = new Color(SUCCESS.getRed(), SUCCESS.getGreen(), SUCCESS.getBlue(), 30);
        fillWithOutline(g, circle, circleFill, SUCCESS, size);
        g.setColor(SUCCESS);
        g.setStroke(stroke(size, 3f));
        g.draw(new Line2D.Float(cx - 8 * sc, cy, cx - 2 * sc, cy + 8 * sc));
        g.draw(new Line2D.Float(cx - 2 * sc, cy + 8 * sc, cx + 10 * sc, cy - 8 * sc));
    }

    void drawTaskRecurring(Graphics2D g, int size) {
        float sc = s(size);
        float cx = size / 2f, cy = size / 2f;
        g.setColor(INFO);
        g.setStroke(stroke(size, 2.5f));
        g.draw(new Arc2D.Float(cx - 14 * sc, cy - 14 * sc, 28 * sc, 28 * sc, 30, 240, Arc2D.OPEN));
        // Arrowhead
        float ax = cx + 14 * sc * (float) Math.cos(Math.toRadians(30));
        float ay = cy - 14 * sc * (float) Math.sin(Math.toRadians(30));
        g.draw(new Line2D.Float(ax, ay, ax - 4 * sc, ay - 4 * sc));
        g.draw(new Line2D.Float(ax, ay, ax + 4 * sc, ay - 2 * sc));
    }
}
