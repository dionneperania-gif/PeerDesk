package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * Whimsical styled modal dialog for changing a ticket's status.
 * Beautifully customized to match PeerDesk's signature Whimsy & Botanical theme
 * with pastel pink, sage green, warm cream, and lavender accents, custom floral
 * iconography, and interactive whimsical status selection cards.
 */
public class ChangeStatusDialog extends JDialog {

    private String selectedStatus;
    private final List<StatusOptionCard> optionCards = new ArrayList<>();
    private PillButton updateBtn;

    public ChangeStatusDialog(JFrame parent, PeerDeskClient client, int ticketId, String currentStatus) {
        super(parent, "Update Ticket #" + ticketId + " Status", true);
        this.selectedStatus = currentStatus;

        setSize(490, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
        setIconImage(createWhimsicalBlossomIcon());

        initContent(client, ticketId, currentStatus);
        setupKeyBindings(client, ticketId, currentStatus);
    }

    private void initContent(PeerDeskClient client, int ticketId, String currentStatus) {
        // 1. Full-window Botanical Pastel Gradient Background
        BotanicalBackgroundPanel bg = new BotanicalBackgroundPanel(new BorderLayout());
        bg.setBorder(new EmptyBorder(16, 18, 16, 18));

        // 2. Floating Ivory Editorial Card Panel
        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(0, 14), 22);
        card.setFillColor(BotanicalTheme.IVORY_CARD);
        card.setBorderColor(BotanicalTheme.BORDER_ROSE);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        // 3. Whimsical Header Section
        JPanel headerPanel = new JPanel(new BorderLayout(12, 4));
        headerPanel.setOpaque(false);

        // Header Title & Subtitle
        JPanel titleArea = new JPanel(new GridLayout(2, 1, 0, 2));
        titleArea.setOpaque(false);

        JLabel titleLbl = new JLabel("Update Ticket Status");
        titleLbl.setFont(BotanicalTheme.fontSerifTitle(20));
        titleLbl.setForeground(BotanicalTheme.FOREST_DEEP);
        titleArea.add(titleLbl);

        JLabel subtitleLbl = new JLabel("Choose a new status for this help request");
        subtitleLbl.setFont(BotanicalTheme.fontSans(12, Font.PLAIN));
        subtitleLbl.setForeground(BotanicalTheme.MUTED_TEXT);
        titleArea.add(subtitleLbl);

        headerPanel.add(titleArea, BorderLayout.CENTER);

        // Top-Right: Ticket ID Badge Pill
        JLabel ticketPill = new JLabel(" Ticket #" + ticketId + " ");
        ticketPill.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        ticketPill.setOpaque(true);
        ticketPill.setBackground(BotanicalTheme.PASTEL_LAVENDER);
        ticketPill.setForeground(new Color(74, 45, 94));
        ticketPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 180, 220), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        headerPanel.add(ticketPill, BorderLayout.EAST);

        card.add(headerPanel, BorderLayout.NORTH);

        // 4. Center Content: Current Status Banner + Whimsical Status Cards
        JPanel centerContent = new JPanel(new BorderLayout(0, 12));
        centerContent.setOpaque(false);

        // Current status banner
        JPanel currentStatusBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        currentStatusBanner.setOpaque(false);
        currentStatusBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(245, 225, 232), 1),
                new EmptyBorder(5, 8, 5, 8)
        ));

        JLabel curLabel = new JLabel("CURRENT STATUS:");
        curLabel.setFont(BotanicalTheme.fontSans(11, Font.BOLD));
        curLabel.setForeground(BotanicalTheme.MUTED_TEXT);
        currentStatusBanner.add(curLabel);

        JLabel curBadge = createStatusBadge(currentStatus);
        currentStatusBanner.add(curBadge);

        centerContent.add(currentStatusBanner, BorderLayout.NORTH);

        // Interactive Status Selection Cards Panel
        JPanel cardsList = new JPanel(new GridLayout(3, 1, 0, 10));
        cardsList.setOpaque(false);

        StatusOptionCard openCard = new StatusOptionCard(
                "OPEN",
                "OPEN",
                "Needs assistance - open for any peer to claim",
                BotanicalTheme.BLUSH_PINK,
                BotanicalTheme.DEEP_ROSE,
                BotanicalTheme.BUBBLEGUM_PINK
        );

        StatusOptionCard inProgressCard = new StatusOptionCard(
                "IN_PROGRESS",
                "IN PROGRESS",
                "Active - peer is currently assisting with this problem",
                BotanicalTheme.PASTEL_YELLOW,
                BotanicalTheme.PASTEL_AMBER,
                new Color(220, 160, 50)
        );

        StatusOptionCard resolvedCard = new StatusOptionCard(
                "RESOLVED",
                "RESOLVED",
                "Completed - question or bug has been solved",
                BotanicalTheme.MINT_SOFT,
                BotanicalTheme.FOREST_DEEP,
                BotanicalTheme.SAGE_GREEN
        );

        optionCards.add(openCard);
        optionCards.add(inProgressCard);
        optionCards.add(resolvedCard);

        cardsList.add(openCard);
        cardsList.add(inProgressCard);
        cardsList.add(resolvedCard);

        centerContent.add(cardsList, BorderLayout.CENTER);
        card.add(centerContent, BorderLayout.CENTER);

        // 5. Whimsical Bottom Buttons: Cancel & Update Status
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        PillButton cancelBtn = new PillButton("Cancel", BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.MUTED_TEXT);
        cancelBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        cancelBtn.setPreferredSize(new Dimension(100, 38));
        cancelBtn.addActionListener(e -> dispose());

        updateBtn = new PillButton("Update Status", BotanicalTheme.SAGE_GREEN, Color.WHITE);
        updateBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        updateBtn.setPreferredSize(new Dimension(140, 38));
        updateBtn.addActionListener(e -> applyStatusChange(client, ticketId, currentStatus));

        bottomBar.add(cancelBtn);
        bottomBar.add(updateBtn);
        card.add(bottomBar, BorderLayout.SOUTH);

        bg.add(card, BorderLayout.CENTER);
        setContentPane(bg);
    }

    private void applyStatusChange(PeerDeskClient client, int ticketId, String currentStatus) {
        if (selectedStatus != null && !selectedStatus.equals(currentStatus)) {
            client.updateTicketStatus(ticketId, selectedStatus);
        }
        dispose();
    }

    private void setSelectedStatus(String status) {
        this.selectedStatus = status;
        for (StatusOptionCard opt : optionCards) {
            opt.repaint();
        }
        if (updateBtn != null) {
            updateBtn.repaint();
        }
    }

    private JLabel createStatusBadge(String status) {
        JLabel badge = new JLabel("  " + status + "  ");
        badge.setFont(BotanicalTheme.fontSans(11, Font.BOLD));
        badge.setOpaque(true);
        if ("OPEN".equals(status)) {
            badge.setBackground(BotanicalTheme.BLUSH_PINK);
            badge.setForeground(BotanicalTheme.DEEP_ROSE);
        } else if ("IN_PROGRESS".equals(status)) {
            badge.setBackground(BotanicalTheme.PASTEL_YELLOW);
            badge.setForeground(BotanicalTheme.PASTEL_AMBER);
        } else {
            badge.setBackground(BotanicalTheme.MINT_SOFT);
            badge.setForeground(BotanicalTheme.FOREST_DEEP);
        }
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1),
                new EmptyBorder(2, 8, 2, 8)
        ));
        return badge;
    }

    private void setupKeyBindings(PeerDeskClient client, int ticketId, String currentStatus) {
        // Escape closes the dialog
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "action_escape");
        getRootPane().getActionMap().put("action_escape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // Enter submits the update
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, "action_enter");
        getRootPane().getActionMap().put("action_enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyStatusChange(client, ticketId, currentStatus);
            }
        });
    }

    private Image createWhimsicalBlossomIcon() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BotanicalTheme.paintBlossom(g2, 16, 16, 13, BotanicalTheme.BUBBLEGUM_SOFT, BotanicalTheme.PASTEL_YELLOW);
        g2.dispose();
        return img;
    }

    /**
     * Interactive Whimsical Status Option Card component.
     * Features soft pastel coloring, hover glow, and a custom painted radio/indicator.
     */
    private class StatusOptionCard extends JPanel {
        private final String statusValue;
        private final String badgeText;
        private final String description;
        private final Color badgeBg;
        private final Color badgeFg;
        private final Color activeAccent;
        private boolean isHovered = false;

        public StatusOptionCard(String statusValue, String badgeText, String description,
                                Color badgeBg, Color badgeFg, Color activeAccent) {
            this.statusValue = statusValue;
            this.badgeText = badgeText;
            this.description = description;
            this.badgeBg = badgeBg;
            this.badgeFg = badgeFg;
            this.activeAccent = activeAccent;

            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setLayout(new BorderLayout(14, 0));
            setBorder(new EmptyBorder(10, 16, 10, 16));

            // Left side content: Status pill + helpful explanation
            JPanel textCol = new JPanel(new GridLayout(2, 1, 0, 3));
            textCol.setOpaque(false);

            JPanel pillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pillRow.setOpaque(false);
            JLabel pill = new JLabel("  " + badgeText + "  ");
            pill.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
            pill.setOpaque(true);
            pill.setBackground(badgeBg);
            pill.setForeground(badgeFg);
            pill.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(activeAccent, 1),
                    new EmptyBorder(3, 8, 3, 8)
            ));
            pillRow.add(pill);
            textCol.add(pillRow);

            JLabel descLbl = new JLabel(description);
            descLbl.setFont(BotanicalTheme.fontSans(11, Font.PLAIN));
            descLbl.setForeground(BotanicalTheme.MUTED_TEXT);
            textCol.add(descLbl);

            add(textCol, BorderLayout.CENTER);

            // Mouse interaction
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    setSelectedStatus(statusValue);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isSelected = statusValue.equals(selectedStatus);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            int radius = 16;

            // 1. Fill Background
            if (isSelected) {
                // Soft tinted pastel background matching the status color
                g2.setColor(new Color(badgeBg.getRed(), badgeBg.getGreen(), badgeBg.getBlue(), 80));
            } else if (isHovered) {
                g2.setColor(new Color(255, 255, 255, 240));
            } else {
                g2.setColor(new Color(255, 255, 255, 180));
            }
            g2.fillRoundRect(0, 0, w, h, radius, radius);

            // 2. Stroke / Border
            if (isSelected) {
                g2.setColor(activeAccent);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, radius, radius);
            } else if (isHovered) {
                g2.setColor(BotanicalTheme.BORDER_ROSE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w, h, radius, radius);
            } else {
                g2.setColor(new Color(240, 228, 234));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w, h, radius, radius);
            }

            // 3. Cute Whimsical Radio / Selection Pill on Right Side
            int circleSize = 20;
            int cx = w - 28;
            int cy = h / 2 - circleSize / 2;

            if (isSelected) {
                // Filled active ring with inner flower/dot
                g2.setColor(activeAccent);
                g2.fillOval(cx, cy, circleSize, circleSize);
                g2.setColor(Color.WHITE);
                g2.fillOval(cx + 6, cy + 6, 8, 8);
            } else {
                // Subtle soft outline
                g2.setColor(new Color(215, 200, 208));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx, cy, circleSize, circleSize);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
