package com.mycompany.peerdesk.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * BotanicalTheme provides color palettes, robust font hierarchies, and custom Java2D
 * botanical/floral rendering routines for PeerDesk.
 * All emojis and unsupported Unicode characters have been completely removed to prevent
 * broken square box (□) artifacts across all platforms and fonts.
 */
public final class BotanicalTheme {

    private BotanicalTheme() {}

    // --- FONT AVAILABILITY & HIERARCHY ---
    public static final String SERIF_FAMILY;
    public static final String SANS_FAMILY;

    static {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));
        SERIF_FAMILY = available.contains("Georgia") ? "Georgia" : Font.SERIF;
        SANS_FAMILY = available.contains("Segoe UI") ? "Segoe UI" : (available.contains("Arial") ? "Arial" : Font.SANS_SERIF);
    }

    public static Font fontSerifTitle(float size) {
        return new Font(SERIF_FAMILY, Font.BOLD, (int) size);
    }

    public static Font fontSerif(float size, int style) {
        return new Font(SERIF_FAMILY, style, (int) size);
    }

    public static Font fontSans(float size, int style) {
        return new Font(SANS_FAMILY, style, (int) size);
    }

    // Pre-computed standard typography
    public static final Font FONT_TITLE_MAIN    = fontSerifTitle(26);       // Main app titles (24-28px)
    public static final Font FONT_TITLE_SECTION = fontSerifTitle(18);       // Section titles (17-20px)
    public static final Font FONT_BUTTON        = fontSans(14, Font.BOLD);  // Buttons (13-15px)
    public static final Font FONT_INPUT         = fontSans(13, Font.PLAIN); // Text input (13-14px)
    public static final Font FONT_CHAT          = fontSans(12.5f, Font.PLAIN); // Chat text (12-13px)
    public static final Font FONT_LABEL         = fontSans(12, Font.BOLD);  // Form/mini labels (12-14px)
    public static final Font FONT_TIMESTAMP     = fontSans(11, Font.PLAIN); // Timestamps (11-12px)
    public static final Font FONT_STATUS_BADGE  = fontSans(12, Font.BOLD);  // Status pills

    // --- COLOR PALETTE ---
    // Pinks & Blush
    public static final Color BUBBLEGUM_PINK  = new Color(255, 133, 161); // #FF85A1
    public static final Color BUBBLEGUM_SOFT  = new Color(255, 179, 198); // #FFB3C6
    public static final Color BLUSH_PINK      = new Color(255, 214, 224); // #FFD6E0
    public static final Color BLUSH_LIGHT     = new Color(255, 240, 243); // #FFF0F3
    public static final Color DEEP_ROSE       = new Color(158, 42, 75);   // #9E2A4B (high-contrast text)

    // Warm Creams & Ivory
    public static final Color CREAM_BG        = new Color(253, 251, 247); // #FDFBF7
    public static final Color IVORY_CARD      = new Color(255, 253, 249); // #FFFDF9
    public static final Color WARM_PEACH      = new Color(255, 229, 217); // #FFE5D9

    // Greens (Sage & Lime)
    public static final Color SAGE_GREEN      = new Color(163, 201, 168); // #A3C9A8
    public static final Color SAGE_DARK       = new Color(45, 106, 79);   // #2D6A4F
    public static final Color LIME_ACCENT     = new Color(199, 249, 204); // #C7F9CC
    public static final Color MINT_SOFT       = new Color(216, 243, 220); // #D8F3DC
    public static final Color FOREST_DEEP     = new Color(27, 67, 50);    // #1B4332

    // Pastels (Yellow & Lavender)
    public static final Color PASTEL_YELLOW   = new Color(255, 243, 176); // #FFF3B0
    public static final Color PASTEL_AMBER    = new Color(181, 101, 29);  // For yellow tag text
    public static final Color PASTEL_LAVENDER = new Color(232, 215, 241); // #E8D7F1

    // Neutrals
    public static final Color CHARCOAL_TEXT   = new Color(43, 45, 66);    // #2B2D42
    public static final Color MUTED_TEXT      = new Color(125, 110, 120); // Soft plum-gray
    public static final Color BORDER_ROSE     = new Color(244, 194, 208); // Subtle pink border
    public static final Color CARD_SHADOW     = new Color(210, 150, 175, 45); // Soft translucent shadow

    /**
     * Paints a full-window pastel gradient background with organic wavy shapes
     * and decorative botanical flowers, petals, and leaves using pure Java2D vectors.
     */
    public static void paintFullBotanicalBackground(Graphics g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Base Multi-Stop Gradient (Blush -> Warm Peach -> Ivory -> Soft Mint)
        Point2D start = new Point2D.Float(0, 0);
        Point2D end = new Point2D.Float(w, h);
        float[] fractions = {0.0f, 0.35f, 0.70f, 1.0f};
        Color[] colors = {
                new Color(255, 220, 230),
                new Color(255, 238, 228),
                new Color(253, 250, 245),
                new Color(225, 245, 230)
        };
        LinearGradientPaint bgPaint = new LinearGradientPaint(start, end, fractions, colors);
        g2.setPaint(bgPaint);
        g2.fillRect(0, 0, w, h);

        // 2. Large Organic Curved Blobs
        g2.setColor(new Color(255, 182, 193, 40));
        Path2D wave1 = new Path2D.Float();
        wave1.moveTo(0, 0);
        wave1.curveTo(w * 0.35f, h * 0.05f, w * 0.2f, h * 0.45f, 0, h * 0.55f);
        wave1.closePath();
        g2.fill(wave1);

        g2.setColor(new Color(232, 215, 241, 55));
        Path2D wave2 = new Path2D.Float();
        wave2.moveTo(w, 0);
        wave2.curveTo(w * 0.7f, h * 0.15f, w * 0.85f, h * 0.5f, w, h * 0.6f);
        wave2.closePath();
        g2.fill(wave2);

        g2.setColor(new Color(183, 228, 199, 50));
        Path2D wave3 = new Path2D.Float();
        wave3.moveTo(w * 0.2f, h);
        wave3.curveTo(w * 0.45f, h * 0.75f, w * 0.8f, h * 0.85f, w, h * 0.75f);
        wave3.lineTo(w, h);
        wave3.closePath();
        g2.fill(wave3);

        // 3. Decorative Botanical Motifs (Java2D vectors, zero emojis)
        paintBlossom(g2, 50, 45, 28, BUBBLEGUM_SOFT, PASTEL_YELLOW);
        paintLeafStem(g2, 85, 30, 40, -35, SAGE_GREEN);
        paintBlossom(g2, 115, 75, 18, PASTEL_LAVENDER, PASTEL_YELLOW);

        paintLeafStem(g2, w - 80, h - 60, 55, 140, SAGE_GREEN);
        paintBlossom(g2, w - 55, h - 85, 26, BUBBLEGUM_SOFT, PASTEL_YELLOW);
        paintBlossom(g2, w - 110, h - 45, 16, WARM_PEACH, PASTEL_YELLOW);

        paintDaintyFlower(g2, w - 30, 80, 14, BLUSH_PINK);
        paintDaintyFlower(g2, 35, h - 50, 16, LIME_ACCENT);

        g2.dispose();
    }

    public static void paintBlossom(Graphics2D g2, int cx, int cy, int radius, Color petalColor, Color centerColor) {
        Color softPetal = new Color(petalColor.getRed(), petalColor.getGreen(), petalColor.getBlue(), 160);
        g2.setColor(softPetal);

        int petalDist = (int) (radius * 0.65);
        int petalSize = (int) (radius * 0.85);

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 - 90);
            int px = cx + (int) (Math.cos(angle) * petalDist);
            int py = cy + (int) (Math.sin(angle) * petalDist);
            g2.fill(new Ellipse2D.Double(px - petalSize / 2.0, py - petalSize / 2.0, petalSize, petalSize));
        }

        g2.setColor(new Color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), 220));
        int centerRadius = (int) (radius * 0.45);
        g2.fill(new Ellipse2D.Double(cx - centerRadius / 2.0, cy - centerRadius / 2.0, centerRadius, centerRadius));
    }

    public static void paintDaintyFlower(Graphics2D g2, int cx, int cy, int size, Color color) {
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 140));
        g2.fillOval(cx - size / 2, cy - size / 2, size, size);
        g2.setColor(new Color(255, 255, 255, 180));
        int inner = size / 3;
        g2.fillOval(cx - inner / 2, cy - inner / 2, inner, inner);
    }

    public static void paintLeafStem(Graphics2D g2, int x, int y, int length, double angleDeg, Color leafColor) {
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(x, y);
        g.rotate(Math.toRadians(angleDeg));

        g.setColor(new Color(leafColor.getRed(), leafColor.getGreen(), leafColor.getBlue(), 150));
        g.drawArc(-10, 0, 40, length, 70, 100);

        int[] leafY = {length / 4, length / 2, (length * 3) / 4};
        for (int ly : leafY) {
            g.fill(new Ellipse2D.Double(-14, ly, 10, 6));
            g.fill(new Ellipse2D.Double(8, ly + 4, 10, 6));
        }
        g.fill(new Ellipse2D.Double(-3, length - 2, 7, 12));

        g.dispose();
    }
}
