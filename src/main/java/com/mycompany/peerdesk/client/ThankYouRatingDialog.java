package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.model.Ticket;
import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import com.mycompany.peerdesk.ui.WhimsicalDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Whimsy-themed post-resolution dialog with five interactive clickable star icons (1-5 stars),
 * similar to Play Store ratings.
 * Allows the original requester to award a star rating to the helper(s) who assisted them.
 */
public class ThankYouRatingDialog extends JDialog {

    private final PeerDeskClient client;
    private final Ticket ticket;
    private int selectedRating = 5; // Default 5 stars
    private int hoverRating = 0;
    private final StarWidget[] starWidgets = new StarWidget[5];
    private final JLabel ratingTextLabel;

    public ThankYouRatingDialog(JFrame parent, PeerDeskClient client, Ticket ticket) {
        super(parent, "Thank You! Rate Helper", true);
        this.client = client;
        this.ticket = ticket;

        setSize(480, 310);
        setLocationRelativeTo(parent);
        setResizable(false);

        BotanicalBackgroundPanel bg = new BotanicalBackgroundPanel(new BorderLayout());
        bg.setBorder(new EmptyBorder(14, 14, 14, 14));

        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(0, 10), 20);
        card.setFillColor(BotanicalTheme.IVORY_CARD);
        card.setBorderColor(BotanicalTheme.BORDER_ROSE);
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLbl = new JLabel("Thank You! Resolution Feedback");
        titleLbl.setFont(BotanicalTheme.fontSerifTitle(16));
        titleLbl.setForeground(BotanicalTheme.FOREST_DEEP);
        header.add(titleLbl, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new javax.swing.BoxLayout(centerPanel, javax.swing.BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        String helperDisplay = ticket.getHelper();
        if (helperDisplay == null || helperDisplay.trim().isEmpty() || "None".equalsIgnoreCase(helperDisplay)) {
            helperDisplay = "the helper";
        }

        JTextArea descArea = new JTextArea("Ticket #" + ticket.getId() + " is marked resolved! How helpful was " + helperDisplay + "? Tap the stars below to rate:");
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setEditable(false);
        descArea.setFocusable(false);
        descArea.setOpaque(false);
        descArea.setFont(BotanicalTheme.fontSans(12.5f, Font.PLAIN));
        descArea.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        descArea.setAlignmentX(0.5f);
        centerPanel.add(descArea);
        centerPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));

        // Interactive Star Rating Bar (Play Store style)
        JPanel starsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        starsPanel.setOpaque(false);
        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            starWidgets[i] = new StarWidget(starIndex);
            starWidgets[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hoverRating = starIndex;
                    updateStarVisuals();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverRating = 0;
                    updateStarVisuals();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedRating = starIndex;
                    updateStarVisuals();
                }
            });
            starsPanel.add(starWidgets[i]);
        }
        starsPanel.setAlignmentX(0.5f);
        centerPanel.add(starsPanel);

        ratingTextLabel = new JLabel("5 Stars - Exceptional Help!", SwingConstants.CENTER);
        ratingTextLabel.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        ratingTextLabel.setForeground(BotanicalTheme.DEEP_ROSE);
        ratingTextLabel.setAlignmentX(0.5f);
        centerPanel.add(ratingTextLabel);

        card.add(centerPanel, BorderLayout.CENTER);

        // Buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        PillButton skipBtn = new PillButton("Skip", BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.MUTED_TEXT);
        skipBtn.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        skipBtn.setPreferredSize(new Dimension(85, 34));
        skipBtn.addActionListener(e -> dispose());
        bottom.add(skipBtn);

        PillButton submitBtn = new PillButton("Submit Rating", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        submitBtn.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        submitBtn.setPreferredSize(new Dimension(135, 34));
        submitBtn.addActionListener(e -> {
            client.submitRating(ticket.getId(), selectedRating);
            dispose();
        });
        bottom.add(submitBtn);

        card.add(bottom, BorderLayout.SOUTH);
        bg.add(card, BorderLayout.CENTER);
        setContentPane(bg);

        updateStarVisuals();

        // Key bindings: ESC to close
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc_close");
        getRootPane().getActionMap().put("esc_close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void updateStarVisuals() {
        int active = hoverRating > 0 ? hoverRating : selectedRating;
        for (int i = 0; i < 5; i++) {
            starWidgets[i].setFilled(i < active);
        }
        switch (active) {
            case 1:
                ratingTextLabel.setText("1 Star - Needs Improvement");
                break;
            case 2:
                ratingTextLabel.setText("2 Stars - Fair");
                break;
            case 3:
                ratingTextLabel.setText("3 Stars - Helpful");
                break;
            case 4:
                ratingTextLabel.setText("4 Stars - Very Helpful!");
                break;
            case 5:
            default:
                ratingTextLabel.setText("5 Stars - Exceptional Help!");
                break;
        }
    }

    /**
     * Vector-drawn interactive Star Component with smooth anti-aliased rendering
     * matching the Botanical/Whimsy theme.
     */
    private static class StarWidget extends JComponent {
        private boolean filled = true;

        public StarWidget(int starIndex) {
            setPreferredSize(new Dimension(36, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(starIndex + " Star" + (starIndex > 1 ? "s" : ""));
        }

        public void setFilled(boolean filled) {
            this.filled = filled;
            repaint();
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
            double outerR = Math.min(w, h) * 0.42;
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
                // Warm golden yellow fill with subtle amber gradient
                g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(255, 215, 0), 0, h, new Color(255, 179, 0)));
                g2.fill(star);
                g2.setColor(new Color(218, 140, 0));
                g2.setStroke(new java.awt.BasicStroke(1.4f));
                g2.draw(star);
            } else {
                // Unfilled / empty star
                g2.setColor(new Color(245, 240, 243));
                g2.fill(star);
                g2.setColor(new Color(205, 185, 195));
                g2.setStroke(new java.awt.BasicStroke(1.2f));
                g2.draw(star);
            }
            g2.dispose();
        }
    }
}
