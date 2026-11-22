package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Whimsy-themed dialog displaying the ratings and feedback that an Active Classmate
 * has received from requesters they helped.
 * Shows average star rating, total ratings received, and individual breakdown of feedback.
 */
public class ClassmateRatingsDialog extends JDialog {

    public ClassmateRatingsDialog(JFrame parent, String classmateName, double avgRating, int totalCount, String details) {
        super(parent, "Peer Rating Profile", true);
        setSize(500, 420);
        setLocationRelativeTo(parent);
        setResizable(false);

        BotanicalBackgroundPanel bg = new BotanicalBackgroundPanel(new BorderLayout());
        bg.setBorder(new EmptyBorder(14, 14, 14, 14));

        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(0, 12), 20);
        card.setFillColor(BotanicalTheme.IVORY_CARD);
        card.setBorderColor(BotanicalTheme.BORDER_ROSE);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Header
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JLabel titleLbl = new JLabel(classmateName + " - Helper Profile");
        titleLbl.setFont(BotanicalTheme.fontSerifTitle(16));
        titleLbl.setForeground(BotanicalTheme.FOREST_DEEP);
        header.add(titleLbl, BorderLayout.WEST);

        JLabel badge = new JLabel(" CLASSMATE RATINGS ");
        badge.setFont(BotanicalTheme.fontSans(10, Font.BOLD));
        badge.setOpaque(true);
        badge.setBackground(BotanicalTheme.BLUSH_PINK);
        badge.setForeground(BotanicalTheme.DEEP_ROSE);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        header.add(badge, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Center: Summary Card + Scrollable Feedback List
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        // Summary Card
        EditorialCardPanel summaryCard = new EditorialCardPanel(new GridBagLayout(), 14);
        summaryCard.setFillColor(new Color(255, 248, 250));
        summaryCard.setBorderColor(new Color(245, 225, 232));
        summaryCard.setBorder(new EmptyBorder(10, 14, 10, 14));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;

        JLabel avgNumberLbl = new JLabel(String.format(java.util.Locale.US, "%.1f", avgRating));
        avgNumberLbl.setFont(BotanicalTheme.fontSerifTitle(28));
        avgNumberLbl.setForeground(new Color(218, 140, 0)); // Golden amber
        summaryCard.add(avgNumberLbl, gbc);

        gbc.gridx = 1;
        JPanel starVisualPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        starVisualPanel.setOpaque(false);
        for (int i = 1; i <= 5; i++) {
            boolean full = i <= Math.round(avgRating);
            starVisualPanel.add(new MiniStar(full));
        }
        summaryCard.add(starVisualPanel, gbc);

        gbc.gridx = 2; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.EAST;
        String countText = totalCount == 1 ? "1 rating received" : totalCount + " ratings received";
        JLabel countLbl = new JLabel(countText);
        countLbl.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        countLbl.setForeground(BotanicalTheme.MUTED_TEXT);
        summaryCard.add(countLbl, gbc);

        center.add(summaryCard, BorderLayout.NORTH);

        // Feedback List
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new javax.swing.BoxLayout(listPanel, javax.swing.BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        List<RatingEntry> entries = parseDetails(details);
        if (entries.isEmpty()) {
            JPanel emptyRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
            emptyRow.setOpaque(false);
            JLabel emptyLbl = new JLabel("No ratings received yet. Helper hasn't been reviewed on any resolved tickets.");
            emptyLbl.setFont(BotanicalTheme.fontSans(11.5f, Font.ITALIC));
            emptyLbl.setForeground(BotanicalTheme.MUTED_TEXT);
            emptyRow.add(emptyLbl);
            listPanel.add(emptyRow);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                RatingEntry entry = entries.get(i);
                listPanel.add(buildRatingRow(entry, i % 2 == 0));
                listPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1));
        scroll.getViewport().setBackground(new Color(255, 253, 250));
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        center.add(scroll, BorderLayout.CENTER);

        card.add(center, BorderLayout.CENTER);

        // Bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);
        PillButton closeBtn = new PillButton("Close", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        closeBtn.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        closeBtn.setPreferredSize(new Dimension(85, 32));
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);

        card.add(bottom, BorderLayout.SOUTH);
        bg.add(card, BorderLayout.CENTER);
        setContentPane(bg);

        // ESC close
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc_close");
        getRootPane().getActionMap().put("esc_close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private JPanel buildRatingRow(RatingEntry entry, boolean altColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(true);
        row.setBackground(altColor ? Color.WHITE : new Color(254, 250, 252));
        row.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);

        JLabel reqLbl = new JLabel(entry.requester + " (Ticket #" + entry.ticketId + "):");
        reqLbl.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        reqLbl.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        left.add(reqLbl);

        for (int i = 1; i <= 5; i++) {
            left.add(new MiniStar(i <= entry.stars));
        }
        row.add(left, BorderLayout.WEST);

        JLabel timeLbl = new JLabel(entry.timestamp);
        timeLbl.setFont(BotanicalTheme.fontSans(11, Font.PLAIN));
        timeLbl.setForeground(BotanicalTheme.MUTED_TEXT);
        row.add(timeLbl, BorderLayout.EAST);

        return row;
    }

    private static class RatingEntry {
        String requester;
        int stars;
        String timestamp;
        int ticketId;
    }

    private List<RatingEntry> parseDetails(String details) {
        List<RatingEntry> list = new ArrayList<>();
        if (details == null || details.trim().isEmpty()) return list;
        String[] items = details.split(";");
        for (String item : items) {
            String[] parts = item.split("~");
            if (parts.length >= 3) {
                try {
                    RatingEntry e = new RatingEntry();
                    e.requester = parts[0];
                    e.stars = Integer.parseInt(parts[1]);
                    e.timestamp = parts[2];
                    e.ticketId = parts.length >= 4 ? Integer.parseInt(parts[3]) : 0;
                    list.add(e);
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private static class MiniStar extends JComponent {
        private final boolean filled;

        public MiniStar(boolean filled) {
            this.filled = filled;
            setPreferredSize(new Dimension(16, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            double cx = w / 2.0;
            double cy = h / 2.0;
            double outerR = Math.min(w, h) * 0.44;
            double innerR = outerR * 0.45;

            Path2D.Double star = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2.0 + i * (Math.PI / 5.0);
                double r = (i % 2 == 0) ? outerR : innerR;
                double x = cx + r * Math.cos(angle);
                double y = cy + r * Math.sin(angle);
                if (i == 0) {
                    star.moveTo(x, y);
                } else {
                    star.lineTo(x, y);
                }
            }
            star.closePath();

            if (filled) {
                g2.setColor(new Color(255, 195, 0));
                g2.fill(star);
                g2.setColor(new Color(218, 140, 0));
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                g2.draw(star);
            } else {
                g2.setColor(new Color(240, 235, 238));
                g2.fill(star);
                g2.setColor(new Color(210, 195, 202));
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                g2.draw(star);
            }
            g2.dispose();
        }
    }
}
