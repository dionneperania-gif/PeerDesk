package com.mycompany.peerdesk.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * Modern pill-shaped button with solid pastel filled background,
 * comfortable padding, Segoe UI / Sans font (14px Bold), and clean text labels.
 */
public class PillButton extends JButton {

    private Color bgNormal;
    private Color bgHover;
    private Color bgPressed;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 24;

    public PillButton(String text, Color background, Color foreground) {
        super(text);
        this.bgNormal = background;
        this.bgHover = brighten(background, 0.12f);
        this.bgPressed = darken(background, 0.15f);

        setForeground(foreground);
        setFont(BotanicalTheme.FONT_BUTTON);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(new EmptyBorder(8, 18, 8, 18));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setButtonColors(Color normal, Color foreground) {
        this.bgNormal = normal;
        this.bgHover = brighten(normal, 0.12f);
        this.bgPressed = darken(normal, 0.15f);
        setForeground(foreground);
        repaint();
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        setCursor(b ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int radius = Math.min(cornerRadius, h);

        Color currentBg;
        if (!isEnabled()) {
            currentBg = new Color(230, 225, 230);
        } else if (isPressed) {
            currentBg = bgPressed;
        } else if (isHovered) {
            currentBg = bgHover;
        } else {
            currentBg = bgNormal;
        }

        g2.setColor(currentBg);
        g2.fillRoundRect(0, 0, w, h, radius, radius);

        g2.setColor(new Color(255, 255, 255, 75));
        g2.drawRoundRect(1, 1, w - 2, h - 2, radius, radius);

        g2.dispose();
        super.paintComponent(g);
    }

    private static Color brighten(Color c, float factor) {
        int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * factor));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * factor));
        int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * factor));
        return new Color(r, g, b, c.getAlpha());
    }

    private static Color darken(Color c, float factor) {
        int r = Math.max(0, (int) (c.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (c.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (c.getBlue() * (1 - factor)));
        return new Color(r, g, b, c.getAlpha());
    }
}
