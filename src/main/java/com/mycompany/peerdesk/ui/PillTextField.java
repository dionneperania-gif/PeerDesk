package com.mycompany.peerdesk.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * A rounded text field with comfortable 14px Segoe UI typography,
 * soft filled cream background, and blush pink focus outline.
 */
public class PillTextField extends JTextField {

    private String placeholder = "";
    private Color fillColor = Color.WHITE;
    private Color borderColor = BotanicalTheme.BORDER_ROSE;
    private Color focusColor = BotanicalTheme.BUBBLEGUM_PINK;
    private int cornerRadius = 20;

    public PillTextField(int columns) {
        super(columns);
        initStyle();
    }

    public PillTextField(String text, int columns) {
        super(text, columns);
        initStyle();
    }

    private void initStyle() {
        setOpaque(false);
        setFont(BotanicalTheme.FONT_INPUT);
        setForeground(BotanicalTheme.CHARCOAL_TEXT);
        setCaretColor(BotanicalTheme.DEEP_ROSE);
        setBorder(new EmptyBorder(8, 14, 8, 14));
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth() - 1;
        int h = getHeight() - 1;

        // Fill background
        g2.setColor(isEnabled() ? fillColor : new Color(245, 240, 242));
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // Border (accentuated when focused)
        g2.setColor(hasFocus() ? focusColor : borderColor);
        g2.drawRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);

        // Placeholder text
        if (getText().isEmpty() && !placeholder.isEmpty() && !hasFocus()) {
            Graphics2D gP = (Graphics2D) g.create();
            gP.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gP.setColor(new Color(155, 140, 150));
            gP.setFont(getFont().deriveFont(Font.ITALIC, 13f));
            Insets insets = getInsets();
            int fontAscent = gP.getFontMetrics().getAscent();
            gP.drawString(placeholder, insets.left, (getHeight() + fontAscent) / 2 - 2);
            gP.dispose();
        }
    }
}
