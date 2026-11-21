package com.mycompany.peerdesk.ui;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.LayoutManager;
import javax.swing.JPanel;

/**
 * A full-window JPanel that renders a dreamy, multi-stop pastel gradient
 * with painted botanical flowers, foliage, and organic editorial curves.
 */
public class BotanicalBackgroundPanel extends JPanel {

    public BotanicalBackgroundPanel() {
        super(new BorderLayout());
        setOpaque(false);
    }

    public BotanicalBackgroundPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        BotanicalTheme.paintFullBotanicalBackground(g, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
