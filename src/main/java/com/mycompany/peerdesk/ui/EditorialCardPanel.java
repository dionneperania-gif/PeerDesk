package com.mycompany.peerdesk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * EditorialCardPanel is a modern floating card container with rounded corners,
 * warm ivory/cream filled background, subtle blush outline, and a soft shadow.
 */
public class EditorialCardPanel extends JPanel {

    private int cornerRadius = 22;
    private Color fillColor = BotanicalTheme.IVORY_CARD;
    private Color borderColor = BotanicalTheme.BORDER_ROSE;
    private boolean showShadow = true;

    public EditorialCardPanel() {
        this(new BorderLayout(), 22);
    }

    public EditorialCardPanel(LayoutManager layout) {
        this(layout, 22);
    }

    public EditorialCardPanel(LayoutManager layout, int cornerRadius) {
        super(layout);
        this.cornerRadius = cornerRadius;
        setOpaque(false);
        setBorder(new EmptyBorder(12, 14, 12, 14));
    }

    public void setFillColor(Color color) {
        this.fillColor = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setShowShadow(boolean show) {
        this.showShadow = show;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int shadowOffset = showShadow ? 3 : 0;
        int w = getWidth() - shadowOffset - 2;
        int h = getHeight() - shadowOffset - 2;

        // 1. Drop Shadow
        if (showShadow) {
            g2.setColor(BotanicalTheme.CARD_SHADOW);
            g2.fillRoundRect(shadowOffset, shadowOffset, w, h, cornerRadius, cornerRadius);
        }

        // 2. Card Background Fill (Warm Cream / Ivory / Blush)
        g2.setColor(fillColor);
        g2.fillRoundRect(1, 1, w, h, cornerRadius, cornerRadius);

        // 3. Delicate Stroke / Border
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(1, 1, w, h, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
