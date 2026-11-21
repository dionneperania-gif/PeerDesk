package com.mycompany.peerdesk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * Whimsical styled alert / message dialog matching PeerDesk's signature
 * pastel botanical aesthetic (pink, sage green, cream, lavender).
 */
public class WhimsicalDialog extends JDialog {

    private boolean confirmed = false;

    private WhimsicalDialog(Window owner, String title, String message, String badgeText, boolean isConfirm) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setSize(450, 240);
        setLocationRelativeTo(owner);
        setResizable(false);
        setIconImage(createBlossomIcon());

        initContent(title, message, badgeText, isConfirm);
        setupKeyBindings(isConfirm);
    }

    public static void showNotice(Window owner, String title, String message) {
        showNotice(owner, title, message, "NOTE");
    }

    public static void showNotice(Window owner, String title, String message, String badgeText) {
        WhimsicalDialog dialog = new WhimsicalDialog(owner, title, message, badgeText, false);
        dialog.setVisible(true);
    }

    public static boolean showConfirm(Window owner, String title, String message, String confirmBtnText) {
        WhimsicalDialog dialog = new WhimsicalDialog(owner, title, message, "CONFIRM", true);
        dialog.setVisible(true);
        return dialog.confirmed;
    }

    private void initContent(String title, String message, String badgeText, boolean isConfirm) {
        BotanicalBackgroundPanel bg = new BotanicalBackgroundPanel(new BorderLayout());
        bg.setBorder(new EmptyBorder(14, 14, 14, 14));

        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(0, 12), 20);
        card.setFillColor(BotanicalTheme.IVORY_CARD);
        card.setBorderColor(BotanicalTheme.BORDER_ROSE);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Top Header Row
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(BotanicalTheme.fontSerifTitle(15));
        titleLbl.setForeground(BotanicalTheme.FOREST_DEEP);
        header.add(titleLbl, BorderLayout.WEST);

        if (badgeText != null && !badgeText.trim().isEmpty()) {
            JLabel badge = new JLabel(" " + badgeText.trim() + " ");
            badge.setFont(BotanicalTheme.fontSans(10, Font.BOLD));
            badge.setOpaque(true);
            badge.setBackground(BotanicalTheme.BLUSH_PINK);
            badge.setForeground(BotanicalTheme.DEEP_ROSE);
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1),
                    new EmptyBorder(2, 6, 2, 6)
            ));
            header.add(badge, BorderLayout.EAST);
        }
        card.add(header, BorderLayout.NORTH);

        // Center Message with cute rounded background
        EditorialCardPanel msgBox = new EditorialCardPanel(new BorderLayout(), 14);
        msgBox.setFillColor(new Color(255, 248, 250));
        msgBox.setBorderColor(new Color(245, 225, 232));
        msgBox.setBorder(new EmptyBorder(12, 14, 12, 14));

        JTextArea msgArea = new JTextArea(message != null ? message : "");
        msgArea.setWrapStyleWord(true);
        msgArea.setLineWrap(true);
        msgArea.setOpaque(false);
        msgArea.setEditable(false);
        msgArea.setFocusable(false);
        msgArea.setFont(BotanicalTheme.fontSans(11.5f, Font.PLAIN));
        msgArea.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        msgBox.add(msgArea, BorderLayout.CENTER);
        card.add(msgBox, BorderLayout.CENTER);

        // Bottom Action Buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        if (isConfirm) {
            PillButton cancelBtn = new PillButton("Cancel", BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.MUTED_TEXT);
            cancelBtn.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
            cancelBtn.setPreferredSize(new Dimension(85, 32));
            cancelBtn.addActionListener(e -> {
                confirmed = false;
                dispose();
            });
            bottom.add(cancelBtn);

            PillButton confirmBtn = new PillButton("Confirm", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
            confirmBtn.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
            confirmBtn.setPreferredSize(new Dimension(90, 32));
            confirmBtn.addActionListener(e -> {
                confirmed = true;
                dispose();
            });
            bottom.add(confirmBtn);
        } else {
            PillButton okBtn = new PillButton("Got it", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
            okBtn.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
            okBtn.setPreferredSize(new Dimension(85, 32));
            okBtn.addActionListener(e -> dispose());
            bottom.add(okBtn);
        }

        card.add(bottom, BorderLayout.SOUTH);
        bg.add(card, BorderLayout.CENTER);
        setContentPane(bg);
    }

    private void setupKeyBindings(boolean isConfirm) {
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "action_esc");
        getRootPane().getActionMap().put("action_esc", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, "action_enter");
        getRootPane().getActionMap().put("action_enter", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isConfirm) confirmed = true;
                dispose();
            }
        });
    }

    private Image createBlossomIcon() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BotanicalTheme.paintBlossom(g2, 16, 16, 13, BotanicalTheme.BUBBLEGUM_SOFT, BotanicalTheme.PASTEL_YELLOW);
        g2.dispose();
        return img;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
