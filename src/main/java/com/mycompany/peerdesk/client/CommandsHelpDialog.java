package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Modal dialog displaying available chat commands with styled command badges.
 */
public class CommandsHelpDialog extends JDialog {

    public CommandsHelpDialog(JFrame parent) {
        super(parent, "PeerDesk Commands Guide", true);
        setSize(460, 340);
        setLocationRelativeTo(parent);
        initContent();
        setVisible(true);
    }

    private void initContent() {
        BotanicalBackgroundPanel bg = new BotanicalBackgroundPanel(new BorderLayout());
        bg.setBorder(new EmptyBorder(14, 14, 14, 14));

        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(0, 12), 18);
        card.setFillColor(BotanicalTheme.IVORY_CARD);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Header Title
        JLabel title = new JLabel("Available Chat Commands");
        title.setFont(BotanicalTheme.FONT_TITLE_SECTION);
        title.setForeground(BotanicalTheme.FOREST_DEEP);
        card.add(title, BorderLayout.NORTH);

        // Center Content - Commands List
        JPanel listPanel = new JPanel(new GridLayout(3, 1, 0, 8));
        listPanel.setOpaque(false);

        listPanel.add(createCommandRow("/name <newname>", "Change your active nickname across the room",
                BotanicalTheme.MINT_SOFT, BotanicalTheme.FOREST_DEEP));
        listPanel.add(createCommandRow("/exit", "Safely disconnect from the server and close session",
                BotanicalTheme.WARM_PEACH, BotanicalTheme.DEEP_ROSE));
        listPanel.add(createCommandRow("/help", "Display this commands reference anytime in chat",
                BotanicalTheme.PASTEL_LAVENDER, new Color(74, 45, 94)));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(listPanel, BorderLayout.CENTER);

        JLabel tipLbl = new JLabel("Tip: You can type these commands directly into the bottom message composer.");
        tipLbl.setFont(BotanicalTheme.fontSans(11, Font.ITALIC));
        tipLbl.setForeground(BotanicalTheme.MUTED_TEXT);
        centerPanel.add(tipLbl, BorderLayout.SOUTH);

        card.add(centerPanel, BorderLayout.CENTER);

        // Bottom Close Pill
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomBar.setOpaque(false);
        PillButton closeBtn = new PillButton("Got it", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        closeBtn.addActionListener(e -> dispose());
        bottomBar.add(closeBtn);
        card.add(bottomBar, BorderLayout.SOUTH);

        bg.add(card, BorderLayout.CENTER);
        setContentPane(bg);
    }

    private JPanel createCommandRow(String cmd, String desc, Color badgeBg, Color badgeFg) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel badge = new JLabel(" " + cmd + " ");
        badge.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        badge.setOpaque(true);
        badge.setBackground(badgeBg);
        badge.setForeground(badgeFg);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(BotanicalTheme.fontSans(12, Font.PLAIN));
        descLbl.setForeground(BotanicalTheme.CHARCOAL_TEXT);

        row.add(badge, BorderLayout.WEST);
        row.add(descLbl, BorderLayout.CENTER);
        return row;
    }
}
